package unit

import io.zeko.db.sql.Query
import observable.Observable
import org.assertj.core.api.Assertions
import org.junit.Test
import ruleExtraction1.*
import sql.SqlStepInterpreter

class SqlStepInterpreterTest {
    
    @Test
    fun testFilter(){
        
        val steps = listOf<Step<*, *>>(
                FilterStep<QueryObject>(listOf(NormalizedCompareRule(listOf(QueryObject::one), "filterValue", CompareType.NOT_EQUALS))),
                FilterStep<QueryObject>(listOf(NormalizedCompareRule(listOf(QueryObject::two), "twoValue", CompareType.LESS_EQUALS)))
        )
        
        testInterpretSteps(steps, "SELECT one, two FROM table1 WHERE one != 'filterValue' AND two <= 'twoValue'")
        
    }
    
    @Test
    fun testFind(){
    
        val steps = listOf<Step<*, *>>(
                FindStep(
                    FilterStep<QueryObject>(listOf(NormalizedCompareRule(listOf(QueryObject::one), "filterValue", CompareType.NOT_EQUALS)))
                )
        )
    
        testInterpretSteps(steps, "SELECT one, two FROM table1 WHERE one != 'filterValue' LIMIT 1 OFFSET 0")
    }
    
    @Test
    fun testCount(){
        
        val steps = listOf<Step<*, *>>(
                MappingStep<QueryObject, Int>(
                        MappingType.COUNT
                )
        )
        
        testInterpretSteps(steps, "SELECT COUNT(  *  ) FROM table1")
        
    }
    
    private fun testInterpretSteps(steps: List<Step<*, *>>, expected: String){
    
        val sql = SqlStepInterpreter.interpretSteps(getQuery(), steps)
        println(sql)
        Assertions.assertThat(sql).isEqualTo(expected)
        
    }
    
    private fun getQuery() = Query()
            .fields("one", "two")
            .from("table1")
    
}

class QueryObject : Observable(){
    val one by observable("")
    val two by observable("")
}