package db

import lazyCollections.LazyObservableArrayList
import observable.LazyObservableProperty
import observable.Observable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : Observable> Observable.detachedList(key: String) : ReadWriteProperty<Any?, T> {
    return detachedList(this, key, T::class)
}

fun <T : Observable> detachedList(key: String, clazz: KClass<T>) : ReadWriteProperty<Any?, T> {
    val prop = object : LazyObservableProperty<LazyObservableArrayList<T>>({

    })
    lazy {  }
//    val list = LazyObservableArrayList()
}

typealias UninitializedLazyInitializer<T> = () -> T

class UninitializedLazyReadWriteProperty<T>() : ReadWriteProperty<Any?, T> {

    private var initializer: UninitializedLazyInitializer<T>? = null

    private var initialized = false
    private var obj: T? = null

    fun setInitializer(initializer: UninitializedLazyInitializer<T>){
        this.initializer = initializer
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(!initialized){
            obj = initializer!!()
            initializer = null
        }
        return obj!!
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

//class DetachedListReadWriteProperty<T : Observable>(val observable : Observable, val key: String, val clazz: KClass<T>, val initializer: (DetachedListReadWriteProperty<T>) -> T) : ReadWriteProperty<Any?, LazyObservableArrayList<T>> {
//
//    private var initialized = false
//    private var value: T? = null //TODO Nulls dont work, they throw a npe even if type is nullable
//
//    /*open*/ fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean = true
//
//    private fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?): Unit {
//        if(newValue!!.classListeners.none { it is DB.DetachedBackendListener<*> }){
//            DB.backendConnector.forEachBackend { //TODO Check if that gets executed correctly
//                it.insert(key, clazz, newValue) //Be careful that this will not be used in combination with ObservableArrayList
//            }
//            DB.addBackendListener(newValue, key, newValue::class as KClass<T>)
//        }
//        //TODO Safely delete the new and old detached objects
//        // + When will unused objects be deleted? When theres no reference any more or when it gets removed from the list?
//        // + I guess the first options would be more compliant with the "consistent state" paradigm
//        newValue.classListeners.map { it as ChangeListener<Any?> }.forEach { it(newValue::uuid, null, newValue.uuid, observable.LevelInformation(listOf(observable.ObservableLevel(newValue, property)))) }
////        TODO notify this@Observable about changes in the object (hookToObservable)
//        //TODO Edit: Done?
//        observable.changed(property, oldValue, newValue, observable.LevelInformation(listOf(observable.ObservableLevel(observable, property))))
//    }
//
//    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
//        if(!initialized){
//            value = initializer(a)
//            observable.hookToObservable(value, property)
//        }
//        return value!!
//    }
//
//    public override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
//        val oldValue = this.value
//        this.value = value
//        afterChange(property, oldValue, value)
//        initialized = true
//    }
//
//    private lateinit var a: DetachedObjectReadWriteProperty<T>
//
//    fun initArg(a: DetachedObjectReadWriteProperty<T>){
//        this.a = a
//    }
//
//    protected fun getValue() : T{
//        if(!initialized){
//            value = initializer(a)
//            println("Should not happen - LazyObservableProperty :: 41")
//        }
//        return value!!
//    }
//
//    fun isInitialized() = initialized
//
//    private var pkInternal: Any? = null
//
//    fun <V> setPk(v: V){
//        pkInternal = v
//    }
//
//    fun <V> getPk() : V{
//        return pkInternal!! as V
//    }
//
//    fun <V> getPkOrNull() : V?{
//        return pkInternal as? V
//    }
//}