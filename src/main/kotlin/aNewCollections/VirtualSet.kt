package aNewCollections

import lazyCollections.IObservableSet
import lazyCollections.IReadonlyVirtualSet
import lazyCollections.IVirtualSet
import observable.*


open class ReadOnlyVirtualSet<T : Observable>(
    val loader: (Any) -> T?,
    val steps: List<Step<T, *>>
): AbstractObservable<ElementChangedListener<T>>(), IReadonlyVirtualSet<T>{
    override fun view(): IObservableSet<T> {
        //Make a ObservableSet here
//        val references = getDB().retrieve(steps)

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override operator fun get(key: Any) : T?{
        return loader(key)
    }

    fun <V> map(f: (T) -> V) : ReadOnlyVirtualSet<T>{
        return ReadOnlyVirtualSet({
            get(it)
        }, steps + MapStep<T, V>())
    }

}

open class VirtualSet<T : Observable>(
    loader: (Any) -> T?,
    val setter: (T) -> Unit,
    steps: List<Step<T, *>>
) : ReadOnlyVirtualSet<T>(loader, steps), IVirtualSet<T> {

    override fun add(t: T) {
        setter(t)
    }


    fun filter() : VirtualSet<T>{
        return VirtualSet({
            get(it)
        }, { add(it) }, steps + FilterStep())
    }
}