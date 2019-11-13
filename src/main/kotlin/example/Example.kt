package example

import db.ChangeObserver
import db.DB
import json.JsonBackend
import db.Observable

class Person : Observable(){

    var name: String by observable("")

    var description: String? by observable(null)

}

class PersonObserver(t: Person) : ChangeObserver<Person>(t){

    fun name(new: String){
        println("New name: $new!!!!")
    }

    fun all(new: Any?){
        println("Prop changed $new")
    }

}

fun main() {

    DB.primaryBackend = JsonBackend()

    val obj = DB.getObject("person") {
        Person()
    }

    obj.name = "John Miller"

    obj.description = "This is some random stuff"

    val list = DB.getList<Person>("persons")

    val p1 = Person()

    list.add(p1)

    p1.name = "John Miller 2"
    p1.description = "Something"

    val p2 = list.add(Person())

    p2.name = "John Miller 3"
    p2.description = "Something else"

    // #####
    //look into data/tournaments.json
    // #####

}