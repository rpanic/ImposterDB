package test

import com.beust.klaxon.Klaxon
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.verify
import com.sun.org.apache.xpath.internal.operations.Bool
import db.*
import example.Person
import example.Trait
import json.JsonBackend
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.lang.RuntimeException
import kotlin.reflect.KProperty

class ListTest{

//    val jsonBackend = Mockito.mock(JsonBackend::class.java)
//
//    @Before
//    fun setup(){
//        DB.primaryBackend = jsonBackend//.apply { baseFile = baseFile.resolve("test") }
//    }

    @Test
    fun exampleTest(){
        val jsonBackend = JsonBackend()
        val file = jsonBackend.baseFile.resolve("exampleTest.json")
        file.delete()
        DB.primaryBackend = jsonBackend

        val obj = DB.getObject("exampleTest") {
            Person()
        }

        val imposter = PersonObserver(obj)

        val mock = Mockito.spy(imposter as IPersonObserver)
//        val mock = Mockito.mock(PersonObserver::class.java)

        imposter.target(mock)

//        val p2 = PersonObserver(obj)
//
//        imposter.target(p2)

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

        val property = ArgumentCaptor.forClass(KProperty::class.java)
        val old = ArgumentCaptor.forClass(Any::class.java)
        val new = ArgumentCaptor.forClass(Any::class.java)
        val levelInfo = ArgumentCaptor.forClass(LevelInformation::class.java)

//        verify(mock).all(eq(Person::description), isNull(), eq("This is some random stuff"), argThat {
//            list.size == 1 &&
//            list[0].getObservable() == obj
//        })
//
//        verify(mock).all(eq(Person::trait), isNull(), eq(trait), argThat {
//            true
//        })

        Mockito.verify(mock).all(property.capture(), old.capture(), new.capture(), levelInfo.capture())

        val verifier = QuadrupleVerifier(property.allValues, old.allValues, new.allValues, levelInfo.allValues)
        verifier.apply {

            verify(Person::description, null, "This is some random stuff"){
                assertThat(list.size).isEqualTo(1)
                assertThat(list[0].getObservable()).isEqualTo(obj)
            }

            verify(Person::trait, null, trait){
                assertThat(list.size).isEqualTo(2)
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
        DB.primaryBackend = jsonBackend

        val list = DB.getList<TestObject>("test1")

        val obj2 = TestObject()

        list.add(obj2)

        Mockito.verify(jsonBackend).keyExists("test1")

        Mockito.verify(jsonBackend).saveList("test1", TestObject::class, list.collection)

    }

    @After
    fun tearDown(){
//        DB.primaryBackend = Mockito.mock(Backend::class.java)
        Mockito.validateMockitoUsage()
    }

    class TestObject : Observable(){
        var testProperty: String by observable("")
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

        fun verify(a: A, b: B, c: C, d: D.() -> Unit){

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

open class X : L{
    override fun x(x: String){
        println(x)
    }
}

interface L{
    fun x(x: String)
}