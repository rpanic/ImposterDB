package json

import com.beust.klaxon.*
import kotlin.reflect.KClass

fun <T : Any> Klaxon.parseFromJsonArray2(clazz: KClass<T>, map: JsonArray<*>): List<T> {
    val result = arrayListOf<Any?>()
    map.forEach { jo ->
        if (jo is JsonObject) {
            val t = fromJsonObject(jo, clazz.java, clazz) as T?
            if (t != null) result.add(t)
            else throw KlaxonException("Couldn't convert $jo")
        } else if (jo != null) {
            val converter = findConverterFromClass(clazz.java, null)
            val convertedValue = converter.fromJson(JsonValue(jo, null, null, this))
            result.add(convertedValue)
        } else {
            throw KlaxonException("Couldn't convert $jo")
        }
    }
    @Suppress("UNCHECKED_CAST")
    return result as List<T>
}