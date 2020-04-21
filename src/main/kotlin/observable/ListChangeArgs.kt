package observable

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


//### List Change Args
open class ListChangeArgs<T>(
        elementChangeType: ElementChangeType,
        elements: List<T>,
        val indizes: List<Int>
) : ChangeArgs<T>(elementChangeType, elements){
    constructor(elementChangeType: ElementChangeType,
                element: T,
                index: Int) : this(elementChangeType, listOf(element), listOf(index))
}

class UpdateListChangeArgs<T>(
        elementChangeType: ElementChangeType,
        elements: List<T>,
        indizes: List<Int>,
        val prop: KProperty<*>
) : ListChangeArgs<T>(elementChangeType, elements, indizes)


class SetListChangeArgs<T>(
        elementChangeType: ElementChangeType,
        elements: List<T>,
        indizes: List<Int>,
        val replacedElements: List<T>
) : ListChangeArgs<T>(elementChangeType, elements, indizes)