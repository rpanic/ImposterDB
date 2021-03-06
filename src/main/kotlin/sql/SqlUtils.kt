package sql

import connection.MtoNTable
import connection.MtoNTableEntry
import db.Ignored
import example.ReflectionUtils
import example.info
import net.bytebuddy.agent.builder.AgentBuilder
import observable.Observable
import java.lang.IllegalStateException
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

fun SqlContext.checkIfTableExists(key: String) : Boolean{
    val result = this.executeQuery("SELECT COUNT(*) " +
            "FROM information_schema.TABLES " +
            "WHERE (TABLE_SCHEMA = '${this.dbName}') AND (UPPER(TABLE_NAME) = '${key.toUpperCase()}')")
    return result.next() && ( result.getInt(1) > 0 )
}

fun <T : Observable> SqlContext.query(query: String, clazz: KClass<T>) : Set<T>{

    val resultSet = this.executeQuery(query)
    println(resultSet)

    return setOf()
}

fun SqlContext.createMToNConstraints(table: MtoNTable) : String{

    //TODO

    return ""
}

fun <T : Observable> SqlContext.createOrUpdateTable(key: String, clazz: KClass<T>) : Boolean{

     /* if(checkIfTableExists(key)){

        //TODO Check if the schema is the same as the DAO
        //this.executeQuery("SELECT * FROM $key ")
        true
    }else{*/

    val pkProp = ReflectionUtils.getPkOfClass(clazz)

    val props = ReflectionUtils.getPropertySqlNames(clazz)
            .map {
                val type = it.second.getter.returnType.javaType
                val notNull = " " + if(it.second.returnType.isMarkedNullable) "" else "NOT NULL"
                val datatype = if (it.second.javaField == pkProp?.javaField /* because uuid would be Observable.uuid and Test.uuid */
                        && type == String::class.java) {
                    "VARCHAR(255)"
                } else {
                    typeMap[type]!!
                }
                it.first to (datatype) + notNull
            }
            .toMap().toMutableMap()
    props["PRIMARY KEY"] = "(${pkProp.name})"

    val sql = "CREATE TABLE $key (${props.map { "${it.key} ${it.value}" }.joinToString (",\n")})"

    this.execute(sql)

    val check = checkIfTableExists(key)

    return if(!check){
        example.error("Creation of table $key failed - Something went wrong on the SQL Server")
        false
    }else{
        info("Created Table $key")

        //Not needed, Mysql does this automatically
//            val index = "CREATE UNIQUE INDEX pkidx_$key ON $key (${pkProp.name})"
//            val indexRet = this.execute(index)
//            info("Created PK-Index for $key: $indexRet")

        true
    }

}

fun String.replaceWildCards(vararg values: Any) = this.replaceWildCards(values.toList())

fun String.replaceWildCards(values: List<Any>) : String{

    var tmp = this
    values.forEach {
        val processed = when(it){
            is String, is Char -> "'$it'" //quotes
            else -> "$it"
        }
        var insideQuotes = false
        val index = tmp.toCharArray().indexOfFirst { c ->
            if(c == '\''){
                insideQuotes = !insideQuotes
                false
            } else if(c == '?'){
                !insideQuotes
            } else {
                false
            }
        }
        tmp = tmp.replaceRange(index, index + 1, processed)
    }
    return tmp

}

fun <T : Any> getPrimitiveTypeFromResultSet(set: ResultSet, propName: String, clazz: KClass<T>) : Any?{
    return when(clazz) {
        String::class -> set.getString(propName)
        Double::class -> set.getDouble(propName)
        Float::class -> set.getFloat(propName)
        Byte::class -> set.getByte(propName)
        Short::class -> set.getShort(propName)
        Int::class -> set.getInt(propName)
        Long::class -> set.getLong(propName)
        Char::class -> set.getInt(propName).toChar()
        Boolean::class -> set.getBoolean(propName)
        else -> null
    }
}

fun getSqlFieldName(props : Iterable<KProperty1<*, *>>) = props.map { it.name }.joinToString("_")

val kotlinTypeMap = mapOf(
        String::class to "VARCHAR(2000)",
        Byte::class to "TINYINT",
        Short::class to "SMALLINT",
        Int::class to "INTEGER",
        Long::class to "BIGINT",
        Float::class to "REAL",
        Double::class to "FLOAT",
        Boolean::class to "BIT",
        Date::class to "DATE"
)

val typeMap = kotlinTypeMap.mapKeys { it.key.java }