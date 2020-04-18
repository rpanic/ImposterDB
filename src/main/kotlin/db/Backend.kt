package db

import aNewCollections.Step
import observable.DBAwareObject
import observable.Observable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

interface Backend {

    fun keyExists(key: String) : Boolean

    fun <T : Observable> createSchema(key: String, clazz: KClass<T>)

    fun <T : Observable, K> loadByPK (key: String, pk: K, clazz: KClass<T>) : T? //TODO Make nullable

    fun <T : Observable> load(key: String, clazz: KClass<T>, steps: List<Step<T, *>>) : Set<T>

    fun <T : Observable> loadAll (key: String, clazz: KClass<T>) : List<T>

    fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>)

    fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K)

    fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T)
}

abstract class DBBackend : DBAwareObject(), Backend {
}

annotation class Ignored {

}