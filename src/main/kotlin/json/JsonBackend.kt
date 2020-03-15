package json

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import db.Backend
import db.DB
import db.DetachedReadWriteProperty
import observable.Observable
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
        return klaxon.parseFromJsonArray2(clazz, klaxon.parser(clazz).parse(FileReader(this.baseFile.child("$key.json"))) as JsonArray<*>).find { it.key<K>() == pk }!!
    }

    override fun <T : Observable> loadAll(key: String, clazz: KClass<T>): List<T> {
        return klaxon.parseFromJsonArray2(clazz, klaxon.parser(clazz).parse(FileReader(this.baseFile.child("$key.json"))) as JsonArray<*>)
    }

    fun <T : Observable> load(key: String, clazz: KClass<T>) : List<T> {
        val arr = klaxon.parseJsonArray(FileReader(this.baseFile.child("$key.json"))) as JsonArray<JsonObject>
        val properties = findDelegatingProperties(clazz, DetachedReadWriteProperty::class)
        val values = properties.map { arr.map { json -> json.string("uuid") to json.string(it.name).apply { json.remove(it.name) } } }

        val list = klaxon.parseFromJsonArray2(clazz, arr)
        values.forEach {
            
        }
//        val properties = clazz.memberProperties
//                .map { it.apply { it.isAccessible = true } }
//                .filter { it. }
//        arr.string("uuid")
////        val arr = klaxon.parseFromJsonArray2(clazz, klaxon.parser(clazz).parse() as JsonArray<T>)
//        arr.forEach {
//            clazz.memberProperties.forEach { prop ->
//                prop.isAccessible = true
//                val delegate = prop.getDelegate(obj)
//                if(delegate != null){
//                    if(delegate is DetachedReadWriteProperty<*>){
//                        if(delegate.isInitialized())
//                            it.set(prop.name, (prop.get(obj) as Observable).uuid)
//                        else
//                            it.remove(prop.name)
//                    }
//                }
//            }
//        }
    }

    override fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>) {
        save(key, clazz)
    }

    override fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K) {
        save(key, clazz)
    }

    override fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T) {
        save(key, clazz)
    }

    fun <T : Observable> save(key: String, clazz : KClass<T>) {
        val list = DB.getCachedList<T>(key)
        val jsonString = klaxon.toJsonString(list!!.list())
        val json = klaxon.parseJsonArray(jsonString.reader()) as JsonArray<JsonObject>

        json.forEach {
            val uuid = it.string("uuid")
            val obj = list.find { it.uuid == uuid }!!
            clazz.memberProperties.forEach { prop ->
                prop.isAccessible = true
                val delegate = prop.getDelegate(obj)
                if(delegate != null){
                    if(delegate is DetachedReadWriteProperty<*>){
                        if(delegate.isInitialized())
                            it.set(prop.name, (prop.get(obj) as Observable).uuid)
                        else
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