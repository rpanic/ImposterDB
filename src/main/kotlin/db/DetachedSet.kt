package db

import aNewCollections.*
import connection.MtoNTable
import connection.MtoNTableEntry
import observable.*
import virtual.VirtualSet
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

//TODO Think about making detachedList Mutable and Observable -> Basically a proxy for removeAll() addAll()
inline fun <reified T : Observable> Observable.detachedSet(key: String) : VirtualSetReadOnlyProperty<Observable, VirtualSet<T>> {
    return detachedSet(this, key, T::class)
}

fun <P : Observable, T : Observable> detachedSet(parent: P, key: String, clazz: KClass<T>) : VirtualSetReadOnlyProperty<P, VirtualSet<T>> {

    return VirtualSetReadOnlyProperty(key) { table ->
        val db = parent.getDB()

        //Add Listeners for Changes in List
        val performEvent = { args : SetChangeArgs<T>, levelinfo: List<Level> ->

            //TODO Maybe not necessarily load Object when calling list.removeAt(1), since this is not really necessary.
            // Do this by changing the ChangeListener to give Objectreference instead of The Object itself

            db.performListAddEventsOnBackend(table.child(), clazz, args)
            db.performListUpdateEventsOnBackend(table.child(), clazz, args, LevelInformation(levelinfo)) //TODO Is this actually necessary or does this actually cause a second update call?

            args.elements.forEachIndexed { i, obj ->
                var mnkeys = listOf(parent.keyValue<P, String>(), obj.keyValue<T, String>())
                if (table.namesFlipped()) {
                    mnkeys = mnkeys.reversed()
                }

                //Convenience functions
                val findEntry = { m: String, n: String ->
                    db.cache.findCachedObject<MtoNTableEntry>(table.tableName()) {
                        it.m == m && it.n == n
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
                        //TODO Is this ever used?
                        if (args is SetSetChangeArgs<T>) {
                            var deleteKeys = listOf(parent.keyValue<P, String>(), args.replacedElements[i].keyValue<T, String>())
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
            db.addBackendUpdateListener(obj, key, clazz)
        }
        val virtualSet = VirtualSet({

            val mToNTableEntry = it.find { it is MtoNRule<*> }?.let { s ->

                db.backendConnector.loadWithRules(table.tableName(), listOf(
                    FilterStep(listOf(
                            NormalizedCompareRule<String>(listOf(
                                    if(!table.namesFlipped()) MtoNTableEntry::m else MtoNTableEntry::n),
                                    parent.uuid,
                                    CompareType.EQUALS)
                    ))
                ), MtoNTableEntry::class)
            }

            checkNotNull(mToNTableEntry){
                "MtoN Data could not be fetched for table ${table.tableName()}"
            }

            val nSteps = mToNTableEntry.map { entry ->
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