package db

import connection.MtoNTable
import connection.MtoNTableEntry
import lazyCollections.LazyObservableArrayList
import lazyCollections.LazyReadWriteProperty
import lazyCollections.ObjectReference
import observable.ElementChangeType
import observable.Observable
import observable.SetListChangeArgs
import observable.UpdateListChangeArgs
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

//TODO Think about making detachedList Observable -> Basically a proxy for removeAll() addAll()
inline fun <reified T : Observable> Observable.detachedList(key: String) : DetachedListReadOnlyProperty<LazyObservableArrayList<T>> {
    return detachedList(this, key, T::class)
}

fun <T : Observable> detachedList(parent: Observable, key: String, clazz: KClass<T>) : DetachedListReadOnlyProperty<LazyObservableArrayList<T>> {
    return DetachedListReadOnlyProperty(key) { table, list ->
        val list = if(list.isEmpty()) {
            LazyObservableArrayList<T>()
        } else {

            LazyObservableArrayList<T>(list.map { key ->

                ObjectReference(key) { pk ->

                    DB.getDetached(table.tableName(), pk, clazz = clazz) { //TODO Cant be right
                        throw IllegalAccessException("Cant initialize missing M:N Reference Object\nMissing Object wih pk $pk which is referenced in Table ${table.tableName()}")
                    }
                }
            })
        }
        val parentRef = parent //TODO Does this affect the bytecode? Is it necessary?
        //Add Listeners for Changes in List
        list.addListener { args, levelinfo ->

            //TODO Maybe not necessarily load Object when calling list.removeAt(1), since this is not really necessary.
            // Do this by changing the ChangeListener to give Objectreference instead of The Object itself

            DB.backendConnector.forEachBackend {  backend ->

                args.elements.forEachIndexed { i, obj ->

                    var mnkeys = listOf(parentRef.key<Any>(), obj.key())
                    if(table.namesFlipped()){
                        mnkeys = mnkeys.reversed()
                    }

                    //Convenience functions
                    val findEntry = { m: Any, n: Any ->
                        table.tableData!!.find { it.getMKey<Any>() == m && it.getNKey<Any>() == n }!!
                    }
                    val createEntry = {
                        MtoNTableEntry(mnkeys[0], mnkeys[1])
                    }

                    when(args.elementChangeType){

                        ElementChangeType.Add -> {
                            val entry = createEntry()
                            backend.insert(table.tableName(), MtoNTableEntry::class, entry)
                            DB.cache.getComplete<MtoNTableEntry>(table.tableName())!!.add(entry)
                            //TODO Stehengeblieben: Doppelte Person
                        }
                        ElementChangeType.Set -> {
                            if(args is SetListChangeArgs<T>){
                                var deleteKeys = listOf(parentRef.key<Any>(), args.replacedElements[i].key<Any>())
                                if(table.namesFlipped()){
                                    deleteKeys = deleteKeys.reversed()
                                }
                                backend.delete(table.tableName(), MtoNTableEntry::class, findEntry(deleteKeys[0], deleteKeys[1]))
                                backend.insert(table.tableName(), MtoNTableEntry::class, createEntry())
                            }else{
                                throw IllegalStateException("Args with Type Set must be instance of SetListChangeArgs!!")
                            }
                        }
                        //Updates are ignored since it has no effect on the relation table

                        ElementChangeType.Remove -> {
                            backend.delete(table.tableName(), MtoNTableEntry::class, findEntry(mnkeys[0], mnkeys[1]).key<Any>())
                        }
                    }
                }

                //After relation table remove operations because of constraints
                //TODO Split up: Add before, remove after
                DB.performListEventOnBackend(table.child(), clazz, args)
            }
        }

        list
    }
}

class DetachedListReadOnlyProperty<L : Any>(val key: String, f: (MtoNTable, List<Any>) -> L) : MutableLazyReadOnlyProperty<L>(f){
}

abstract class MutableLazyReadOnlyProperty<T>(protected var initFunction: (MtoNTable, List<Any>) -> T) : ReadOnlyProperty<Any?, T> {

    private var initialized = false
    private var value: T? = null //TODO Nulls dont work, they throw a npe even if type is nullable

    var table: MtoNTable? = null
    var pks: List<Any> = listOf()

    fun setArgs(table: MtoNTable, pks: List<Any>){
        this.table = table
        this.pks = pks
    }

    fun setInitializer(f: (MtoNTable, List<Any>) -> T){
        this.initFunction = f
    }

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
//        if(table == null){ //Is only the case, if the parent object did not get loaded, but initialized
//            table = MtoNTable(key, )
//        }
        if(!initialized){
            value = initFunction(table!!, pks)
        }
        return value!!
    }

    protected fun getValue() : T{
        if(!initialized){
            value = initFunction(table!!, pks)
            println("Should not happen - LazyReadWriteProperty :: 41")
        }
        return value!!
    }
}