package example

import db.Ignored
import db.VirtualSetReadOnlyProperty
import observable.Observable
import sql.getSqlFieldName
import sql.typeMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

object ReflectionUtils {

    fun <T : Any> getValue(list: List<KProperty1<Any, Any>>, t: T): Any {
        var value: Any = t
        for(prop in list){
            value = prop.get(value)
        }
        return value
    }

    fun <T : Any> getNotIgnoredOrDelegatedProperties(clazz: KClass<T>): List<KProperty1<T, Any>> {
        return clazz.memberProperties
                .filter { it.findAnnotation<Ignored>() == null }
                .map{ it as KProperty1<T, Any> }
                .filter { !(VirtualSetReadOnlyProperty::class.java == it.javaField?.type) }
    }

    fun <T : Any> getPropertyTree(clazz: KClass<T>, path: List<KProperty1<Any, Any>> = listOf()) : Set<Pair<String, List<KProperty1<Any, Any>>>> {
        return getNotIgnoredOrDelegatedProperties(clazz)
                .map {
                    val p = (path + it).map { it as KProperty1<Any, Any> }.toList()
                    if(it.returnType.javaType in typeMap.keys){
                        setOf(getSqlFieldName(p) to p)
                    }else{
                        getPropertyTree((it.returnType.javaType as Class<Any>).kotlin, p)
                    }
                }
                .flatten()
                .toSet()
    }

    fun <T : Any> getPropertySqlNames(clazz: KClass<T>, prefix: String = "") : Set<Pair<String, KProperty1<Any, Any>>>{
        return getNotIgnoredOrDelegatedProperties(clazz)
                .map {
                    if(it.returnType.javaType in typeMap.keys){
                        setOf(prefix + it.name to it as KProperty1<Any, Any>)
                    }else{
                        getPropertySqlNames((it.returnType.javaType as Class<Any>).kotlin, prefix + it.name + "_")
                    }
                }
                .flatten()
                .toSet()
    }

    fun <T : Observable> getPkOfClass(clazz: KClass<T>): KProperty1<T, Any> {
        return clazz.createInstance().key<T, Any>()
    }

}