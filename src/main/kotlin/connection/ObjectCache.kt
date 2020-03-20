package main.kotlin.connection

import observable.Observable
import observable.ObservableArrayList
import java.lang.IllegalStateException

class ObjectCache (){

    val completeCollections = mutableMapOf<String, ObservableArrayList<*>>()

    val parsedObjects = mutableMapOf<String, MutableMap<Any, Observable>>() //<Key, PK> -> Observable

    fun <T : Observable, K : Any> getCachedObject(key: String, pk: K) : T?{
        return (parsedObjects[key]?.get(pk)) as? T
    }

    fun <K : Any> containsObject(key: String, pk: K) : Boolean{
        return parsedObjects.containsKey(key) && parsedObjects[key]?.containsKey(pk) == true
    }

    fun <T : Observable> putObject(key: String, obj: T){
        if(!parsedObjects.containsKey(key)){
            parsedObjects[key] = mutableMapOf()
        }

        if(containsObject(key, obj.key())){
            throw IllegalStateException()
        }

        parsedObjects[key]?.set(obj.key(), obj)
    }

    fun <T : Observable> getComplete(key: String) : ObservableArrayList<T>?{
        return completeCollections[key] as? ObservableArrayList<T>
    }

    fun <T : Observable> putComplete(key: String, list: ObservableArrayList<T>){
        if(containsComplete(key)){
            throw IllegalStateException("Please investigate if something is wrong here")
        }
        completeCollections[key] = list
    }

    fun containsComplete(key: String) = completeCollections.containsKey(key)

    //evtl operator fun get

}