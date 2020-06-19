package collections

import example.ReflectionUtils
import observable.DBAwareObject
import observable.Observable
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

abstract class Indexable : DBAwareObject(){

    //TODO Remove all direct references to uuid
    //TODO Enable other types than String to be PK, See f.e. MtoNTableEntry

    var uuid: String = UUID.randomUUID().toString()

    companion object {

        val cache = mutableMapOf<String, KProperty1<*, *>>()

        fun <T : Indexable> getKeyProperty(implementingClass: KClass<T>): KProperty1<T, *> {

            val cacheKey = implementingClass.qualifiedName!!
            if(cache.containsKey(cacheKey)){
                return cache[cacheKey]!! as KProperty1<T, *>
            }

            val props = ReflectionUtils.findMemberPropertiesWithAnnotation(implementingClass, PrimaryKey::class)

            check(props.size <= 1){ "You can only define one Property as Primary Key, Aggregate Keys are not supported (yet)" }

            val pkProp = if(props.isEmpty()){
                implementingClass.memberProperties.find { it.name == "uuid" }!!
            }else{
                val prop = props.first()
                check(!prop.returnType.isMarkedNullable){ "The Primary Key cannot be nullable" }

                prop
            }

            cache[cacheKey] = pkProp
            return pkProp
        }

    }

    fun <O : Observable> key() : KProperty1<O, Any>{

        return getKeyProperty(this.javaClass.kotlin) as KProperty1<O, Any>

    }

    fun <O : Observable> keyValue() : Any {
        return key<O>().get(this as O)
    }
}

annotation class PrimaryKey