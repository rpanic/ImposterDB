package virtual

import aNewCollections.SetChangeArgs
import aNewCollections.Step
import main.kotlin.connection.BackendConnector
import observable.LevelInformation
import observable.Observable

interface ReadOnlyVirtualSetAccessor<T : Observable> {
    
    fun load(steps: List<Step<T, *>>): Set<T>
    
    fun count(steps: List<Step<T, *>>): Int

}

interface VirtualSetAccessor<T : Observable> : ReadOnlyVirtualSetAccessor<T> {
    
    fun performEvent(instance: VirtualSet<T>, listChangeArgs: SetChangeArgs<T>, levelInformation: LevelInformation)
}