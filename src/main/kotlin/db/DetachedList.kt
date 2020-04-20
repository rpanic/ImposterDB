package db

import aNewCollections.*
import connection.MtoNTable
import connection.MtoNTableEntry
import lazyCollections.LazyObservableArrayList
import lazyCollections.ObjectReference
import observable.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

inline fun <reified T : Observable> Observable.detachedSet(key: String) : VirtualSetReadOnlyProperty<Observable, VirtualSet<T>> {
    return detachedSet(this, key, T::class)
}

fun <P : Observable, T : Observable> detachedSet(parent: P, key: String, clazz: KClass<T>) : VirtualSetReadOnlyProperty<P, VirtualSet<T>> {

    return VirtualSetReadOnlyProperty(key) { table ->
        val db = parent.getDB()

        //Add Listeners for Changes in List
        val performEvent = { args : ListChangeArgs<T>, levelinfo: List<Level> ->

            //TODO Maybe not necessarily load Object when calling list.removeAt(1), since this is not really necessary.
            // Do this by changing the ChangeListener to give Objectreference instead of The Object itself

            db.performListAddEventsOnBackend(table.child(), clazz, args)
            db.performListUpdateEventsOnBackend(table.child(), clazz, args) //TODO Is this actually necessary or does this actually cause a second update call?

            args.elements.forEachIndexed { i, obj ->
                var mnkeys = listOf(parent.keyValue<P, Any>(), obj.keyValue<T, Any>())
                if (table.namesFlipped()) {
                    mnkeys = mnkeys.reversed()
                }

                //Convenience functions
                val findEntry = { m: Any, n: Any ->
                    db.cache.findCachedObject<MtoNTableEntry>(table.tableName()) {
                        it.getMKey<Any>() == m && it.getNKey<Any>() == n
                    }!!
                }
                val createEntry = {
                    MtoNTableEntry(mnkeys[0], mnkeys[1])
                }

                when (args.elementChangeType) {

                    ElementChangeType.Add -> {
                        val entry = createEntry()
                        db.backendConnector.insert(table.tableName(), entry, MtoNTableEntry::class)
                    }
                    ElementChangeType.Set -> {
                        if (args is SetListChangeArgs<T>) {
                            var deleteKeys = listOf(parent.keyValue<P, Any>(), args.replacedElements[i].keyValue<T, Any>())
                            if (table.namesFlipped()) {
                                deleteKeys = deleteKeys.reversed()
                            }
                            val deleteEntry = findEntry(deleteKeys[0], deleteKeys[1])
                            db.backendConnector.delete(table.tableName(), deleteEntry.keyValue<MtoNTableEntry, Any>(), MtoNTableEntry::class)

                            val insertEntry = createEntry()
                            db.backendConnector.insert(table.tableName(), insertEntry, MtoNTableEntry::class)

                        } else {
                            throw IllegalStateException("Args with Type Set must be instance of SetListChangeArgs!!")
                        }
                    }
                    //Updates are ignored since it has no effect on the relation table

                    ElementChangeType.Remove -> {
                        val removeEntry = findEntry(mnkeys[0], mnkeys[1])
                        db.backendConnector.delete(table.tableName(), removeEntry.keyValue<MtoNTableEntry, Any>(), MtoNTableEntry::class)
                        db.cache.removeObject(table.tableName(), removeEntry)

                    }
                }
            }

            //After relation table remove operations because of constraints
            db.performListDeleteEventsOnBackend(table.child(), clazz, args)
        }

        val initObservable = { obj : T ->
            obj.setDbReference(db)
            db.addBackendListener(obj, key, clazz)
        }
        val virtualSet = VirtualSet({

            val mToN = it.find { it is MtoNRule<*> }?.let { s ->

                db.backendConnector.loadWithRules(table.tableName(), listOf(
                    FilterStep(listOf(
                            NormalizedCompareRule<String>(listOf(
                                    if(!table.namesFlipped()) MtoNTableEntry::m else MtoNTableEntry::n),
                                    parent.uuid,
                                    CompareType.EQUALS)
                    ))
                ), MtoNTableEntry::class)
            }

            checkNotNull(mToN){
                "MtoN Data could not be fetched for table ${table.tableName()}"
            }

            val nSteps = mToN.map { entry ->
                FilterStep<T>(listOf(
                        NormalizedCompareRule(listOf(
                                clazz.memberProperties.find { it.name == "uuid" }!!),
                                if(table.namesFlipped()) entry.m else entry.n,
                                CompareType.EQUALS)
                ))
            }

//            val set = db.backendConnector.loadWithRules(key, it.filter { it !is MtoNRule<*> }, clazz)
            val set = nSteps.map { rule -> db.backendConnector.loadWithRules(key, listOf(rule), clazz)
                        .let { if(it.size == 1) it.first() else throw java.lang.IllegalStateException("This should not be possible") } }
                    .toSet()
            set.forEach(initObservable)
            set

            },
            { instance, listChangeArgs, levelInformation ->
                println("Parent set got called")

                performEvent(listChangeArgs, levelInformation.list)

                when(listChangeArgs.elementChangeType){

                    ElementChangeType.Add -> {
                        listChangeArgs.elements.forEach {
                            instance.loadedState?.add(it)
                        }
                    }
                    ElementChangeType.Remove -> {
                        listChangeArgs.elements.forEach {
                            instance.loadedState?.remove(it)
                        }
                    }

                }
            },
            listOf(MtoNRule()), clazz)

        virtualSet
    }
}

