package db

import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties

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
                        if (function.parameters.size == 4){
                            function.call(reciever, old, new, levels)
                        } else if (function.parameters.size == 3) {
                            function.call(reciever, old, new)
                        } else if (function.parameters.size == 2) {
                            function.call(reciever, new)
                        }
                    }
                }
            }
            if(function.name == "all"){
                t.addListener<Any?>{ prop, old, new, levels ->
                    //                    if(old != new){ //TODO Implement equality checks the right way
                    if(function.parameters.size == 5){
                        function.call(reciever, prop, old, new, levels)
                    } else if(function.parameters.size == 4){  //TODO Can be optimized to call by parameter types
                        function.call(reciever, prop, old, new)
                    }else if(function.parameters.size == 3){
                        function.call(reciever, prop, new)
                    }else if(function.parameters.size == 2){
                        function.call(reciever, new)
                    }
//                    }
                }
            }
        }
    }

    fun getObserved() = t
}