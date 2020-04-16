package aNewCollections

import lazyCollections.IObservableSet
import lazyCollections.IReadonlyVirtualSet
import lazyCollections.IVirtualSet
import observable.*
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


open class ReadOnlyVirtualSet<T : Observable>(
    val loader: (Any) -> T?,
    val steps: List<Step<T, *>>,
    val clazz: KClass<T>
): AbstractObservable<ElementChangedListener<T>>(), IReadonlyVirtualSet<T>{
    override fun view(): IObservableSet<T> {
        //Make a ObservableSet here
//        val references = getDB().retrieve(steps)

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override operator fun get(key: Any) : T?{
        return loader(key)
    }

    fun <V> map(f: (T) -> V) : ReadOnlyVirtualSet<T>{
        return ReadOnlyVirtualSet({
            get(it)
        }, steps + MapStep<T, V>(), clazz)
    }

}

open class VirtualSet<T : Observable>(
    loader: (Any) -> T?,
    val setter: (T) -> Unit,
    steps: List<Step<T, *>>,
    clazz: KClass<T>
) : ReadOnlyVirtualSet<T>(loader, steps, clazz), IVirtualSet<T> {

    override fun add(t: T) {
        setter(t)
    }

    fun filter(f: (T) -> Boolean) : VirtualSet<T>{

        val extractor = RuleExtractionFramework.rulesExtractor(clazz)

        val normalizedConditions = extractor.extractFilterRules(f)

        return VirtualSet({
            get(it)
        }, { add(it) }, steps + FilterStep(normalizedConditions), clazz)
    }
}