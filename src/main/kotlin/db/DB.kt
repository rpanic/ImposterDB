package db

import java.lang.Exception

object DB{

    val parsed = mutableMapOf<String, ObservableArrayList<*>>()

    val parsedObjects = mutableMapOf<String, Observable>()

    val backends = mutableListOf<Backend>()

    var txActive = false

    val txQueue = mutableListOf<RevertableAction>()

    fun addBackend(backend: Backend){
        backends.add(backend)
    }

    fun setBackend(backend: Backend){
        if (::primaryBackend.isInitialized){
            throw IllegalAccessException("Primary Backend already set. Use addBackend to add additional backends")
        }else{
            primaryBackend = backend
        }
    }

    lateinit var primaryBackend: Backend  //TODO make private

    fun tx(f: () -> Unit){

        txActive = true

        try {

            f()

            for (i in txQueue.indices) {

                try {

                    txQueue[i].action()

                } catch (e: Exception) {

                    e.printStackTrace()

                    println("Reverting Transaction...")

                    txActive = false

                    for (j in (i) downTo 0) {
                        txQueue[j].revert()
                    }

                    break
                }
            }

        }catch(e: Exception){
            e.printStackTrace()
        }finally {
            txActive = false
            txQueue.clear()
        }
    }

    inline fun <reified T : Observable> getList(key: String) : ObservableArrayList<T>{

        if(parsed.containsKey(key)){
            return parsed[key]!! as ObservableArrayList<T>
        }

        val lread : List<T>? = if(primaryBackend.keyExists(key)){
            primaryBackend.loadList(key, T::class)
        }else{
            listOf()
        }

        val list = observableListOf(*lread!!.toTypedArray())

        list.addListener { _, _ ->
            for (backend in listOf(primaryBackend) + backends){
                backend.saveList(key, T::class, list.collection)
            }
        }

        parsed.put(key, list)

        return list

    }

    inline fun <reified T : Observable> getObject(key: String, init : () -> T) : T{

        if(parsedObjects.containsKey(key)){
            return parsedObjects[key]!! as T
        }

        val obj = if(primaryBackend.keyExists(key)){
            primaryBackend.load(key, T::class)
        }else{
            init.invoke()
        }

        GenericChangeObserver(obj!!){
            for (backend in listOf(primaryBackend) + backends){
                backend.save(key, T::class, obj)
            }
        }.all("")

        parsedObjects.put(key, obj)

        return obj

    }

}