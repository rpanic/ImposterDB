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
inline fun <reified T : Observable> Observable.detachedList(key: String) : ReadOnlyProperty<Any?, LazyObservableArrayList<T>> {
    return detachedList(this, key, T::class)
}

fun <T : Observable> detachedList(parent: Observable, key: String, clazz: KClass<T>) : ReadOnlyProperty<Any?, LazyObservableArrayList<T>> {
    return DetachedListReadOnlyProperty(key) { table, list ->
        val list = if(list.isEmpty()) {
            LazyObservableArrayList<T>()
        } else {

            LazyObservableArrayList<T>(list.map { key ->

                ObjectReference(key) { pk ->

                    DB.getDetached(table.tableName(), pk, clazz = clazz) {
                        throw IllegalAccessException("Cant initialize missing M:N Reference Object\nMissing Object wih pk $pk which is referenced in Table ${table.tableName()}")
                    }
                }
            })
        }
        val parentRef = parent //TODO Does this affect the bytecode? Is it necessary?
        //Add Listeners for Changes in List
        list.addListener { args, levelinfo ->

            //TODO Stehengeblieben
            //Process any changes to the List. Basically M to N Table changes (add, remove) and

            //TODO Maybe not necessarily load Object when calling list.removeAt(1), since this is not really necessary.
            // Do this by changing the ChangeListener to give Objectreference instead of The Object itself

            DB.backendConnector.forEachBackend {  backend ->

                args.elements.forEachIndexed { i, obj ->
                    val entry = if(!table.namesFlipped()) MtoNTableEntry(parentRef.key(), obj.key()) else MtoNTableEntry(obj.key(), parentRef.key())
                    when(args.elementChangeType){

                        ElementChangeType.Add -> {
                            backend.insert(table.child(), MtoNTableEntry::class, entry)
                        }
                        ElementChangeType.Set -> {
                            if(args is SetListChangeArgs<T>){
                                backend.delete(key, MtoNTableEntry::class, args.replacedElements[i].key())
                                backend.insert(key, MtoNTableEntry::class, obj)
                            }else{
                                throw IllegalStateException("Args with Type Set must be instance of SetListChangeArgs!!")
                            }
                        }
                        ElementChangeType.Update -> {
                            if(args is UpdateListChangeArgs<T>) {
                                backend.update(key, MtoNTableEntry::class, obj, args.prop)
                            }
                        }
                        ElementChangeType.Remove -> {
                            backend.delete(key, MtoNTableEntry::class, obj.key<Any>())
                        }
                    }
                }

                //After because of constraints
                DB.performListEventOnBackend(backend, table.child(), clazz, args)
            }
        }

        list
    }
    //TODO Add Listener to List to connect to DB
}

class DetachedListReadOnlyProperty<L : Any>(val key: String, f: (MtoNTable, List<Any>) -> L) : MutableLazyReadOnlyProperty<L>(f){
}

abstract class MutableLazyReadOnlyProperty<T>(protected var initFunction: (MtoNTable, List<Any>) -> T) : ReadOnlyProperty<Any?, T> {

    private var initialized = false
    private var value: T? = null //TODO Nulls dont work, they throw a npe even if type is nullable

    var table: MtoNTable? = null
    var pks: List<Any> = listOf()

    fun setArgs(table: MtoNTable, pks: List<Any>{
        this.table = table
        this.pks = pks
    }

    fun setInitializer(f: (MtoNTable, List<Any>) -> T){
        this.initFunction = f
    }

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
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