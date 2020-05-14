package integration

import collections.ElementChangeType
import collections.SetChangeArgs
import virtual.*
import db.Backend
import db.DB
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import observable.LevelInformation
import org.junit.Test
import ruleExtraction.*
import kotlin.reflect.KMutableProperty1
import org.assertj.core.api.Assertions.assertThat as assertThat

class VirtualSetOperationsTest {

    @Test
    fun testFind(){
        //Since Filter uses the same procedures as Filter, we only have to test the basic execution

        val backend = mockkClass(Backend::class)
        val objs = setOf("TestKey", "OtherKey").map { s -> TestObject().apply { testProperty = s } }
        every { backend.load<TestObject>(any(), any(), match { it.size == 1 } ) } returns objs.toSet()
        every { backend.keyExists(any()) } returns true

        val db = DB()
        db += backend

        val set = db.getSet<TestObject>("test")
        val obj = set.find { it.testProperty eq "TestKey" } //TODO Investiage why () == "TestKey"() is not working

        assertThat(obj).isNotNull
        assertThat(obj.testProperty).isEqualTo("TestKey")

    }

    @Test
    fun testChainWithFilter(){

        val backend = mockkClass(Backend::class)
        val testObj = setOf("Hallo").map { s -> TestObject().apply { testProperty = s } }
        every { backend.load<TestObject>(any(), any(), match { it.size == 1 } ) } returns testObj.toSet()
        every { backend.insert<TestObject>(any(), any(), any()) } returns Unit
        every { backend.keyExists(any()) } returns true

        val db = DB()
        db += backend

        val set = db.getSet<TestObject>("test")
        val set2 = set.filter { it.testProperty eq "Hello" }

        val view = set2.view()
        assertThat(view.size).isEqualTo(1)
        assertThat(view.first()).isEqualTo(testObj.first())

        assertThat(set.loadedState).isNull()

        val testObject2 = TestObject().apply { testProperty = "Hello" }

        val listener = mockk<SetElementChangedListener<TestObject>>(relaxed = true)
        val viewListener = mockk<SetElementChangedListener<TestObject>>(relaxed = true)
        set.addListener(listener)
        set2.addListener(listener)
        view.addListener(viewListener)

        set2.add(testObject2)

        verify { backend.insert("test", TestObject::class, testObject2) }
        verify (exactly = 2) { listener.invoke(match { it.elementChangeType == ElementChangeType.Add && it.elements.first() == testObject2 }, any()) }
        verify (exactly = 1) { viewListener.invoke(match { it.elementChangeType == ElementChangeType.Add && it.elements.first() == testObject2 }, any()) }

        //TODO Write test with insert where filter condition is not true

    }

    @Test
    fun filterTestEquals(){

        performExtractionCheck(CompareType.EQUALS, SimpleObservableChildParent::test){ it.test == SimpleObservableChild() }
        performExtractionCheck(CompareType.NOT_EQUALS, SimpleObservableChildParent::test){ it.test != SimpleObservableChild()}
        performExtractionCheck(CompareType.LESS_EQUALS, SimpleObservableChildParent::s, "Hello"){ it.s() <= "Hello"}
        performExtractionCheck(CompareType.LESS, SimpleObservableChildParent::s, "Hello2"){ it.s() < "Hello2"}
        performExtractionCheck(CompareType.GREATER_EQUALS, SimpleObservableChildParent::s, "Hello3"){ it.s() >= "Hello3"}
        performExtractionCheck(CompareType.GREATER, SimpleObservableChildParent::s, "Hello4"){ it.s() > "Hello4"}
        performExtractionCheck(CompareType.EQUALS, SimpleObservableChildParent::s, "Hello5"){ it.s eq "Hello5"}

    }

    private fun performExtractionCheck(type: CompareType, prop: KMutableProperty1<*, *>, obj: Any? = null, filter: (SimpleObservableChildParent) -> Boolean){

        val accessor = object : VirtualSetAccessor<SimpleObservableChildParent>{
            override fun load(steps: List<Step<SimpleObservableChildParent, *>>) =
                    setOf(SimpleObservableChildParent())
    
            override fun count(steps: List<Step<SimpleObservableChildParent, *>>) = 1
            override fun performEvent(set: VirtualSet<SimpleObservableChildParent>, changeArgs: SetChangeArgs<SimpleObservableChildParent>, levels: LevelInformation){}
        }
        
        val set = VirtualSet(accessor, listOf(), SimpleObservableChildParent::class)

        val set2 = set.filter (filter)

        assertThat(set2.steps.size).isEqualTo(1)
        assertThat(set2.steps[0]).isInstanceOf(FilterStep::class.java)
        (set2.steps[0] as FilterStep<*>).also { filterStep ->
            assertThat(filterStep.conditions[0]).isInstanceOf(NormalizedCompareRule::class.java)
            (filterStep.conditions[0] as NormalizedCompareRule<*>).apply {
                assertThat(this.type).isEqualTo(type)
                assertThat(this.prop!![0]).isEqualTo(prop)
                if(obj != null){
                    assertThat(this.obj2).isEqualTo(obj)
                }
            }
        }
    }

}