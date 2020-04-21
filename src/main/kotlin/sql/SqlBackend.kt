package sql

import aNewCollections.Step
import db.Backend
import observable.Observable
import java.sql.DriverManager
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class SqlBackend : Backend{

    init {


    }

    override fun keyExists(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun <T : Observable> createSchema(key: String, clazz: KClass<T>) {
        TODO("Not yet implemented")
    }

    override fun <T : Observable, K> loadByPK(key: String, pk: K, clazz: KClass<T>): T? {
        TODO("Not yet implemented")
    }

    override fun <T : Observable> load(key: String, clazz: KClass<T>, steps: List<Step<T, *>>): Set<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Observable> loadAll(key: String, clazz: KClass<T>): List<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>) {
        TODO("Not yet implemented")
    }

    override fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K) {
        TODO("Not yet implemented")
    }

    override fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T) {
        TODO("Not yet implemented")
    }

}