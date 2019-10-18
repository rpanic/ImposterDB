package db

class Tournament : Observable(){

    var name: String by observable("")

    var description: String? by observable(null)

}

class TournamentObserver(t: Tournament) : ChangeObserver<Tournament>(t){

    fun name(new: String){
        println("New name: $new!!!!")
    }

    fun all(new: Any?){
        println("Prop changed $new")
    }

}

fun main() {

    val list = DB.getList<Tournament>("tournaments")

    val t = Tournament()

    list.add(t)

    t.description = "15 Years"

    t.name = "New Tournament"

    //look into data/tournaments.json

}