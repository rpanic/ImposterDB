package test

import com.beust.klaxon.Klaxon
import db.*
import example.Person
import example.Trait
import json.JsonBackend
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.*
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

        val mock = Mockito.spy(imposter)
//        val mock = Mockito.mock(PersonObserver::class.java)

        imposter.target(mock)

//        val p2 = PersonObserver(obj)
//
//        imposter.target(p2)

//        DB.tx {
//
//            obj.description = "This is some random stuff"
//
//            obj.trait = Trait()
//
//            obj.trait.value = 10
//
//            obj.traits.add(Trait())
//
//            obj.traits[0].value = 1337

            obj.name = "John Miller"

//            println("Finished")
//        }

        mock.name("Hallo")

        doThrow(RuntimeException()).`when`(mock).name(ArgumentMatchers.anyString())
//        verify(mock).name(ArgumentMatchers.anyString())

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
        validateMockitoUsage()
    }

    class TestObject : Observable(){
        var testProperty: String by observable("")
    }

    open class MockableImposter<T : Observable, M : Any>(t: T) : ChangeObserver<T>(t) {
        fun target(mock: M) = init(mock)
    }

    open class PersonObserver(t: Person) : MockableImposter<Person, PersonObserver>(t){

        fun name(new: String){
            println("New name: $new!!!!")

//        throw IllegalAccessException()
        }

        fun all(prop: KProperty<Any?>, new: Any?, old: Any?, levelInformation: LevelInformation){
            println("Prop ${prop.name} changed to $new")

            println(levelInformation)
        }

    }

}