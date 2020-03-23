package db

import lazyCollections.LazyObservableArrayList
import lazyCollections.LazyReadWriteProperty
import observable.Observable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

//TODO Think about making detachedList Observable -> Basically a proxy for removeAll() addAll()
inline fun <reified T : Observable> Observable.detachedList(key: String) : ReadOnlyProperty<Any?, LazyObservableArrayList<T>> {
    return detachedList(key, T::class)
}

fun <T : Observable> detachedList(key: String, clazz: KClass<T>) : ReadOnlyProperty<Any?, LazyObservableArrayList<T>> {
    return DetachedListReadOnlyProperty(key, clazz) {
        LazyObservableArrayList<T>()
    }
}

class DetachedListReadOnlyProperty<T : Observable, L : Any>(val key: String, val clazz: KClass<T>, f: () -> L) : MutableLazyReadOnlyProperty<L>(f)

open class MutableLazyReadOnlyProperty<T>(f: () -> T) : LazyReadWriteProperty<T>(f) {

    fun setInitializer(f: () -> T){
        this.initFunction = f
    }

}