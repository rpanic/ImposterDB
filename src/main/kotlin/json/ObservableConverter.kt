package json

import com.beust.klaxon.*
import db.DetachedObjectReadWriteProperty
import db.Ignored
import db.VirtualSetReadOnlyProperty
import observable.Observable
import java.lang.reflect.ParameterizedType
import java.math.BigDecimal
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

//Copied from com com.beust.klaxon.DefaultConverter then modified
/**
 * The default Klaxon converter, which attempts to convert the given value as an enum first and if this fails,
 * using reflection to enumerate the fields of the passed object and assign them values.
 */
class ObservableConverter(private val klaxon: Klaxon, private val allPaths: HashMap<String, Any>) : Converter {
    override fun canConvert(cls: Class<*>) = Observable::class.java.isAssignableFrom(cls)

    override fun fromJson(jv: JsonValue): Any? {
        val value = jv.inside
        val propertyType = jv.propertyClass
        val result =
            when(value) {
                is Boolean, is String, is Long -> value
                is Int -> fromInt(value, propertyType)
                is Double ->
                    if (jv.propertyKClass?.classifier == kotlin.Float::class) fromFloat(value.toFloat(), propertyType)
                    else fromDouble(value, propertyType)
                is Float ->
                    if (jv.propertyKClass?.classifier == kotlin.Double::class) fromDouble(value.toDouble(),
                            propertyType)
                    else fromFloat(value, propertyType)
                is Collection<*> -> fromCollection(value, jv)
                is JsonObject -> fromJsonObject(value, jv)
                null -> null
                else -> {
                    throw KlaxonException("Don't know how to convert $value")
                }
            }
        return result

    }

    override fun toJson(value: Any): String {
        fun joinToString(list: Collection<*>, open: String, close: String)
                = open + list.joinToString(", ") + close

        val result = when (value) {
            is String, is Enum<*> -> "\"" + Render.escapeString(value.toString()) + "\""
            is Double, is Float, is Int, is Boolean, is Long -> value.toString()
            is Array<*> -> {
                val elements = value.map { klaxon.toJsonString(it) }
                joinToString(elements, "[", "]")
            }
            is Collection<*> -> {
                val elements = value.map { klaxon.toJsonString(it) }
                joinToString(elements, "[", "]")
            }
            is Map<*, *> -> {
                val valueList = arrayListOf<String>()
                value.entries.forEach { entry ->
                    val jsonValue = klaxon.toJsonString(entry.value as Any)
                    valueList.add("\"${entry.key}\": $jsonValue")
                }
                joinToString(valueList, "{", "}")
            }
            else -> {
                val valueList = arrayListOf<String>()
                val properties = Annotations.findNonIgnoredProperties(value::class, klaxon.propertyStrategies)
                        .filter { it.findAnnotation<Ignored>() == null }
                        .filter {
                            it.isAccessible = true
                            val delegate = ((it as? KProperty1<Any, Any?>)?.getDelegate(value)) ?: true
                            !(delegate is VirtualSetReadOnlyProperty<*, *> || delegate is DetachedObjectReadWriteProperty<*>)
                        }
                if (properties.isNotEmpty()) {
                    properties.forEach { prop ->
                        prop.getter.call(value)?.let { getValue ->
                            val jsonValue = klaxon.toJsonString(getValue, prop)
                            val fieldName = Annotations.retrieveJsonFieldName(klaxon, value::class, prop)
                            valueList.add("\"$fieldName\" : $jsonValue")
                        }
                    }
                    joinToString(valueList, "{", "}")
                } else {
                    """"$value""""
                }
            }

        }
        return result
    }

    private fun fromInt(value: Int, propertyType: java.lang.reflect.Type?): Any {
        // If the value is an Int and the property is a Long, widen it
        val isLong = java.lang.Long::class.java == propertyType || Long::class.java == propertyType
        return if (isLong) value.toLong() else value
    }

    private fun fromDouble(value: Double, propertyType: java.lang.reflect.Type?): Any {
        return if (propertyType == BigDecimal::class.java) {
            BigDecimal(value)
        } else {
            value
        }
    }

