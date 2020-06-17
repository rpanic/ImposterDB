package integration

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import db.Backend
import db.ChangeObserver
import db.DB
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.Test
import ruleExtraction1.CompareRule
import ruleExtraction1.FilterStep

class ImposterTest{

    @Test
    fun wrongParameterTest(){

        val mockedBackend: Backend = mockk(relaxed = true)
        val db = DB()
        db.addBackend(mockedBackend)

        every { mockedBackend.load("test", TestObject::class, match {
            if(it.isNotEmpty()){
                val step = it[0]
                if(step is FilterStep<*>){
                    (step.conditions[0] as? CompareRule<*>)?.obj2 == "pk"
                }else{
                    false
                }
            }else{
                false
            }
        }) } answers { setOf(TestObject()) }

        val obj = db.getDetached<TestObject, String>("test", "pk"){
            throw IllegalAccessException()
        }

        val imposter = TestObjectImposter(obj)

        Assertions.assertThatThrownBy { obj.testProperty = "test" }.hasMessageStartingWith("Type kotlin.String cannot be casted to kotlin.Int")
    }

    class TestObjectImposter(t: TestObject) : ChangeObserver<TestObject>(t){
        fun all(old: Int, new: Int){

        }
    }

}