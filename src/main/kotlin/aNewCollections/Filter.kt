package aNewCollections

import com.sun.org.apache.xpath.internal.operations.Bool
import io.mockk.*
import observable.Observable
import java.lang.Exception
import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException
import java.util.*
import kotlin.Comparator
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter


fun <T : Observable> getFilter(clazz: KClass<T>) : T{
    val mock = mockkClass(clazz)

    mock::class.memberProperties.forEach { prop ->
        every { (prop.javaGetter!!.invoke(mock)) } returns ""
    }
    return mock
}

val mockList = mutableSetOf<Any>()

fun <T : Any> getMock(clazz: KClass<T>, f: (FilterCondition) -> Unit) : T{

    val mock = mockkClass(clazz)

    val answer : MockKAnswerScope<Any, Any>.(Call) -> Any = { invocation ->
        try {
            val prop = clazz.memberProperties.find { it.javaGetter?.name == method.name} as? KProperty1<Any, Any>

            println("Prop")

            if(prop == null){

                throw IllegalStateException("Shouldnt be, there is something wrong")

            } else {

                val type = method.returnType
                val ret = getMock(type as KClass<Any>) {
                    f(NestedFilterCondition(prop, it))
                }

                ret
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    mock::class.memberProperties.forEach { prop ->
        every { (prop.javaGetter!!.invoke(mock)) } answers answer
    }

    every { mock == any() } answers {
        f(EqualsFilterCondition<Any>(mock, arg(0)))
        true
    }

    if(mock is Comparable<*>){
        var count = 0
        every { (mock as Comparable<Any>).compareTo(any()) } answers {

            f(CompareFilterCondition(mock, arg(0)))

            if(count == 0) {
                count++
                1
            } else {
                0
            }
        }
    }

    mockList.add(mock)

    return mock
}

open class Test : Observable(){
    open var s: String by observable("")

    open var test: Test2? by observable(null)

}

open class Test2 : Observable(){

    open var s: String by observable("")
}

interface FilterCondition {
}

open class NestedFilterCondition(
        val prop: KProperty1<Any, Any>,
        val condition: FilterCondition
) : FilterCondition

open class EqualsFilterCondition<T>(
        val obj: T,
        val obj2: T,
        var eq: Boolean = true
) : FilterCondition

open class CompareFilterCondition<T>(
    val obj: T,
    val obj2 : T,
    var type: CompareType? = null
) : FilterCondition

enum class CompareType{
    LESS, GREATER, LESS_EQUALS, GREATER_EQUALS, EQUALS, NOT_EQUALS
}

fun main() {

    val set = VirtualSet({Test()}, {}, listOf(), Test::class)

    val mock = getMock(Test::class){
        println(it)
    }

    val test = mock.test

//    println(test.hashCode())

    println(test == Test2())

    val set2 = set.filter { it.test == Test2() }
//    println(set2)

//    val mock = mock<Test>()
//    println(mock)
//        on { s }.thenReturn("")

//    val mock2 = getMock(Test::class)

//    println(mock2.s)
//    println(mock2.test)
//    println(mock2.test?.s)

//    whenever(mock.s).thenReturn("")

//    mock::class.memberProperties.forEach { prop ->
//        whenever((prop as KProperty1<Test, String>).invoke(mock)).thenReturn("")
//    }

    val t = Test()
//    println(t.s)
//    val obj = getFilter(Test::class)

//    println(obj.s)

}