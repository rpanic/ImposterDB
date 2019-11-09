package example

import db.ChangeObserver
import db.DB
import db.Observable
import db.ObservableArrayList
import kotlin.reflect.KProperty

class Person : Observable(){

    var name: String by observable("")

    var description: String? by observable(null)

    var traits: ArrayList<Trait> by observableList()

    var trait: Trait by observable(Trait())

}

class Trait : Observable(){

    var name: String by observable("")

    var value: Int by observable(0)

}

class PersonObserver(t: Person) : ChangeObserver<Person>(t){

    fun name(new: String){
        println("New name: $new!!!!")
    }

    fun all(prop: KProperty<Any?>, new: Any?){
        println("Prop ${prop.name} changed to $new")
    }

}

fun main() {

//    File(userdir().absolutePath + "/data/person.json").delete()

    val obj = DB.getObject("person") {
        Person()
    }

    PersonObserver(obj)

    obj.name = "John Miller"

    obj.description = "This is some random stuff"

    obj.trait = Trait()

    obj.trait.value = 10

    obj.traits.add(Trait())

    obj.traits[0].value = 1337

    println("Finished")

    val list = DB.getList<Person>("persons")

    val p1 = Person()

    list.add(p1)

    p1.name = "John Miller 2"
    p1.description = "Something"

    val trait = Trait()

    p1.traits.add(trait)

    trait.name = "Test Trait"

    val p2 = list.addAndReturn(Person())

    p2.name = "John Miller 3"
    p2.description = "Something else"

    // #####
    //look into data/tournaments.json
    // #####

}