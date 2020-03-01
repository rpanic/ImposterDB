package observable

class LevelInformation(val list: List<Level>){

    fun append(level: Level) = LevelInformation(list.toMutableList().apply { add(0, level) })

    fun append(obj: Observable) = append(ObservableLevel(obj))

}

interface Level{
    fun isObservable() : Boolean
    fun getObservable() : Observable
    fun getArrayList() : ObservableList<*> //TODO Check if ArrayList would be possible
}

class ObservableLevel(
    val obj: Observable
) : Level{
    override fun isObservable() = true

    override fun getObservable() = obj

    override fun getArrayList() = throw IllegalAccessException("Field of Level is not a ObservableArrayList")
}

class ObservableListLevel(
        val list: ObservableList<*>,
        changeArgs: ListChangeArgs<*>
) : Level{
    override fun isObservable() = false

    override fun getObservable() = throw IllegalAccessException("Field of Level is not a ObservableArrayList")

    override fun getArrayList() = list
}

data class ListChangeArgs<T>(
        val elementChangeType: ElementChangeType,
        val elements: List<T>,
        val indizes: List<Int>
){
    constructor(elementChangeType: ElementChangeType,
                element: T,
                index: Int) : this(elementChangeType, listOf(element), listOf(index))
}

fun <T> getIndizesFromElements(list: List<T>, arrayList: ObservableList<T>) : List<Int>{
    val indizes = mutableListOf<Int>()
    arrayList.collection.forEachIndexed { index, t ->
        if(t in list)
            indizes += index
    }
    return indizes
}