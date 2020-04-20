package lazyCollections

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class LazyReadWriteProperty<T>(protected var initFunction: () -> T) : ReadOnlyProperty<Any?, T> {

    private var initialized = false
    private var value: T? = null //TODO Nulls dont work, they throw a npe even if type is nullable

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(!initialized){
            value = initFunction()
        }
        return value!!
    }

    protected fun getValue() : T{
        if(!initialized){
            value = initFunction()
            println("Should not happen - LazyReadWriteProperty :: 41")
        }
        return value!!
    }
}