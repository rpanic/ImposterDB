package integration

import collections.ElementChangeType
import connection.MtoNTableEntry
import db.Backend
import db.DB
import io.mockk.*
import observable.Level
import observable.LevelInformation
import observable.ObservableLevel
import observable.VirtualSetLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import virtual.SetElementChangedListener
import virtual.VirtualSet
import kotlin.reflect.KProperty

class EventPropagationTests{

    @Test
    fun testDetachedVirtualSetEventPropagation(){

        val db = DB()

        val backend = mockkClass(Backend::class)
        db += backend

        every { backend.keyExists(any()) } returns true
        val parent = Parent().apply { name = "parent1" }
        val first_child = Child().apply { value = "child1" }
        every { backend.load("testdetached", Parent::class, any()) } returns setOf(parent)
        every { backend.load("children", Child::class, any()) } returns setOf(first_child)
        every { backend.load("ChildrenTestdetached", MtoNTableEntry::class, any()) } returns setOf(MtoNTableEntry(first_child.uuid, parent.uuid))
        every { backend.update("children", Child::class, any(), any(), any()) } returns Unit
        every { backend.insert("testdetached", Parent::class, any()) } returns Unit //Remove

        val parentset = db.getSet<Parent>("testdetached")
        val childrenset = parentset.first().children

        val mockListener = mockk<SetElementChangedListener<Parent>>(relaxed = true)

        parentset.addListener(mockListener)

        val child = childrenset.first()
        child.value = "newValue"
        
        val slot = slot<LevelInformation>()
        
        verify (exactly = 1) { mockListener.invoke(match {
            it.elements.size == 1 &&  //TODO Test if this gets called multiple times if there are multiple invoke calls, or if it does LRE
            it.elementChangeType == ElementChangeType.Update &&
            it.elements[0].name == "parent1"
        }, capture(slot)) }
    
        val assertIsObservableLevel = { it: Level, clazz: Class<*>, old: Any?, new: Any?, prop: KProperty<Any>, instance: Any ->
            assertThat(it.isObservable()).isTrue()
            assertThat(it.getObservable()).isInstanceOf(clazz)
            assertThat(it.getObservable()).isEqualTo(instance)
            val observableLevel = it as ObservableLevel
            assertThat(observableLevel.old).isEqualTo(old)
            assertThat(observableLevel.new).isEqualTo(new)
            assertThat(observableLevel.prop).isEqualTo(prop)
        }
        
        val assertIsVirtualSetLevel = { it: Level, updatedChild: Any?, instance: VirtualSet<*> ->
            assertThat(it.isObservable()).isFalse()
            assertThat(it).isInstanceOf(VirtualSetLevel::class.java)
            assertThat(it.getSet()).isEqualTo(instance)
            val observableLevel = it as VirtualSetLevel
            assertThat(observableLevel.changeArgs.elementChangeType).isEqualTo(ElementChangeType.Update)
            assertThat(observableLevel.changeArgs.elements[0]).isEqualTo(updatedChild)
        }
        
        slot.captured.apply {
            
            this.list.forEachIndexed { index, level ->
                println("$index: $level")
            }
            
            assertIsObservableLevel(this.list[3], Child::class.java, "child1", "newValue", Child::value, child)
            assertIsVirtualSetLevel(this.list[2], child, childrenset)
            assertIsObservableLevel(this.list[1], Parent::class.java, childrenset, childrenset, Parent::children, parent)
            assertIsVirtualSetLevel(this.list[0], parent, parentset)
        }

    }

}