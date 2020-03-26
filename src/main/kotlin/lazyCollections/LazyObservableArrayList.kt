package lazyCollections

import db.*
import observable.*
import java.lang.Exception
import kotlin.reflect.KProperty

class LazyObservableArrayList<X : Observable> : LazyObservableList<X>, IMutableObservableList<X> {

    constructor(vararg arr: ObjectReference<X>) : super(*arr)
    constructor(arr: List<ObjectReference<X>>) : super(arr)

    private fun addActions(index: Int, element: X){
        addHook(element)
        val args = ListChangeArgs(ElementChangeType.Add, listOf(element), listOf(index))
        signalChanged(args, LevelInformation(listOf())){ //TODO Why empty List?
            removeAt(index)
        }
    }

    private fun addAndReturn(element: X) : X {
        add(size, element)
        return element
    }

    override fun add(element: X): Boolean {
        addAndReturn(element)
        return true
    }

    override fun add(index: Int, element: X) {
        collection.add(index, ObjectReference(element))
        addActions(index, element)
    }

    override fun addAll(elements: Collection<X>): Boolean {
        return addAll(size, elements)
    }

    override fun addAll(index: Int, elements: Collection<X>): Boolean {
        val added = collection.addAll(elements.map { ObjectReference(it) })
        if (added){
            elements.forEachIndexed { index2, it ->
                addHook(it)
                val args = ListChangeArgs(ElementChangeType.Add, it, index + index2)
                signalChanged(args){
                    (index until (index + elements.size)).forEach { i -> removeAt(i) }
                }
            }
        }
        return added
    }

    override fun remove(element: X) : Boolean{

//        val b = collection.remove(element)
        val indizes = collection.indices.filter { x -> collection[x].getObject() == element }
        //signalChanged(ElementChangeType.Remove, element)
        return removeAt(indizes)
    }

    override fun removeAll(elements: Collection<X>): Boolean {
        val indizes = collection.indices.filter { x -> collection[x].getObject() in elements }
        return removeAt(indizes)
    }

    override fun removeAt(index: Int): X {
        val x = collection[index]
        if(removeAt(listOf(index)))
            return x.getObject()
        else
            throw Exception("Should not happen")
    }

    private fun removeAt(indizes: List<Int>) : Boolean {

        var removed = mutableListOf<X>()
        for(i in indizes){
            removed.add(collection.removeAt(i - removed.size).getObject())
        }
        val args = ListChangeArgs(ElementChangeType.Remove, removed, indizes)
        signalChanged(args){ //TODO See if it is possible to make the reversing Functions work with existing ObjectReference Instances, so that no new ones are created
            for(i in indizes.reversed()){
                add(i + removed.size - 1, removed.removeAt(removed.size - 1))
            }
        } //TODO checken wie es sich mit mehreren signalChanged calls verhält

        return removed.size == indizes.size

    }

    override fun set(index: Int, element: X): X {
        val old = collection.set(index, ObjectReference(element)).getObject()
        val args = SetListChangeArgs(ElementChangeType.Set, listOf(element), listOf(index), listOf(old))
        signalChanged(args){
            set(index, old)
        }
        return old
    }

    override fun clear() {
        removeAt(collection.indices.toList())
    }

    override fun iterator(): MutableIterator<X> {
        return collection.map { it.getObject() }.toMutableList().iterator() //TODO Really expensive, but used for JsonBackend
    }

    override fun listIterator(): MutableListIterator<X> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listIterator(index: Int): MutableListIterator<X> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<X> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun retainAll(elements: Collection<X>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class GenericChangeObserver <X : Observable> (t : X, val f: (KProperty<*>, LevelInformation) -> Unit) : ChangeObserver<X>(t){

    fun all(prop: KProperty<*>, new: Any?, old: Any?, levels: LevelInformation){
        f(prop, levels)
    }
}

fun <X> observableListOf(vararg initial: X) = ObservableArrayList(*initial)
fun <X> observableListOf(initial: List<X>) = ObservableArrayList(initial)