package test

import com.nhaarman.mockitokotlin2.*
import connection.MtoNTableEntry
import db.DB
import db.DBBackend
import example.Person
import observable.LevelInformation
import observable.Observable
import org.assertj.core.api.Assertions.*
import org.junit.Test
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

        whenever(backend.keyExists(any())).thenReturn(true)
//        whenever(backend.getDB()).thenReturn(db)

        val list = db.getDetachedList<Parent>("test12")

        val parent1 = Parent().apply { name = "One" }
        val parent2 = Parent().apply { name = "Two" }

        list.add(parent1)
        list.add(parent2)

        val child = Child().apply { value = "Child" }

        list[0].children.add(child)
        list[1].children.add(child)

        assertThat(backend.getDB()).isEqualTo(db)

        val key = argumentCaptor<String>()
        val clazz = argumentCaptor<KClass<Observable>>()
        val obj = argumentCaptor<Observable>()

        verify(backend, times(5)).insert(key.capture(), clazz.capture(), obj.capture())

        validateMockitoUsage()

        val verifier = TripleVerifier(key.allValues, clazz.allValues, obj.allValues)
        verifier.apply {

            verify("test12", Parent::class as KClass<Observable>){
                assertThat (this).isInstanceOf(Parent::class.java)
                assertThat ((this as Parent).name).isEqualTo("One")
            }
            verify("test12", Parent::class as KClass<Observable>){
                assertThat (this).isInstanceOf(Parent::class.java)
                assertThat ((this as Parent).name).isEqualTo("Two")
            }
            verify("children", Child::class as KClass<Observable>){
                assertThat (this).isInstanceOf(Child::class.java)
                assertThat ((this as Child).value).isEqualTo("Child")
            }
            verify("ChildrenTest12", MtoNTableEntry::class as KClass<Observable>){
                assertThat (this).isInstanceOf(MtoNTableEntry::class.java)
                if(this is MtoNTableEntry){
                    assertThat (this.n).isEqualTo(parent1.key())
                    assertThat (this.m).isEqualTo(child.key())
                }
            }
            verify("ChildrenTest12", MtoNTableEntry::class as KClass<Observable>){
                assertThat (this).isInstanceOf(MtoNTableEntry::class.java)
                if(this is MtoNTableEntry){
                    assertThat (this.n).isEqualTo(parent2.key())
                    assertThat (this.m).isEqualTo(child.key())
                }
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