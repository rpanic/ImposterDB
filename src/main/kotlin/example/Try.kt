package example

import aNewCollections.invoke
import observable.Observable

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