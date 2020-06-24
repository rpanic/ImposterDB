package observable

import com.beust.klaxon.Json
import db.ChangeObserver
import db.Ignored
import db.RevertableAction
import collections.Indexable
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

typealias ChangeListener<T> = (prop: KProperty<*>, old: T, new: T, levels: LevelInformation) -> Unit

abstract class Observable : Indexable(){
    //TODO Use AbstractObservable at some point in the future, but requires restructuring of Type Hierachy

    @Json(ignored = true)
    @Ignored
    val propListeners = mutableMapOf<String, MutableList<ChangeListener<*>>>()

    @Json(ignored = true)
    @Ignored
    val listeners = mutableListOf<ChangeListener<*>>()

    fun <T : Any?> changed(prop: KProperty<*>, old: T, new: T, levels: LevelInformation){

        hookToObservable(new, prop)

        val action = object : ObservableRevertableAction<T>(this, prop, old, new){

            override fun executeListeners(prop: KProperty<*>, old_p: T, new_p: T) {
                if(propListeners.containsKey(prop.name)){
                    val list = propListeners[prop.name]!! as List<ChangeListener<T>>
                    list.forEach { it(prop, old_p, new_p, levels) }
                }
                (listeners as List<ChangeListener<T>>).toList().forEach { it(prop, old_p, new_p, levels) }
            }
        }

        if(db != null && getDB().txActive){
            getDB().txQueue.add(action)
        }else{
            action.action()
        }

    }

    fun <T> addListener(prop: KProperty<T>, listener: ChangeListener<T>){
        if(!propListeners.containsKey(prop.name)){
            propListeners[prop.name] = mutableListOf()
        }
        propListeners[prop.name]!!.add(listener)
    }

    fun <T : Any?> addListener(listener: ChangeListener<T>){
        listeners.add(listener)
    }

    internal fun <T> hookToObservable(obj: T, parentProperty: KProperty<*>?){
        if(obj is Observable){
            obj.addListener { childProp: KProperty<*>, old: T, new: T, levels: LevelInformation ->
                changed(childProp, old, new, levels.append(this, old, new, parentProperty))
            }
        }
    }

    fun <T : Any?> observable(initialValue: T) : ReadWriteProperty<Any?, T>{

        hookToObservable(initialValue, null) //Is null atm, because there is technically no real way of getting the correct Property. But this should only be a outside api issue

        return object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T){
                changed(property, oldValue, newValue, LevelInformation(listOf(ObservableLevel(this@Observable, oldValue, newValue, property))))
            }
        }
    }

    fun <T : Any> lazyObservable() : ReadWriteProperty<Any?, T>{

        return object : LazyObservableProperty<T>() {
            override fun afterChange(property: KProperty<*>, oldValue: T?, newValue: T){
                changed(property, oldValue, newValue, LevelInformation(listOf(ObservableLevel(this@Observable, oldValue, newValue, property))))
            }
        }

    }

    override fun equals(other: Any?): Boolean {
        return if(other is Observable){
            other.keyValue<Observable>() == keyValue<Observable>()
        }else {
            super.equals(other)
        }
    }

}

abstract class ObservableRevertableAction<T>(val observable: Observable, val prop: KProperty<*>, val old: T, val new: T) : RevertableAction {

    override fun action() {
        executeListeners(prop, old, new)
    }

    override fun revert() {
        if(prop is KMutableProperty<*>){
            prop.setter.call(observable, old)
        }
    }

    abstract fun executeListeners(prop: KProperty<*>, old_p: T, new_p: T)

}

class GenericChangeObserver <X : Observable> (t : X, val f: (KProperty<*>, LevelInformation) -> Unit) : ChangeObserver<X>(t){

    fun all(prop: KProperty<*>, new: Any?, old: Any?, levels: LevelInformation){
        f(prop, levels)
    }
}