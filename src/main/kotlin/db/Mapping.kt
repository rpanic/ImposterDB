package db

fun <T, R> ObservableList<T>.map(f: (T) -> R) : ObservableList<R>{
    val next = ObservableList<R>()

    val transform = { next.collection = this.collection.map(f).toMutableList()}

    addListener { type, t, levels ->
        transform()
        next.listeners.forEach { it(type, f(t), levels) }
    }
    transform()

    return next
}

fun <T, R> ObservableArrayList<T>.map(f: (T) -> R, reverse: (R) -> T) : ObservableArrayList<R>{
    val next = ObservableArrayList<R>()

    val transform = { next.collection = this.collection.map(f).toMutableList()}

    addListener { type, t, levels ->
        transform()
        next.listeners.forEach { it(type, f(t), levels) }
    }
    transform()

    next.addListener { type, t, levels ->
        this.collection = next.collection.map(reverse).toMutableList()
        this.listeners.forEach { it(type, reverse(t), levels) }
    }
    return next
}

fun <T> ObservableList<T>.filter(f: (T) -> Boolean) : ObservableList<T>{

    val next = ObservableList<T>()
    val transform = {next.collection = this.collection.filter(f).toMutableList()}
    addListener{ type, t, levels ->
        if(f(t)){
            transform()
            next.listeners.forEach { it(type, t, levels) }
        }
    }
    transform()
    return next

}