package db

import ruleExtraction1.Step
import observable.DBAwareObject
import observable.LevelInformation
import observable.Observable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface Backend {
    
    /**
     * Checks if a certain key has been created previously
     * @param key: The key to check against
     */
    fun keyExists(key: String) : Boolean
    
    /**
     * Creates the schema of a given class
     * @param key: The key where the Schema should be created
     * @param clazz: KClass extending Observable with the Schema to be created.
     * @see example.ReflectionUtils for some utils
     */
    fun <T : Observable> createSchema(key: String, clazz: KClass<T>)
    
    /**
     * @see loadTransformed but without Mapping- or Aggregation Steps
     */
    fun <T : Observable> load(key: String, clazz: KClass<T>, steps: List<Step<T, *>>) : Set<T>
    
    /**
     * Update a object of Type T
     * @param key: Schema key
     * @param clazz: KClass<T> extending Observable
     * @param obj: Instance of Type clazz
     * @param prop: Property of obj which was changed
     * @param levelInformation: Information about the change possibly made in child Objects of obj
     */
    fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>, levelInformation: LevelInformation)

    /**
     * Delete all objects of Type T with a certain primary key
     * @param key: Schema key
     * @param clazz: KClass<T> extending Observable
     * @param pk: Primary Key of type K
     */
    fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K)
    
    /**
     * Inserts an object into a schema
     * @param key: Schema key
     * @param clazz: KClass of the object
     * @param obj: The object, extending Observable
     */
    fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T)
    
    /**
     * Loads Records with some transformation going on before that.
     * f.e. "SELECT COUNT(*) FROM key
     * or "SELECT LOWER(prop) FROM key
     * Records should be parsed into a Object of type T which is guaranteed to be parseable form the result
     * @param key: Schema key
     * @param clazz: KClass extending Observable representing the Observable instance which was used as base-point for the transformations. Should only be used if some steps cannot be done of the Backend-side
     * @param steps: All Steps to be applied in ascending order
     * @param to: Desired result KClass
     * @return Mutable or Immutable Set<V> with all loaded Data. This data is considered complete and accurate by ImposterDB
     */
    fun <T : Observable, V: Any> loadTransformed(key: String, clazz: KClass<T>, steps: List<Step<T, *>>, to: KClass<V>) : Set<V>
}

abstract class DBBackend : DBAwareObject(), Backend {
}

annotation class Ignored {

}