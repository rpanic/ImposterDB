package example

import kotlin.reflect.jvm.isAccessible

fun main() {

    val p = Person()

    val prop = Person::trait
    prop.isAccessible = true
    val delegate = prop.getDelegate(p)
    println()

}