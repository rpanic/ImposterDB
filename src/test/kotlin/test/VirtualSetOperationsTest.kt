package test

import aNewCollections.*
import com.nhaarman.mockitokotlin2.times
import db.Backend
import db.DB
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import observable.ElementChangeType
import org.junit.Test
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import org.assertj.core.api.Assertions.assertThat as assertThat

fun compareTo(set: VirtualSet<Parent>) : Int {

    true() == true()

    true eq true

    println("1000")
    return 1000
}

class VirtualSetOperationsTest {

    @Test
    fun testChainWithFilter(){

        val backend = mockkClass(Backend::class)
        val testObj = TestObject().apply { testProperty = "Hello" }
        every { backend.load<TestObject>(any(), any(), any()) } returns setOf(testObj)
        every { backend.insert<TestObject>(any(), any(), any()) } returns Unit

        val db = DB()
        db += backend

        val set = db.getSet<TestObject>("test")
        val set2 = set.filter { it.testProperty eq "Hello" }

        val view = set2.view()
        assertThat(view.first()).isEqualTo(testObj)

        val testObject2 = TestObject().apply { testProperty = "Hello" }

        val listener = mockk<ElementChangedListener<TestObject>>(relaxed = true)
        set.addListener(listener)
        set2.addListener(listener)

        set2.add(testObject2)

        verify { backend.insert("test", TestObject::class, testObject2) }
        verify (exactly = 2) { listener.invoke(match { it.elementChangeType == ElementChangeType.Add }, any()) }

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

        val set = VirtualSet({ setOf(SimpleObservableChildParent()) }, {_, _, _ -> }, listOf(), SimpleObservableChildParent::class)

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