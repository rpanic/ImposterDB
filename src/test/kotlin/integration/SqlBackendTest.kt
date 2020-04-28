package integration

import aNewCollections.VirtualSet
import aNewCollections.eq
import com.beust.klaxon.Klaxon
import com.mchange.v2.sql.SqlUtils
import db.DB
import example.print
import example.printData
import integration.TestObject
import io.mockk.*
import observable.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import sql.SqlBackend
import sql.SqlContext
import sql.checkIfTableExists
import sql.createOrUpdateTable
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class SqlBackendTest{

    lateinit var backend: SqlBackend

    @Test
    fun testInsertAndDelete() {

        assertSchemaIsEmpty("inserttest")

        val testObject = TestObject().apply { testProperty = "TestProp" }

        val db = DB()
        db += backend

        val virtualSet = db.getSet<TestObject>("inserttest")

        virtualSet.add(testObject)

        val query = "SELECT * FROM PUBLIC.inserttest"
        val rs = backend.context.executeQuery(query)

        assertThat(rs.next()).isTrue()
        assertThat(rs.getString("testProperty")).isEqualTo(testObject.testProperty)
        assertThat(rs.getString("uuid")).isEqualTo(testObject.uuid)

        virtualSet.remove(testObject)

        val rsDel = backend.context.executeQuery(query)
        assertThat(rs.next()).isFalse()

    }

    @Test
    fun testFilterSelect(){

        assertSchemaIsEmpty("testFilterSelect")

        val testObject = TestObject().apply { testProperty = "TestProp" }
        val testObject2 = TestObject().apply { testProperty = "TestProp2" }

        val db = DB()
        db += backend

        val virtualSet = db.getSet<TestObject>("testFilterSelect")

        virtualSet.add(testObject)
        virtualSet.add(testObject2)

        val filtered = db.getSet<TestObject>("testFilterSelect").filter { it.testProperty eq "TestProp2" }.view()

        assertThat(filtered.size).isEqualTo(1)
        assertThat(filtered.first().testProperty).isEqualTo(testObject2.testProperty)
        assertThat(filtered.first().uuid).isEqualTo(testObject2.uuid)

//        verify (exactly = 1) { backend.context.executeQuery(match {
//            it.startsWith("SELECT count(*)") && it.contains("information_schema.TABLES")
//        }) }
//        verify (exactly = 1 ) { backend.context.execute("CREATE TABLE table2 (testProperty VARCHAR(2000)") }
//        verify (exactly = 1 ) { backend.context.executeQuery("SELECT testProperty FROM test2 WHERE s = 'hello'") }
//
//        assertThat(loaded.first()).isEqualTo(testObject)

//        val obj = loaded.first()
//        obj.s = "Updated"
//        obj.test!!.s2 = "Updated S2"
//        virtualSet.remove(obj)
//        println(loaded)
    }

//    @Test
//    fun executeOnHsql(){
//        val rs = createHsqlBackend().context.executeQuery("SELECT * FROM information_schema.TABLES")
//        rs.printData()
//        val rs2 = createHsqlBackend().context.executeQuery("SELECT * FROM PUBLIC.table2")
//        rs2.printData()
//    }

    private fun assertSchemaIsEmpty(key: String){
        var empty = backend.context.checkIfTableExists(key)
        if(empty){
            val rs = backend.context.executeQuery("SELECT * FROM $key")
            empty = rs.next()
        }
        assertThat(empty).isFalse().withFailMessage("Schema has to be empty $key")
    }

    @Before
    fun prepare(){
        backend = createHsqlBackend()
    }

    @After
    fun clearDatabase(){
        backend.context.execute("DROP SCHEMA PUBLIC CASCADE")
        backend.context.connection.close()
    }

//    private fun <T : Observable> VirtualSet<T>.dump(){
//        println("Dump of full datastore")
//        this.view().forEach {
//            Klaxon().toJsonString(it).print()
//        }
//    }

    private fun createHsqlBackend() : SqlBackend{

        return SqlBackend("jdbc:hsqldb:file:testdb", "PUBLIC", "sa", "", null)

    }

}