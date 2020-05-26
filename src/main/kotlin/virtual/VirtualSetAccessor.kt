package virtual

import collections.SetChangeArgs
import ruleExtraction1.Step
import observable.LevelInformation
import observable.Observable

interface ReadOnlyVirtualSetAccessor<T : Observable> {
    
    fun load(steps: List<Step<T, *>>): Set<T>
    
    fun count(steps: List<Step<T, *>>): Int

}

interface VirtualSetAccessor<T : Observable> : ReadOnlyVirtualSetAccessor<T> {
    
    fun performEvent(instance: VirtualSet<T>, listChangeArgs: SetChangeArgs<T>, levelInformation: LevelInformation)
}