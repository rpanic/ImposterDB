package test

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import db.ChangeObserver
import db.DB
import org.assertj.core.api.Assertions
import org.junit.Test

class ImposterTest{

    @Test
    fun wrongParameterTest(){

        DB.primaryBackend = mock()
        whenever(DB.primaryBackend.load("test", TestObject::class)).thenReturn(TestObject())

        val obj = DB.getObject("test"){
            TestObject()
        }

        val imposter = TestObjectImposter(obj)

        Assertions.assertThatThrownBy { obj.testProperty = "test" }.hasMessageStartingWith("Type kotlin.String cannot be casted to kotlin.Int")
    }

    class TestObjectImposter(t: TestObject) : ChangeObserver<TestObject>(t){
        fun all(old: Int, new: Int){

        }
    }

}