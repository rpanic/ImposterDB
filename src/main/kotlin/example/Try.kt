package example

import db.DB
import io.zeko.model.Entity
import observable.Observable
import ruleExtraction.*
import sql.SqlBackend

class RuleExtractorTest{
    fun getOne() = RuleExtractorTestChild()

    fun getStr() = "hello"
}

class RuleExtractorTestChild{
    fun getTwo(s: String) = RuleExtractorTest3(s)
}

class RuleExtractorTest3(val x: String){

    fun getThree() = x

}

fun callsToString(list: List<Parameterable>) : String{
    return list.map { call ->
        when(call){
            is InputObject<*> -> "x"//"Input ${call.clazz.simpleName}"
            is Call<*> -> "${callsToString(listOf(call.parent))}${""/*call.clazz.simpleName*/}.${call.method.name}(${call.parameters.map { callsToString(listOf(it)) }.joinToString(", ") })"
            is ConstantParameter<*> -> "${call.value}"
            else -> ""
        }
    }.joinToString("\n")
}

fun main2(){

//    var calls = RuleExtractionFramework(RuleExtractorTest::class)
//            .getAllExecutedFunctions {     it.getOne().getTwo(it.getStr()).getThree()     }
//
//    println("Lambda1: " + callsToString(calls))

    var calls = RuleExtractionFramework(RuleExtractorTest::class)
//            .getAllExecutedFunctions { it.getOne() eq RuleExtractorTestChild() }
            .getAllExecutedFunctions { it.getStr()() == "hallo"() }

    println("Lambda2: " + callsToString(calls))


    System.exit(0)

//    val db = DB();
//    val backend = SqlBackend();
//    db += backend;

//    val set = db.getSet<>("asd")

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
    open var s: String by observable("")
    open var b: Int by observable(10)

//    companion object{
//        val key = Test::s
//    }

    open var test: Test2? by observable(null)

}

open class Test2 : Observable(){

    open var s2: String by observable("")
}