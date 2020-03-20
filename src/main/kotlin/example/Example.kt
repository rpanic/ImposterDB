package example

import com.beust.klaxon.Json
import db.*
import json.JsonBackend
import json.userdir
import observable.LevelInformation
import observable.Observable
import observable.ObservableArrayList
import java.io.File
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible

class Person : Observable(){

    var name: String by observable("")

    var description: String? by observable(null)

//    var traits: ObservableArrayList<Trait> by observableList()

    @Json(ignored = true)
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

//    File(userdir().absolutePath + "/data/person.json").delete()
    DB.primaryBackend = JsonBackend()

    val obj = DB.getDetached("person", "asd", false) {
        Person().apply { uuid = "asd" }
    }

    obj.addListener { prop: KProperty<*>, old: Any?, new: Any?, levels: LevelInformation ->
        println("")
    }

    obj.trait = Trait()
    val trait2 = obj.trait
    trait2.value = 1299

//    obj.trait = Trait()

//    val trait = obj.trait

    println(obj.key<UUID>())

    println(obj.trait.uuid)

    val trait = obj.trait

    obj.description = "asd"

//    obj.trait = Trait()

    obj.trait.value = 1337

    obj.name = "asdsadg"

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