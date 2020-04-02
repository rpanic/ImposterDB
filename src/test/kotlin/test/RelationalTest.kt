package test

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import db.DB
import db.DBBackend
import example.Person
import observable.LevelInformation
import observable.Observable
import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class RelationalTest{

    @Test
    fun testAddOneToTwo(){
        //Test: 1 Trait and 2 Persons

        val db = DB()

        val backend = Mockito.mock(DBBackend::class.java)
        backend.setDbReference(db)
        db += backend

        Mockito.`when`(backend.keyExists(any())).thenReturn(true)
//        whenever(backend.getDB()).thenReturn(db)

        val list = db.getDetachedList<Parent>("test12")

        list.add(Parent().apply { name = "One" })
        list.add(Parent().apply { name = "Two" })

        val child = Child().apply { value = "Child" }

        list[0].children.add(child)
        list[1].children.add(child)

        verify(backend).setDbReference(db)

        val key = argumentCaptor<String>()
        val clazz = argumentCaptor<KClass<Observable>>()
        val obj = argumentCaptor<Observable>()
        //, times(3)
        verify(backend).insert(key.capture(), clazz.capture(), obj.capture())

        val verifier = TripleVerifier(key.allValues, clazz.allValues, obj.allValues)
        verifier.apply {

            verify("test12", Parent::class as KClass<Observable>){
                assertThat (this).isInstanceOf(Parent::class.java)
                assertThat ((this as Parent).name).isEqualTo("One")
            }

        }

    }

    class TripleVerifier<A, B, C>(val aa: List<A>, val bs: List<B>, val cs: List<C>){
        var index = 0

        fun verify(a: A?, b: B?, c: C.() -> Unit){

            assertThat(aa[index]).isEqualTo(a)
            assertThat(bs[index]).isEqualTo(b)
            c(cs[index])

            index++
        }
    }

    @Test
    fun testDbReferenceSetOnce(){

    }

    @Test
    fun testRemoveRelation(){

    }


}