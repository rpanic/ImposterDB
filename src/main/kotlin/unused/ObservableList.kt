package unused

import aNewCollections.ElementChangeType
import com.beust.klaxon.Json
import db.ChangeObserver
import db.Ignored
import db.RevertableAction
import observable.AbstractObservable
import observable.DBAwareObject
import observable.LevelInformation
import observable.Observable

typealias ElementChangedListener<X> = (ListChangeArgs<X>, LevelInformation) -> Unit


open class ObservableList<T> : AbstractObservable<ElementChangedListener<T>>, IObservableList<T> {
    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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

    protected fun signalChanged(args: ListChangeArgs<T>, revert: () -> Unit){
//        signalChanged(args, LevelInformation(listOf(ObservableListLevel(this, args))), revert)
    }

    protected fun signalChanged(args: ListChangeArgs<T>, levels: LevelInformation, revert: () -> Unit) {

        println("signalChanged: ${args.elementChangeType.name}")
        println("New size: $size")

        val action = object : RevertableAction {
            override fun action() {
                if(db != null) {//Db is null when addAll() is called in the constructor
                    if (args.elementChangeType == ElementChangeType.Add || args.elementChangeType == ElementChangeType.Set) {
                        args.elements.forEach {
                            if(it is DBAwareObject) {
                                it.setDbReference(getDB())
                            }
                        }
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

        if (element is Observable) {
            hooks.add(GenericChangeObserver(element) { prop, levels ->
                //TODO Check, if element references and indizes are 1 to 1 in all ListChangeArgs, so element and indizes can be correlated. Like below
                val indizes = getIndizesFromElements(listOf(element))
                signalChanged(UpdateListChangeArgs(ElementChangeType.Update, indizes.indices.map { element }, indizes, prop), levels) {}
            })
        }
    }

    override val size: Int
        get() = collection.size


    override fun iterator() = collection.iterator()

    fun list() = collection.toList()

    override operator fun get(i: Int) = collection[i]

    override fun contains(element: T): Boolean {
        return collection.contains(element)
    }

    override fun containsAll(elements: Collection<T>) = collection.containsAll(elements)

    override fun indexOf(element: T) = collection.indexOf(element)

    override fun isEmpty() = collection.isEmpty()

    override fun lastIndexOf(element: T) = collection.lastIndexOf(element)

    override fun listIterator() = collection.listIterator()

    override fun listIterator(index: Int) = collection.listIterator(index)

    fun getIndizesFromElements(list: List<T>) : List<Int>{
        val indizes = mutableListOf<Int>()
        this.collection.forEachIndexed { index, t ->
            if(t in list)
                indizes += index
        }
        return indizes
    }


}

//fun <T> ObservableArrayList<T>.view(){
//    val view = ObservableListView<T>()
//    this.addListener { elementChangeType, t ->
//        view.listeners.forEach { it.invoke(elementChangeType, t) }
//    }
//}