package db

import observable.*
import java.lang.Exception
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

object DB{

    val parsed = mutableMapOf<String, ObservableArrayList<*>>()

    val parsedObjects = mutableMapOf<Pair<String, Any>, Observable>() //<Key, PK> -> Observable

    val backends = mutableListOf<Backend>()

    var txActive = false

    val txQueue = mutableListOf<RevertableAction>()

    fun <T : Observable, K : Any> getCachedObject(key: String, pk: K) : T?{
        return parsedObjects[key to pk] as? T
    }

    fun <T : Observable> getCachedList(key: String) : ObservableArrayList<T>?{
        return parsed[key] as? ObservableArrayList<T>
    }

    fun addBackend(backend: Backend){
        backends.add(backend)
    }

    fun setBackend(backend: Backend){
        if (::primaryBackend.isInitialized){
            throw IllegalAccessException("Primary Backend already set. Use addBackend to add additional backends")
        }else{
            primaryBackend = backend
        }
    }

    lateinit var primaryBackend: Backend  //TODO make private

    fun tx(f: () -> Unit){

        txActive = true

        try {

            f()

            for (i in txQueue.indices) {

                try {

                    txActive = false

                    txQueue[i].action()

                } catch (e: Exception) {

                    e.printStackTrace()

                    println("Reverting Transaction...")

                    for (j in (i) downTo 0) {
                        try{
                            txQueue[j].revert()
                        }catch(e: Exception){
                            println("Error reverting Transaction step $j:")
                            e.printStackTrace()
                        }
                    }

                    break
                }
            }

        }catch(e: Exception){
            e.printStackTrace()
        }finally {
            txActive = false
            txQueue.clear()
        }
    }

    inline fun <reified T : Observable, K : Any> getDetached(key: String, pk: K, ignoreInit: Boolean = false, noinline init : () -> T) : T{
        return getDetached(key, pk, ignoreInit, T::class, init)
    }

    fun <T : Observable, K: Any> getDetached(key: String, pk: K, ignoreInit: Boolean = false, clazz: KClass<T>, init : () -> T) : T{

        println("hallo")
        if(parsedObjects.containsKey(key to pk)){
            val obj = parsedObjects[key to pk]//.find { it.key<K>() == pk }
            println(obj!!::class.java.simpleName)
            if(obj != null){
                return obj as T
            }
        }

        var read : T? = if(primaryBackend.keyExists(key)){
            primaryBackend.loadByPK(key, pk, clazz)
        }else null

        if(read == null){
            if(ignoreInit)
                throw IllegalAccessException("Couldn´t find key $key")
            else
               read = init.invoke()
        }

        addBackendListener(read, key, clazz)

        return read

    }

    fun <T : Observable> addBackendListener(observable: T, key: String, clazz: KClass<T>){
        observable.addListener (DetachedBackendListener(observable, key, clazz))

        parsed[key] = observableListOf(observable) //TODO Add to list
        parsedObjects[key to observable.key()] = observable
    }

    class DetachedBackendListener<T : Observable>(val observable: T, val key: String, val clazz: KClass<T>) : ChangeListener<Any?> {
        override fun invoke(prop: KProperty<*>, old: Any?, new: Any?, levels: LevelInformation) {
            for (backend in listOf(primaryBackend) + backends){
                backend.update(key, clazz, observable, prop)
//                levels.list.any {
//                    true
//                }
            }
        }
    }

    inline fun <reified T : Observable> getDetachedList(key: String) : ObservableArrayList<T> {

        if(parsed.containsKey(key)){
            return parsed[key]!! as ObservableArrayList<T>
        }

        val lread : List<T>? = if(primaryBackend.keyExists(key)){
            primaryBackend.loadAll(key, T::class)
        }else{
            listOf()
        }

        val list = observableListOf(*lread!!.toTypedArray())

        list.addListener { args,  levels -> //TODO Add Level stuff to Backend interface for incremental saves
            for (backend in listOf(primaryBackend) + backends){
                performOnBackend(backend, key, T::class, args)
            }
        }

        parsed[key] = list
        list.collection.forEach {
            parsedObjects[key to it.key()] = it
        }

        return list

    }

    fun <T : Observable> performOnBackend(backend: Backend, key: String, clazz: KClass<T>, args: ListChangeArgs<T>){
        args.elements.forEachIndexed { i, obj ->
            when(args.elementChangeType){
                ElementChangeType.Add -> {
                    backend.insert(key, clazz, obj)
                }
                ElementChangeType.Set -> {
                    if(args is SetListChangeArgs<T>){
                        backend.delete(key, clazz, args.replacedElements[i])
                        backend.insert(key, clazz, obj)
                    }
                }
                ElementChangeType.Update -> {
                    if(args is UpdateListChangeArgs<T>) {
                        //TODO Check if a property of the object is detached, and then ignore the event
                        backend.update(key, clazz, obj, args.prop)
                    }
                }
                ElementChangeType.Remove -> {
                    backend.delete(key, clazz, obj.key<Any>())
                }
            }
        }

    }

//    inline fun <reified T : Observable> getList(key: String) : ObservableArrayList<T> {
//
//        if(parsed.containsKey(key)){
//            return parsed[key]!! as ObservableArrayList<T>
//        }
//
//        val lread : List<T>? = if(primaryBackend.keyExists(key)){
//            primaryBackend.loadList(key, T::class)
//        }else{
//            listOf()
//        }
//
//        val list = observableListOf(*lread!!.toTypedArray())
//
//        list.addListener { _,  _ -> //TODO Add Level stuff to Backend interface for incremental saves
//            for (backend in listOf(primaryBackend) + backends){
//                backend.saveList(key, T::class, list.collection)
//            }
//        }
//
//        parsed.put(key, list)
//        parsedObjects.put(key, list.collection.toList())
//
//        return list
//
//    }
//
//    //TODO Rework to getSingleton
//    inline fun <reified T : Observable> getObject(key: String, init : () -> T) : T{
//
//        if(parsedObjects.containsKey(key)){
//            return parsedObjects[key]!! as T
//        }
//
//        val obj = if(primaryBackend.keyExists(key)){
//            primaryBackend.load(key, T::class)
//        }else{
//            init.invoke()
//        }
//
//        GenericChangeObserver(obj!!) {
//            for (backend in listOf(primaryBackend) + backends) {
//                backend.save(key, T::class, obj)
//            }
//        }.all(LevelInformation::list /* unused, so doesn´t whats in there*/, null, null, LevelInformation(emptyList()))
//
//        parsedObjects.put(key, listOf(obj))
//
//        return obj
//
//    }

}