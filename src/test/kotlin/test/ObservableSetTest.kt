package test

import aNewCollections.ElementChangedListener
import aNewCollections.LazyObservableSet
import io.mockk.mockk
import org.junit.Test

class ObservableSetTest{

    @Test
    fun testMutableObservableSet() {

        val set = LazyObservableSet<TestObject>()

        val listener: ElementChangedListener<TestObject> = mockk()

    }

}