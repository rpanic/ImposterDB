package sql

import db.Backend
import db.VirtualSetReadOnlyProperty
import example.ReflectionUtils
import io.zeko.db.sql.Delete
import io.zeko.db.sql.Insert
import io.zeko.db.sql.Query
import io.zeko.db.sql.Update
import io.zeko.db.sql.dsl.*
import io.zeko.model.Entity
import mu.KotlinLogging
import observable.LevelInformation
import observable.Observable
import observable.ObservableLevel
import ruleExtraction1.*
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.reflect.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

class SqlBackend (
        val connectionString: String,
        val dbname: String,
        val user: String,
        val password: String,
        val driver: String? = "com.mysql.jdbc.Driver",
        val createContextFun: (SqlBackend) -> SqlContext = SqlBackend::createContext
) : Backend{

    val logger = KotlinLogging.logger("SQLBackend")

    val context: SqlContext

    init {
        context = createContextFun(this)
    }

    private fun createContext(): SqlContext {
        val connString = connectionString + (if (connectionString.endsWith("/")) "" else "/") + dbname

        if(driver != null){
            Class.forName(driver).newInstance()
        }
        val conn = DriverManager.getConnection(connString, user, password)

        return SqlContext(conn, dbname )
    }

    override fun keyExists(key: String): Boolean {
        return context.checkIfTableExists(key)
    }

    override fun <T : Observable> createSchema(key: String, clazz: KClass<T>) {
        context.createOrUpdateTable(key, clazz)
    }
    
    override fun <T : Observable, V : Any> loadTransformed(key: String, clazz: KClass<T>, steps: List<Step<T, *>>, to: KClass<V>): Set<V> {
    
        val query = createQueryFromClass(clazz, key)
        
        val sql = SqlStepInterpreter.interpretSteps(query, steps)
        
        logger.info(sql)
        
        val rs = context.executeQuery(sql)
    
        return parse(rs, to)
        
    }

    private fun <T : Observable> createQueryFromClass(clazz: KClass<T>, key: String) : Query{
        return Query()
                .fields(*ReflectionUtils.getPropertySqlNames(clazz).map { it.first }.toTypedArray()) //TODO Check if performance is better when using *
                .from(key)
    }
    
    override fun <T : Observable> load(key: String, clazz: KClass<T>, steps: List<Step<T, *>>): Set<T> {
    
        return loadTransformed(key, clazz, steps, clazz)

    }

    fun <T : Any> parse(set : ResultSet, clazz: KClass<T>) : Set<T>{
        val parsed = mutableSetOf<T>()
        while(set.next()) {
            if(clazz in kotlinTypeMap.keys){
                check(set.metaData.columnCount == 1){ "Result must have only one column to be parsed into type ${clazz.simpleName}" }
                parsed.add(getPrimitiveTypeFromResultSet(set, set.metaData.getColumnName(0), clazz) as T)
            }else{
                parsed.add(parseClass(set, clazz))
            }
            
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
            
            val value = getPrimitiveTypeFromResultSet(set, propName, (type as Class<Any>).kotlin) //If primitive type returns null, parse Class recursively
                    ?: parseClass(set, (type as Class<Any>).kotlin, prefix + it.name + "_")
            
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
                .replaceWildCards(obj.keyValue<T>().toString())

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


    class GenericEntity : Entity {
        constructor(map: Map<String, Any?>) : super(map)
        constructor(vararg props: Pair<String, Any?>) : super(*props)
        var test: String? by map
    }
    
    companion object {
        internal fun createDefaultSqlBackend() =
                SqlBackend("jdbc:mysql://localhost",
                        "test",
                        "root",
                        "root")
    
    }
    
}