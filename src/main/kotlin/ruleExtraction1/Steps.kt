package ruleExtraction1

import ruleExtraction.Call
import kotlin.reflect.KProperty1

interface Step<T, V>

class FilterStep<T> (
        val conditions: List<ExtractedRule>
) : Step<T, T>

class FindStep<T> (
        val filter: FilterStep<T>
) : Step<T, T>

class MappingStep<T, V>(
    val type: MappingType
) : Step<T, V>

enum class MappingType {
    COUNT
}

class SortingStep<T, V>(
    val calls: List<Call<*>>
) : Step<T, V>

class MtoNRule<T>() : Step<T, T> //TODO Make Generic work reasonable

interface ExtractedRule

open class CompareRule<T>(
        val prop: List<KProperty1<*, T>>?,
//        val obj1: Any?, //TODO Enable cases where the mock is no used at all
        val obj2 : T,
        var type: CompareType? = null
) : ExtractedRule

