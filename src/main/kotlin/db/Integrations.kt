package db

import observable.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor

inline fun <reified T : Observable> Observable.detached(key: String) : DetachedReadWriteProperty<T>{
    return detachedInternal(key, this, T::class)
}

fun <T : Observable> detachedInternal(key: String, obj: Observable, clazz: KClass<T>) : DetachedReadWriteProperty<T> {
    val property = DetachedReadWriteProperty<T>(obj, key, clazz){
        DB.getDetached(key, it.getPk(), true, clazz){
            clazz.primaryConstructor!!.call()
        }
        //TODO First try primaryBackend and then all others
    }
    property.initArg(property)
    return property
}

class DetachedReadWriteProperty<T : Observable>(val observable : Observable, val key: String, val clazz: KClass<T>, val initializer: (DetachedReadWriteProperty<T>) -> T) : ReadWriteProperty<Any?, T> {

    private var initialized = false
    private var value: T? = null //TODO Nulls dont work, they throw a npe even if type is nullable

    protected /*open*/ fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean = true

    protected open fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?): Unit {
        if(newValue!!.classListeners.none { it is DB.DetachedBackendListener<*> }){
            val backends = (listOf(DB.primaryBackend) + DB.backends) //TODO Check if that gets executed correctly
            backends.forEach {
                it.insert(key, clazz, newValue) //Be careful that this will not be used in combination with ObservableArrayList
            }
            DB.addBackendListener(newValue, key, newValue::class as KClass<T>)
        }
        //TODO Safely delete the new and old detached objects
        // + When will unused objects be deleted? When theres no reference any more or when it gets removed from the list?
        // + I guess the first options would be more compliant with the "consistent state" paradigm
        newValue.classListeners.map { it as ChangeListener<Any?> }.forEach { it(newValue::uuid, null, newValue.uuid, LevelInformation(listOf(ObservableLevel(newValue, property)))) }
//        TODO notify this@Observable about changes in the object (hookToObservable)
        //TODO Edit: Done?
        observable.changed(property, oldValue, newValue, LevelInformation(listOf(ObservableLevel(observable, property))))
    }

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
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

    private lateinit var a: DetachedReadWriteProperty<T>

    fun initArg(a: DetachedReadWriteProperty<T>){
        this.a = a
    }

    protected fun getValue() : T{
        if(!initialized){
            value = initializer(a)
            println("Should not happen - LazyObservableProperty :: 41")
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