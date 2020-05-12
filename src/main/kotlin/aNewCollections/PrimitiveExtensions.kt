package aNewCollections

import io.mockk.every
import io.mockk.mockk
import ruleExtraction.CompareComparisonRule
import ruleExtraction.CompareType
import ruleExtraction.NestedRule
import ruleExtraction.RuleExtractionFramework

private inline fun <reified V : Any, reified T : Comparable<V>> createMock(t: V) : T{
    val answerer = RuleExtractionFramework.answerer[t]!!
    val mock = mockk<T>()  //TODO Optimize, so that mock doesn´t get created every invoke call, even though behaviour stays the same, performance will increase
    every { mock.compareTo(any()) } answers {
        answerer.answer(this)
    }
    every { mock == any() } answers {
        answerer.f(NestedRule(mutableListOf(), CompareComparisonRule<Any>(mock, arg(0), CompareType.EQUALS)))
        true
    }
    return mock
}

infix fun <T : Any> T.eq(b2: T) : Boolean{
    if(this is Comparable<*> && b2 is Comparable<*>){
        var (mock, nonMock) = this to b2
        if(b2 in RuleExtractionFramework.answerer.keys){
            mock = b2
            nonMock = this
        }
        return createMock(mock) == nonMock
    }else{
        throw IllegalAccessException("Eq can only be called by Permitted Types")
    }
}

operator fun String.invoke() : StringClone {
    return createMock(this)
}

operator fun Double.invoke() : DoubleClone{
    return createMock(this)
}

operator fun Int.invoke() : IntClone{
    return createMock(this)
}

operator fun Byte.invoke() : ByteClone{
    return createMock(this)
}

operator fun Long.invoke() : LongClone{
    return createMock(this)
}

operator fun Short.invoke() : ShortClone{
    return createMock(this)
}

operator fun Float.invoke() : FloatClone{
    return createMock(this)
}

operator fun Boolean.invoke() : BooleanClone{
    return createMock(this) //Won´t work since value comparison at answerer[this] doesn´t work
}

operator fun Char.invoke() : CharClone{
    return createMock(this)
}

interface StringClone : Comparable<String>, CharSequence
interface IntClone : Comparable<Int>, CharSequence
interface DoubleClone : Comparable<Double>, CharSequence
interface ByteClone : Comparable<Byte>, CharSequence
interface LongClone : Comparable<Long>, CharSequence
interface ShortClone : Comparable<Short>, CharSequence
interface FloatClone : Comparable<Float>, CharSequence
interface BooleanClone : Comparable<Boolean>, CharSequence
interface CharClone : Comparable<Char>