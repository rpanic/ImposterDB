package db

import observable.Observable
import kotlin.reflect.KClass

interface Backend {

    fun keyExists(key: String) : Boolean

    fun createKey(key: String) //TODO Needed?

    fun <T : Observable, K> loadByPK (key: String, pk: K, clazz: KClass<T>) : T

    fun <T : Observable> loadAll (key: String, clazz: KClass<T>) : List<T>

    fun <T : Observable> loadSingleton (key: String, clazz: KClass<T>) : T

    //TODO Definieren von Save Methoden

    fun <T : Observable> save(key: String, clazz: KClass<T>, obj: T)

    fun <T : Observable> saveList(key: String, clazz: KClass<T>, obj: List<T>)

}

annotation class Ignored {

}