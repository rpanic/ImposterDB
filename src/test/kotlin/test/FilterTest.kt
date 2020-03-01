package test

import db.DB
import db.filter
import db.observableListOf
import json.JsonBackend
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

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



    }

}