    private fun fromFloat(value: Float, propertyType: java.lang.reflect.Type?): Any {
        return if (propertyType == BigDecimal::class.java) {
            BigDecimal(value.toDouble())
        } else {
            value
        }
    }

    private fun fromCollection(value: Collection<*>, jv: JsonValue): Any {
        val kt = jv.propertyKClass
        val jt = jv.propertyClass
        val convertedCollection = value.map {
            // Try to find a converter for the element type of the collection
            if (jt is ParameterizedType) {
                val typeArgument = jt.actualTypeArguments[0]
                val converter =
                        when (typeArgument) {
                            is Class<*> -> klaxon.findConverterFromClass(typeArgument, null)
                            is ParameterizedType -> {
                                val ta = typeArgument.actualTypeArguments[0]
                                when (ta) {
                                    is Class<*> -> klaxon.findConverterFromClass(ta, null)
                                    is ParameterizedType -> klaxon.findConverterFromClass(ta.rawType.javaClass, null)
                                    else -> throw KlaxonException("SHOULD NEVER HAPPEN")
                                }
                            }
                            else -> throw IllegalArgumentException("Should never happen")
                        }
                val kTypeArgument = kt?.arguments!![0].type
                converter.fromJson(JsonValue(it, typeArgument, kTypeArgument, klaxon))
            } else {
                if (it != null) {
                    val converter = klaxon.findConverter(it)
                    converter.fromJson(JsonValue(it, jt, kt, klaxon))
                } else {
                    throw KlaxonException("Don't know how to convert null value in array $jv")
                }
            }

        }

        val result =
                if (Annotations.isSet(jt)) {
                    convertedCollection.toSet()
                } else if (Annotations.isArray(kt)) {
                    val componentType = kt?.jvmErasure?.java?.componentType
                    val array = java.lang.reflect.Array.newInstance(componentType, value.size)
                    convertedCollection.indices.forEach { i ->
                        java.lang.reflect.Array.set(array, i, convertedCollection[i])
                    }
                    array
                } else {
                    convertedCollection
                }
        return result
    }

    private fun fromJsonObject(value: JsonObject, jv: JsonValue): Any {
        val jt = jv.propertyClass
        val result =
            if (jt is ParameterizedType) {
                val isMap = Map::class.java.isAssignableFrom(jt.rawType as Class<*>)
                val isCollection = List::class.java.isAssignableFrom(jt.rawType as Class<*>)
                when {
                    isMap -> {
                        // Map
                        val result = linkedMapOf<String, Any?>()
                        value.entries.forEach { kv ->
                            val key = kv.key
                            kv.value?.let { mv ->
                                val typeValue = jt.actualTypeArguments[1]
                                val converter = klaxon.findConverterFromClass(
                                        typeValue.javaClass, null)
                                val convertedValue = converter.fromJson(
                                        JsonValue(mv, typeValue, jv.propertyKClass!!.arguments[1].type,
                                                klaxon))
                                result[key] = convertedValue
                            }
                        }
                        result
                    }
                    isCollection -> {
                        val type =jt.actualTypeArguments[0]
                        when(type) {
                            is Class<*> -> {
                                val cls = jt.actualTypeArguments[0] as Class<*>
                                klaxon.fromJsonObject(value, cls, cls.kotlin)
                            }
                            is ParameterizedType -> {
                                val result2 = JsonObjectConverter(klaxon, HashMap<String, Any>()).fromJson(value,
                                        jv.propertyKClass!!.jvmErasure)
                                result2

                            }
                            else -> {
                                throw IllegalArgumentException("Couldn't interpret type $type")
                            }
                        }
                    }
                    else -> throw KlaxonException("Don't know how to convert the JsonObject with the following keys" +
                            ":\n  $value")
                }
            } else {
                if ((jt as Class<*>).isArray) {
                    val typeValue = jt.componentType
                    klaxon.fromJsonObject(value, typeValue, typeValue.kotlin)
                } else {
                    JsonObjectConverter(klaxon, allPaths).fromJson(jv.obj!!, jv.propertyKClass!!.jvmErasure)
                }
            }
        return result
    }

}