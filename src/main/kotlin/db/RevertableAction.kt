package db

interface RevertableAction{
    fun action()

    fun revert()
}