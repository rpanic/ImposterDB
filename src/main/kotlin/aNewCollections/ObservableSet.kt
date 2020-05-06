package aNewCollections

import com.beust.klaxon.Json
import db.*
import lazyCollections.IObservableSet
import lazyCollections.ObjectReference
import observable.*

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

//    protected fun signalChanged(args: ListChangeArgs<T>, revert: () -> Unit){
//        signalChanged(args, LevelInformation(listOf(ObservableListLevel(this, args))), revert)
//    }

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
            //TODO Check, if element references and indizes are 1 to 1 in all ListChangeArgs, so element and indizes can be correlated. Like below
            val indizes = getIndizesFromElements(listOf(element))
            signalChanged(UpdateListChangeArgs(ElementChangeType.Update, indizes.indices.map { element }, indizes, prop), levels) {}
        })
    }

    override val size: Int
        get() = collection.size

    operator fun get(key: Any?) : T?{
        return collection.find { it.keyValue<T, Any>() == key }
    }

    override fun iterator() = getAndResolveObjects().iterator()

    fun list() = collection.toList()

//    override operator fun get(i: Int) = collection[i].getObject()

    override fun contains(element: T): Boolean {
        return collection.contains(element)
    }

    override fun containsAll(elements: Collection<T>) = elements.all { element -> collection.any { it.keyValue<T, Any>() == element.keyValue<T, Any>() } }

    override fun isEmpty() = collection.isEmpty()

    private fun getAndResolveObjects() : List<T>{
        //TODO Optimize
        return collection
    }

    fun resolve() {
        getAndResolveObjects()
    }

    fun <K : Any> getIndizesFromElements(list: List<K>) : List<Int>{
        val indizes = mutableListOf<Int>()
        this.collection.forEachIndexed { index, t ->
            if(t in list)
                indizes += index
        }
        return indizes
    }

}