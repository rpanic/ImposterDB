package virtual

import observable.Observable
import ruleExtraction1.Step
import kotlin.reflect.KClass

class SortedVirtualSet<T : Observable>(
        mutableAccessor: VirtualSetAccessor<T>,
        steps: List<Step<T, *>>,
        clazz: KClass<T>,
        parent: VirtualSet<T>? = null
) : VirtualSet<T>(mutableAccessor, steps, clazz, parent)
{
    internal var sortedLoadedState: LinkedHashSet<T>? = null
    
    override var loadedState: MutableSet<T>?
        get() = sortedLoadedState
        set(value) { sortedLoadedState = value as LinkedHashSet<T> }
}