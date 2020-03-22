package observable

import db.DetachedObjectReadWriteProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class LazyObservableProperty<T : Observable>(val initializer: (DetachedObjectReadWriteProperty<T>) -> T) : ReadWriteProperty<Any?, T> {

    init {
        print(1)
    }

    private var initialized = false
    private var value: T? = null //TODO Nulls dont work, they throw a npe even if type is nullable

    protected /*open*/ fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean = true

    protected open fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?): Unit {}

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
    }

    private lateinit var a: DetachedObjectReadWriteProperty<T>

    fun initArg(a: DetachedObjectReadWriteProperty<T>){
        this.a = a
    }

    protected fun getValue() : T{
        if(!initialized){
            value = initializer(a)
            println("Should not happen - LazyObservableProperty :: 41")
        }
        return value!!
    }
}