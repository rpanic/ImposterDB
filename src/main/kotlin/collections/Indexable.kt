package collections

import observable.Observable
import kotlin.reflect.KProperty1

interface Indexable{
    fun <O : Observable, T> key() : KProperty1<O, T>
    fun <O : Observable, T> keyValue() : T
}