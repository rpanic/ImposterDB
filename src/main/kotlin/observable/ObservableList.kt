package observable

import com.beust.klaxon.Json
import db.*

typealias ElementChangedListener<X> = (ListChangeArgs<X>, LevelInformation) -> Unit

enum class ElementChangeType {
    Add, Update, Remove, Set
}

open class ObservableList<T> : AbstractObservable<ElementChangedListener<T>>, List<T> {
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
        signalChanged(args, LevelInformation(listOf(ObservableListLevel(this, args))), revert)
    }

    //TODO A lot of information gets lost here, which will be needed in the transformations to work efficiently
    //F.e. Add Index, Remove Indizes etc.
    //Maybe add Event Objects to contain this information as a Level Subtype
    protected fun signalChanged(args: ListChangeArgs<T>, levels: LevelInformation, revert: () -> Unit) {

        println("New size: $size")

        val action = object : RevertableAction {
            override fun action() {
                listeners.forEach { it.invoke(args, levels) }
            }

            override fun revert() {
                revert()
            }

        }

        if (DB.txActive) {
            DB.txQueue.add(action)
        } else {
            action.action()
        }

    }

    @Json(ignored = true)
    @Ignored
    val hooks = mutableListOf<ChangeObserver<Observable>>()

    fun addHook(element: T) {

        if (element is Observable) {
            hooks.add(GenericChangeObserver(element) { levels ->
                //TODO Check, if element references and indizes are 1 to 1 in all ListChangeArgs, so element and indizes can be correlated. Like below
                val indizes = getIndizesFromElements<T>(listOf(element), this)
                signalChanged(ListChangeArgs(ElementChangeType.Update, indizes.indices.map { element }, indizes), levels) {}
            })
        }
    }

    override val size: Int
        get() = collection.size


    override fun iterator() = collection.iterator()

    fun list() = collection.toList()

    override operator fun get(i: Int) = collection.get(i)


    override fun contains(element: T): Boolean {
        return collection.contains(element)
    }

    override fun containsAll(elements: Collection<T>) = collection.containsAll(elements)

    override fun indexOf(element: T) = collection.indexOf(element)

    override fun isEmpty() = collection.isEmpty()

    override fun lastIndexOf(element: T) = collection.lastIndexOf(element)

    override fun listIterator() = collection.listIterator()

    override fun listIterator(index: Int) = collection.listIterator(index)

//    lateinit var transform: (ObservableList<T>) -> ObservableList<S>

//    fun <S> map(f: (T) -> S){
//
//        val next = ObservableList<S>()
//
//        val criteria = {type: ElementChangeType, t: T ->
//            true
//        }
//
//        addListener{ type, t ->
//            if(criteria(type, t)){
//                next.listeners.forEach { it(type, t) }
//            }
//        }
//
//        next.transform = {
//            ObservableArrayList(it.map(f))
//        }
//
//    }
//
//    fun forEach(f: (T) -> Unit){
//
//    }


}

//fun <T> ObservableArrayList<T>.view(){
//    val view = ObservableListView<T>()
//    this.addListener { elementChangeType, t ->
//        view.listeners.forEach { it.invoke(elementChangeType, t) }
//    }
//}