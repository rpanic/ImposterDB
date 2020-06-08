package observable

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Implementation copied from ObservableProperty
 */
abstract class LazyObservableProperty<T : Any> : ReadWriteProperty<Any?, T> {

    private lateinit var value: T

    protected open fun beforeChange(property: KProperty<*>, oldValue: T?, newValue: T): Boolean = true

    protected open fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T): Unit {}

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(!this::value.isInitialized){
            throw IllegalAccessException("Value of property ${thisRef?.javaClass?.kotlin?.simpleName}.${property.name} has not been set for lazy property")
        }
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val oldValue = if(this::value.isInitialized){
            this.value
        }else{
            null
        }

        if (!beforeChange(property, oldValue, value)) {
            return
        }
        this.value = value
        afterChange(property, oldValue, value)
    }

}