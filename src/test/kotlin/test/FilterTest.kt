package test

import com.nhaarman.mockitokotlin2.*
import db.*
import json.JsonBackend
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.internal.verification.VerificationModeFactory
import org.mockito.verification.VerificationMode
import kotlin.reflect.KProperty

class FilterTest{

//    @Before
//    fun setup(){
//        DB.primaryBackend = Mockito.mock(JsonBackend::class.java)
//    }

    @Test
    fun testFilter(){

        val objs = listOf(TestObject(), TestObject("one"), TestObject("two"), TestObject("three"), TestObject())

        val list = observableListOf(*objs.toTypedArray())

        val list2 = list.filter { it.testProperty != "" }

        Assertions.assertThat(list2.size).isEqualTo(3)
        Assertions.assertThat(list2[0]).isEqualTo(objs[1])
        Assertions.assertThat(list2[2]).isEqualTo(objs[3])

//        val listener: ElementChangedListener<TestObject> = {
//
//        }

        val listenerMock = mock<ElementChangedListener<TestObject>>()

        list2.addListener(listenerMock)

        list.add(0, TestObject("four"))
        list.add(4, TestObject("five"))
        list.add(TestObject()) //Should not execute the listener

        list.removeAt(2)

        val argCaptor = argumentCaptor<ListChangeArgs<TestObject>>()

        verify(listenerMock, times(3)).invoke(argCaptor.capture(), any())

        val allValues = argCaptor.allValues

        Assertions.assertThat(allValues[0].indizes[0]).isEqualTo(0)
        Assertions.assertThat(allValues[1].indizes[0]).isEqualTo(3)
        Assertions.assertThat(allValues[2].indizes[0]).isEqualTo(1)

//        argThat {
            //            println("--")
//            println(indizes)
//            println(elements)
//            println(elementChangeType)
//            this.indizes[0] == 1
//        }

    }

}