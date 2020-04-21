package example

import com.beust.klaxon.Json
import db.ChangeObserver
import db.DB
import db.detached
import db.detachedSet
import json.JsonBackend
import observable.LevelInformation
import observable.Observable
import kotlin.reflect.KProperty

class Person : Observable(){

    var name: String by observable("")

    var description: String? by observable(null)

    val traits by detachedSet<Trait>("traits")

    var trait: Trait by detached<Trait>("trait")

    //var trait: Trait by relation()

}

class Trait : Observable(){

//    var name: String by observable("")

    var value: Int by observable(0)

}

class ExamplePersonObserver(t: Person) : ChangeObserver<Person>(t){

    fun name(new: String){
        println("New name: $new!!!!")

//        throw IllegalAccessException()
    }

    fun all(prop: KProperty<Any?>, new: Any?, old: Any?, levelInformation: LevelInformation){
        println("Prop ${prop.name} changed to $new")

        println(levelInformation)
    }

}

fun main() {

//    userdir().resolve("data").listFiles()?.forEach { it.delete() }
//    File(userdir().absolutePath + "/data/persons.json").delete()
    val db = DB()
    db += JsonBackend()

    val list = db.getSet<Person>("persons")

    val view = list.view()

    val traits = view.first().traits.view()

    println(traits.size == 1)

    val person = Person()
    list.add(person)

    person.name = "test"
    val trait = Trait()
    person.traits.add(trait)

    //Test2: Remove Relation
//    list[0].traits.removeAt(1)

//    list[0].traits.add(Trait().apply { value = 100 })

    println()

    //TODO Collect all the i.e. Traits in Complete List, so that no double references are there and to remove that hideous JsonBackend stuff

    //TODO Test what happens if you add an existing Reference, if that gets added again or not

//    val obj = DB.getDetached("person", "asd", false) {
//        println("Person init")
//        Person().apply { uuid = "asd" }
//    }

//    obj.addListener { prop: KProperty<*>, old: Any?, new: Any?, levels: LevelInformation ->
//        println("person listener")
//    }

//    obj.trait = Trait()
//    val trait2 = obj.traitc

//    trait2.addListener { prop: KProperty<*>, old: Any?, new: Any?, levels: LevelInformation ->
//        println("trait listener")
//    }
//
//    trait2.value = 1999



//    obj.trait = Trait()

//    val trait = obj.trait

//    println(obj.key<UUID>())
//
//    println(obj.trait.uuid)
//
//    val trait = obj.trait

//    obj.description = "asd"

//    obj.trait = Trait()

//    obj.trait.value = 1337

//    obj.name = "asdsadg"

//    obj.trait.value = 13387

//    val trait = obj::trait
//    trait.isAccessible = true
//    val delegate = trait.getDelegate()

    println()

//    DB.tx {
//
//        ExamplePersonObserver(obj)
//
//        obj.description = "This is some random stuff"
//
//        obj.trait = Trait()
//
//        obj.trait.value = 10
//
//        obj.traits.add(Trait())
//
//        obj.traits[0].value = 1337
//
//        obj.name = "John Miller"
//
//        println("Finished")
//
//    }

//    val mapped = obj.traits.map ({ "${it.value}Heyo" })
//    {
//        Trait().apply { value = it.replace("Heyo", "").toInt() }
//    }
//
//            println(mapped.collection)
//
//            obj.traits.add(Trait().apply { value = 110 })
//
//            println(mapped.collection)

//    val list = DB.getList<Person>("persons")
//
//    val p1 = Person()
//
//    list.add(p1)
//
//    p1.name = "John Miller 2"
//    p1.description = "Something"
//
//    val trait = Trait()
//
//    p1.traits.add(trait)
//
//    trait.name = "Test Trait"
//
//    val p2 = list.addAndReturn(Person())
//
//    p2.name = "John Miller 3"
//    p2.description = "Something else"

    // #####
    //look into data/tournaments.json
    // #####

}