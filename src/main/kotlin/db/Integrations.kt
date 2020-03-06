package db

import observable.LazyObservableProperty
import observable.LevelInformation
import observable.Observable
import observable.ObservableLevel
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <reified T : Observable> Observable.relation(key: String) : ReadWriteProperty<Any?, T> {
    return object : LazyObservableProperty<T>({
            DB.primaryBackend.load(key, T::class)
        }){
        override fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T?) {

            changed(property, oldValue, newValue, LevelInformation(listOf(ObservableLevel(this@relation))))
        }
    }
}