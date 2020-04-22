package sql

import aNewCollections.*
import aNewCollections.eq
import db.Backend
import db.Ignored
import example.GenericEntity
import example.info
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.zeko.db.sql.Insert
import io.zeko.db.sql.Query
import io.zeko.db.sql.connections.HikariDBPool
import io.zeko.db.sql.connections.HikariDBSession
import io.zeko.db.sql.dsl.*
import io.zeko.model.Select
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import observable.Observable
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

class SqlBackend : Backend{

//    private val pool: HikariDBPool
//    private var session: HikariDBSession? = null

    val context: SqlContext

    init {
//        val mysqlConfig = json {
//            obj(
//                    "url" to "jdbc:mysql://localhost/test?user=root&password=root",
//                    "max_pool_size" to 5
//            )
//        }
//        pool = HikariDBPool(mysqlConfig)
//        GlobalScope.launch {
//            session = HikariDBSession(pool, pool.createConnection())
//        }

        Class.forName("com.mysql.jdbc.Driver").newInstance()
        val conn = DriverManager.getConnection("jdbc:mysql://localhost/test", "root", "root")

        context = SqlContext(conn, "test")

    }

    override fun keyExists(key: String): Boolean {
        return context.checkIfTableExists(key)
    }

    override fun <T : Observable> createSchema(key: String, clazz: KClass<T>) {
        context.createOrUpdateTable(key, clazz)
    }

    override fun <T : Observable, K> loadByPK(key: String, pk: K, clazz: KClass<T>): T? {
        TODO("Not yet implemented")
    }

    override fun <T : Observable> load(key: String, clazz: KClass<T>, steps: List<Step<T, *>>): Set<T> {

        val query = Query()
                .fields(*getProperties(clazz).map { it.name }.toTypedArray()) //TODO Check if performance is better when using *
                .from(key)

        steps.forEach { step ->
            if(step is FilterStep<*>){
                step.conditions.forEach { condition ->
                    if(condition is NormalizedCompareRule<*>) {
                        val field = getSqlFieldName(condition.prop!!)
                        val value = condition.obj2!!.toString()
                        when(condition.type){
                            CompareType.EQUALS -> query.where(field eq value)
                            CompareType.NOT_EQUALS -> query.where(field neq value)
                            CompareType.LESS -> query.where(field less value)
                            CompareType.LESS_EQUALS -> query.where(field lessEq value)
                            CompareType.GREATER -> query.where(field greater value)
                            CompareType.GREATER_EQUALS -> query.where(field greaterEq value)
                        }
                    }
                }
            }
        }

        val rs = context.executeQuery(query.toSql())

        return parse(rs, clazz)

    }

    override fun <T : Observable> loadAll(key: String, clazz: KClass<T>): List<T> {
        TODO("Not yet implemented")
    }

    fun <T : Observable> parse(set : ResultSet, clazz: KClass<T>) : Set<T>{
        return setOf()
    }

    override fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>) {
        TODO("Not yet implemented")
    }

    override fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K) {
        TODO("Not yet implemented")
    }

    override fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T) {
        val sql = Insert(GenericEntity(getPropertyStringMap(obj, clazz))).toSql()
                .replace("generic_entity", key)
        val res = context.executeUpdate(sql)
        info("Insert in $key: ${if(res == 1) "OK" else "NOT OK"}") //TODO Debug
    }

    fun <T : Observable> getPropertyStringMap(t: T, clazz: KClass<T>) : Map<String, Any?>{
        return getPropertyMap(t, clazz).map{ it.key.name to it.value }.toMap()
    }

    fun <T : Observable> getPropertyMap(t: T, clazz: KClass<T>) : Map<KProperty1<T, Any>, Any?>{

        return getProperties(clazz)
                .map { it to it.get(t) }
                .toMap()
    }

    fun <T : Observable> getProperties(clazz: KClass<T>) : List<KProperty1<T, Any>>{
        return clazz.memberProperties
                .filter { it.findAnnotation<Ignored>() == null }
                .map { it as KProperty1<T, Any> }
    }

}