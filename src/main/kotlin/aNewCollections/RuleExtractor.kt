package aNewCollections

import observable.Observable
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class RuleExtractor<T : Observable>(val clazz: KClass<T>){ //, V: (T) -> Any

//    fun extract(): List<NormalizedCompareRule<Any>> {
//
//
//
//    }

    fun extractFilterRules(f: (T) -> Boolean): List<NormalizedCompareRule<Any>> {

        val conditions = mutableListOf<ComparisonRule>()
        val mock = RuleExtractionFramework.createMock(clazz){
            conditions += it
        }
        val ret = f(mock)

        return conditions.toList().map {
            val resolved = resolveNestedCondition(it)

            val actualCondition = resolved.second

            if(actualCondition is CompareComparisonRule<*>) {

                if(actualCondition.type == CompareType.EQUALS){

                    NormalizedCompareRule(resolved.first, actualCondition.obj2, if(ret) CompareType.EQUALS else CompareType.NOT_EQUALS)

                }else {

                    val results = listOf(ret) + f(mock) + f(mock)
                    val types = mapOf(listOf(false, true, false) to CompareType.EQUALS,
                            listOf(true, false, true) to CompareType.NOT_EQUALS,
                            listOf(true, false, false) to CompareType.GREATER,
                            listOf(true, true, false) to CompareType.GREATER_EQUALS,
                            listOf(false, false, true) to CompareType.LESS,
                            listOf(false, true, true) to CompareType.LESS_EQUALS)

                    actualCondition.type = types[results]

                    NormalizedCompareRule(resolved.first, actualCondition.obj2, actualCondition.type)
                }
            } else {
                throw IllegalStateException("Can't happen")
            }
        }
    }


    private fun resolveNestedCondition(condition : ComparisonRule) : Pair<List<KProperty1<*, *>>, ComparisonRule>{

        if(condition is NestedComparisonRule) {

            val props = mutableListOf<KProperty1<*, *>>()

            var last = condition as NestedComparisonRule
            while(true){
                props += last.prop
                if(last.condition is NestedComparisonRule){
                    last = last.condition as NestedComparisonRule
                }else{
                    return props to last.condition
                }
            }

        }else{
            return listOf<KProperty1<*, *>>() to condition
        }

    }

}