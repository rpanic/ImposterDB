package db

typealias ElementChangedListener<X> = (ElementChangeType, X) -> Unit

enum class ElementChangeType{
    Add, Update, Remove
}

class ObservableArrayList<X : Observable>{

    constructor()

    constructor(vararg arr: X){
        collection.addAll(arr)
        arr.forEach { addHook(it) }
    }

    val hooks = mutableListOf<ChangeObserver<X>>()

    val collection = mutableListOf<X>()

    private val listeners = mutableListOf<ElementChangedListener<X>>()

    private fun signalChanged(type: ElementChangeType, element: X){
        listeners.forEach { it(type, element) }
    }

    fun addListener(f: ElementChangedListener<X>){
        listeners.add(f)
    }

    fun add(element: X) : X {
        collection.add(element)
        addHook(element)
        signalChanged(ElementChangeType.Add, element)
//        element.updateHook = { l -> signalChanged(ElementChangeType.Update, l) }
//        element.deleteHook = { l -> signalChanged(ElementChangeType.Remove, l) }
        return element
    }

    fun addHook(element: X){

        hooks.add(GenericChangeObserver(element){
            signalChanged(ElementChangeType.Update, element)
        })

    }

    fun addAll(elements: Collection<X>): Boolean {
        val added = collection.addAll(elements)
        if (added){
            elements.forEach {
                addHook(it)
                signalChanged(ElementChangeType.Add, it)
//                it.updateHook = { l -> signalChanged(ElementChangeType.Update, l) }
//                it.deleteHook = { l -> signalChanged(ElementChangeType.Remove, l) }
            }
        }
        return added
    }

    fun remove(element: X) : Boolean{

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

    operator fun iterator() = list().iterator()

    operator fun get(i : Int) = collection.get(i)

}

class GenericChangeObserver <X : Observable> (t : X, val f: () -> Unit) : ChangeObserver<X>(t){

    fun all(new: Any?){
        f()
    }
}

fun <X : Observable> observableListOf(vararg initial: X) = ObservableArrayList(*initial)