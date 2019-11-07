package db

import com.beust.klaxon.Klaxon
import models.ChangeListElement
import models.ChangeProperty
import java.io.File

object DB {

    val klaxon = Klaxon()

    val baseFile = userdir().child("data/")

    val parsed = mutableMapOf<String, ObservableArrayList<*>>()

    val parsedObjects = mutableMapOf<String, Observable>()

    inline fun <reified T : Observable> getList(key: String): ObservableArrayList<T> {

        if (parsed.containsKey(key)) {
            return parsed[key]!! as ObservableArrayList<T>
        }

        val file = baseFile.child("$key.json")

        val lread: List<T>? = if (file.exists()) {
            klaxon.parseArray<T>(file)
        } else {
            listOf()
        }

        val list = observableListOf(*lread!!.toTypedArray())

        list.addListener { _, _ ->
            val s = klaxon.toJsonString(list.collection)
            file.writeText(s)
        }

        parsed.put(key, list)

        return list

    }

    inline fun <reified T : Observable> getObject(key: String, init: () -> T): T {

        if (parsedObjects.containsKey(key)) {
            return parsedObjects[key]!! as T
        }

        val file = baseFile.child("$key.json")

        val obj = if (file.exists()) {
            klaxon.parse<T>(file)
        } else {
            init.invoke()
        }

        GenericChangeObserver(obj!!) {
            val s = klaxon.toJsonString(obj)
            file.writeText(s)
        }.all("")

        parsedObjects[key] = obj

        return obj

    }

    val changedObjects = mutableMapOf<ChangeListener<Any?>, ChangeProperty<*>>()
    val changedLists = mutableMapOf<ElementChangedListener<Any?>, ChangeListElement<*>>()
    fun commit(transaction: () -> Unit) {
        transaction()
        try {
            changedObjects.forEach {
                it.key(it.value.prop, it.value.old, it.value.new)
            }
            changedLists.forEach {
                it.key(it.value.type, it.value.element)
            }
            changedObjects.clear()
            changedLists.clear()
        } catch (ex: Exception) {
            changedObjects.forEach {
                it.key(it.value.prop, it.value.old, it.value.old)
            }
            changedObjects.clear()
            changedLists.clear()
        }
    }
}

fun userdir() = File(System.getProperty("user.dir"))

fun File.child(s: String) = File(this.absolutePath + "${File.separator}$s").apply {
    if (name.lastIndexOf('.') == -1) {
        mkdir()
    }
}