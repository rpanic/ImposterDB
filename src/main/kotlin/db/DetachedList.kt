package db

import lazyCollections.LazyObservableArrayList
import lazyCollections.LazyReadWriteProperty
import lazyCollections.ObjectReference
import observable.Observable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

//TODO Think about making detachedList Observable -> Basically a proxy for removeAll() addAll()
inline fun <reified T : Observable> Observable.detachedList(key: String) : ReadOnlyProperty<Any?, LazyObservableArrayList<T>> {
    return detachedList(key, T::class)
}

fun <T : Observable> detachedList(key: String, clazz: KClass<T>) : ReadOnlyProperty<Any?, LazyObservableArrayList<T>> {
    return DetachedListReadOnlyProperty { aggregateKey, list ->
        if(list.isEmpty()) {
            LazyObservableArrayList<T>()
        }else {

            LazyObservableArrayList<T>(list.map { key ->

                ObjectReference(key) { pk ->

                    DB.getDetached(aggregateKey, pk, clazz = clazz) {
                        throw IllegalAccessException("Cant initialize missing M:N Reference Object\nMissing Object wih pk $pk which is referenced in Table $aggregateKey")
                    }
                }
            })
        }
    }
    //TODO Add Listener to List to connect to DB
}

class DetachedListReadOnlyProperty<L : Any>(f: (String, List<Any>) -> L) : MutableLazyReadOnlyProperty<L>(f){
}

abstract class MutableLazyReadOnlyProperty<T>(protected var initFunction: (String, List<Any>) -> T) : ReadOnlyProperty<Any?, T> {

    private var initialized = false
    private var value: T? = null //TODO Nulls dont work, they throw a npe even if type is nullable

    var aggregatedKey: String? = null
    var pks: List<Any> = listOf()

    fun setArgs(aggregatedKey: String, pks: List<Any>){
        this.aggregatedKey = aggregatedKey
        this.pks = pks
    }

    fun setInitializer(f: (String, List<Any>) -> T){
        this.initFunction = f
    }

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(!initialized){
            value = initFunction(aggregatedKey ?: "", pks)
        }
        return value!!
    }

    protected fun getValue() : T{
        if(!initialized){
            value = initFunction(aggregatedKey ?: "", pks)
            println("Should not happen - LazyReadWriteProperty :: 41")
        }
        return value!!
    }
}