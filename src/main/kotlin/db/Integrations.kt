package db

import observable.LazyObservableProperty
import observable.LevelInformation
import observable.Observable
import observable.ObservableLevel
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <reified T : Observable> Observable.detached(key: String) : ReadWriteProperty<Any?, T> {
    return object : DetachedReadWriteProperty<T>({
            DB.primaryBackend.load(key, T::class)
        }){
        override fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?) {
            changed(property, oldValue, newValue, LevelInformation(listOf(ObservableLevel(this@detached))))
        }
    }
}

open class DetachedReadWriteProperty<T : Observable>(initializer: () -> T?) : LazyObservableProperty<T>(initializer){

    fun <V> getPk() : V{
        return getValue().key()
    }

}