package db

import com.beust.klaxon.internal.firstNotNullResult
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

        if(parsedObjects.containsKey(key to pk)){
            val obj = parsedObjects[key to pk]

            if(obj != null){
                return obj as T
            }
        }

        var read : T? = retrieveObjectIfKeyExists(key, clazz) {
            primaryBackend.loadByPK(key, pk, clazz)
        }

        if(read == null){
            if(ignoreInit)
                throw IllegalAccessException("Couldn´t find key $key")
            else {
                read = init.invoke()
                (listOf(primaryBackend) + backends).forEach {
                    it.insert(key, clazz, read) //Be careful that this will not be used in combination with ObservableArrayList
                }
            }
        }

        addBackendListener(read, key, clazz)

        return read

    }

    fun <T : Any, V : Observable> retrieveObjectIfKeyExists(key: String, clazz: KClass<V>, f: () -> T) : T?{
        return (listOf(primaryBackend) + backends).firstNotNullResult { backend ->
            if(backend.keyExists(key)){
                f()
            }else {
                backend.createSchema(clazz)
                null
            }
        }
    }

    //Is for detached objects
    fun <T : Observable> addBackendListener(observable: T, key: String, clazz: KClass<T>){
        observable.addListener (DetachedBackendListener(observable, key, clazz))

        if(parsed.containsKey(key)){
            (parsed[key]!! as ObservableArrayList<T>).add(observable)
        }else{
            parsed[key] = observableListOf(observable) //TODO Add to list
        }
        parsedObjects[key to observable.key()] = observable
    }

    class DetachedBackendListener<T : Observable>(val observable: T, val key: String, val clazz: KClass<T>) : ChangeListener<Any?> {
        override fun invoke(prop: KProperty<*>, old: Any?, new: Any?, levels: LevelInformation) {
            for (backend in listOf(primaryBackend) + backends){
//                levels.list
//                if(prop is KProperty1<*, *>){
//                    (prop as KProperty1<Any?, Any?>).getDelegate(observable) is DetachedReadWriteProperty<*>
//                }

                backend.update(key, clazz, observable, prop)
            }
        }
    }

    inline fun <reified T : Observable> getDetachedList(key: String) : ObservableArrayList<T> {

        if(parsed.containsKey(key)){
            return parsed[key]!! as ObservableArrayList<T>
        }

        val lread : List<T>? = retrieveObjectIfKeyExists(key, T::class) {
            primaryBackend.loadAll(key, T::class)
        } ?: listOf()

        val list = observableListOf(*lread!!.toTypedArray())

        list.addListener { args, levels -> //TODO Add Level stuff to Backend interface for incremental saves
            for (backend in listOf(primaryBackend) + backends){
                performListEventOnBackend(backend, key, T::class, args)
            }

            //Object is self aware, therefore should be responsible for updates, since that is not relevant for the list
            if(args.elementChangeType == ElementChangeType.Add || args.elementChangeType == ElementChangeType.Set){
                args.elements.forEach { obj ->
                    obj.addListener { prop: KProperty<*>, old: T, new: T, levels: LevelInformation ->
                        (listOf(primaryBackend) + backends).forEach { backend ->
                            backend.update(key, T::class, obj, prop)
                        }
                    }
                }

            }
        }

        parsed[key] = list
        list.collection.forEach {
            parsedObjects[key to it.key()] = it
        }

        return list

    }

    fun <T : Observable> performListEventOnBackend(backend: Backend, key: String, clazz: KClass<T>, args: ListChangeArgs<T>){
        args.elements.forEachIndexed { i, obj ->
            when(args.elementChangeType){
                ElementChangeType.Add -> {
                    backend.insert(key, clazz, obj)
                }
                ElementChangeType.Set -> {
                    if(args is SetListChangeArgs<T>){
                        backend.delete(key, clazz, args.replacedElements[i])
                        backend.insert(key, clazz, obj)
                    }else{
                        throw IllegalStateException("Args with Type Set must be instance of SetListChangeArgs!!")
                    }
                }
                ElementChangeType.Update -> {
                    if(args is UpdateListChangeArgs<T>) {
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