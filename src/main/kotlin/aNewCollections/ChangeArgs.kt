package aNewCollections

import kotlin.reflect.KProperty

open class ChangeArgs<T>(
        val elementChangeType: ElementChangeType,
        val elements: List<T>
){
    constructor(elementChangeType: ElementChangeType,
                element: T) : this(elementChangeType, listOf(element))
}
//### Set Change Args
typealias SetChangeArgs<T> = ChangeArgs<T>

class UpdateSetChangeArgs<T>(
        elementChangeType: ElementChangeType,
        elements: List<T>,
        val prop: KProperty<*>
) : SetChangeArgs<T>(elementChangeType, elements)

class SetSetChangeArgs<T>(
        elementChangeType: ElementChangeType,
        elements: List<T>,
        val replacedElements: List<T>
) : SetChangeArgs<T>(elementChangeType, elements)