package lazyCollections

import observable.IAbstractObservable

interface IObservableList <T> : IAbstractObservable<ElementChangedListener<T>>, List<T>