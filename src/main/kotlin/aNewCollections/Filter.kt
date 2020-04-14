package aNewCollections

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import observable.Observable
import org.mockito.Mockito
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter


fun <T : Observable> getFilter(clazz: KClass<T>) : T{
    val mock = Mockito.mock(clazz.java)

    mock::class.memberProperties.forEach { prop ->
        whenever(prop.javaGetter!!.invoke(mock)).thenReturn("")
    }
    return mock
}
open class Test : Observable(){
    open var s: String by observable("")

}

fun main() {


    val mock = mock<Test>()
//        on { s }.thenReturn("")


//    whenever(mock.qq).thenReturn("")

    mock::class.memberProperties.forEach { prop ->
        whenever((prop as KProperty1<Any?, Any?>).getter.invoke(mock)).thenReturn("")
    }

    val t = Test()
//    println(t.s)
//    val obj = getFilter(Test::class)

//    println(obj.s)

}