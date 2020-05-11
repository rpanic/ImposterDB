package sql

import aNewCollections.*
import db.Backend
import db.VirtualSetReadOnlyProperty
import example.GenericEntity
import example.ReflectionUtils
import example.info
import example.logger
import io.zeko.db.sql.Delete
import io.zeko.db.sql.Insert
import io.zeko.db.sql.Query
import io.zeko.db.sql.Update
import io.zeko.db.sql.dsl.*
import io.zeko.db.sql.operators.eq
import mu.KotlinLogging
import observable.LevelInformation
import observable.Observable
import observable.ObservableLevel
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.reflect.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

class SqlBackend (
        val url: String = "jdbc:mysql://localhost", //TODO Remove default (+user, password, dbName)
        val dbname: String = "test",
        val user: String = "root",
        val password: String = "root",
        val driver: String? = "com.mysql.jdbc.Driver",
        val createContextFun: (SqlBackend) -> SqlContext = SqlBackend::createContext
) : Backend{

    val logger = KotlinLogging.logger("SQLBackend")

    val context: SqlContext

    init {
        context = createContextFun(this)
    }

    private fun createContext(): SqlContext {
        val connectionString = url + (if (url.endsWith("/")) "" else "/") + dbname

        if(driver != null){
            Class.forName(driver).newInstance()
        }
        val conn = DriverManager.getConnection(connectionString, user, password)

        return SqlContext(conn, dbname )
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

        val (query, valuesToReplace) = createFilterQuery(key, clazz, steps)

        val sql = query.toSql()
                .replaceWildCards(valuesToReplace)
        
        logger.info(sql)

        val rs = context.executeQuery(sql)

        return parse(rs, clazz)

    }
    
    private fun <T : Observable> createFilterQuery(key: String, clazz: KClass<T>, steps: List<Step<T, *>>) : Pair<Query, List<Any>>{
        val query = Query()
                .fields(*ReflectionUtils.getPropertySqlNames(clazz).map { it.first }.toTypedArray()) //TODO Check if performance is better when using *
                .from(key)
    
        val valuesToReplace = mutableListOf<Any>()
    
        steps.forEach { step ->
            evaluateStep(step, query, valuesToReplace)
        }
        return query to valuesToReplace
    }

    private fun evaluateStep(step: Step<*, *>, query: Query, valuesToReplace: MutableList<Any>) {

        if(step is FilterStep<*>){
            step.conditions.forEach { condition ->
                if(condition is NormalizedCompareRule<*>) {
                    val field = getSqlFieldName(condition.prop!!)
                    when(condition.type){
                        CompareType.EQUALS -> query.where(field eq "")
                        CompareType.NOT_EQUALS -> query.where(field neq "")
                        CompareType.LESS -> query.where(field less "")
                        CompareType.LESS_EQUALS -> query.where(field lessEq "")
                        CompareType.GREATER -> query.where(field greater "")
                        CompareType.GREATER_EQUALS -> query.where(field greaterEq "")
                    }
                    val value = condition.obj2!!
                    valuesToReplace += value
                }
            }
        }else if(step is FindStep<*>){
            query.limit(1)
            evaluateStep(step.filter, query, valuesToReplace)
        }
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
            (it as KMutableProperty1<T, Any?>).set(instance, value)
        }
        return instance
    }

    override fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>, levels: LevelInformation) {

        val props = levels.list.map { it as ObservableLevel }.map { it.prop as KProperty1<Any, Any> }
        val value = ReflectionUtils.getValue(props, obj)

        val pkProp = ReflectionUtils.getPkOfClass(clazz)

        var sql = Update(GenericEntity(getSqlFieldName(props) to value))
                .where(pkProp.name eq "")
                .toSql()
                .replace("generic_entity", key)
                .replaceWildCards(obj.keyValue<T, Any>().toString())

        logger.info(sql)

        val res = context.executeUpdate(sql)

        logger.info("Updated $res records in table $key")
    }

    override fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K) {

        val pkProp = ReflectionUtils.getPkOfClass(clazz)

        val sql = Delete(GenericEntity())
                .where(pkProp.name eq "")
                .toSql()
                .replace("generic_entity", key)
                .replaceWildCards(pk.toString())

        logger.info(sql)
        val res = context.executeUpdate(sql)

        logger.info("Deleted $res records in table $key")
    }

    override fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T) {
        val sql = Insert(GenericEntity(getPropertyMap(obj, clazz))).toSql()
                .replace("generic_entity", key)

        logger.info(sql)
        val res = context.executeUpdate(sql)
        logger.info("Insert in $key: ${if(res == 1) "OK" else "NOT OK"}") //TODO Debug
    }
    
    fun <T : Observable> getPropertyMap(t: T, clazz: KClass<T>) : Map<String, Any> {

        return ReflectionUtils.getPropertyTree(clazz)
                .map { it.first to ReflectionUtils.getValue(it.second, t) }
                .toMap()
    }

}