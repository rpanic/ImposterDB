package ruleExtraction1

import kotlin.reflect.KClass

class RuleExtractor<T : Any>(val clazz: KClass<T>){ //, V: (T) -> Any

//    fun extract(): List<CompareRule<Any>> {
//
//
//
//    }

    fun extractFilterRules(f: (T) -> Boolean): List<CompareRule<Any>> {

        val conditions = mutableListOf<ComparisonRule>()
        val mock = RuleExtractionFramework.createMock(clazz) {
            conditions += it
        }
        val ret = f(mock)

        return conditions.toList().map {

            val rule = it as NestedRule
            rule.props.reverse()

            val actualCondition = rule.rule

            if(actualCondition is CompareComparisonRule<*>) {

                if(actualCondition.type == CompareType.EQUALS){
    
                    CompareRule(rule.props, actualCondition.obj2, if (ret) CompareType.EQUALS else CompareType.NOT_EQUALS)

                }else {

                    val results = listOf(ret) + f(mock) + f(mock)
                    val types = mapOf(listOf(false, true, false) to CompareType.EQUALS,
                            listOf(true, false, true) to CompareType.NOT_EQUALS,
                            listOf(true, false, false) to CompareType.GREATER,
                            listOf(true, true, false) to CompareType.GREATER_EQUALS,
                            listOf(false, false, true) to CompareType.LESS,
                            listOf(false, true, true) to CompareType.LESS_EQUALS)

                    actualCondition.type = types[results]
    
                    CompareRule(rule.props, actualCondition.obj2, actualCondition.type)
                }
            } else {
                throw IllegalStateException("Can't happen")
            }
        }
    }

}