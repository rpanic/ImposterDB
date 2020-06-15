package db

import collections.*
import connection.ObjectCache
import example.ReflectionUtils
import main.kotlin.connection.BackendConnector
import observable.*
import ruleExtraction1.MappingStep
import ruleExtraction1.MappingType
import ruleExtraction1.Step
import virtual.VirtualSet
import virtual.VirtualSetAccessor
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

    //Is currently used by detached() Delegated
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

        addBackendUpdateListener(read, key, clazz)

        read.setDbReference(this)

        return read

    }

    //Is for detached objects
    fun <T : Observable> addBackendUpdateListener(observable: T, key: String, clazz: KClass<T>){
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
                }else it is SetLevel<*> //Prevent Events from VirtualSets to trigger update
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
            addBackendUpdateListener(obj, key, clazz)
            ensureAllDetachedAreInserted(obj)
        }
        
        val accessor = object : VirtualSetAccessor<T>{
            override fun load(steps: List<Step<T, *>>): Set<T> {
                val set = backendConnector.loadWithRules(key, steps, clazz)
                set.forEach(initObservable)
                return set
            }
    
            override fun count(steps: List<Step<T, *>>): Int {
                return backendConnector
                        .loadTransformed(key, listOf(MappingStep<T, Int>(MappingType.COUNT)) + steps, clazz, Int::class).firstOrNull() ?: 0
            }
    
            override fun performEvent(instance: VirtualSet<T>, listChangeArgs: SetChangeArgs<T>, levelInformation: LevelInformation) {
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
            }
    
        }
        
        val set = VirtualSet(accessor, listOf(), clazz)

        return set
        //TODO Hook Set to observable elements and relay Update events?
    }
    
    /**
     * If the Detached object got set when the parent Observable was not added to a DB VirtualSet yet, the detached object will not be in the database yet
     */
    fun <T : Observable> ensureAllDetachedAreInserted(obj: T){
        ReflectionUtils.findPropertiesWithType(obj::class, listOf(DetachedObjectReadWriteProperty::class))
                .map { it as KProperty1<T, Any> }
                .forEach {
                    val delegate = it.getDelegate(obj) as? DetachedObjectReadWriteProperty<*>
                    if(delegate != null && !delegate.dbInserted){
                        delegate.initValue()
                    }
                }
    }

    fun <T : Observable> performListAddEventsOnBackend(key: String, clazz: KClass<T>, args: ChangeArgs<T>){
        args.elements.forEachIndexed { i, obj ->
            when(args.elementChangeType){
                ElementChangeType.Add -> {
                    backendConnector.insert(key, obj, clazz) //Check for duplicates, bc used references could be added
                }
                ElementChangeType.Set -> {
                    if(args is SetSetChangeArgs<T>){
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
                    if(args is SetSetChangeArgs<T>){
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
                    if(args is UpdateSetChangeArgs<T>) {
                        backendConnector.update(key, obj, clazz, args.prop, levels)
                    }
                }
            }
        }
    }

    fun <T : Observable> performListEventOnBackend(key: String, clazz: KClass<T>, args: ChangeArgs<T>, levels: LevelInformation){
        performListAddEventsOnBackend(key, clazz, args)
        performListDeleteEventsOnBackend(key, clazz, args)
        performListUpdateEventsOnBackend(key, clazz, args, levels)
    }

    operator fun plusAssign(b: Backend) {
        addBackend(b)
    }

}