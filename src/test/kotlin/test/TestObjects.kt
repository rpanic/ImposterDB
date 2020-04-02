package test

import com.beust.klaxon.Json
import db.detachedList
import observable.Observable

class TestObject() : Observable(){
    var testProperty: String by observable("")

    constructor(s: String) : this(){
        testProperty = s
    }
}

class Parent : Observable(){

    var name by observable("")

    @Json(ignored = true)
    val children by detachedList<Child>("testChildren")

//    @Json(ignored = true)
//    var trait: Trait by detached<Trait>("trait")

}

class Child : Observable(){

    var value by observable("")

}