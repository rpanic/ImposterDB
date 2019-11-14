package json

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import db.Backend
import db.Observable
import java.io.File
import java.io.FileReader
import kotlin.reflect.KClass


class JsonBackend : Backend {

    val klaxon = Klaxon()

    val baseFile = userdir().child("data/")

    override fun keyExists(key: String) = this.baseFile.child("$key.json").exists()

    override fun <T : Observable> load(key: String, clazz: KClass<T>): T {
        return klaxon.fromJsonObject(klaxon.parser(clazz).parse(FileReader(this.baseFile.child("$key.json"))) as JsonObject, clazz.java, clazz) as T
    }

    override fun <T : Observable> loadList(key: String, clazz: KClass<T>): List<T> {
        return klaxon.parseFromJsonArray2(clazz, klaxon.parser(clazz).parse(FileReader(this.baseFile.child("$key.json"))) as JsonArray<*>)
    }

    override fun <T : Observable> save(key: String, clazz: KClass<T>, obj: T) {
        val json = klaxon.toJsonString(obj)
        this.baseFile.child("$key.json").writeText(json)
    }

    override fun <T : Observable> saveList(key: String, clazz: KClass<T>, obj: List<T>) {
        val json = klaxon.toJsonString(obj)
        this.baseFile.child("$key.json").writeText(json)
    }

}

fun userdir() = File(System.getProperty("user.dir"))

fun File.child(s: String) = File(this.absolutePath + "${File.separator}$s").apply { if(name.lastIndexOf('.') == -1){ mkdir() } }