package sql

import java.sql.Connection

data class SqlContext(val connection: Connection, val dbName: String) {
    fun executeQuery(s: String) =
        this.connection.createStatement().executeQuery(s)

    fun execute(s: String) = this.connection.createStatement().execute(s)

    fun executeUpdate(s: String) = this.connection.createStatement().executeUpdate(s)
}