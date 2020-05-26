package ruleExtraction

import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions

inline infix fun <reified T : Any> T.eq(b2: T) = this.eq(b2, T::class)

@PublishedApi
internal fun <T : Any> T.eq(b2: T, clazz: KClass<T>) : Boolean{
//    if(this is Comparable<*> && b2 is Comparable<*>){

        val framework = RuleExtractionFramework.frameworks.first() //TODO find { this in it.mocks.values  }

        val parent = framework.mocks[this]!!
        framework.answerMethod(listOf(b2), clazz,
                clazz.memberFunctions.find { it.name == "equals" && it.parameters.size == 2 }!!,
                Boolean::class, parent)

//    }else{
//        throw IllegalAccessException("Eq can only be called by Permitted Types")
//    }
    return false //TODO Make gscheid
}

private fun <T : Any> createCloneMock(original: Any, clazz: KClass<T>): T {

    val framework = RuleExtractionFramework.findFrameworkByMock(original)

    val call = framework.mocks[original] ?: ConstantParameter(original) //If mock is not registered it can only be: someString() == "test"()

    val childmock = framework.mockRecursive(clazz, call)
    framework.mocks[childmock] = call

    return childmock
}

operator fun String.invoke() : StringClone {
    return createCloneMock(this, StringClone::class)
}

operator fun Double.invoke() : DoubleClone{
    return createCloneMock(this, DoubleClone::class)
}

operator fun Int.invoke() : IntClone{
    return createCloneMock(this, IntClone::class)
}

operator fun Byte.invoke() : ByteClone{
    return createCloneMock(this, ByteClone::class)
}

operator fun Long.invoke() : LongClone{
    return createCloneMock(this, LongClone::class)
}

operator fun Short.invoke() : ShortClone{
    return createCloneMock(this, ShortClone::class)
}

operator fun Float.invoke() : FloatClone{
    return createCloneMock(this, FloatClone::class)
}

operator fun Boolean.invoke() : BooleanClone{
    return createCloneMock(this, BooleanClone::class)  //Won´t work since value comparison at answerer[this] doesn´t work
}

operator fun Char.invoke() : CharClone{
    return createCloneMock(this, CharClone::class)
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