package observable

import collections.ObservableSet
import collections.SetChangeArgs
import virtual.ReadOnlyVirtualSet
import kotlin.reflect.KProperty

class LevelInformation(val list: List<Level>){

    fun append(level: Level) = LevelInformation(list.toMutableList().apply { add(0, level) })

    fun append(obj: Observable, old: Any?, new: Any?, prop: KProperty<*>?) = append(ObservableLevel(obj, old, new, prop))

}

interface Level{
    fun isObservable() : Boolean
    fun getObservable() : Observable
    fun getSet() : Set<*>
}

class ObservableLevel(
    val obj: Observable,
    val old: Any?,
    val new: Any?,
    val prop: KProperty<*>?
) : Level{
    override fun isObservable() = true

    override fun getObservable() = obj

    override fun getSet() = throw IllegalAccessException("Field of Level is not a ObservableArrayList")
    
    override fun toString(): String {
        return "ObservableLevel(obj=$obj, old=$old, new=$new, prop=$prop)"
    }
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

    abstract fun getVirtualSet() : ReadOnlyVirtualSet<*>
    abstract fun getObservableSet() : ObservableSet<*>
    
    override fun toString(): String {
        return "SetLevel(set=$set, changeArgs=$changeArgs)"
    }
    
}

class VirtualSetLevel(
        set: ReadOnlyVirtualSet<*>,
        changeArgs: SetChangeArgs<*>
) : SetLevel<ReadOnlyVirtualSet<*>>(set, changeArgs){

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