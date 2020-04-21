package example

import aNewCollections.invoke
import io.zeko.db.sql.Insert
import io.zeko.db.sql.Query
import io.zeko.db.sql.Update
import io.zeko.model.Entity
import observable.Observable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager
import javax.sql.DataSource

fun main() {

//    Class.forName("com.mysql.jdbc.Driver").newInstance()
//    val conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/test", "root", "testpassword")

    val db = Database.connect("jdbc:mysql://127.0.0.1:3306/test", user = "root", password = "testpassword")
    transaction {
        val x = this.exec("SHOW TABLES"){
            it.print()
        }
    }

//    val results = conn.createStatement().executeQuery("SHOW TABLES")
//    println(results)

    val entity = GenericEntity("uuid" to "Hello", "name" to "wtf", "testint" to 0)
//
//    Update(entity).toSql().print()
//
//    Insert(entity).toSql().print()

}

class GenericEntity : Entity {
    constructor(map: Map<String, Any?>) : super(map)
    constructor(vararg props: Pair<String, Any?>) : super(*props)

}

open class Test : Observable(){
    open var s: String by observable("")

    open var test: Test2? by observable(null)

}

open class Test2 : Observable(){

    open var s: String by observable("")
}