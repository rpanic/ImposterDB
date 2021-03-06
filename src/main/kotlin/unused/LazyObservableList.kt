package unused

import collections.ElementChangeType
import com.beust.klaxon.Json
import db.ChangeObserver
import db.Ignored
import db.RevertableAction
import example.debug
import observable.*

open class LazyObservableList<T : Observable> : AbstractObservable<ElementChangedListener<T>>, IObservableList<T> {
    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    constructor(vararg arr: ObjectReference<T>) {
        collection.addAll(arr)
        arr.forEach { ref -> ref.onLoad { addHook(it) } }
    }

    constructor(arr: List<ObjectReference<T>>) {
        collection.addAll(arr)
        arr.forEach { ref -> ref.onLoad { addHook(it) } }
    }

    @Json(ignored = true)
    @Ignored
    var collection = mutableListOf<ObjectReference<T>>()

    protected fun signalChanged(args: ListChangeArgs<T>, revert: () -> Unit){
//        signalChanged(args, LevelInformation(listOf(ObservableListLevel(this, args))), revert)
    }

    protected fun signalChanged(args: ListChangeArgs<T>, levels: LevelInformation, revert: () -> Unit) {

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


    override fun iterator() = getAndResolveObjects().iterator()

    fun list() = collection.toList()

    override operator fun get(i: Int) = collection[i].getObject()


    override fun contains(element: T): Boolean {
        return collection.any { it.pk == element.keyValue<T>() }
    }

    override fun containsAll(elements: Collection<T>) = elements.all { element -> collection.any { it.pk == element.keyValue<T>() } }

    override fun indexOf(element: T) = collection.indexOfFirst { it.pk == element.keyValue<T>() }

    override fun isEmpty() = collection.isEmpty()

    override fun lastIndexOf(element: T) = collection.indexOfLast { it.pk == element.keyValue<T>() }

    override fun listIterator() = getAndResolveObjects().listIterator()

    override fun listIterator(index: Int) = getAndResolveObjects().listIterator(index)

    private fun getAndResolveObjects() : List<T>{
        //TODO Optimize
        return collection.map { it.getObject() }
    }

    fun resolve() {
        getAndResolveObjects()
    }

    fun <K : Any> getIndizesFromElements(list: List<K>) : List<Int>{
        val indizes = mutableListOf<Int>()
        this.collection.forEachIndexed { index, t ->
            if(t.pk in list)
                indizes += index
        }
        return indizes
    }

}