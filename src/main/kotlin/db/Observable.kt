package db

import com.beust.klaxon.Json
import java.lang.reflect.Proxy
import kotlin.reflect.KProperty
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.system.measureTimeMillis

typealias ChangeListener<T> = (prop: KProperty<*>, old: T, new: T) -> Unit

typealias GeneralChangeListener = (prop: KProperty<*>, old: Any?, new: Any?) -> Unit

abstract class Observable{

    @Json(ignored = true)
    val listeners = mutableMapOf<String, MutableList<ChangeListener<*>>>()

    @Json(ignored = true)
    val classListeners = mutableListOf<GeneralChangeListener>()

    fun <T : Any?> changed(prop: KProperty<*>, old: T, new: T){
        println("${prop.name}: $old -> $new")
        if(listeners.containsKey(prop.name)){
            val list = listeners[prop.name]!! as List<ChangeListener<T>>
            list.forEach {
                DB.changed[(it as ChangeListener<Any?>)] = ChangeProperty(prop,old,new)
            }
        }

        (classListeners as List<ChangeListener<T>>).forEach {
            DB.changed[it as ChangeListener<Any?>] = ChangeProperty(prop,old,new)
        }
    }

    fun <T> addListener(prop: KProperty<T>, listener: ChangeListener<T>){
        if(!listeners.containsKey(prop.name)){
            listeners[prop.name] = mutableListOf()
        }
        listeners[prop.name]!!.add(listener)
    }

    fun <T> addListener(listener: GeneralChangeListener){
        classListeners.add(listener)
    }

    fun <T : Any?> observable(initialValue: T) : ReadWriteProperty<Any?, T>{
        return object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = changed(property, oldValue, newValue)
        }
    }

}

abstract class ChangeObserver<T : Observable>(val t: T){

    init {
        this::class.functions.forEach {function ->
            val p = t::class.memberProperties.find { it.name == function.name }
            if(p != null){
                t.addListener(p){ prop, old, new ->
                    if(old != new) {
                        if (function.parameters.size == 3) {
                            function.call(this, old, new)
                        } else if (function.parameters.size == 2) {
                            function.call(this, new)
                        }
                    }
                }
            }
            if(function.name == "all"){
                t.addListener<Any>{ prop, old, new ->
                    if(old != new){
                        if(function.parameters.size == 4){  //TODO Can be optimized to call by parameter types
                            function.call(this, prop, old, new)
                        }else if(function.parameters.size == 3){
                            function.call(this, prop, new)
                        }else if(function.parameters.size == 2){
                            function.call(this, new)
                        }
                    }
                }
            }
        }
    }

    fun getObserved() = t
}