package db

import com.beust.klaxon.Json
import java.lang.Exception

typealias ElementChangedListener<X> = (ElementChangeType, X) -> Unit

enum class ElementChangeType{
    Add, Update, Remove, Set
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
    @Ignored
    val hooks = mutableListOf<ChangeObserver<Observable>>()

    var collection = mutableListOf<X>()

    @Json(ignored = true)
    @Ignored
    private val listListeners = mutableListOf<ElementChangedListener<X>>()

    private fun signalChanged(type: ElementChangeType, element: X, revert: () -> Unit){

        println("New size: $size")

        val action = object : RevertableAction{
            override fun action() {
                listListeners.forEach { it.invoke(type, element) }
            }

            override fun revert() {
                revert()
            }

        }

        if(DB.txActive){
            DB.txQueue.add(action)
        }else{
            action.action()
        }

    }

    fun addListener(f: ElementChangedListener<X>){
        listListeners.add(f)
    }

    private fun addActions(index: Int, element: X){
        addHook(element)
        signalChanged(ElementChangeType.Add, element){
            removeAt(index)
        }
    }

    fun addAndReturn(element: X) : X {
        add(size, element)
        return element
    }

    override fun add(element: X): Boolean {
        addAndReturn(element)
        return true
    }

    override fun add(index: Int, element: X) {
        collection.add(index, element)
        addActions(index, element)
    }

    fun addHook(element: X){

        if(element is Observable){
            hooks.add(GenericChangeObserver(element){
                signalChanged(ElementChangeType.Update, element){}
            })
        }
    }

    override fun addAll(elements: Collection<X>): Boolean {
        return addAll(size, elements)
    }

    override fun addAll(index: Int, elements: Collection<X>): Boolean {
        val added = collection.addAll(elements)
        if (added){
            elements.forEach {
                addHook(it)
                signalChanged(ElementChangeType.Add, it){
                    (index until (index + elements.size)).forEach { i -> removeAt(i) }
                }
            }
        }
        return added
    }

    override fun remove(element: X) : Boolean{

//        val b = collection.remove(element)
        val indizes = collection.indices.filter { x -> collection[x] == element }
        //signalChanged(ElementChangeType.Remove, element)
        return removeAt(indizes)
    }

    override fun removeAll(elements: Collection<X>): Boolean {
        val indizes = collection.indices.filter { x -> collection[x] in elements }
        return removeAt(indizes)
    }

    override fun removeAt(index: Int): X {
        val x = collection[index]
        if(removeAt(listOf(index)))
            return x
        else
            throw Exception("Should not happen")
    }

    private fun removeAt(indizes: List<Int>) : Boolean {

        var removed = mutableListOf<X>()
        for(i in indizes){
            removed.add(collection.removeAt(i - removed.size))
        }
        signalChanged(ElementChangeType.Remove, removed[0]){
            for(i in indizes.reversed()){
                add(i + removed.size - 1, removed.removeAt(removed.size - 1))
            }
        } //TODO checken wie es sich mit mehreren signalChanged calls verhält

        return removed.size == indizes.size

    }

    fun list() = collection.toList()

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

    override fun clear() {
        removeAt(collection.indices.toList())
    }

    override fun retainAll(elements: Collection<X>): Boolean {
        throw UnsupportedOperationException()
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun set(index: Int, element: X): X {
        val old = collection.set(index, element)
        signalChanged(ElementChangeType.Set, element){
            set(index, old)
        }
        return old
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