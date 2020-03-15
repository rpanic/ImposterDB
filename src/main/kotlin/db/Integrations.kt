package db

import observable.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor

inline fun <reified T : Observable> Observable.detached(key: String) : ReadWriteProperty<Any?, T>{
    return detachedInternal(key, this, T::class)
}

fun <T : Observable> detachedInternal(key: String, obj: Observable, clazz: KClass<T>) : ReadWriteProperty<Any?, T> {
    val property = DetachedReadWriteProperty<T>(obj, key, clazz){
        DB.getDetached(key, ""/*it.getPk()*/, true, clazz){
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
            DB.addBackendListener(newValue, key, newValue::class as KClass<T>)
        }
        newValue.classListeners.map { it as ChangeListener<Any?> }.forEach { it(newValue::uuid, null, newValue.uuid, LevelInformation(listOf(ObservableLevel(newValue)))) }
//        TODO notify this@Observable about changes in the object (hookToObservable)
        observable.changed(property, oldValue, newValue, LevelInformation(listOf(ObservableLevel(observable))))
    }

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(!initialized){
            value = initializer(a)
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

//    var pk: V? = null
//
//    fun setPk(v: V){
//
//    }

    fun <V> getPk() : V{
        return getValue().key()
    }
}

//open class DetachedReadWriteProperty<T : Observable>(initializer: (DetachedReadWriteProperty<T>) -> T) : LazyObservableProperty<T>(initializer){
//
////    fun init() {
////        initArg(this)
////    }
//
//    fun <V> getPk() : V{
//        return getValue().key()
//    }
//
//    override fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?) {
//        //TODO notify this@Observable about changes in the object (hookToObservable)
////        obj.changed(property, oldValue, newValue, LevelInformation(listOf(ObservableLevel(obj))))
//    }
//
//}