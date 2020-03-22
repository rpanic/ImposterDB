package json

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import db.Backend
import db.DB
import db.DetachedObjectReadWriteProperty
import observable.Observable
import observable.observableListOf
import java.io.File
import java.io.FileReader
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

open class JsonBackend : Backend {

    override fun <T : Observable> createSchema(clazz: KClass<T>) {
    }

    override fun <T : Observable, K> loadByPK(key: String, pk: K, clazz: KClass<T>): T {
//        return klaxon.parseFromJsonArray2(clazz, klaxon.parser(clazz).parse(FileReader(this.baseFile.child("$key.json"))) as JsonArray<*>).find { it.key<K>() == pk }!!
        println("loadByPk $pk $key ${clazz.simpleName} ")
        val list = load(key, clazz)
        DB.cache.putComplete(key, observableListOf(list))
        return list.find { it.key<K>() == pk }!!
    }
    }

    override fun <T : Observable> loadAll(key: String, clazz: KClass<T>): List<T> {
//        return klaxon.parseFromJsonArray2(clazz, klaxon.parser(clazz).parse(FileReader(this.baseFile.child("$key.json"))) as JsonArray<*>)
        println("loadAll $key ${clazz.simpleName} ")
        return load(key, clazz)
    }

    fun <T : Observable> load(key: String, clazz: KClass<T>) : List<T> {
        val arr = klaxon.parseJsonArray(FileReader(this.baseFile.child("$key.json"))) as JsonArray<JsonObject>
        val properties = findDelegatingProperties(clazz, DetachedObjectReadWriteProperty::class)
        val values = properties.map { arr.map { json -> json.string("uuid") to json.string(it.name).apply { json.remove(it.name) } } }

        val list = klaxon.parseFromJsonArray2(clazz, arr)
        values.forEachIndexed { i, propValues ->
            val property = properties[i]
            propValues.forEach { pair ->
                val observable = list.find { it.uuid == pair.first }!!
                val delegate = property.getDelegate(observable) as DetachedObjectReadWriteProperty<*>
                delegate.setPk(pair.second)
            }

        }
        return list
    }

    override fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>) {
        println("update ${obj.key<Any>()} ${prop.name} $key ${clazz.simpleName} ")
        if(prop.name == "uuid"){
            println("")
        }
        save(key, clazz)
    }

    override fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K) {
        println("delete $pk $key ${clazz.simpleName} ")
        loadIfNotLoaded(key, clazz)
        DB.cache.getComplete<T>(key)!!.removeAt(DB.cache.getComplete<T>(key)!!.indexOfFirst { it.key<K>() == pk })
        save(key, clazz)
    }

    fun <T : Observable> loadIfNotLoaded(key: String, clazz: KClass<T>){
        if(!DB.cache.containsComplete(key)){
            DB.cache.putComplete(key, observableListOf(loadAll(key, clazz)))
        }
    }

    override fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T) {
        println("insert ${obj.key<Any>()} $key ${clazz.simpleName} ")
        loadIfNotLoaded(key, clazz)
        DB.cache.getComplete<T>(key)!!.add(obj)
        save(key, clazz)
    }

    fun <T : Observable> save(key: String, clazz : KClass<T>) {
        val list = DB.cache.getComplete<T>(key)
        val jsonString = klaxon.toJsonString(list!!.list())
        val json = klaxon.parseJsonArray(jsonString.reader()) as JsonArray<JsonObject>

        json.forEach {
            val uuid = it.string("uuid")
            val obj = list.find { it.uuid == uuid }!!
            clazz.memberProperties.forEach { prop ->
                prop.isAccessible = true
                val delegate = prop.getDelegate(obj)
                if(delegate != null){
                    if(delegate is DetachedObjectReadWriteProperty<*>){
                        if(delegate.isInitialized())
                            it.set(prop.name, (prop.get(obj) as Observable).uuid)
                        else if(delegate.getPkOrNull<String>() != null){
                            it.set(prop.name, delegate.getPkOrNull()) //TODO Unify Usage of PK, not necessarly uuid
                        }else
                            it.remove(prop.name)
                    }
                }
            }
        }
        this.baseFile.child("$key.json").writeText(json.toJsonString())
    }

    val klaxon = Klaxon()

    val baseFile = userdir().child("data/")

//    constructor(basedir: File){ //TODO Better configurability of Json Backend
//        baseFile = basedir
//    }

    override fun keyExists(key: String) = this.baseFile.child("$key.json").exists()

}

fun userdir() = File(System.getProperty("user.dir"))

fun File.child(s: String) = File(this.absolutePath + "${File.separator}$s").apply { if(name.lastIndexOf('.') == -1){ mkdir() } }