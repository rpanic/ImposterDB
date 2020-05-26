package sql

import io.zeko.db.sql.Query
import io.zeko.db.sql.aggregations.count
import io.zeko.db.sql.dsl.*
import ruleExtraction1.*

object SqlStepInterpreter {

    fun interpretSteps(query: Query, steps: List<Step<*, *>>) : String{
    
        steps.forEach { step ->
            evaluateStep(step, query)
        }
        
        return query.toSql()
    }
    
    private fun evaluateStep(step: Step<*, *>, query: Query) {
        
        if(step is FilterStep<*>){
            step.conditions.forEach { condition ->
                if(condition is NormalizedCompareRule<*>) {
                    val field = getSqlFieldName(condition.prop!!)
                    val value = condition.obj2!!
                    val stringValue = "?".replaceWildCards(value)
                    when(condition.type){
                        CompareType.EQUALS -> query.where(io.zeko.db.sql.operators.eq(field, stringValue, true))
                        CompareType.NOT_EQUALS -> query.where(io.zeko.db.sql.operators.neq(field, stringValue, true))
                        CompareType.LESS -> query.where(field less stringValue)
                        CompareType.LESS_EQUALS -> query.where(field lessEq stringValue)
                        CompareType.GREATER -> query.where(field greater stringValue)
                        CompareType.GREATER_EQUALS -> query.where(field greaterEq stringValue)
                    }
                }
            }
        }else if(step is FindStep<*>){
            query.limit(1)
            evaluateStep(step.filter, query)
        }else if(step is MappingStep<*, *>){ //TODO Make this multi-level, f.e. UPPER_CASE(COUNT(*)) or CONCAT('Size: ', COUNT(*))
            when(step.type){
                MappingType.COUNT -> query.fields(count("*").getStatement())
            }
        }
    }

}