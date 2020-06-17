package collections

import observable.Observable
import kotlin.reflect.KProperty1

interface Indexable{ //TODO Rework Indexable to work with annotations
    fun <O : Observable, T> key() : KProperty1<O, T>
    fun <O : Observable, T> keyValue() : T
}