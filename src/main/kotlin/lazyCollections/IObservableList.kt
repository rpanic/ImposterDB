package lazyCollections

import observable.IAbstractObservable

interface IObservableList <T> : IAbstractObservable<ElementChangedListener<T>>, List<T>

interface IMutableObservableList <T> : IObservableList<T>, MutableList<T>{
//    protected fun view() : ObservableArrayList <T> //For Transformations
}