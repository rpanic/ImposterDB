package db

import observable.GenericChangeObserver
import observable.LevelInformation
import observable.Observable
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

abstract class ChangeObserver<T : Observable>(val t: T){

    init {
        init(this)
    }

    //This seperation of Interface definition and Callback reciever is because of mockability of Imposters
    protected fun init(reciever: Any){
        reciever::class.functions.forEach {function ->
            val p = t::class.memberProperties.find { it.name == function.name }
            if(p != null){
                t.addListener(p){ prop, old, new, levels ->
                    if(old != new) {
                        val parameters = parseParameters(function.parameters.drop(1), prop, old, new, levels)
                        function.call(reciever, *parameters.toTypedArray())
                    }
                }
            }
            if(function.name == "all"){
                t.addListener<Any?>{ prop, old, new, levels ->
                    //                    if(old != new){ //TODO Implement equality checks the right way
                    val parameters = parseParameters(function.parameters.drop(1), prop, old, new, levels)
                    function.call(reciever, *parameters.toTypedArray())
//                    }
                }
            }
        }
    }

    private fun parseParameters(functionParameters: List<KParameter>, prop: KProperty<*>, old: Any?, new: Any?, levels: LevelInformation) : List<Any?>{
        val parameters = mutableListOf<Any?>()
        var oldIndex = -1
        var oldWanted = false
        functionParameters.forEach {
            if(it.type.jvmErasure == KProperty::class){
                parameters += prop
            }else if(it.type.jvmErasure == LevelInformation::class) {
                parameters += levels
            }else if(prop.returnType.jvmErasure.isSubclassOf(it.type.jvmErasure)){
                parameters += if(oldIndex == -1){
                    oldIndex = parameters.size
                    old
                } else {
                    oldWanted = true
                    new
                }
            }else{
                throw IllegalArgumentException("Type ${prop.returnType.jvmErasure.qualifiedName} cannot be casted to ${it.type.jvmErasure.qualifiedName}\nCheck your imposter methods for correct Parameter types")
            }
        }
        if(!oldWanted && oldIndex != -1) {
            parameters[oldIndex] = new
        }
        return parameters
    }

    fun getObserved() = t
}