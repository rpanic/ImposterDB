package aNewCollections

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass

operator fun String.invoke() : StringClone { //f: (String) -> Unit
    val answerer = RuleExtractionFramework.answerer[this]!!
    val mock = mockk<StringClone>()
    every { mock.compareTo(any()) } answers {
        answerer.answer(this)
    }
    return mock
}

operator fun Double.invoke() : StringClone{
    return mockkClass(StringClone::class)
}

operator fun Int.invoke() : StringClone{
    return mockkClass(StringClone::class)
}

interface StringClone : Comparable<String>, CharSequence