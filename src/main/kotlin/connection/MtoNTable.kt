package connection

import observable.Observable

class MtoNTable(private val m: Any, private val n: Any) : Observable(){
    fun <T> getMKey() : T{
        return m as T
    }

    fun <T> getNey() : T{
        return n as T
    }
}