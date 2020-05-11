package aNewCollections

import kotlin.reflect.KProperty1

interface Step<T, V>

class FilterStep<T> (
        val conditions: List<NormalizedExtractedRule>
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

class MtoNRule<T>() : Step<T, T> //TODO Make Generic work reasonable

interface NormalizedExtractedRule

open class NormalizedCompareRule<T>(
        val prop: List<KProperty1<*, *>>?,
//        val obj1: Any?, //TODO Enable cases where the mock is no used at all
        val obj2 : T,
        var type: CompareType? = null
) : NormalizedExtractedRule


class MapStep<T, V> : Step<T, V> //Add fields