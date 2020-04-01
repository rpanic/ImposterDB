package observable

import com.beust.klaxon.Json
import db.Ignored

interface IAbstractObservable<T : Any?>{

    fun addListener(listener: T) {
    }
}

abstract class AbstractObservable<T : Any?> : DBAwareObject(), IAbstractObservable<T>{

    @Json(ignored = true)
    @Ignored
    val listeners = mutableListOf<T>()

    override fun addListener(listener: T){
        listeners.add(listener)
    }

}