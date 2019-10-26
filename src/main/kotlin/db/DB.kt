package db

import com.beust.klaxon.Json
import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import java.io.File
import java.io.FileReader
import java.nio.file.FileSystem

object DB{

    val klaxon = Klaxon()

    val baseFile = userdir().child("data/")

    inline fun <reified T : Observable> getList(key: String) : ObservableArrayList<T>{

        val file = baseFile.child("$key.json")

        val lread : List<T>? = if(file.exists()){
            klaxon.parseArray<T>(file)
        }else{
            listOf()
        }

        val list = observableListOf(*lread!!.toTypedArray())

        list.addListener { _, _ ->
            val s = klaxon.toJsonString(list.collection)
            file.writeText(s)
        }

        return list

    }

    inline fun <reified T : Observable> getObject(key: String, init : () -> T) : T{

        val file = baseFile.child("$key.json")

        val obj = if(file.exists()){
            klaxon.parse<T>(file)
        }else{
            init.invoke()
        }

        GenericChangeObserver(obj!!){
            val s = klaxon.toJsonString(obj)
            file.writeText(s)
        }

        return obj

    }

}


open class DBSingleton : Observable(){

    @Json(ignored = true)
    private var changedF = {}

    fun setListener(f: () -> Unit){
        changedF = f
    }

    fun changed(){
        changedF.invoke()
    }

}

fun userdir() = File(System.getProperty("user.dir"))

fun File.child(s: String) = File(this.absolutePath + "${File.separator}$s").apply { if(name.lastIndexOf('.') == -1){ mkdir() } }