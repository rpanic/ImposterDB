package db

import com.beust.klaxon.Json

typealias ElementChangedListener<X> = (ElementChangeType, X) -> Unit

enum class ElementChangeType{
    Add, Update, Remove
}

class ObservableArrayList<X : Observable> : ArrayList<X>{

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
    val hooks = mutableListOf<ChangeObserver<X>>()

    var collection = mutableListOf<X>()

    @Json(ignored = true)
    private val listListeners = mutableListOf<ElementChangedListener<X>>()

    private fun signalChanged(type: ElementChangeType, element: X){
        listListeners.forEach { it.invoke(type, element) }
    }

    fun addListener(f: ElementChangedListener<X>){
        listListeners.add(f)
    }

    fun addAndReturn(element: X) : X {
        collection.add(element)
        addHook(element)
        signalChanged(ElementChangeType.Add, element)
        return element
    }

    override fun add(element: X): Boolean {
        addAndReturn(element)
        return true
    }

    fun addHook(element: X){

        hooks.add(GenericChangeObserver(element){
            signalChanged(ElementChangeType.Update, element)
        })

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

    @Deprecated("")
    fun update(element: X){
        if(element in collection){
            signalChanged(ElementChangeType.Update, element)
        }
    }

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

    override fun isEmpty(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun lastIndexOf(element: X): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class GenericChangeObserver <X : Observable> (t : X, val f: () -> Unit) : ChangeObserver<X>(t){

    fun all(new: Any?){
        f()
    }
}

fun <X : Observable> observableListOf(vararg initial: X) = ObservableArrayList(*initial)