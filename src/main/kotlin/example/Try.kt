package example

import db.DB
import io.mockk.every
import io.mockk.mockk
import io.zeko.model.Entity
import observable.LevelInformation
import observable.Observable
import ruleExtraction.*
import sql.SqlBackend
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties

fun main2(){

    //TODO Create Test for this logic (lazyObservable)

    val obj = Test()

    obj.addListener { prop: KProperty<*>, old: Any?, new: Any, levels: LevelInformation ->
        println("$old -> $new")
    }

    obj.s = "Hallo"

    obj.s.print()

}

fun main3(){
    
    val db = DB()
    val backend = SqlBackend()
    db += backend
    
    val virtualSet = db.getSet<Person>("person")
    
    val person = Person()
    person.name = "Raphael Panic"
    
}

fun main() {

    main2()

    System.exit(0)
    
    mstart()
    val db = DB(); mpoint()
    val backend = SqlBackend(); mpoint()
    db += backend; mpoint()

    val virtualSet = db.getSet<Person>("person"); mpoint()

    //Part 1
    val person = Person()
    person.name = "Raphael Panic"
//    person.trait = Trait().apply { value = 98 }

    virtualSet.add(person)

    person.traits.add(Trait().apply { this.value = 111 })

    //Part 2

//    val loaded = virtualSet.view(); mpoint()
//    println(loaded.size == 1); mpoint()
//    val obj = loaded.first(); mpoint()
//    obj.description = "Desc2"; mpoint()
//    obj.traits.view().first().value = 1234; mpoint()

    TimeMeasurer.dump()
}

class GenericEntity : Entity {
    constructor(map: Map<String, Any?>) : super(map)
    constructor(vararg props: Pair<String, Any?>) : super(*props)
    var test: String? by map
}

open class Test : Observable(){
    open var s: String by lazyObservable()
    open var b: Int by observable(10)

//    companion object{
//        val key = Test::s
//    }

    open var test: Test2? by observable(null)

}

open class Test2 : Observable(){

    open var s2: String by observable("")
}