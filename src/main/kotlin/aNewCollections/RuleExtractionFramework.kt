package aNewCollections

import io.mockk.*
import observable.Observable
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

object RuleExtractionFramework {

    val mockList = mutableSetOf<Any>()
    val answerer = mutableMapOf<Any, CompareAnswer>()

    fun <T : Observable> rulesExtractor(clazz: KClass<T>): RuleExtractor<T> {
        return RuleExtractor(clazz)
    }

    fun <T : Any> createMock(clazz: KClass<T>, f: (NestedRule) -> Unit): T {

        val primitive = getPrimitivesValue(clazz, f)
        if(primitive != null){
            return primitive
        }

        val mock = mockkClass(clazz)

        val answer: MockKAnswerScope<Any, Any>.(Call) -> Any = { _ ->
            try {
                val prop = clazz.memberProperties.find { it.javaGetter?.name == method.name } as? KProperty1<Any, Any>

                println("Prop")

                if (prop == null) {

                    throw IllegalStateException("Shouldnt be, there is something wrong")

                } else {

                    val type = method.returnType
                    val ret = createMock(type as KClass<Any>) {
                        f(it.apply { props.add(prop) } )
                    }

                    ret
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        mock::class.memberProperties.forEach { prop ->
            val cache = Cache<Any>()
            every { (prop.javaGetter!!.invoke(mock)) } answers { call ->
                cache{ answer(this, call) }
            }
        }

        every { mock == any() } answers {
            f(NestedRule(mutableListOf(), CompareComparisonRule<Any>(mock, arg(0), CompareType.EQUALS)))
            true
        }

        if (mock is Comparable<*>) {
            val compareAnswer = CompareAnswer(mock, f)
            every { (mock as Comparable<Any>).compareTo(any()) } answers {
                compareAnswer.answer(this)
            }
        }

        mockList.add(mock)

        return mock
    }

    fun <T : Any> getPrimitivesValue(clazz: KClass<T>, f: (NestedRule) -> Unit) : T?{
        val randomValue: () -> Any? = { when(clazz){
            String::class -> UUID.randomUUID().toString()
            Int::class -> (Math.random() * Int.MAX_VALUE).toInt()
            Byte::class -> (Math.random() * Byte.MAX_VALUE).toByte()
            Long::class -> (Math.random() * Long.MAX_VALUE).toLong()
            Short::class -> (Math.random() * Short.MAX_VALUE).toShort()
            Char::class -> (Math.random() * Char.MAX_VALUE.toInt()).toChar()
            else -> null
        } }
        var value = randomValue()
        while(value in mockList){
            value = randomValue()
        }
        return value?.apply { mockList += this; answerer[this] = CompareAnswer(this, f) } as? T
    }

    class CompareAnswer(val mock: Any, val f: (NestedRule) -> Unit){

        var count = 0

        fun answer(scope: MockKAnswerScope<Int, Int>) : Int{
            f(NestedRule(mutableListOf(), CompareComparisonRule(mock, scope.arg(0))))

            val returns = listOf(1, 0, -1)
            count++
            return returns[count - 1]
        }

    }

}

class Cache <T> {
    var cache: T? = null

    operator fun invoke(f: () -> T) : T{

        if(cache == null){
            cache = f()
        }
        return cache!!
    }
}

operator fun String.compareTo(s : String) : Int{
    println("Hello")
    return 100
}

interface ComparisonRule {
}

open class NestedRule(
        val props: MutableList<KProperty1<Any, Any>>,
        val rule: ComparisonRule
) : ComparisonRule

open class MappingComparisonRule<T : Any, V : Any>(
        val obj: T
)

open class CompareComparisonRule<T : Any>(
        val obj: T,
        val obj2: T,
        var type: CompareType? = null
) : ComparisonRule

enum class CompareType {
    LESS, GREATER, LESS_EQUALS, GREATER_EQUALS, EQUALS, NOT_EQUALS
}
