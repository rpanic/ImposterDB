package unit

import ruleExtraction.eq
import db.DB
import integration.TestObject
import io.mockk.*
import observable.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import ruleExtraction.MappingStep
import ruleExtraction.MappingType
import sql.SqlBackend
import sql.SqlContext
import sql.createOrUpdateTable
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class SqlBackendTest{

    @Test
    fun testCountReturn(){
        
        val resultSet = createIntResultSet(100)
        
        val backend = SqlBackend(createContextFun = {
            val context = mockk<SqlContext>()
            every { context.executeQuery(any()) } returns resultSet
            context
        })
        
        val ints = backend.loadTransformed("key", TestObject::class, listOf(MappingStep<TestObject, Int>(MappingType.COUNT)), Int::class)
        
        assertThat(ints.first()).isEqualTo(100)
        
    }
    
    //TODO
    fun testCreateTableAndFilterSelect(){

        val testObject = TestObject().apply { testProperty = "TestProp" }

        val db = DB()
        val backend = createMockedBackend(testObject, TestObject::class)

        every { backend.context.executeQuery(match {
            it.startsWith("SELECT count(*)") && it.contains("information_schema.TABLES")
        }) } answers {
            val rs2 = mockk<ResultSet>()
            every { rs2.next() } returns false
            rs2
        }

        db += backend

        val virtualSet = db.getSet<TestObject>("table2")
//    backend.context.createOrUpdateTable("table2", Test::class)
//    backend.insert("table2", Test::class, Test().apply { b = 1337; s = "Hallo"; test = Test2().apply { s2 = "String 2" } })
//    val loaded = backend.load("table2", Test::class, listOf())


        val loaded = virtualSet.filter { it.testProperty eq "hello" }.view()

        verify (exactly = 1) { backend.context.executeQuery(match {
            it.startsWith("SELECT count(*)") && it.contains("information_schema.TABLES")
        }) }
        verify (exactly = 1 ) { backend.context.execute("CREATE TABLE table2 (testProperty VARCHAR(2000)") }
        verify (exactly = 1 ) { backend.context.executeQuery("SELECT testProperty FROM test2 WHERE s = 'hello'") }

        assertThat(loaded.first()).isEqualTo(testObject)

//        val obj = loaded.first()
//        obj.s = "Updated"
//        obj.test!!.s2 = "Updated S2"
//        virtualSet.remove(obj)
        println(loaded)
    }
    
    private fun createIntResultSet(value: Int) : ResultSet{
    
        val resultSet = resultsSetFromObject(value, Int::class)
    
        every { resultSet.getInt("a") } returns value
        every { resultSet.next() } returnsMany listOf(true, false)
        
        val metadata = mockk<ResultSetMetaData>()
        every { resultSet.metaData } returns metadata
        every { metadata.columnCount } returns 1
        every { metadata.getColumnName(0) } returns "a"
        
        return resultSet
    }

    private fun <T : Observable> createMockedBackend(t: T, clazz: KClass<T>) : SqlBackend{

        val function: (SqlBackend) -> SqlContext = {
            val mock = mockk<SqlContext>(relaxed = true)
            every { mock.executeQuery(any()) } answers {
                val resultSet = resultsSetFromObject(t, clazz)
                every { resultSet.next() } returnsMany listOf(true, false)
                resultSet
            }
            
            mock
        }

        return SqlBackend(createContextFun = function)
    }

    private fun <T : Any> resultsSetFromObject(t: T, clazz: KClass<T>) : ResultSet{
        val rs = mockk<ResultSet>()  //TODO Write resultset mock for test
        every { rs.next() } returns true

        val getValue = { scope: MockKAnswerScope<*, *>, cl : KClass<T>, obj: T ->
            cl.memberProperties.find { it.name == scope.args[0] }!!.get(obj)}

        every { rs.getString(any<String>()) } answers { getValue(this, clazz, t) as String }
        every { rs.getInt(any<String>()) } answers { getValue(this, clazz, t) as Int }
        return rs
    }

}