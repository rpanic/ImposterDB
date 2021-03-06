package integration

import virtual.VirtualSet
import db.detached
import db.detachedSet
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

    var child by detached<Child>("children_1toM")

}

open class Parent : Observable(){

    var name by observable("")

    val children by detachedSet<Child>("children")

//    @Json(ignored = true)
//    var trait: Trait by detached<Trait>("trait")

}

open class Child : Observable(){

    var value by observable("")

}

open class SimpleObservableChildParent : Observable(){
    var s: String by observable("")

    var test: SimpleObservableChild? by observable(null)

}

open class SimpleObservableChild : Observable(){

    var s: String by observable("")
}