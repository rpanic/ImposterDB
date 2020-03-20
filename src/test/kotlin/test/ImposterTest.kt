package test

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import db.Backend
import db.ChangeObserver
import db.DB
import org.assertj.core.api.Assertions
import org.junit.Test

class ImposterTest{

    @Test
    fun wrongParameterTest(){

        val mockedBackend: Backend = mock()
        DB.addBackend(mockedBackend)
        whenever(mockedBackend.loadByPK("test", "pk", TestObject::class)).thenReturn(TestObject())

        val obj = DB.getDetached("test", "pk"){
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