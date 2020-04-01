package observable

import db.DB

abstract class DBAwareObject {

    var db: DB? = null

    fun setDbReference(db: DB){
        this.db = db
    }

    fun getDB() = db!!

}