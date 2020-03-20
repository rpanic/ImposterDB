package main.kotlin.connection

import com.beust.klaxon.internal.firstNotNullResult
import db.Backend
import db.DB
import observable.Observable
import observable.ObservableArrayList
import observable.observableListOf
import java.util.NoSuchElementException
import kotlin.reflect.KClass

class BackendConnector (private val cache: ObjectCache){

    private val backends = mutableListOf<Backend>()

    fun addBackend(backend: Backend) {
        backends += backend
    }

    val initialized = mutableListOf<KClass<*>>()

    fun <T : Observable> initIfNotYet(clazz: KClass<T>){
        if(clazz !in initialized) {
            backends.forEach { it.createSchema(clazz) }
            initialized += clazz
        }
    }

    fun <T : Observable, K : Any> loadByPK(key: String, pk: K, clazz: KClass<T>): T? {

        val read = BackendObjectRetriever<T>(backends, cache)
            .tryCache { getCachedObject(key, pk) }
            .orBackends { it.loadByPK(key, pk, clazz) }
            .thenCache { cache.putObject(key, it) }

        return read
//        return if(read != null) {
//            if (!cache.containsObject(key, pk)){
//                cache.putObject(key, read)
//            }
//            read
//        }else{
////            backends.forEach { it.createSchema(clazz) }
//            null
//        }

    }

    fun <T : Observable> loadList(key: String, clazz: KClass<T>) : ObservableArrayList<T>?{
        if(backends.none { it.keyExists(key) }){
            forEachBackend { it.createSchema(clazz) }
            return null
        }

        val list = BackendObjectRetriever<ObservableArrayList<T>>(backends, cache)
                .tryCache { getComplete(key) }
                .orBackends { observableListOf(it.loadAll(key, clazz)) }
                .thenCache {

                    if(!containsComplete(key)) {
                        putComplete(key, it)

                        it.collection.forEach { obj ->
                            if (!this.containsObject(key, obj.key())) {
                                this.putObject(key, obj)
                            }
                        }
                    }
                }

        return list

    }

    fun <T : Observable> insert(key: String, obj: T, clazz: KClass<T>) {
        backends.forEach {
            it.insert(key, clazz, obj)
        }
        cache.putObject(key, obj)
    }

    class BackendObjectRetriever <T : Any> (val backends : List<Backend>, val cache: ObjectCache){

        var obj: T? = null
        var cached = false

        fun tryCache(retrieve: ObjectCache.() -> T?) : BackendObjectRetriever<T>{
            obj = retrieve(cache)
            cached = obj != null
            return this
        }

        fun orBackends(retrieve: (Backend) -> T?) : BackendObjectRetriever<T>{
            if(!cached){
                obj = backends.firstNotNullResult(retrieve)
            }

            return this
        }

        fun thenCache(cache: ObjectCache.(T) -> Unit) : T?{
            val ret = obj ?: orElse()
            if(!cached && ret != null) {
                cache(this.cache, ret)
            }
            return ret// ?: throw NoSuchElementException("Object could not be found"))
        }

        var orElse: () -> T? = {
            null
        }

        fun orElse(f: () -> T?) : BackendObjectRetriever<T>{
            orElse = f
            return this
        }

    }

    fun <T : Any, V : Observable> retrieveObjectIfKeyExists(key: String, clazz: KClass<V>, f: () -> T) : T?{
        return (backends).firstNotNullResult { backend ->
            if(backend.keyExists(key)){
                f()
            }else {
                backend.createSchema(clazz)
                null
            }
        }
    }

    fun forEachBackend(f: (Backend) -> Unit){
        backends.forEach(f)
    }

}