class VirtualSetReadOnlyProperty<P : Observable, T>(val key: String, protected var initFunction: (MtoNTable) -> T) : ReadOnlyProperty<P?, T> {

    private var initialized = false
    private var value: T? = null //TODO Nulls dont work, they throw a npe even if type is nullable

    var parentkey: String? = null

    fun setParentKey(parentkey: String){
        this.parentkey = parentkey
    }

    override fun getValue(thisRef: P?, property: KProperty<*>): T {
        checkNotNull(parentkey) { //Is only the case, if the parent object did not get loaded, but initialized
            "For Detached Properties to load, the Parent object must be added to a DB Set" //TODO At some place, this should be possible
        }
        if(!initialized){
            init()
        }
        return value!!
    }

    fun init(){
        val table = MtoNTable(parentkey!!, key)
        value = initFunction(table)
        initialized = true
    }

    protected fun getValue() : T{
        if(!initialized){
            init()
            error("Should not happen - LazyReadWriteProperty :: 53")
        }
        return value!!
    }
}

//TODO Think about making detachedList Observable -> Basically a proxy for removeAll() addAll()
inline fun <reified T : Observable> Observable.detachedList(key: String) : DetachedListReadOnlyProperty<Observable, LazyObservableArrayList<T>> {
    return detachedList(this, key, T::class)
}

