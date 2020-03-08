package observable

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class LazyObservableProperty<T>(val initializer: () -> T?) : ReadWriteProperty<Any?, T> {

    private var initialized = false
    private var value: T? = null //TODO Nulls dont work, they throw a npe even if type is nullable

    protected /*open*/ fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean = true

    protected open fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?): Unit {}

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(!initialized){
            value = initializer()
        }
        return value!!
    }

    public override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val oldValue = this.value
        this.value = value
        afterChange(property, oldValue, value)
    }

    protected fun getValue() : T{
        if(!initialized){
            value = initializer()
            println("Should not happen - LazyObservableProperty :: 31")
        }
        return value!!
    }
}