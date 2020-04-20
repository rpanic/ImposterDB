package example

import aNewCollections.invoke
import io.zeko.db.sql.Insert
import io.zeko.db.sql.Query
import io.zeko.db.sql.Update
import io.zeko.model.Entity
import observable.Observable

fun main() {

    val entity = GenericEntity("uuid" to "Hello", "name" to "wtf", "testint" to 0)

    Update(entity).toSql().print()

    Insert(entity).toSql().print()

}

class GenericEntity : Entity {
    constructor(map: Map<String, Any?>) : super(map)
    constructor(vararg props: Pair<String, Any?>) : super(*props)

}

open class Test : Observable(){
    open var s: String by observable("")

    open var test: Test2? by observable(null)

}

open class Test2 : Observable(){

    open var s: String by observable("")
}