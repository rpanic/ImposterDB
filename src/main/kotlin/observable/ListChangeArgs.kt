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

fun <T> getIndizesFromElements(list: List<T>, arrayList: ObservableList<T>) : List<Int>{
    val indizes = mutableListOf<Int>()
    arrayList.collection.forEachIndexed { index, t ->
        if(t in list)
            indizes += index
    }
    return indizes
}

class SetListChangeArgs<T>(
        elementChangeType: ElementChangeType,
        elements: List<T>,
        indizes: List<Int>,
        val replacedElements: List<T>
) : ListChangeArgs<T>(elementChangeType, elements, indizes)