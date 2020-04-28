package example

import aNewCollections.eq
import aNewCollections.invoke
import db.DB
import io.mockk.every
import io.mockk.mockk
import io.zeko.model.Entity
import observable.Observable
import sql.SqlBackend
import sql.SqlContext
import sql.createOrUpdateTable
import java.sql.DriverManager
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*

fun main() {

//    var prop: KProperty1<Any, Any>? = null
//
//    val c = { clazz: KClass<Any> ->
//        val companion = clazz.companionObject
//        if(companion == null)
//            clazz.superclasses.find { it. }
//        !!.memberProperties.find { it.name == "key" }!! as KProperty1<Any, KProperty1<Test, Any>>
//    }
//    val pkProp = Test::class.companionObject!!.memberProperties.find { it.name == "key" }!! as KProperty1<Any, KProperty1<Test, Any>>

//    val prop = pkProp.get(Test::class.companionObjectInstance!!)

    val db = DB()
    val backend = SqlBackend(createContextFun = function)
    db += backend

    val virtualSet = db.getSet<Test>("table2")
//    backend.context.createOrUpdateTable("table2", Test::class)
//    backend.insert("table2", Test::class, Test().apply { b = 1337; s = "Hallo"; test = Test2().apply { s2 = "String 2" } })
//    val loaded = backend.load("table2", Test::class, listOf())



    val loaded = virtualSet.filter { it.s eq "hello" }.view()
    val obj = loaded.first()
//    obj.s = "Updated"
    obj.test!!.s2 = "Updated S2"
    virtualSet.remove(obj)
    println(loaded)

//    Class.forName("com.mysql.jdbc.Driver").newInstance()
//    val conn = DriverManager.getConnection("jdbc:mysql://localhost/test", "root", "root")

//    val results = conn.createStatement().execute("CREATE TABLE test (name VARCHAR(100))")
//    println(results)
//
//    val entity = GenericEntity("uuid" to "Hello", "name" to "wtf", "testint" to 0)
//
//    Update(entity).toSql().print()
//
//    Insert(entity).toSql().print()

}

class GenericEntity : Entity {
    constructor(map: Map<String, Any?>) : super(map)
    constructor(vararg props: Pair<String, Any?>) : super(*props)
    var test: String? by map
}

open class Test : Observable(){
    open var s: String by observable("")
    open var b: Int by observable(10)

//    companion object{
//        val key = Test::s
//    }

    open var test: Test2? by observable(null)

}

open class Test2 : Observable(){

    open var s2: String by observable("")
}