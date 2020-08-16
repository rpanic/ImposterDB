package connection

import observable.Observable

class MtoNTableEntry(var m: Any, var n: Any) : Observable(){ //TODO Make this val again and find some way to parse MToNTableEntry directly

    @Deprecated("Use primary constructor (m, n)")
    constructor() : this("", "") //This constructor is only for reflection createInstance()

}

class MtoNTable(private val key1: String, private val key2: String){

    private val tablename: String
    private var flip: Boolean

    init {
        val name = listOf(key1, key2).map { it.toLowerCase() }
        flip = name[0] > name[1]
        //TODO Implement a way to configure the naming, probably by providing a "owner table" of a Relation Table or by defining an Order
        tablename = "${name[if(flip) 1 else 0].capitalize()}${name[if(flip) 0 else 1].capitalize()}"
    }

    fun namesFlipped() = flip

    fun parent() = key1

    fun child() = key2

    fun first() =
        if(flip) key2 else key1

    fun second() =
        if(flip) key1 else key2

    fun tableName() = tablename

}
