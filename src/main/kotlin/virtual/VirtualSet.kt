package virtual

import collections.*
import example.logger
import observable.*
import ruleExtraction1.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties

typealias SetElementChangedListener<T> = (SetChangeArgs<T>, LevelInformation) -> Unit

open class ReadOnlyVirtualSet<T : Observable>(
        val accessor: ReadOnlyVirtualSetAccessor<T>,
        val steps: List<Step<T, *>>,
        val clazz: KClass<T>,
        val parent: VirtualSet<T>? = null
): AbstractObservable<SetElementChangedListener<T>>(), IReadonlyVirtualSet<T>{

    override fun iterator(): Iterator<T> {

        if(steps.isEmpty()){
            logger().warn("You are retrieving a VirtualSet without any filtering.\nThis retrieves all elements and is very expensive, try using filter{} or find{}")
        }

        return view().iterator()
    }

    internal open var loadedState: MutableSet<T>? = null

    //View is a 1:1 display of the state of a VirtualView, but concret
    override fun view(): IObservableSet<T> {
        //Make a ObservableSet here
        if(loadedState == null){
            //TODO Check if parent already has a loaded state, and if yes, take data from there
            loadedState = accessor.load(steps).toMutableSet()
            loadedState!!.forEach { initObj(it) }
        }

        val view = ObservableSet(loadedState!!.toList())

        addListener { setChangeArgs, levelInformation ->
            when(setChangeArgs.elementChangeType){
                ElementChangeType.Add -> {
                    setChangeArgs.elements.forEach { view.collection.add(it) } //TODO Maybe it is reasonable to only add it, so the user sees only the events of the actual ObservableSet and not the parent VirtualSet
                    view.listeners.forEach { it(setChangeArgs, levelInformation) }
                }
                ElementChangeType.Remove -> {
                    setChangeArgs.elements.forEach { view.collection.remove(it) }
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
            return loadedState!!.find { it.keyValue<T>() == key }
        }
        return accessor.load(listOf(
                FilterStep(listOf(
                        CompareRule(listOf(
                                clazz.createInstance().key<T>()),
                                key)
                ))
        )).firstOrNull()
    }

    fun tellChildren(args: SetChangeArgs<T>, levels: LevelInformation){
        listeners.forEach { it(args, levels) }
    }

    override val size: Int
        get() {
            return if(loadedState != null){
                loadedState!!.size
            }else{
                accessor.count(listOf(MappingStep<T, Int>(MappingType.COUNT)) + steps)
            }
        }

    override fun contains(element: T): Boolean {
        return containsAll(listOf(element))
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return loadedState?.containsAll(elements) ?:
                elements.all { element ->
                    accessor.count(listOf(FilterStep<T>(listOf(CompareRule(
                            listOf(element.key<T>()),
                            element.keyValue<T>(),
                            CompareType.EQUALS
                            )))) + steps) > 0
                }
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }
    
    fun getOrParent() : ReadOnlyVirtualSet<T>{
        return parent?.getOrParent() ?: this
    }
    
    private fun makeListener(t: T) : ChangeListener<Any>{
        return { prop, old, new, levels -> //Stehengeblieben: Information gets lost here, make new way of relaying Events
            val changeargs = SetChangeArgs(ElementChangeType.Update, t)
            getOrParent().tellChildren(changeargs, levels.append(VirtualSetLevel(this, changeargs)))
        }
    }
    
    protected fun initObj(t: T){
        //TODO Figure out if the object is not already been initialized by a parent VirtualSet
//        val isLastInChain = if(parent != null){
//            if(parent.loadedState != null){
//                !parent!!.loadedState.contains(t)
//            }else true
//        }else true
        t.addListener(makeListener(t))
    }
}

open class VirtualSet<T : Observable>(
        private val mutableAccessor: VirtualSetAccessor<T>,
        steps: List<Step<T, *>>,
        clazz: KClass<T>,
        parent: VirtualSet<T>? = null
) : ReadOnlyVirtualSet<T>(mutableAccessor, steps, clazz, parent), IVirtualSet<T> {
    
    override fun add(t: T) {
        val args = SetChangeArgs(ElementChangeType.Add, t)
        val level = LevelInformation(listOf(VirtualSetLevel(this, args)))
        mutableAccessor.performEvent(getOrParent() as VirtualSet<T>, args, level)
        getOrParent().tellChildren(args, level)
        
        //Add Listener to obj
        initObj(t)
    }

    override fun remove(t: T) {
        val args = SetChangeArgs(ElementChangeType.Remove, t)
        val level = LevelInformation(listOf(VirtualSetLevel(this, args)))
        mutableAccessor.performEvent(getOrParent() as VirtualSet<T>, args, level)
        getOrParent().tellChildren(args, level)
        
//   TODO     t.classListeners.remove(objListener)
    }

    fun <K : Any> sortedBy(f: (T) -> K){
        val extractor = ruleExtraction.RuleExtractionFramework(clazz)
        val calls = extractor.getAllExecutedFunctions(f)
        
//        val newSet = MutableSet
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

    protected fun createDependent(newStep: Step<T, T>, f:(T, (T) -> Unit) -> Unit): VirtualSet<T> {

//        val accessor2 = object : VirtualSetAccessor<T>{
//            override fun load(steps: List<Step<T, *>>) = mutableAccessor.load(steps)
//            override fun count(steps: List<Step<T, *>>) = mutableAccessor.count(steps)
//            override fun contains(steps: List<Step<T, *>>, elements: Collection<T>) = mutableAccessor.contains(steps, elements)
//
//            override fun performEvent(set: VirtualSet<T>, changeArgs: SetChangeArgs<T>, levels: LevelInformation) =
//                    mutableAccessor.performEvent(this@VirtualSet, changeArgs, levels)
//
//        } //TODO Investigate if this is needed or if it works with getOrParent()
        
        val newSet = VirtualSet(mutableAccessor, steps + newStep, clazz, this)

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
}