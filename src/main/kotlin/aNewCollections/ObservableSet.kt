package aNewCollections

import com.beust.klaxon.Json
import db.*
import lazyCollections.IObservableSet
import observable.*
import virtual.SetElementChangedListener

open class ObservableSet<T : Observable> : AbstractObservable<SetElementChangedListener<T>>, IObservableSet<T> {

    constructor(vararg arr: T) {
        collection.addAll(arr)
        arr.forEach { addHook(it) }
    }

    constructor(arr: List<T>) {
        collection.addAll(arr)
        arr.forEach { addHook(it) }
    }

    @Json(ignored = true)
    @Ignored
    var collection = mutableListOf<T>()

    protected fun signalChanged(args: SetChangeArgs<T>, revert: () -> Unit){
        signalChanged(args, LevelInformation(listOf(ObservableSetLevel(this, args))), revert)
    }

    protected fun signalChanged(args: SetChangeArgs<T>, levels: LevelInformation, revert: () -> Unit) {

        println("New size: $size")

        val action = object : RevertableAction {
            override fun action() {
                if(db != null) {//Db is null when addAll() is called in the constructor
                    if (args.elementChangeType == ElementChangeType.Add || args.elementChangeType == ElementChangeType.Set) {
                        args.elements.forEach { it.setDbReference(getDB()) }
                    }
                }
                listeners.forEach { it.invoke(args, levels) }
            }

            override fun revert() {
                revert()
            }

        }

        if (db != null && getDB().txActive) {
            getDB().txQueue.add(action)
        } else {
            action.action()
        }

    }

    @Json(ignored = true)
    @Ignored
    val hooks = mutableListOf<ChangeObserver<Observable>>()

    fun addHook(element: T) {

        hooks.add(GenericChangeObserver(element) { prop, levels ->
            signalChanged(UpdateSetChangeArgs(ElementChangeType.Update, listOf(element), prop), levels) {}
        })
    }

    override val size: Int
        get() = collection.size

    operator fun get(key: Any?) : T?{
        return collection.find { it.keyValue<T, Any>() == key }
    }

    override fun iterator() = collection.iterator()

    fun list() = collection.toList()

//    override operator fun get(i: Int) = collection[i].getObject()

    override fun contains(element: T): Boolean {
        return collection.contains(element)
    }

    override fun containsAll(elements: Collection<T>) = elements.all { element -> collection.any { it.keyValue<T, Any>() == element.keyValue<T, Any>() } }

    override fun isEmpty() = collection.isEmpty()

}

fun <T : Observable> observableSetOf(vararg e: T): ObservableSet<T> {
    return observableSetOf(e.toList())
}
fun <T : Observable> observableSetOf(e: List<T>): ObservableSet<T> {
    return ObservableSet(e)
}