fun <P : Observable, T : Observable> detachedList(parent: P, key: String, clazz: KClass<T>) : DetachedListReadOnlyProperty<P, LazyObservableArrayList<T>> {

    return DetachedListReadOnlyProperty(key) { table, list ->
        val db = parent.getDB()

        val obslist = if(list.isEmpty()) {
            LazyObservableArrayList<T>()
        } else {

            LazyObservableArrayList<T>(list.map { mappingPK ->

                ObjectReference(mappingPK) { pk ->

//                    val mapping = DB.getDetached(table.tableName(), pk, clazz = MtoNTableEntry::class) { //TODO Cant be right
//                        //This will actually never happen
//                        throw IllegalAccessException("Cant initialize missing M:N Reference Object\nMissing Reference with uuid $pk which should be in Table ${table.tableName()}")
//                    }
                    db.getDetached(key, pk, clazz = clazz) {
                        throw IllegalAccessException("Cant initialize missing M:N Reference Object\nMissing Object wih pk $pk which is referenced in Table ${table.tableName()}")
                    }
                    //TODO Add to Complete Cache
                }
            })
        }
        val parentRef = parent //TODO Does this affect the bytecode? Is it necessary?
        //Add Listeners for Changes in List
        obslist.addListener { args, levelinfo ->

            //TODO Maybe not necessarily load Object when calling list.removeAt(1), since this is not really necessary.
            // Do this by changing the ChangeListener to give Objectreference instead of The Object itself

            db.performListAddEventsOnBackend(table.child(), clazz, args)
            db.performListUpdateEventsOnBackend(table.child(), clazz, args) //TODO Is this actually necessary or does this actually cause a second update call?

            args.elements.forEachIndexed { i, obj ->
                var mnkeys = listOf(parentRef.keyValue<P, Any>(), obj.keyValue<T, Any>())
                if(table.namesFlipped()){
                    mnkeys = mnkeys.reversed()
                }

                //Convenience functions
                val findEntry = { m: Any, n: Any ->
                    println(list)
                    db.cache.findCachedObject<MtoNTableEntry>(table.tableName()){
                        it.getMKey<Any>() == m && it.getNKey<Any>() == n
                    }!!
                }
                val createEntry = {
                    MtoNTableEntry(mnkeys[0], mnkeys[1])
                }

                when(args.elementChangeType){

                    ElementChangeType.Add -> {
                        val entry = createEntry()
                        db.backendConnector.insert(table.tableName(), entry, MtoNTableEntry::class)
                        db.cache.putObject(table.tableName(), entry)
                    }
                    ElementChangeType.Set -> {
                        if(args is SetListChangeArgs<T>){
                            var deleteKeys = listOf(parentRef.keyValue<P, Any>(), args.replacedElements[i].keyValue<T, Any>())
                            if(table.namesFlipped()){
                                deleteKeys = deleteKeys.reversed()
                            }
                            val deleteEntry = findEntry(deleteKeys[0], deleteKeys[1])
                            db.backendConnector.delete(table.tableName(), deleteEntry.keyValue<MtoNTableEntry, Any>(), MtoNTableEntry::class)
                            db.cache.removeObject(table.tableName(), deleteEntry)

                            val insertEntry = createEntry()
                            db.backendConnector.insert(table.tableName(), insertEntry, MtoNTableEntry::class)
                            db.cache.putObject(table.tableName(), insertEntry)

                        }else{
                            throw IllegalStateException("Args with Type Set must be instance of SetListChangeArgs!!")
                        }
                    }
                    //Updates are ignored since it has no effect on the relation table

                    ElementChangeType.Remove -> {
                        val removeEntry = findEntry(mnkeys[0], mnkeys[1])
                        db.backendConnector.delete(table.tableName(), removeEntry.keyValue<MtoNTableEntry, Any>(), MtoNTableEntry::class)
                        db.cache.removeObject(table.tableName(), removeEntry)

                    }
                }
            }

            //After relation table remove operations because of constraints
            db.performListDeleteEventsOnBackend(table.child(), clazz, args)
        }

        obslist
    }
}

class DetachedListReadOnlyProperty<P : Observable, L : Any>(val key: String, f: (MtoNTable, List<Any>) -> L) : MutableLazyReadOnlyProperty<P, L>(f){
}

abstract class MutableLazyReadOnlyProperty<P : Observable, T>(protected var initFunction: (MtoNTable, List<Any>) -> T) : ReadOnlyProperty<P?, T> {

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

    override fun getValue(thisRef: P?, property: KProperty<*>): T {
//        if(table == null){ //Is only the case, if the parent object did not get loaded, but initialized
//            table = MtoNTable(key, )
//        }
        if(!initialized){
            value = initFunction(table!!, pks)
            initialized = true
        }
        return value!!
    }

    protected fun getValue() : T{
        if(!initialized){
            value = initFunction(table!!, pks)
            initialized = true
            println("Should not happen - LazyReadWriteProperty :: 41")
        }
        return value!!
    }
}