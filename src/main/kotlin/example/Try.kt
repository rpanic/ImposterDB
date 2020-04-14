package example

import io.mockk.every
import io.mockk.mockk
import observable.Observable
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaGetter

fun main() {

//    val p = Person()
//
//    val prop = Person::trait
//    prop.isAccessible = true
//    val delegate = prop.getDelegate(p)
//    println()

    val test = mockk<Test>()
//    every { test.s } returns "Mocked"
//    every { test == any() } answers {
//        println("Mocked equals!!")
//        true
//    }
    test::class.memberProperties.forEach { prop ->
        every { ((prop as KProperty1<Any, Any>).get(test)) } answers {
            println("Mocked!!")
        }
    }
//    every { test.equals(any()) } answers {"Mocked"}
//    test::class.memberFunctions.forEach { f ->
//        every {
//            if(f.parameters.size == 0) {
//                f.call(test)
//            }else{
//                f.call(test, *((0 until (f.parameters.size - 1)).map { any<Any>() }.toTypedArray()))
//            }
//        } answers {
//            println("Mocked2!!")
//        }
//        Unit
//    }


    println(test.s)
    test == Test()

}

open class Test : Observable(){
    open var s: String by observable("")

    open var test: Test2? by observable(null)

}

open class Test2 : Observable(){

    open var s: String by observable("")
}