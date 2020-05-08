package observable

import aNewCollections.ObservableSet
import aNewCollections.SetChangeArgs
import aNewCollections.VirtualSet
import kotlin.reflect.KProperty

class LevelInformation(val list: List<Level>){

    fun append(level: Level) = LevelInformation(list.toMutableList().apply { add(0, level) })

    fun append(obj: Observable, prop: KProperty<*>?) = append(ObservableLevel(obj, prop))

}

interface Level{
    fun isObservable() : Boolean
    fun getObservable() : Observable
    fun getSet() : Set<*> //TODO Check if ArrayList would be possible
}

class ObservableLevel(
    val obj: Observable,
    val prop: KProperty<*>?
) : Level{
    override fun isObservable() = true

    override fun getObservable() = obj

    override fun getSet() = throw IllegalAccessException("Field of Level is not a ObservableArrayList")
}

abstract class SetLevel<T : Set<*>>(
        private val set: T,
        val changeArgs: SetChangeArgs<*>
) : Level{
    override fun isObservable() = false

    override fun getObservable() = throw IllegalAccessException("Field of Level is not a ObservableArrayList")

    override fun getSet() = set

    abstract fun isVirtualSet() : Boolean
    abstract fun isObservableSet() : Boolean

    abstract fun getVirtualSet() : VirtualSet<*>
    abstract fun getObservableSet() : ObservableSet<*>
}

class VirtualSetLevel(
        set: VirtualSet<*>,
        changeArgs: SetChangeArgs<*>
) : SetLevel<VirtualSet<*>>(set, changeArgs){

    override fun isVirtualSet() = true
    override fun isObservableSet() = false

    override fun getVirtualSet() = getSet()
    override fun getObservableSet() = throw IllegalAccessException("Field of Level is not a VirtualSet")

}

class ObservableSetLevel(
        set: ObservableSet<*>,
        changeArgs: SetChangeArgs<*>
) : SetLevel<ObservableSet<*>>(set, changeArgs){

    override fun isVirtualSet() = false
    override fun isObservableSet() = true

    override fun getVirtualSet() = throw IllegalAccessException("Field of Level is not a ObservableSet")
    override fun getObservableSet() = getSet()

}