package observable

import com.beust.klaxon.Json
import db.DB
import db.Ignored

abstract class DBAwareObject {

    @Ignored
    @Json(ignored = true)
    var db: DB? = null

    fun setDbReference(db: DB){
        if(this.db != null && this.db != db){
            throw IllegalStateException("This object is already used in another DB Context. Clone it or use multiple Backends to use with another DB Instance")
        }
        this.db = db
    }

    fun getDB() = db!!

}