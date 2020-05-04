package aNewCollections

import lazyCollections.IObservableSet
import lazyCollections.IReadonlyVirtualSet
import lazyCollections.IVirtualSet
import lazyCollections.ObjectReference
import observable.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

typealias SetElementChangedListener<T> = (SetChangeArgs<T>, LevelInformation) -> Unit

open class ReadOnlyVirtualSet<T : Observable>(
    val loader: (List<Step<T, *>>) -> Set<T>,
    val steps: List<Step<T, *>>,
    val clazz: KClass<T>,
    val parent: VirtualSet<T>? = null
): AbstractObservable<SetElementChangedListener<T>>(), IReadonlyVirtualSet<T>{

    internal var loadedState: MutableSet<T>? = null

    //View is a 1:1 display of the state of a VirtualView, but concret
    override fun view(): IObservableSet<T> {
        //Make a ObservableSet here
        if(loadedState == null){
            loadedState = loader(steps).toMutableSet()
        }

        val view = LazyObservableSet(loadedState!!.map { ObjectReference(it) })

        addListener { setChangeArgs, levelInformation ->
            when(setChangeArgs.elementChangeType){
                ElementChangeType.Add -> {
                    setChangeArgs.elements.forEach { view.collection.add(ObjectReference(it)) }
                    view.listeners.forEach { it(setChangeArgs, levelInformation) }
                }
                ElementChangeType.Remove -> {
                    setChangeArgs.elements.forEach { view.collection.remove(ObjectReference(it)) }
                    view.listeners.forEach { it(setChangeArgs, levelInformation) }
                }
            }
        }

        return view

    }

    fun forEach(f: (T) -> Unit){
        view().forEach(f)
    }

    override operator fun get(key: Any) : T?{
        if(loadedState != null) {
            return loadedState!!.find { it.keyValue<T, Any>() == key }
        }
        return loader(listOf(
                FilterStep(listOf(
                        NormalizedCompareRule(listOf(
                                clazz.createInstance().key<T, Any>()),
                                key)
                ))
        )).firstOrNull()
    }

    fun tellChildren(args: SetChangeArgs<T>, levels: LevelInformation){
        listeners.forEach { it(args, levels) }
    }
}

open class VirtualSet<T : Observable>(
    loader: (List<Step<T, *>>) -> Set<T>,
    val performEvent: (VirtualSet<T>, SetChangeArgs<T>, LevelInformation) -> Unit,
    steps: List<Step<T, *>>,
    clazz: KClass<T>,
    parent: VirtualSet<T>? = null
) : ReadOnlyVirtualSet<T>(loader, steps, clazz, parent), IVirtualSet<T> {

    override fun add(t: T) {
        val args = SetChangeArgs(ElementChangeType.Add, t) //TODO Replace ListChangeArgs by SetChangeArgs
        val level = LevelInformation(listOf(ObservableLevel(t, Observable::uuid)))
        performEvent(getOrParent(), args, level)
        getOrParent().tellChildren(args, level)
    }

    override fun remove(t: T) {
        val args = SetChangeArgs(ElementChangeType.Remove, t)
        val level = LevelInformation(listOf(ObservableLevel(t, Observable::uuid)))
        performEvent(getOrParent(), args, level)
        getOrParent().tellChildren(args, level)
    }

    fun <K> groupBy(f: (T) -> K){

    }

    //TODO Tests
    fun find(f: (T) -> Boolean) : T{
        val extractor = RuleExtractionFramework.rulesExtractor(clazz)

        val filterStep = FilterStep<T>(extractor.extractFilterRules(f))

        val findCondition = FindStep(filterStep)

        val newSet = createDependent(findCondition){ t, f ->
            if(StepInterpreter.interpretFilter(filterStep, t)) {
                f(t)
            }
        }

        return newSet.view().first()

    }

    fun filter(f: (T) -> Boolean) : VirtualSet<T>{

        val extractor = RuleExtractionFramework.rulesExtractor(clazz)

        val normalizedConditions = FilterStep<T>(extractor.extractFilterRules(f))

        val newSet = createDependent( normalizedConditions ){ t, f ->
            if(StepInterpreter.interpretFilter(normalizedConditions, t)) {
                f(t)
            }
        }

        return newSet
    }

    protected fun createDependent(newSteps: Step<T, T>, f:(T, (T) -> Unit) -> Unit): VirtualSet<T> {

        val newSet = VirtualSet(loader, { _, a, b -> performEvent(this, a, b) }, steps + newSteps, clazz, this)

        this.addListener { setChangeArgs, levelInformation ->
            println("Children got called")
            //TODO: Check if that can be unified with DetachedList#2? maybe integrate it into a part of VirtualSet itself
            val relay = { v: VirtualSet<T> -> v.tellChildren(setChangeArgs, levelInformation) }
            when(setChangeArgs.elementChangeType){
                ElementChangeType.Add -> setChangeArgs.elements.forEach {
                    f(it){ t ->
                        newSet.loadedState?.add(t)
                        relay(newSet)
                    }
                }
                ElementChangeType.Remove -> setChangeArgs.elements.forEach {
                    f(it){ t ->
                        newSet.loadedState?.remove(t)
                        relay(newSet)
                    }
                }
            }
        }
        return newSet
    }

    fun getOrParent() : VirtualSet<T>{
        return parent?.getOrParent() ?: this
    }
}