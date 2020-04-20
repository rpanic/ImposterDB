package observable

import com.beust.klaxon.Json
import db.Ignored
import db.RevertableAction
import lazyCollections.Indexable
import java.util.*
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

typealias ChangeListener<T> = (prop: KProperty<*>, old: T, new: T, levels: LevelInformation) -> Unit

abstract class Observable : DBAwareObject(), Indexable{

    @Json(ignored = true)
    @Ignored
    val listeners = mutableMapOf<String, MutableList<ChangeListener<*>>>()

    @Json(ignored = true)
    @Ignored
    val classListeners = mutableListOf<ChangeListener<*>>()

    var uuid: String = UUID.randomUUID().toString()

    fun <T : Any?> changed(prop: KProperty<*>, old: T, new: T, levels: LevelInformation){

        hookToObservable(new, prop)

        val action = object : ObservableRevertableAction<T>(this, prop, old, new){

            override fun executeListeners(prop: KProperty<*>, old_p: T, new_p: T) {
                if(listeners.containsKey(prop.name)){
                    val list = listeners[prop.name]!! as List<ChangeListener<T>>
                    list.forEach { it(prop, old_p, new_p, levels) }
                }
                (classListeners as List<ChangeListener<T>>).toList().forEach { it(prop, old_p, new_p, levels) }
            }
        }

        if(db != null && getDB().txActive){
            getDB().txQueue.add(action)
        }else{
            action.action()
        }

    }

    fun <T> addListener(prop: KProperty<T>, listener: ChangeListener<T>){
        if(!listeners.containsKey(prop.name)){
            listeners[prop.name] = mutableListOf()
        }
        listeners[prop.name]!!.add(listener)
    }

    fun <T : Any?> addListener(listener: ChangeListener<T>){ //TODO Use AbstractObservable
        classListeners.add(listener)
    }

    override fun <O : Observable, T> key() : KProperty1<O, T>{
        return key as KProperty1<O, T>
    }

    override fun <O : Observable, T> keyValue() : T{
        return key<O, T>().get(this as O)
    }

    companion object{
        val key = Observable::uuid
    }

    internal fun <T> hookToObservable(obj: T, parentProperty: KProperty<*>?){
        if(obj is Observable){
            obj.addListener { childProp: KProperty<*>, old: T, new: T, levels: LevelInformation ->
                changed(childProp, old, new, levels.append(this, parentProperty))
            }
        } else if(obj is ObservableArrayList<*>){
            obj.addListener { args, levels ->
                changed(ObservableArrayList<*>::collection, args.elements[0], args.elements[0], levels.append(ObservableListLevel(obj, args)))
            }
        }
    }

    fun <T : Any?> observable(initialValue: T) : ReadWriteProperty<Any?, T>{

        hookToObservable(initialValue, null) //Is null atm, because there is technically no real way of getting the correct Property. But this should only be a outside api issue

        return object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T){
                changed(property, oldValue, newValue, LevelInformation(listOf(ObservableLevel(this@Observable, property))))
            }
        }
    }

    fun <S> observableList(vararg initialValues: S) : ReadWriteProperty<Any?, ObservableArrayList<S>>{

        val list = observableListOf(*initialValues)
        hookToObservable(list, null)

        return object : ObservableProperty<ObservableArrayList<S>>(list) {
            override fun afterChange(property: KProperty<*>, oldValue: ObservableArrayList<S>, newValue: ObservableArrayList<S>){
                changed(property, oldValue, newValue, LevelInformation(emptyList()))
            }
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