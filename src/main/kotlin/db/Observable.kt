package db

import com.beust.klaxon.Json
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties

typealias ChangeListener<T> = (prop: KProperty<*>, old: T, new: T, levels: LevelInformation) -> Unit

//typealias GeneralChangeListener = (prop: KProperty<*>, old: Any?, new: Any?) -> Unit

abstract class Observable{

    @Json(ignored = true)
    @Ignored
    val listeners = mutableMapOf<String, MutableList<ChangeListener<*>>>()

    @Json(ignored = true)
    @Ignored
    val classListeners = mutableListOf<ChangeListener<*>>()

    fun <T : Any?> changed(prop: KProperty<*>, old: T, new: T, levels: LevelInformation){
//        println("${prop.name}: $old -> $new")
        hookToObservable(new)

        val action = object : ObservableRevertableAction<T>(this, prop, old, new){

            override fun executeListeners(prop: KProperty<*>, old_p: T, new_p: T) {
                if(listeners.containsKey(prop.name)){
                    val list = listeners[prop.name]!! as List<ChangeListener<T>>
                    list.forEach { it(prop, old_p, new_p, levels) }
                }
                (classListeners as List<ChangeListener<T>>).toList().forEach { it(prop, old_p, new_p, levels) }
            }
        }

        if(DB.txActive){
            DB.txQueue.add(action)
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

    fun <T : Any?> addListener(listener: ChangeListener<T>){
        classListeners.add(listener)
    }

    private fun <T> hookToObservable(obj: T){
        if(obj is Observable){
            obj.addListener { prop: KProperty<*>, old: T, new: T, levels: LevelInformation ->
                changed(prop, old, new, levels.append(this))
            }
        } else if(obj is ObservableArrayList<*>){
            obj.addListener { elementChangeType, observable, levels ->
                changed(ObservableArrayList<*>::collection, observable, observable, levels.append(ObservableListLevel(obj, elementChangeType)))
            }
        }
    }

    fun <T : Any?> observable(initialValue: T) : ReadWriteProperty<Any?, T>{

        hookToObservable(initialValue)

        return object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T){
                changed(property, oldValue, newValue, LevelInformation(listOf(ObservableLevel(this@Observable))))
            }
        }
    }

    //TODO Why is observable a requirement for the type for the List?
    fun <S> observableList(vararg initialValues: S) : ReadWriteProperty<Any?, ObservableArrayList<S>>{

        val list = observableListOf(*initialValues)
        hookToObservable(list)

        return object : ObservableProperty<ObservableArrayList<S>>(list) {
            override fun afterChange(property: KProperty<*>, oldValue: ObservableArrayList<S>, newValue: ObservableArrayList<S>){

                if(newValue !is ObservableArrayList<*>){
                    if(property is KMutableProperty<*>){
                        val tranformedlist = ObservableArrayList(newValue)
                        property.setter.call(this@Observable, tranformedlist)
                    }else{
                        throw Exception("Property ${property.name} is not mutable but has by observable")
                    }
                } else
                    changed(property, oldValue, newValue, LevelInformation(emptyList()))
            }
        }

    }

}

abstract class ChangeObserver<T : Observable>(val t: T){

    init {
        this::class.functions.forEach {function ->
            val p = t::class.memberProperties.find { it.name == function.name }
            if(p != null){
                t.addListener(p){ prop, old, new, levels ->
                    if(old != new) {
                        if (function.parameters.size == 4){
                            function.call(this, old, new, levels)
                        } else if (function.parameters.size == 3) {
                            function.call(this, old, new)
                        } else if (function.parameters.size == 2) {
                            function.call(this, new)
                        }
                    }
                }
            }
            if(function.name == "all"){
                t.addListener<Any?>{ prop, old, new, levels ->
//                    if(old != new){ //TODO Implement equality checks the right way
                        if(function.parameters.size == 5){
                            function.call(this, prop, old, new, levels)
                        } else if(function.parameters.size == 4){  //TODO Can be optimized to call by parameter types
                            function.call(this, prop, old, new)
                        }else if(function.parameters.size == 3){
                            function.call(this, prop, new)
                        }else if(function.parameters.size == 2){
                            function.call(this, new)
                        }
//                    }
                }
            }
        }
    }

    fun getObserved() = t
}

abstract class ObservableRevertableAction<T>(val observable: Observable, val prop: KProperty<*>, val old: T, val new: T) : RevertableAction{

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