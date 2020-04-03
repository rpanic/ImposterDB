package test

import com.nhaarman.mockitokotlin2.*
import connection.MtoNTableEntry
import db.DB
import db.DBBackend
import observable.LevelInformation
import observable.Observable
import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.mockito.Mockito
import org.mockito.verification.VerificationMode
import kotlin.reflect.KClass

class RelationalTest{

    @Test
    fun testRemoveRelation(){

        val (db, backend) = createDBWithMockedBackend()

        val list = db.getDetachedList<Parent>("test11")

        val parent = Parent().apply { name = "One" }
        list.add(parent)
        val child = Child().apply { value = "Child" }
        list[0].children.add(child)

        assertThat(list[0].children[0]).isEqualTo(child)

        list[0].children.removeAt(0)

        val mToN = argumentCaptor<MtoNTableEntry>()
        verify(backend).insert(argThat { this == "ChildrenTest11" }, argThat<KClass<Observable>> { this == MtoNTableEntry::class }, mToN.capture())

        verify(backend).delete(argThat { this == "ChildrenTest11" }, argThat<KClass<Observable>> { this == MtoNTableEntry::class }, check <Any> {
            val record = mToN.firstValue
            assertThat(record.key<Any>()).isEqualTo(it)
            assertThat(record.m).isEqualTo(child.key())
            assertThat(record.n).isEqualTo(parent.key())
        })

        //TODO What about the Child?

    }

    @Test
    fun testAddOneToTwo(){
        //Test: 1 Trait and 2 Persons

        val (db, backend) = createDBWithMockedBackend()

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

    @Test
    fun testDbReferenceSetOnce(){

        val (db, backend) = createDBWithMockedBackend()

        val list = db.getDetachedList<Parent>("test12")

        val parent = spy(Parent().apply { name = "name" })
        list.add(parent)
        val child = spy(Child().apply { value = "test" })
        parent.children.add(child)

        verify(parent, times(1)).setDbReference(db)
        verify(child, times(1)).setDbReference(db)

    }

    private fun createDBWithMockedBackend() : Pair<DB, DBBackend> {
        val db = DB()

        val backend = Mockito.mock(DBBackend::class.java)
        db += backend

        whenever(backend.keyExists(any())).thenReturn(true)
        return db to backend
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

}