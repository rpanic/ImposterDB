package db

import ruleExtraction.Step
import observable.DBAwareObject
import observable.LevelInformation
import observable.Observable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface Backend {

    fun keyExists(key: String) : Boolean

    fun <T : Observable> createSchema(key: String, clazz: KClass<T>)

    fun <T : Observable, K> loadByPK (key: String, pk: K, clazz: KClass<T>) : T? //TODO Replace by load(FilterStep)

    fun <T : Observable> load(key: String, clazz: KClass<T>, steps: List<Step<T, *>>) : Set<T>

    fun <T : Observable> loadAll (key: String, clazz: KClass<T>) : List<T>

    fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>, levelInformation: LevelInformation)

    fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K)

    fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T)
    
    /**
     * Loads Records with some transformation going on before that.
     * f.e. "SELECT COUNT(*) FROM key
     * or "SELECT LOWER(prop) FROM key
     * Records should be parsed into a Object of type T which is guaranteed to be parseable form the result
     */
    fun <T : Observable, V: Any> loadTransformed(key: String, clazz: KClass<T>, steps: List<Step<T, *>>, to: KClass<V>) : Set<V>
}

abstract class DBBackend : DBAwareObject(), Backend {
}

annotation class Ignored {

}