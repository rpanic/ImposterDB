package db

import aNewCollections.ObservableSet
import observable.Observable

fun <T : Observable, R : Observable> ObservableSet<T>.map(f: (T) -> R) : ObservableSet<R> {
    val next = ObservableSet<R>()

    val transform = { next.collection = this.collection.map(f).toMutableList()}

    addListener { args, levels ->
        transform()
        val newListChangeArgs = SetChangeArgs(args.elementChangeType, args.elements.map(f))
        next.listeners.forEach { it(newListChangeArgs, levels) }
    }
    transform()

    return next
}

//fun <T, R> ObservableArrayList<T>.map(f: (T) -> R, reverse: (R) -> T) : ObservableArrayList<R>{
//    val next = ObservableArrayList<R>()
//
//    val transform = { next.collection = this.collection.map(f).toMutableList()}
//
//    addListener { type, t, levels ->
//        transform()
//        next.listeners.forEach { it(type, f(t), levels) }
//    }
//    transform()
//
//    next.addListener { type, t, levels ->
//        this.collection = next.collection.map(reverse).toMutableList()
//        this.listeners.forEach { it(type, reverse(t), levels) }
//    }
//    return next
//}

fun <T : Observable> ObservableSet<T>.filter(f: (T) -> Boolean) : ObservableSet<T> {

    val next = ObservableSet<T>()
    addListener{ args, levels ->
        if(args.elements.any(f)){

            val elements = args.elements.filter(f)

            val newListChangeArgs = SetChangeArgs(args.elementChangeType, elements)
            next.listeners.forEach { it(newListChangeArgs, levels) }
        }
    }
    next.collection = this.collection.filter(f).toMutableList()
    return next
}