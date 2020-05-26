package main.kotlin.connection

import ruleExtraction1.Step
import com.beust.klaxon.internal.firstNotNullResult
import connection.ObjectCache
import db.*
import example.findDelegatingProperties
import observable.LevelInformation
import observable.Observable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

//<|°_°|>
class BackendConnector (private val cache: ObjectCache, private val db: DB){

    private val backends = mutableListOf<Backend>()

    fun addBackend(backend: Backend) {
        backends += backend
    }

    val initialized = mutableListOf<String>()

    //TODO Unify usage of initIfNotYet and createSchema calls in backendConnector
    fun <T : Observable> initIfNotYet(key: String, clazz: KClass<T>){
        if(key !in initialized) {
            forEachBackend {
                it.createSchema(key, clazz)
            }
            initialized += key
        }
    }

    fun <T : Observable> loadWithRules(key: String, steps: List<Step<T, *>>, clazz: KClass<T>): Set<T> {

        initIfNotYet(key, clazz)
        
        val read = backends.firstOrNull()?.load(key, clazz, steps)

        read?.forEach {
            resolveRelations(key, clazz, it)
//            it.setDbReference(db)
        }

        return read ?: setOf()

    }
    
    fun <T : Observable, V: Any> loadTransformed(key: String, steps: List<Step<T, *>>, clazz: KClass<T>, to: KClass<V>) : Set<V>{
    
        initIfNotYet(key, clazz)
    
        val read = backends.firstOrNull()?.loadTransformed(key, clazz, steps, to)
        
        return read ?: setOf()
        
    }

    fun <T : Observable, K : Any> loadByPK(key: String, pk: K, clazz: KClass<T>): T? {
    
        initIfNotYet(key, clazz)
        
        val read = BackendObjectRetriever<T>(backends, cache)
            .tryCache { getCachedObject(key, pk) }
            .orBackends {
                val loaded = it.loadByPK(key, pk, clazz)
                if(loaded != null) {
                    resolveRelations(key, clazz, loaded)
                }
                loaded
            }
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

    fun <T : Observable> resolveRelations(key: String, clazz: KClass<T>, obj: T) = resolveRelations(key, clazz, listOf(obj))

    //Only for loading purposes
    fun <T : Observable> resolveRelations(key: String, clazz: KClass<T>, list: List<T>) {
        val oneToNProperties = findDelegatingProperties(clazz, DetachedObjectReadWriteProperty::class)
        val mToNProperties = findDelegatingProperties(clazz, VirtualSetReadOnlyProperty::class)

        //1 to n
        //TODO

        //m to n
        mToNProperties.forEach { prop ->

            if(list.isNotEmpty()) {

                list.forEach {
                    val delegate = prop.getDelegate(it)
                    if (delegate is VirtualSetReadOnlyProperty<*, *>) {

                        delegate.setParentKey(key)

                    } else {
                        throw IllegalStateException("Should definitely not happen")
                    }
                }

                //Old code
//                val firstDelegate = prop.getDelegate(list[0]) as DetachedListReadOnlyProperty<Observable, *>
//                val table = MtoNTable(key, firstDelegate.key)

//                val tabledata = loadList(table.tableName(), MtoNTableEntry::class)!!
//                list.forEach { obj ->
//                    val delegate = prop.getDelegate(obj)
//
//                    val references = tabledata
//                            .filter { (if (table.namesFlipped()) it.getNKey<Any>() else it.getMKey()) == obj.key<Any>() }
//                            .map { if (table.namesFlipped()) it.getMKey<Any>() else it.getNKey() }
//
//                    if (delegate is DetachedListReadOnlyProperty<*>) {
//
//                        delegate.setArgs(table, references)
//
//                    } else {
//                        throw IllegalStateException("Should definitely not happen")
//                    }
//                }

            }

        }

    }

//    fun <T : Observable> loadList(key: String, clazz: KClass<T>) : ObservableArrayList<T>?{
//
//        val thenCache: ObjectCache.(ObservableArrayList<T>) -> Unit = {
//            if(!containsComplete(key)) {
//            putComplete(key, it)
//
//            it.collection.forEach { obj ->
//                if (!this.containsObject(key, obj.key())) {
//                    this.putObject(key, obj)
//                }
//            }
//        }}
//
//        if(backends.none { it.keyExists(key) }){
//            forEachBackend { it.createSchema(key, clazz) }
//            val list = observableListOf<T>()
//            thenCache(cache, list)
//            return list
//        }
//
//        return BackendObjectRetriever<ObservableArrayList<T>>(backends, cache)
//                .tryCache { getComplete(key) }
//                .orBackends {
//                    val loaded = observableListOf(it.loadAll(key, clazz))
//                    loaded.setDbReference(db)
//                    loaded.collection.forEach { item -> item.setDbReference(db) }
//                    resolveRelations(key, clazz, loaded)
//                    loaded
//                }
//                .thenCache (thenCache)
//
//    }

    fun <T : Observable> insert(key: String, obj: T, clazz: KClass<T>) {
        //Add Table definition for new Objects which got constructed like T()
        obj::class.memberProperties.forEach {
            it.isAccessible = true
            val virtualSetDelegate = ((it as? KProperty1<Any?, Any?>)?.getDelegate(obj) as? VirtualSetReadOnlyProperty<Observable, *>)
            virtualSetDelegate?.setParentKey(key)
        }

        obj.setDbReference(db)

        if(!cache.containsObject(key, obj.keyValue<T, Any>())) { //Since all Objects which can be inserted have to be loaded and therefore put into the cache, this Check is sufficient
            forEachBackend {
                if(!it.keyExists(key)){
                    it.createSchema(key, clazz)
                }
                it.insert(key, clazz, obj)
            }
            cache.putObject(key, obj)
        }
    }

    fun <T : Observable> update(key: String, obj: T, clazz: KClass<T>, prop: KProperty<*>, levelInformation: LevelInformation){
        forEachBackend {
            it.update(key, clazz, obj, prop, levelInformation)
        }
    }

    fun <T : Observable, K : Any> delete(key: String, pk: K, clazz: KClass<T>){

        //TODO Make a decicion whether delete should be called with a PK or Object and then unify the interfaces
        val pk2 = if(pk is Observable){
            pk.keyValue<T, Any>()
        }else
            pk

        //TODO Think about how to handle deletes
        forEachBackend {
            it.delete(key, clazz, pk2)
        }
        cache.removeObject(key, pk2)
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
                backend.createSchema(key, clazz)
                null
            }
        }
    }

    private fun forEachBackend(f: (Backend) -> Unit){
        backends.forEach(f)
    }

}