package db

import com.beust.klaxon.Json

typealias ElementChangedListener<X> = (ElementChangeType, X) -> Unit

enum class ElementChangeType{
    Add, Update, Remove
}

class ObservableArrayList<X> : MutableList<X>{

    override fun iterator() = collection.iterator()

    constructor(vararg arr: X) {
        collection.addAll(arr)
        arr.forEach { addHook(it) }
    }

    constructor(arr: List<X>) {
        collection.addAll(arr)
        arr.forEach { addHook(it) }
    }

    @Json(ignored = true)
    val hooks = mutableListOf<ChangeObserver<Observable>>()

    var collection = mutableListOf<X>()

    @Json(ignored = true)
    private val listListeners = mutableListOf<ElementChangedListener<X>>()

    private fun signalChanged(type: ElementChangeType, element: X){
        listListeners.forEach { it.invoke(type, element) }
    }

    fun addListener(f: ElementChangedListener<X>){
        listListeners.add(f)
    }

    private fun addActions(element: X){
        addHook(element)
        signalChanged(ElementChangeType.Add, element)
    }

    fun addAndReturn(element: X) : X {
        collection.add(element)
        addActions(element)
        return element
    }

    override fun add(element: X): Boolean {
        addAndReturn(element)
        return true
    }

    override fun add(index: Int, element: X) {
        collection.add(index, element)
        addActions(element)
    }

    fun addHook(element: X){

        if(element is Observable){
            hooks.add(GenericChangeObserver(element){
                signalChanged(ElementChangeType.Update, element)
            })
        }
    }

    override fun addAll(elements: Collection<X>): Boolean {
        val added = collection.addAll(elements)
        if (added){
            elements.forEach {
                addHook(it)
                signalChanged(ElementChangeType.Add, it)
            }
        }
        return added
    }

    override fun remove(element: X) : Boolean{

        val b = collection.remove(element)
        signalChanged(ElementChangeType.Remove, element)
        return b
    }

//    @Deprecated("")
//    fun update(element: X){
//        if(element in collection){
//            signalChanged(ElementChangeType.Update, element)
//        }
//    }

    fun list() = collection.toList()

//    operator fun iterator() = list().iterator()

    override operator fun get(i : Int) = collection.get(i)

    override val size: Int
        get() = collection.size

    override fun contains(element: X): Boolean {
        return collection.contains(element)
    }

    override fun containsAll(elements: Collection<X>) = collection.containsAll(elements)

    override fun indexOf(element: X) = collection.indexOf(element)

    override fun isEmpty() = collection.isEmpty()

    override fun lastIndexOf(element: X) = collection.lastIndexOf(element)

    override fun listIterator() = collection.listIterator()

    override fun listIterator(index: Int) = collection.listIterator(index)

    override fun addAll(index: Int, elements: Collection<X>): Boolean {
        throw UnsupportedOperationException()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear() {
        throw UnsupportedOperationException()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAll(elements: Collection<X>): Boolean {
        throw UnsupportedOperationException()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAt(index: Int): X {
        throw UnsupportedOperationException()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun retainAll(elements: Collection<X>): Boolean {
        throw UnsupportedOperationException()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun set(index: Int, element: X): X {
        throw UnsupportedOperationException()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<X> {
        throw UnsupportedOperationException()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class GenericChangeObserver <X : Observable> (t : X, val f: () -> Unit) : ChangeObserver<X>(t){

    fun all(new: Any?){
        f()
    }
}

fun <X> observableListOf(vararg initial: X) = ObservableArrayList(*initial)