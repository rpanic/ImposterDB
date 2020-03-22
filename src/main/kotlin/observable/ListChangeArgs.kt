package observable

import kotlin.reflect.KProperty

open class ListChangeArgs<T>(
        val elementChangeType: ElementChangeType,
        val elements: List<T>,
        val indizes: List<Int>
){
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