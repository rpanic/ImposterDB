package json

import collections.Indexable
import ruleExtraction1.Step
import ruleExtraction1.StepInterpreter
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import db.DBBackend
import db.DetachedObjectReadWriteProperty
import example.findDelegatingProperties
import observable.LevelInformation
import observable.Observable
import java.io.File
import java.io.FileReader
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

open class JsonBackend : DBBackend() {

    val loaded = mutableMapOf<String, MutableList<*>>()

    override fun <T : Observable> createSchema(key: String, clazz: KClass<T>) {
        println("createSchema $key ${clazz.simpleName}")
        if(this.baseFile.child("$key.json").exists())
            throw UnsupportedOperationException()
        this.baseFile.child("$key.json").writeText("[]")
    }

    override fun <T : Observable> load(key: String, clazz: KClass<T>, steps: List<Step<T, *>>): Set<T> {
        println("load $key ${clazz.simpleName} [${steps.map { it.javaClass.simpleName }.joinToString(", ")}]")
        val loaded = load(key, clazz)
        return StepInterpreter.interpretSteps(steps, loaded.toSet(), clazz)
    }

    fun <T : Observable> load(key: String, clazz: KClass<T>) : List<T> {
        val arr = klaxon.parseJsonArray(FileReader(this.baseFile.child("$key.json"))) as JsonArray<JsonObject>
        val properties = findDelegatingProperties(clazz, DetachedObjectReadWriteProperty::class)
        val pk = Indexable.getKeyProperty(clazz)
        val values = properties.map { arr.map { json -> json.string(pk.name) to json.string(it.name).apply { json.remove(it.name) } } }

        val list = klaxon.parseFromJsonArray2(clazz, arr)
        values.forEachIndexed { i, propValues ->
            val property = properties[i]
            propValues.forEach { pair ->
                val observable = list.find { it.keyValue<T>() == pair.first }!!
                val delegate = property.getDelegate(observable) as DetachedObjectReadWriteProperty<*>
                delegate.setPk(pair.second)
            }

        }
        loaded[key] = list.toMutableList()
        return list
    }

    override fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>, levelInformation: LevelInformation) {
        println("update ${obj.keyValue<T>()} ${prop.name} $key ${clazz.simpleName} ")
        
        save(key, clazz)
    }
    
    override fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K) {
        println("delete $pk $key ${clazz.simpleName} ")
        loadIfNotLoaded(key, clazz)
        (loaded[key] as? MutableList<T>)!!.removeIf { it.keyValue<T>() == pk }
        save(key, clazz)
    }

    //is used for JsonBackend to not overwrite data, since it saves the collection which is loaded atm, and does not really insert
    fun <T : Observable> loadIfNotLoaded(key: String, clazz: KClass<T>){
        if(!keyExists(key)){
            createSchema(key, clazz)
        }
        if(keyExists(key) && !loaded.containsKey(key)){
            load(key, clazz, listOf())
        }
    }

    override fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T) {
        println("insert ${obj.keyValue<T>()} $key ${clazz.simpleName} ")
        loadIfNotLoaded(key, clazz)
        (loaded[key] as? MutableList<T>)!!.add(obj)
        save(key, clazz)
    }
    
    override fun <T : Observable, V : Any> loadTransformed(key: String, clazz: KClass<T>, steps: List<Step<T, *>>, to: KClass<V>): Set<V> {
        TODO("Not yet implemented")
    }
    
    fun <T : Observable> save(key: String, clazz : KClass<T>) {
        val intermediateList = if(loaded.containsKey(key)) (loaded[key] as List<T>) else listOf()
        val list = intermediateList.distinctBy { it.keyValue<T>() }
        if(intermediateList.size != list.size) {
            println("Something is wrong with the caching")
        }

        val jsonString = klaxon.toJsonString(list)
        val json = klaxon.parseJsonArray(jsonString.reader()) as JsonArray<JsonObject>

        json.forEach {

            val pk = it.string(Indexable.getKeyProperty(clazz).name)
            val obj = list.find { obj -> obj.keyValue<T>() == pk }!!
            clazz.memberProperties.forEach { prop ->
                prop.isAccessible = true
                val delegate = prop.getDelegate(obj)
                if(delegate != null){

                    //Remove Detached Objects
                    if(delegate is DetachedObjectReadWriteProperty<*>){
                        if(delegate.isInitialized())
                            it.set(prop.name, (prop.get(obj) as Observable).keyValue<Observable>())
                        else if(delegate.getPkOrNull<String>() != null){
                            it.set(prop.name, delegate.getPkOrNull()) 
                        }else
                            it.remove(prop.name)
                    }
                }
            }
        }
        this.baseFile.child("$key.json").writeText(json.toJsonString())
    }

    val klaxon = Klaxon().apply { converter(ObservableConverter(this, hashMapOf())) }

    val baseFile = userdir().child("data/")

//    constructor(basedir: File){ //TODO Better configurability of Json Backend
//        baseFile = basedir
//    }

    override fun keyExists(key: String) = this.baseFile.child("$key.json").exists()

}

fun userdir() = File(System.getProperty("user.dir"))

fun File.child(s: String) = File(this.absolutePath + "${File.separator}$s").apply { if(name.lastIndexOf('.') == -1){ mkdir() } }