package ruleExtraction

import io.mockk.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

interface Parameterable

data class InputObject<T : Any>(
        val clazz: KClass<T>
) : Parameterable{
//    lateinit var obj: T
}

data class ConstantParameter<T : Any?>(
        val value: T
) : Parameterable

data class Call<T : Any>(
        val clazz: KClass<T>,
        val method: KCallable<*>,
        val parameters: List<Parameterable>,
        val parent: Parameterable
) : Parameterable


class RuleExtractionFramework<T : Any>(val clazz: KClass<T>) {

    companion object {
        val frameworks = mutableListOf<ruleExtraction.RuleExtractionFramework<*>>()

        fun findFrameworkByMock(mock: Any) : RuleExtractionFramework<*> {
            return frameworks.first()
        }
    }

    init {
        frameworks += this
    }

    val callsWithoutFollower = mutableListOf<Parameterable>()

    val mocks = mutableMapOf<Any, Parameterable>()

    fun <V : Any> getAllExecutedFunctions(f: (T) -> V) : List<Parameterable> {

        val inputObj = InputObject<T>(clazz)
        val mock = mockRecursive(clazz, inputObj)
//        inputObj.obj = mock
        mocks[mock] = inputObj
        f(mock)

        println()
        return callsWithoutFollower

    }

    fun <V : Any> mockRecursive(clazz: KClass<V>, parent: Parameterable): V {
        val mock = mockkClass(clazz, relaxed = true)

        (clazz.declaredMembers + clazz.memberFunctions.find { it.name == "equals" }!!).forEach {
            every {

                val callRecorder = this.getCallRecorder()

                val params = it.parameters.drop(1).map { param ->

                    callRecorder.anyFromClass((param.type.javaType as Class<Any>).kotlin)
                }

                it.call(mock, *params.toTypedArray())
            } answers {
                if (!(this.method.returnsNothing || this.method.returnsUnit)) {

                    answerMethod(this.args, this.method.declaringClass,
                            this.method.declaringClass.declaredMembers.find { it.name == this.method.name }!!,
                            this.method.returnType, parent)

                } else {
                    //Return nothing
                    Unit
                }
            }
            Unit
        }

        return mock

    }

    fun MockKMatcherScope.getCallRecorder(): MockKGateway.CallRecorder {

        val callRecorder = MockKMatcherScope::class.declaredMemberProperties
                .find { it.name == "callRecorder" }!!
                .also { it.isAccessible = true }
                .get(this) as MockKGateway.CallRecorder //Is ok to throw exception is something wrong comes along - which it doesn´t except the API class gets changed
        return callRecorder
    }

    fun <T : Any> MockKGateway.CallRecorder.anyFromClass(clazz: KClass<T>): T {
        return this.matcher(ConstantMatcher<Any>(true), clazz)
    }

    fun answerMethod(methodArgs: List<Any?>, declaringClass: KClass<*>, method: KCallable<*>, returnType: KClass<*>, parent: Parameterable): Any {
        val args = methodArgs.map { arg ->
            val parameterable = mocks[arg]

            if (parameterable != null) {
                callsWithoutFollower -= parameterable
                parameterable
            } else
                ConstantParameter(arg) //TODO Test if arg can actually be a constant (not obj f.e.)
        }

        val call = Call(declaringClass,
                method,
                args,
                parent)

        val stub = if (isClassPrimitive(returnType)) {
            var randomValue = ruleExtraction1.RuleExtractionFramework.getPrimitivesValue(returnType) {} //Replace by getPrimitivevalue without f
            if(returnType.equals(Boolean::class)){
                randomValue = false
            }
            randomValue!!
        } else {
            //Return mock
            val childmock = mockRecursive(returnType, call)
            childmock
        }

        mocks[stub] = call

        callsWithoutFollower -= parent
        callsWithoutFollower += call //TODO Filter out followed Calls

        return stub
    }

    fun <T : Any> isClassPrimitive(clazz: KClass<T>): Boolean {
        return clazz in listOf(String::class, Int::class, Byte::class, Long::class, Short::class, Char::class, Boolean::class)
    }
}