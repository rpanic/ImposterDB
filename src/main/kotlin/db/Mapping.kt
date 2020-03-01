package db

fun <T, R> ObservableList<T>.map(f: (T) -> R) : ObservableList<R>{
    val next = ObservableList<R>()

    val transform = { next.collection = this.collection.map(f).toMutableList()}

    addListener { args, levels ->
        transform()
        val newListChangeArgs = ListChangeArgs(args.elementChangeType, args.elements.map(f), args.indizes?.toList())
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

fun <T> ObservableList<T>.filter(f: (T) -> Boolean) : ObservableList<T>{

    val next = ObservableList<T>()
    addListener{ args, levels ->
        if(args.elements.any(f)){

            val elements = mutableListOf<T>()
            val indices = mutableListOf<Int>()

            var cindex = 0
            this.collection.forEachIndexed { index, t ->
                if(f(t)){
                    if(index in args.indizes){
                        elements += t
                        indices += cindex
                    }
                    cindex++
                }
            }

            val newListChangeArgs = ListChangeArgs(args.elementChangeType, elements, indices)
            next.listeners.forEach { it(newListChangeArgs, levels) }
        }
    }
    next.collection = this.collection.filter(f).toMutableList()
    return next
}