package models

import db.ElementChangeType

data class ChangeListElement<T>(
        val type: ElementChangeType,
        val element: T
)