package db

import kotlin.reflect.KClass

interface Backend {

    fun keyExists(key: String) : Boolean

    fun <T : Observable> load(key: String, clazz: KClass<T>) : T

    fun <T : Observable> loadList(key: String, clazz: KClass<T>) : List<T>

    fun <T : Observable> save(key: String, clazz: KClass<T>, obj: T)

    fun <T : Observable> saveList(key: String, clazz: KClass<T>, obj: List<T>)

}

annotation class Ignored {

}