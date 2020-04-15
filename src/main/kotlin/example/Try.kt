package example

import aNewCollections.invoke
import observable.Observable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

fun main() {

    val x = ""()
    println()

}

open class Test : Observable(){
    open var s: String by observable("")

    open var test: Test2? by observable(null)

}

open class Test2 : Observable(){

    open var s: String by observable("")
}