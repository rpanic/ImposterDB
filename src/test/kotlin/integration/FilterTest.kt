package integration

import virtual.SetElementChangedListener
import collections.observableSetOf
import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions
import org.junit.Test
import db.filter

class FilterTest{

    @Test
    fun testFilter(){

        val objs = listOf(TestObject(), TestObject("one"), TestObject("two"), TestObject("three"), TestObject())

        val set = observableSetOf(*objs.toTypedArray())

        val set2 = set.filter { it.testProperty != "" }

        Assertions.assertThat(set2.size).isEqualTo(3)
        Assertions.assertThat(set2[objs[1].keyValue<TestObject>()]).isEqualTo(objs[1])
        Assertions.assertThat(set2[objs[3].keyValue<TestObject>()]).isEqualTo(objs[3])

        val listenerMock = mock<SetElementChangedListener<TestObject>>()

        set2.addListener(listenerMock)

        //TODO When MutableObservableSet is implemented
//        list.add(0, TestObject("four"))
//        list.add(TestObject("five"))
//        list.add(4, TestObject("six"))
//        list.add(TestObject()) //Should not execute the listener
//
//        list.removeAt(2)
//
//        val argCaptor = argumentCaptor<ListChangeArgs<TestObject>>()

//        verify(listenerMock, times(4)).invoke(argCaptor.capture(), any())
//
//        val allValues = argCaptor.allValues
//
//        Assertions.assertThat(allValues[0].indizes[0]).isEqualTo(0)
//        Assertions.assertThat(allValues[1].indizes[0]).isEqualTo(4)
//        Assertions.assertThat(allValues[2].indizes[0]).isEqualTo(3)
//        Assertions.assertThat(allValues[3].indizes[0]).isEqualTo(1)

    }

}