package connection

import observable.Observable
import java.lang.IllegalStateException

/**
 * Objects generally get added to the cache after all Backend and Imposter Operations are successful, except when adding to a Complete List retrieved by DB.getDetached()
 */
class ObjectCache (){

    val parsedObjects = mutableMapOf<String, MutableMap<Any, Observable>>() //<Key, PK> -> Observable

    fun <T : Observable, K : Any> getCachedObject(key: String, pk: K) : T?{
        return (parsedObjects[key]?.get(pk)) as? T
    }

    fun <T : Observable> findCachedObject(key: String, f: (T) -> Boolean) : T?{
        return (parsedObjects[key]?.values?.find{ f(it as T) } ) as? T
    }

    fun <K : Any> containsObject(key: String, pk: K) : Boolean{
        return parsedObjects.containsKey(key) && parsedObjects[key]?.containsKey(pk) == true
    }

    fun <T : Observable> putObject(key: String, obj: T){
        if(!parsedObjects.containsKey(key)){
            parsedObjects[key] = mutableMapOf()
        }

        if(containsObject(key, obj.keyValue<T, Any>())){
            println("Object ${obj.keyValue<T, Any>()} already exists")
//            throw IllegalStateException()
        }

        parsedObjects[key]?.set(obj.keyValue<T, Any>(), obj)
    }

    fun <K : Any> removeObject(key: String, pk: K) : Boolean{
        return containsObject(key, pk) && parsedObjects[key]?.remove(pk) != null
    }

    //evtl operator fun get

}