package test

import com.beust.klaxon.Klaxon
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import db.*
import example.Person
import example.Trait
import json.JsonBackend
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
        file.delete()
        DB.addBackend(jsonBackend)

        val obj = DB.getDetached("exampleTest", "pk") {
            Person()
        }

        val imposter = PersonObserver(obj)

        val mock = Mockito.spy(imposter as IPersonObserver)

        imposter.target(mock)

        val originalTrait = obj.trait
        val trait = Trait()

        DB.tx {

            obj.description = "This is some random stuff"

            obj.trait = trait
//
//            obj.trait.value = 10
//
//            obj.traits.add(Trait())
//
//            obj.traits[0].value = 1337

            obj.name = "John Miller"

            println("Finished")
        }

        val property = argumentCaptor<KProperty<*>>()
        val old = argumentCaptor<Any>()
        val new = argumentCaptor<Any>()
        val levelInfo = argumentCaptor<LevelInformation>()

        verify(mock, times(3)).all(property.capture(), old.capture(), new.capture(), levelInfo.capture())

        val verifier = QuadrupleVerifier(property.allValues, old.allValues, new.allValues, levelInfo.allValues)
        verifier.apply {

            verify(Person::description, null, "This is some random stuff"){
                assertThat(list.size).isEqualTo(1)
                assertThat(list[0].getObservable()).isEqualTo(obj)
            }

            verify(Person::trait, originalTrait, trait){
                assertThat(list.size).isEqualTo(1)
                assertThat(list[0].getObservable()).isEqualTo(obj)
            }

        }

        verify(mock).name("John Miller")

        assertThat(file).exists()
        assertThat(file).isFile()

        val content = file.readText()
        assertThat(content).isNotEmpty() //Remove?

        val controlContent = Klaxon().toJsonString(obj)
        assertThat(content).isEqualTo(controlContent)

    }

    @Test
    fun additionTest(){

        val jsonBackend = Mockito.mock(JsonBackend::class.java)
        DB.addBackend(jsonBackend)

        val list = DB.getDetachedList<TestObject>("test1")

        val obj2 = TestObject()

        list.add(obj2)

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

    open class PersonObserver(t: Person) : MockableImposter<Person, IPersonObserver>(t), IPersonObserver{

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

    interface IPersonObserver{
        fun name(new: String)
        fun all(prop: KProperty<Any?>, new: Any?, old: Any?, levelInformation: LevelInformation)
    }

}