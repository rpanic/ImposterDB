package db

import example.debug
import observable.ChangeListener
import observable.LevelInformation
import observable.Observable
import observable.ObservableLevel
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : Observable> Observable.detached(key: String) : DetachedObjectReadWriteProperty<T>{
    return detachedInternal(this, key, T::class)
}

fun <T : Observable> detachedInternal(obj: Observable, key: String, clazz: KClass<T>) : DetachedObjectReadWriteProperty<T> {
    val property = DetachedObjectReadWriteProperty<T>(obj, key, clazz){
        obj.getDB().getDetached(key, it.getPk(), true, clazz){
            throw IllegalAccessException("The detached Property has to be initialized before being accessed")
//            clazz.primaryConstructor!!.call()
        }
        //TODO First try primaryBackend and then all others
    }
    property.initArg(property) //TODO Remove this reference, this could be used
    return property
}

class DetachedObjectReadWriteProperty<T : Observable>(val observable : Observable, val key: String, val clazz: KClass<T>, val initializer: (DetachedObjectReadWriteProperty<T>) -> T) : ReadWriteProperty<Any?, T> {

    private var initialized = false
    private var value: T? = null //TODO Nulls dont work, they throw a npe even if type is nullable
    
    internal var dbInserted = false //Gives an indication whether the detached object already got inserted into DB. Depending on if the parent DB Reference got set before the detached object was set

    /*open*/ fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean = true

    private fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?): Unit {
        dbInserted = if(newValue!!.classListeners.none { it is DB.DetachedBackendListener<*> }){
            if(observable.db != null) {
                initValue()
                true
            }else{
                false //dbInserted Stays false
            }
        }else{
            true
        }
        //TODO Safely delete the new and old detached objects
        // + When will unused objects be deleted? When theres no reference any more or when it gets removed from the list?
        // + I guess the first options would be more compliant with the "consistent state" paradigm
        //TODO What does this line?
        newValue.classListeners.map { it as ChangeListener<Any?> }.forEach { it(newValue::uuid, null, newValue.uuid, LevelInformation(listOf(ObservableLevel(newValue, null, newValue.uuid, newValue::uuid)))) }
//        TODO notify this@Observable about changes in the object (hookToObservable)
        //TODO Edit: Done?
        observable.changed(property, oldValue, newValue, LevelInformation(listOf(ObservableLevel(observable, oldValue, newValue, property))))
    }

    fun initValue(){
        if(value != null) {
            val finalizedValue = value!!
            observable.getDB().backendConnector.insert(key, finalizedValue, clazz) //Be careful that this will not be used in combination with ObservableArrayList
    
            observable.getDB().addBackendUpdateListener(finalizedValue, key, finalizedValue::class as KClass<T>)
        }
    }
    
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(!initialized){
            value = initializer(a)
            observable.hookToObservable(value, property)
        }
        return value!!
    }

    public override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val oldValue = this.value
        this.value = value
        afterChange(property, oldValue, value)
        initialized = true
    }

    private lateinit var a: DetachedObjectReadWriteProperty<T>

    fun initArg(a: DetachedObjectReadWriteProperty<T>){
        this.a = a
    }

    protected fun getValue() : T{
        if(!initialized){
            value = initializer(a)
            debug("Should not happen - DetachedObject :: 41")
        }
        return value!!
    }

    fun isInitialized() = initialized

    private var pkInternal: Any? = null

    fun <V> setPk(v: V){
        pkInternal = v
    }

    fun <V> getPk() : V{
        return pkInternal!! as V
    }

    fun <V> getPkOrNull() : V?{
        return pkInternal as? V
    }
}