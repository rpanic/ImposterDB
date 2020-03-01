package observable

import com.beust.klaxon.Json
import db.Ignored

abstract class AbstractObservable<T : Any?>{

    @Json(ignored = true)
    @Ignored
    val listeners = mutableListOf<T>()

    fun addListener(listener: T){
        listeners.add(listener)
    }

}