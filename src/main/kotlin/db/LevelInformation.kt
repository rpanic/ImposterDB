package db

class LevelInformation(val list: List<Level>){

    fun append(level: Level) = LevelInformation(list.toMutableList().apply{ add(0, level)})

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
        val elementChangeType: ElementChangeType
) : Level{
    override fun isObservable() = false

    override fun getObservable() = throw IllegalAccessException("Field of Level is not a ObservableArrayList")

    override fun getArrayList() = list
}