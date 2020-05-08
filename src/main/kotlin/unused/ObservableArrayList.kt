package unused

import aNewCollections.ElementChangeType
import db.ChangeObserver
import observable.LevelInformation
import observable.Observable
import kotlin.reflect.KProperty

class ObservableArrayList<X> : ObservableList<X> {

    constructor(vararg arr: X) : super(*arr)
    constructor(arr: List<X>) : super(arr)

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

    fun add(element: X): Boolean {
        addAndReturn(element)
        return true
    }

    fun add(index: Int, element: X) {
        collection.add(index, element)
        addActions(index, element)
    }

    fun addAll(elements: Collection<X>): Boolean {
        return addAll(size, elements)
    }

    fun addAll(index: Int, elements: Collection<X>): Boolean {
        val added = collection.addAll(elements)
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

    fun remove(element: X) : Boolean{

//        val b = collection.remove(element)
        val indizes = collection.indices.filter { x -> collection[x] == element }
        //signalChanged(ElementChangeType.Remove, element)
        return removeAt(indizes)
    }

    fun removeAll(elements: Collection<X>): Boolean {
        val indizes = collection.indices.filter { x -> collection[x] in elements }
        return removeAt(indizes)
    }

    fun removeAt(index: Int): X {
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
        val args = ListChangeArgs(ElementChangeType.Remove, removed, indizes)
        signalChanged(args){
            for(i in indizes.reversed()){
                add(i + removed.size - 1, removed.removeAt(removed.size - 1))
            }
        } //TODO checken wie es sich mit mehreren signalChanged calls verhält

        return removed.size == indizes.size

    }

    fun set(index: Int, element: X): X {
        val old = collection.set(index, element)
        val args = SetListChangeArgs(ElementChangeType.Set, listOf(element), listOf(index), listOf(old))
        signalChanged(args){
            set(index, old)
        }
        return old
    }

    fun clear() {
        removeAt(collection.indices.toList())
    }
}

fun <X> observableListOf(vararg initial: X) = ObservableArrayList(*initial)
fun <X> observableListOf(initial: List<X>) = ObservableArrayList(initial)