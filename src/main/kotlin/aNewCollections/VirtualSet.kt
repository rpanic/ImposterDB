package aNewCollections

import lazyCollections.IObservableSet
import lazyCollections.IReadonlyVirtualSet
import lazyCollections.IVirtualSet
import lazyCollections.ObjectReference
import observable.*
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createInstance


open class ReadOnlyVirtualSet<T : Observable>(
    val loader: (List<Step<T, *>>) -> Set<T>,
    val steps: List<Step<T, *>>,
    val clazz: KClass<T>
): AbstractObservable<ElementChangedListener<T>>(), IReadonlyVirtualSet<T>{

    internal var loadedState: MutableSet<T>? = null

    //View is a 1:1 display of the state of a VirtualView, but concret
    override fun view(): IObservableSet<T> {
        //Make a ObservableSet here

        if(loadedState == null){
            loadedState = loader(steps).toMutableSet()
        }

        val set = LazyObservableSet(loadedState!!.map { ObjectReference(it) })

        addListener { listChangeArgs, levelInformation ->
            when(listChangeArgs.elementChangeType){
                ElementChangeType.Add -> listChangeArgs.elements.forEach { set.collection.add(ObjectReference(it)) }
                ElementChangeType.Remove -> listChangeArgs.elements.forEach { set.collection.remove(ObjectReference(it)) }
            }
        }

        return set

    }

    override operator fun get(key: Any) : T?{
        return loader(listOf(
                FilterStep(listOf(
                NormalizedCompareRule(listOf(
                        clazz.createInstance().key<T, Any>()),
                        key)
                ))
        )).firstOrNull()
    }

//    fun <V> map(f: (T) -> V) : ReadOnlyVirtualSet<T>{
//        return ReadOnlyVirtualSet({
//            get(it)
//        }, steps + MapStep<T, V>(), clazz)
//    }

    fun addToState(t: T){
        if(loadedState != null){
            loadedState!!.add(t)
        }
        listeners.forEach { it.invoke(ListChangeArgs(ElementChangeType.Add, t, -1), LevelInformation(listOf(ObservableLevel(t, Observable::uuid)))) }
    }

    fun tellChildren(args: ListChangeArgs<T>, levels: LevelInformation){
        listeners.forEach { it(args, levels) }
    }
}

open class VirtualSet<T : Observable>(
    loader: (List<Step<T, *>>) -> Set<T>,
    val setter: (T) -> Unit,
    steps: List<Step<T, *>>,
    clazz: KClass<T>
) : ReadOnlyVirtualSet<T>(loader, steps, clazz), IVirtualSet<T> {

    override fun add(t: T) {
        //TODO Stehengeblieben: back and forward propagartion of Change Events
    }

    fun filter(f: (T) -> Boolean) : VirtualSet<T>{

        val extractor = RuleExtractionFramework.rulesExtractor(clazz)

        val normalizedConditions = extractor.extractFilterRules(f)

        val newSet = VirtualSet(loader, steps + FilterStep(normalizedConditions), clazz)

        newSet.addListener { listChangeArgs, levelInformation -> newSet.tellChildren(listChangeArgs, levelInformation) }

        this.addListener { listChangeArgs, levelInformation ->
            when(listChangeArgs.elementChangeType){
                ElementChangeType.Add -> {
                    listChangeArgs.elements.forEach {
                        if(StepInterpreter.interpretFilter(steps.last() as FilterStep<T>, it)){
                            newSet.addToState(it)
                        }
                    }
                }
                ElementChangeType.Remove -> listChangeArgs.elements.forEach {
                    if(StepInterpreter.interpretFilter(steps.last() as FilterStep<T>, it)){
                        //TODO
//                        addToState(it)
                    }
                }
            }
        }

        return newSet
    }
}