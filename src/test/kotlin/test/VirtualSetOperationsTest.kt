package test

import aNewCollections.*
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
    fun filterTestEquals(){

        performExtractionCheck(CompareType.EQUALS, SimpleObservableChildParent::test){ it.test == SimpleObservableChild() }
        performExtractionCheck(CompareType.NOT_EQUALS, SimpleObservableChildParent::test){ it.test != SimpleObservableChild()}
        performExtractionCheck(CompareType.LESS_EQUALS, SimpleObservableChildParent::s, "Hello"){ it.s() <= "Hello"}
        performExtractionCheck(CompareType.LESS, SimpleObservableChildParent::s, "Hello2"){ it.s() < "Hello2"}
        performExtractionCheck(CompareType.GREATER_EQUALS, SimpleObservableChildParent::s, "Hello3"){ it.s() >= "Hello3"}
        performExtractionCheck(CompareType.GREATER, SimpleObservableChildParent::s, "Hello4"){ it.s() > "Hello4"}

    }

    private fun performExtractionCheck(type: CompareType, prop: KMutableProperty1<*, *>, obj: Any? = null, filter: (SimpleObservableChildParent) -> Boolean){

        val set = VirtualSet({ SimpleObservableChildParent() }, {}, listOf(), SimpleObservableChildParent::class)

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