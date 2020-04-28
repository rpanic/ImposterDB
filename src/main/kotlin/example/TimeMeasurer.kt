package example

object TimeMeasurer{

    var start: Long = -1
    var points = mutableListOf<Long>()

    fun dump(){
        println("Times each line took")
        points.zipWithNext { a: Long, b: Long -> b - a }.forEachIndexed { i, l ->
            println("Line $i took $l ms")
        }
    }

}

fun mstart(){
    TimeMeasurer.start = System.currentTimeMillis()
    TimeMeasurer.points.add(System.currentTimeMillis())
}

fun mpoint(){
    TimeMeasurer.points.add(System.currentTimeMillis())
}
