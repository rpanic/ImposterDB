package models

import kotlin.reflect.KProperty

data class ChangeProperty<T>(
        val prop: KProperty<*>,
        val old: T,
        val new: T
)