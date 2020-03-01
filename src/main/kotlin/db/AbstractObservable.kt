package db

import com.beust.klaxon.Json

abstract class AbstractObservable<T : Any?>{

    @Json(ignored = true)
    @Ignored
    val listeners = mutableListOf<T>()

    fun addListener(listener: T){
        listeners.add(listener)
    }

}