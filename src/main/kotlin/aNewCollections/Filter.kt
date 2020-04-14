package aNewCollections

import io.mockk.*
import observable.Observable
import java.lang.Exception
import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException
import java.util.*
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

//                println("Equals")
//                if (this.method.name == "equals" && method.paramTypes.size == 1 && this.method.paramTypes[0] == Any::class) {
//                }
//                true
                throw IllegalStateException("Shouldnt be, there is something wrong")

            }else {

                val type = method.returnType
                val ret = getMock(type as KClass<Any>) {
                    f(NestedFilterCondition(prop, it))
                }

                ret

               /* val retFunction = {
                    if (0.toDouble().javaClass.isAssignableFrom(type)) {
                        Math.random()
                    } else if ("".javaClass.isAssignableFrom(type)) {
                        UUID.randomUUID().toString()
                    } else if (0.javaClass.isAssignableFrom(type)) {
                        (Math.random() * Int.MAX_VALUE).toInt()
                    } else if (0.toLong().javaClass.isAssignableFrom(type)) {
                        (Math.random() * Long.MAX_VALUE).toLong()
                    } else if (0.toByte().javaClass.isAssignableFrom(type)) {
                        (Math.random() * Byte.MAX_VALUE).toByte()
                    } else if (0.toShort().javaClass.isAssignableFrom(type)) {
                        (Math.random() * Short.MAX_VALUE).toShort()
                    } else if (' '.javaClass.isAssignableFrom(type)) {
                        (Math.random() * Char.MAX_VALUE.toInt()).toChar()
                    } else if (Observable::class.java.isAssignableFrom(type)) {
                        getMock(type.kotlin as KClass<Observable>)
                    } else {
                        throw UnsupportedOperationException("Objects with fields of unsupported Types cannot be used with Virutalized Operations")
                    }
                }
                var ret = retFunction()

                while (referenceMap.containsKey(ret)) {
                    ret = retFunction()
                }

                referenceMap.put(ret, prop)

                ret*/
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

    mockList.add(mock)

    return mock
}

//fun <T : Observable> performFilter(clazz: KClass<T>, obj: T, f: (T) -> Boolean) {
//    val mock = getMock(clazz)
//    val filters = mutableListOf<FilterCondition>()
//    whenever(mock == any()).then {
//        filters.add(EqualsFilterCondition<Any>(it.arguments[0], it.arguments[1]))
//        true
//    }
//    (filters[0] as EqualsFilterCondition<Any>).eq = f(mock)
//}

open class Test : Observable(){
    open var s: String by observable("")

    open var test: Test2? by observable(null)

}

open class Test2 : Observable(){

    open var s: String by observable("")
}

interface FilterCondition

open class NestedFilterCondition(
        val prop: KProperty1<Any, Any>,
        val condition: FilterCondition
) : FilterCondition

open class EqualsFilterCondition<T>(
        val obj: T,
        val obj2: T,
        var eq: Boolean = true
) : FilterCondition{

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