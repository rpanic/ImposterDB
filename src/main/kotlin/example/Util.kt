package example

import observable.Observable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

fun <T : Observable, R : Any?, DELEGATE : Any> isPropertyDelegating(clazz: KClass<T>, prop: KProperty1<T, R>, delegatingTo: KClass<DELEGATE>): Boolean {
    val javaField = prop.javaField
    return if (javaField != null && delegatingTo.java.isAssignableFrom(javaField.type)) {
        javaField.isAccessible = true // is private, have to open that up
        @Suppress("UNCHECKED_CAST")
        val delegateInstance = javaField.type == delegatingTo.java
        delegateInstance
    } else {
        false
    }
}

fun <T : Observable, DELEGATE : Any> findDelegatingProperties(clazz: KClass<T>, delegatingTo: KClass<DELEGATE>): List<KProperty1<T, DELEGATE>> {
    return clazz.declaredMemberProperties.map { prop ->
        if(isPropertyDelegating(clazz, prop, delegatingTo)){
            prop as KProperty1<T, DELEGATE>
        }else
            null
    }.filterNotNull()
}