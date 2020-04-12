package test

import com.beust.klaxon.Json
import db.detached
import db.detachedList
import observable.Observable

class TestObject() : Observable(){
    var testProperty: String by observable("")

    constructor(s: String) : this(){
        testProperty = s
    }
}

open class OneToManyParent : Observable(){

    var name by observable("")

    var description: String? by observable(null)

    @Json(ignored = true)
    var child by detached<Child>("children_1toM")

}

open class Parent : Observable(){

    var name by observable("")

    @Json(ignored = true)
    val children by detachedList<Child>("children")

//    @Json(ignored = true)
//    var trait: Trait by detached<Trait>("trait")

}

open class Child : Observable(){

    var value by observable("")

}