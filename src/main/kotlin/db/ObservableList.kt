package db

import com.beust.klaxon.Json

typealias ElementChangedListener<X> = (ElementChangeType, X, LevelInformation) -> Unit

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

    protected fun signalChanged(type: ElementChangeType, element: T, revert: () -> Unit){
        signalChanged(type, element, LevelInformation(listOf(ObservableListLevel(this, type))), revert)
    }

    protected fun signalChanged(type: ElementChangeType, element: T, levels: LevelInformation, revert: () -> Unit) {

        println("New size: $size")

        val action = object : RevertableAction {
            override fun action() {
                listeners.forEach { it.invoke(type, element, levels) }
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
                signalChanged(ElementChangeType.Update, element, levels) {}
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