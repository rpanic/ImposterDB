package integration

import com.beust.klaxon.Klaxon
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import db.*
import json.JsonBackend
import json.ObservableConverter
import observable.LevelInformation
import observable.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.mockito.Mockito
import kotlin.reflect.KProperty

class ListTest{

//    @Before
//    fun setup(){
//        DB.primaryBackend = jsonBackend//.apply { baseFile = baseFile.resolve("test") }
//    }

    @Test
    fun exampleTest(){
        val jsonBackend = JsonBackend()
        val file = jsonBackend.baseFile.resolve("exampleTest.json")
        jsonBackend.baseFile.resolve("children_1toM.json").delete()
        file.delete()
        val db = DB()
        db.addBackend(jsonBackend)

        val obj = db.getDetached("exampleTest", "pk") {
            OneToManyParent()
        }

        val imposter = OneToManyParentObserver(obj)

        val mock = Mockito.spy(imposter as IOneToManyObserver)

        imposter.target(mock)

//        val originalTrait = obj.trait
        val child = Child()

        db.tx {

            obj.description = "This is some random stuff"

            obj.child = child

            child.value = "asd"

            obj.name = "John Miller"

            println("Finished")
        }

        val property = argumentCaptor<KProperty<*>>()
        val old = argumentCaptor<Any>()
        val new = argumentCaptor<Any>()
        val levelInfo = argumentCaptor<LevelInformation>()

        verify(mock, times(4)).all(property.capture(), old.capture(), new.capture(), levelInfo.capture())

        val verifier = QuadrupleVerifier(property.allValues, old.allValues, new.allValues, levelInfo.allValues)
        verifier.apply {

            verify(OneToManyParent::description, null, "This is some random stuff"){
                assertThat(list.size).isEqualTo(1)
                assertThat(list[0].getObservable()).isEqualTo(obj)
            }

            verify(OneToManyParent::child, null, child){
                assertThat(list.size).isEqualTo(1)
                assertThat(list[0].getObservable()).isEqualTo(obj)
            }

        }

        verify(mock).name("John Miller")

        assertThat(file).exists()
        assertThat(file).isFile()

        val content = file.readText()
        assertThat(content).isNotEmpty() //Remove?

        val klaxon = Klaxon()
        klaxon.converter(ObservableConverter(klaxon, hashMapOf()))
        val jsonObject = klaxon.parseJsonObject(klaxon.toJsonString(obj).reader())
        jsonObject["child"] = child.keyValue<Child>() as String
        assertThat(content).isEqualTo("[${jsonObject.toJsonString()}]")

    }

    @Test
    fun additionTest(){

        val jsonBackend = Mockito.mock(JsonBackend::class.java)
        val db = DB()
        db.addBackend(jsonBackend)

        val set = db.getSet<TestObject>("test1")

        val obj2 = TestObject()

        set.add(obj2)

        verify(jsonBackend).keyExists("test1")

        verify(jsonBackend).insert("test1", TestObject::class, obj2)

    }

    @After
    fun tearDown(){
        Mockito.validateMockitoUsage()
    }

    open class MockableImposter<T : Observable, M : Any>(t: T) : ChangeObserver<T>(t) {
        fun target(mock: M) = init(mock)
    }

    open class OneToManyParentObserver(t: OneToManyParent) : MockableImposter<OneToManyParent, IOneToManyObserver>(t), IOneToManyObserver{

        override fun name(new: String){
            println("New name: $new!!!!")
        }

        override fun all(prop: KProperty<Any?>, old: Any?, new: Any?, levelInformation: LevelInformation){
            println("Prop ${prop.name} changed to $new")

            println(levelInformation)
        }

    }

    class QuadrupleVerifier<A, B, C, D>(val aa: List<A>, val bs: List<B>, val cs: List<C>, val ds: List<D>){
        var index = 0

        fun verify(a: A?, b: B?, c: C?, d: D.() -> Unit){

            assertThat(aa[index]).isEqualTo(a)
            assertThat(bs[index]).isEqualTo(b)
            assertThat(cs[index]).isEqualTo(c)
            d(ds[index])

            index++
        }
    }

    interface IOneToManyObserver{
        fun name(new: String)
        fun all(prop: KProperty<Any?>, new: Any?, old: Any?, levelInformation: LevelInformation)
    }

}