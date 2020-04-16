package aNewCollections

import java.lang.UnsupportedOperationException
import kotlin.reflect.KProperty1

object StepInterpreter {

    fun <T : Any> interpretFilter(step: FilterStep<T>, t: T) : Boolean{
        return step.conditions.map {
            val rule = (it as NormalizedCompareRule<T>)

            var last: Any = t
            rule.prop?.forEach {
                last = (it as KProperty1<Any, Any>).get(last)
            }
            val obj = last

            val res = when(rule.type) {
                CompareType.EQUALS -> obj == rule.obj2
                CompareType.NOT_EQUALS -> obj != rule.obj2
                else -> null
            }

            if(obj is Comparable<*>){
                (obj as Comparable<Any>).apply {
                    when(rule.type) {
                        CompareType.GREATER -> obj > rule.obj2
                        CompareType.GREATER_EQUALS -> obj >= rule.obj2
                        CompareType.LESS -> obj < rule.obj2
                        CompareType.LESS_EQUALS -> obj <= rule.obj2
                        else -> throw UnsupportedOperationException("Not possible")
                    }
                }
            }
            res!!
        }.reduce { o1, o2 -> o1 && o2 }
    }

}