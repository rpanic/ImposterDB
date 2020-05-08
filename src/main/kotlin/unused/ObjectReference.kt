package unused

import observable.Observable

typealias OnLoadListener<T> = (T) -> Unit

class ObjectReference <T : Observable> { //TODO Figure out how to make this Indexable

    val pk : Any
    private val load : ((Any) -> T)?

    constructor(pk: Any, load: (Any) -> T){
        this.pk = pk
        this.load = load
    }

    constructor (obj: T) {
        this.pk = obj.keyValue<T, Any>()
        this.loaded = true
        this.obj = obj
        this.load = null
    }

    var obj: T? = null
    var loaded = false

    fun getObject() : T{
        if(!loaded) {
            obj = load!!(pk)
            onLoadList.forEach { it(obj!!) }
            loaded = true
        }
        return obj!!
    }

    fun setObject(t: T){
        obj = t
        onLoadList.forEach { it(obj!!) }
        loaded = true
    }

    private val onLoadList = mutableListOf<OnLoadListener<T>>()

    fun onLoad(onLoad: OnLoadListener<T>){
        onLoadList += onLoad
    }

}