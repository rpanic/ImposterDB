package example

import ruleExtraction.*

class RuleExtractorTest{
    fun getOne() = RuleExtractorTestChild()

    fun getStr() = "hello"

    val prop = "eyyy"
}

class RuleExtractorTestChild{
    fun getTwo(s: String) = RuleExtractorTest3(s)
}

class RuleExtractorTest3(val x: String){

    fun getThree() = x

}

fun main(args: Array<String>) {

//    var calls = RuleExtractionFramework(RuleExtractorTest::class)
//            .getAllExecutedFunctions {     it.getOne().getTwo(it.getStr()).getThree()     }
//
//    println("Lambda1: " + callsToString(calls))

    var calls = RuleExtractionFramework(RuleExtractorTest::class)
//            .getAllExecutedFunctions { it.getOne() eq RuleExtractorTestChild() }
            .getAllExecutedFunctions { it.prop() == "hallo"() }
    //TODO This won´t work since eq returns false, therefore the second part doesn´t get called
//            .getAllExecutedFunctions { it.prop() eq "hallo" && it.getStr() eq "asd" }

//    calls.filter {  }

    println("Lambda2: " + callsToString(calls))


    System.exit(0)

//    val db = DB();
//    val backend = SqlBackend();
//    db += backend;

//    val set = db.getSet<>("asd")

}

fun callsToString(list: List<Parameterable>) : String{
    return list.map { call ->
        when(call){
            is InputObject<*> -> "x"//"Input ${call.clazz.simpleName}"
            is Call<*> -> {
                val args = if(call is FunctionCall<*>){
                    "(${ call.parameters.map { callsToString(listOf(it)) }.joinToString(", ") })"
                }else{
                    ""
                }
                "${callsToString(listOf(call.parent))}${""/*call.clazz.simpleName*/}.${call.callable.name}$args"
            }
            is ConstantParameter<*> -> "${call.value}"
            else -> ""
        }
    }.joinToString("\n")
}