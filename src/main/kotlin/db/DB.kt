package db

import aNewCollections.VirtualSet
import connection.ObjectCache
import main.kotlin.connection.BackendConnector
import observable.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible

class DB{

    val cache = ObjectCache()

    val backendConnector = BackendConnector(cache, this)

    var txActive = false

    val txQueue = mutableListOf<RevertableAction>()

    fun addBackend(backend: Backend){
        if(backend is DBBackend){
            backend.setDbReference(this)
        }
        backendConnector.addBackend(backend)
    }

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

        backendConnector.initIfNotYet(key, clazz)

        var read = backendConnector.loadByPK(key, pk, clazz)

        if(read == null){
            if(ignoreInit)
                throw IllegalAccessException("Couldn´t find key $key")
            else {
                read = init.invoke()
                backendConnector.insert(key, read, clazz) //Be careful that this will not be used in combination with ObservableArrayList
            }
        }

        addBackendListener(read, key, clazz)

        read.setDbReference(this)

        return read

    }

    //Is for detached objects
    fun <T : Observable> addBackendListener(observable: T, key: String, clazz: KClass<T>){
        observable.addListener (DetachedBackendListener(this, observable, key, clazz))
    }

    class DetachedBackendListener<T : Observable>(val db: DB, val observable: T, val key: String, val clazz: KClass<T>) : ChangeListener<Any?> {
        override fun invoke(prop: KProperty<*>, old: Any?, new: Any?, levels: LevelInformation) {

            //Prevent events from detached Objects from triggering updates on parent
            val pathClear = levels.list.none {
                if(it is ObservableLevel){
                    if(it.prop is KProperty1<*, *>){
                        val v = (it.prop as KProperty1<Any?, Any?>).apply { isAccessible = true }.getDelegate(it.obj)
                        v is DetachedObjectReadWriteProperty<*>
                    }else{
                        false //Is only the case for observable() Properties
                    }
                }else if(it is ObservableListLevel) {
                    //TODO Exclude Detached Lists
                    false
                } else {
                    false
                }
            }
            if(pathClear){
                db.backendConnector.update(key, observable, clazz, prop, levels)
            }

        }
    }

    inline fun <reified T : Observable> getSet(key: String) : VirtualSet<T> {
        return getSet(key, T::class)
    }

    fun <T : Observable> getSet(key: String, clazz: KClass<T>) : VirtualSet<T>{
        val initObservable = { obj : T ->
            obj.setDbReference(this)
            addBackendListener(obj, key, clazz)
        }
        val set = VirtualSet({
            val set = backendConnector.loadWithRules(key, it, clazz)
            set.forEach(initObservable)
            set
        },
        { instance, listChangeArgs, levelInformation ->
            println("Parent set got called")

            when(listChangeArgs.elementChangeType){

                //TODO Maybe performAddEventsOnBackend?
                ElementChangeType.Add -> {
                    listChangeArgs.elements.forEach {
                        backendConnector.insert(key, it, clazz)
                        initObservable(it)
                        instance.loadedState?.add(it)
                    }
                }
                ElementChangeType.Remove -> {
                    listChangeArgs.elements.forEach {
                        backendConnector.delete(key, it, clazz)
                        instance.loadedState?.remove(it)
                    }
                }

            }
        },
        listOf(), clazz)

        return set
        //TODO Hook Set to observable elements and relay Update events?
    }

    //Only for retrieving of "actual" lists, so for Tuples of T
    //TODO Integrate this into view() Process
    //Udpate: This logic should be already included in the 3 lines above, so this can be thrown away
//    inline fun <reified T : Observable> getDetachedList(key: String) : ObservableArrayList<T> {

//        val list = backendConnector.loadList(key, T::class) ?: throw java.lang.IllegalStateException("Collection with key $key could not be found in backends")


//        if(list.listeners.size == 0){
//
//            list.addListener { args, levels -> //TODO Add Level stuff to Backend interface for incremental saves
//                performListEventOnBackend(key, T::class, args)
//
//                //Object is self aware, therefore should be responsible for updates, since that is not relevant for the list
//                if(args.elementChangeType == ElementChangeType.Add || args.elementChangeType == ElementChangeType.Set){
//                    args.elements.forEach { obj ->
//                        obj.addListener { prop: KProperty<*>, old: T, new: T, levels: LevelInformation ->
//                            backendConnector.update(key, obj, T::class, prop)
//                        }
//                    }
//
//                }
//            }
//
//        }
//
//        list.setDbReference(this)
//
//        return list

//    }

    fun <T : Observable> performListAddEventsOnBackend(key: String, clazz: KClass<T>, args: ChangeArgs<T>){
        args.elements.forEachIndexed { i, obj ->
            when(args.elementChangeType){
                ElementChangeType.Add -> {
                    backendConnector.insert(key, obj, clazz) //Check for duplicates, bc used references could be added
                }
                ElementChangeType.Set -> {
                    if(args is SetListChangeArgs<T>){
                        backendConnector.insert(key, obj, clazz)
                    }else{
                        throw IllegalStateException("Args with Type Set must be instance of SetListChangeArgs!!")
                    }
                }
            }
        }
    }

    fun <T : Observable> performListDeleteEventsOnBackend(key: String, clazz: KClass<T>, args: ChangeArgs<T>){
        args.elements.forEachIndexed { i, obj ->
            when(args.elementChangeType){
                ElementChangeType.Set -> {
                    if(args is SetListChangeArgs<T>){
                        backendConnector.delete(key, args.replacedElements[i], clazz)
                    }
                }
                ElementChangeType.Remove -> {
                    backendConnector.delete(key, obj.keyValue<T, Any>(), clazz)
                }
            }
        }
    }

    fun <T : Observable> performListUpdateEventsOnBackend(key: String, clazz: KClass<T>, args: ChangeArgs<T>, levels: LevelInformation){
        args.elements.forEachIndexed { i, obj ->
            when(args.elementChangeType){
                ElementChangeType.Update -> {
                    if(args is UpdateListChangeArgs<T>) {
                        backendConnector.update(key, obj, clazz, args.prop, levels)
                    }
                    if(args is UpdateSetChangeArgs<T>) {
                        backendConnector.update(key, obj, clazz, args.prop, levels)
                    }
                }
            }
        }
    }

    fun <T : Observable> performListEventOnBackend(key: String, clazz: KClass<T>, args: ListChangeArgs<T>, levels: LevelInformation){
        performListAddEventsOnBackend(key, clazz, args)
        performListDeleteEventsOnBackend(key, clazz, args)
        performListUpdateEventsOnBackend(key, clazz, args, levels)
    }

    operator fun plusAssign(b: Backend) {
        addBackend(b)
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