package test

import aNewCollections.eq
import connection.MtoNTableEntry
import connection.ObjectCache
import db.DB
import db.DBBackend
import io.mockk.*
import observable.DBAwareObject
import observable.LevelInformation
import observable.Observable
import org.assertj.core.api.Assertions.*
import org.junit.Test
import kotlin.reflect.KClass

class RelationalTest{

    @Test
    fun testRemoveRelation(){

        val (db, backend) = createDBWithMockedBackend(Parent::class)

        val set = db.getSet<Parent>("test11")

        val parent = Parent().apply { name = "One" }
        set.add(parent)
        val child = Child().apply { value = "Child" }

        every { backend.load("children", Child::class, any()) } returns setOf()

        set.view().first().children.add(child)

        verify { backend.insert("children", Child::class, child) }
        every { backend.load("children", Child::class, match { it.size == 1 }) } returns setOf(child)

        assertThat(set.view().first().children.view().first()).isEqualTo(child)

        set.view().first().children.remove(child)

        assertThat(parent.children.view().size).isEqualTo(0)

        val mToN = slot<MtoNTableEntry>()
        verify { backend.insert(match { it == "ChildrenTest11" }, match<KClass<Observable>> { it == MtoNTableEntry::class }, capture(mToN)) }

        verify { backend.delete(match { it == "ChildrenTest11" }, match<KClass<Observable>> { it == MtoNTableEntry::class }, match <Any> {
            val record = mToN.captured
            assertThat(record.keyValue<MtoNTableEntry, Any>()).isEqualTo(it)
            assertThat(record.m).isEqualTo(child.keyValue<MtoNTableEntry, Any>())
            assertThat(record.n).isEqualTo(parent.keyValue<MtoNTableEntry, Any>())
            true
        } ) }

        verify { backend.delete("children", Child::class, match <Any> {
            it == child.keyValue<Child, String>()
        }) }
    }

    @Test
    fun testAddOneToTwo(){
        //Test: 1 Trait and 2 Persons

        val (db, backend) = createDBWithMockedBackend(Parent::class)

        val set = db.getSet<Parent>("test12")

        val parent1 = Parent().apply { name = "One" }
        val parent2 = Parent().apply { name = "Two" }

        set.add(parent1)
        set.add(parent2)

        val child = Child().apply { value = "Child" }

        set.view().first().children.add(child)
        assertThat(set[parent2.uuid]).isEqualTo(parent2)
        set[parent2.uuid]!!.children.add(child)

        assertThat(backend.getDB()).isEqualTo(db)

        val key = mutableListOf<String>()
        val clazz = mutableListOf<KClass<Observable>>()
        val obj = mutableListOf<Observable>()

        verify(exactly = 5) { backend.insert(capture(key), capture(clazz), capture(obj)) }

        val verifier = TripleVerifier(key, clazz, obj)
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
                    assertThat (this.n).isEqualTo(parent1.keyValue<MtoNTableEntry, Any>())
                    assertThat (this.m).isEqualTo(child.keyValue<MtoNTableEntry, Any>())
                }
            }
            verify("ChildrenTest12", MtoNTableEntry::class as KClass<Observable>){
                assertThat (this).isInstanceOf(MtoNTableEntry::class.java)
                if(this is MtoNTableEntry){
                    assertThat (this.n).isEqualTo(parent2.keyValue<MtoNTableEntry, Any>())
                    assertThat (this.m).isEqualTo(child.keyValue<MtoNTableEntry, Any>())
                }
            }
        }
    }

    @Test
    fun testDbReferenceSetOnce(){

        val (db, backend) = createDBWithMockedBackend(Parent::class)

        val set = db.getSet<Parent>("test12")

        val parent = spyk(Parent().apply { name = "name" })
        set.add(parent)
        val parent2 = Parent().apply { name = "name2" }
        set.add(parent2)
        val child = spyk(Child().apply { value = "test" })
        parent2.children.add(child)

        verify { parent.setDbReference(db) }
        verify { child.setDbReference(db) }

    }

    private fun <T : Observable> createDBWithMockedBackend(clazz: KClass<T>) : Pair<DB, DBBackend> {
        val db = DB()

        val backend = mockkClass(DBBackend::class, relaxed = true)

        val dbAwareObject = object : DBAwareObject(){}
        every { backend.setDbReference(any()) } answers { dbAwareObject.setDbReference(it.invocation.args[0] as DB) }
        every { backend.getDB() } answers { dbAwareObject.getDB() }

        db += backend

        every { backend.keyExists(any()) } returns true

        val set = mutableSetOf<T>()
        val slot = CapturingSlot<Observable>()
        every { backend.insert(any(), any(), capture(slot)) } answers {
            set.add(slot.captured as T)
            println("Inserted ${set.last()}")
        }

        every { backend.load<T>(any(), any(), any()) } answers { set.map { it as T }.toSet() }

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