package sql

import aNewCollections.*
import aNewCollections.eq
import db.Backend
import db.Ignored
import db.VirtualSetReadOnlyProperty
import example.GenericEntity
import example.ReflectionUtils
import example.info
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.zeko.db.sql.Insert
import io.zeko.db.sql.Query
import io.zeko.db.sql.Update
import io.zeko.db.sql.connections.HikariDBPool
import io.zeko.db.sql.connections.HikariDBSession
import io.zeko.db.sql.dsl.*
import io.zeko.model.Select
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import observable.Observable
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.reflect.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

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
                .fields(*ReflectionUtils.getPropertySqlNames(clazz).map { it.first }.toTypedArray()) //TODO Check if performance is better when using *
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

    override fun <T : Observable> loadAll(key: String, clazz: KClass<T>): List<T> { //TODO To Set
        return load(key, clazz, listOf()).toList()
    }

    fun <T : Observable> parse(set : ResultSet, clazz: KClass<T>) : Set<T>{
        val parsed = mutableSetOf<T>()
        while(set.next()) {
            parsed.add(parseClass(set, clazz))
        }

        return parsed
    }

    fun <T : Any> parseClass(set : ResultSet, clazz: KClass<T>, prefix: String = "") : T{

        val instance = clazz.createInstance()
        val props = ReflectionUtils.getNotIgnoredOrDelegatedProperties(clazz)
                .filter { it.isAccessible = true; it.getDelegate(instance) !is VirtualSetReadOnlyProperty<*, *> }

        props.forEach {
            val propName = prefix + it.name
            val type = it.returnType.javaType
            val value = when(type){
                String::class.java -> set.getString(propName)
                Double::class.java -> set.getDouble(propName)
                Float::class.java -> set.getFloat(propName)
                Byte::class.java -> set.getByte(propName)
                Short::class.java -> set.getShort(propName)
                Int::class.java -> set.getInt(propName)
                Long::class.java -> set.getLong(propName)
                Char::class.java -> set.getInt(propName).toChar()
                Boolean::class.java -> set.getBoolean(propName)
                else -> parseClass(set, (type as Class<Any>).kotlin, prefix + it.name + "_")
            }
            (it as KMutableProperty1<T, Any>).set(instance, value)
        }
        return instance
    }

    override fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>) {
        TODO("Not yet implemented")
    }

    override fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K) {
        TODO("Not yet implemented")
    }

    override fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T) {
        val sql = Insert(GenericEntity(getPropertyMap(obj, clazz))).toSql()
                .replace("generic_entity", key)
        val res = context.executeUpdate(sql)
        info("Insert in $key: ${if(res == 1) "OK" else "NOT OK"}") //TODO Debug
    }

    fun <T : Observable> getPropertyMap(t: T, clazz: KClass<T>) : Map<String, Any> {

        return ReflectionUtils.getPropertyTree(clazz)
                .map { it.first to ReflectionUtils.getValue(it.second, t) }
                .toMap()
    }

}