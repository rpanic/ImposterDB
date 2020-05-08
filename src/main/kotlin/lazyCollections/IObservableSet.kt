package lazyCollections

import aNewCollections.SetElementChangedListener
import observable.IAbstractObservable

interface IVirtualSet <T> : IReadonlyVirtualSet<T>{
    fun add(t: T)
    fun remove(t: T)
}

interface IReadonlyVirtualSet <T> : Set<T> {
    operator fun get(v: Any): T?
    fun view() : IObservableSet<T>
}

interface IObservableSet <T> : IAbstractObservable<SetElementChangedListener<T>>, Set<T>

interface IObservableMutableSet <T> : IAbstractObservable<SetElementChangedListener<T>>, Set<T>