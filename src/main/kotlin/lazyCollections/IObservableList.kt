package lazyCollections

import aNewCollections.SetElementChangedListener
import observable.IAbstractObservable

interface IVirtualSet <T> : IReadonlyVirtualSet<T>{
    fun add(t: T)
    fun remove(t: T)
}

interface IReadonlyVirtualSet <T>{
    operator fun get(v: Any): T?
    fun view() : IObservableSet<T>
}

interface IObservableSet <T> : IAbstractObservable<SetElementChangedListener<T>>, Set<T>

interface IObservableList <T> : IAbstractObservable<ElementChangedListener<T>>, List<T>

interface IMutableObservableList <T> : IObservableList<T>, MutableList<T>{
//    protected fun view() : ObservableArrayList <T> //For Transformations
}