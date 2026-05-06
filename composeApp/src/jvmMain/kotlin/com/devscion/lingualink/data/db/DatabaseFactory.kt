package com.devscion.lingualink.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.devscion.lingualink.db.LinguaLinkDB
import java.io.File

object DatabaseFactory {
    fun create(): LinguaLinkDB {
        val dbDir = File(System.getProperty("user.home"), ".lingualink").also { it.mkdirs() }
        val dbFile = File(dbDir, "lingualink.db")
        val preExisting = dbFile.exists() && dbFile.length() > 0

        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        val schema = LinguaLinkDB.Schema
        val target = schema.version
        val current = readUserVersion(driver)

        when {
            // Brand new DB
            !preExisting && current == 0L -> {
                schema.create(driver)
                writeUserVersion(driver, target)
            }
            // Legacy DB created before user_version tracking — assume schema is current
            preExisting && current == 0L -> {
                writeUserVersion(driver, target)
            }
            // Forward migration
            current < target -> {
                schema.migrate(driver, current, target)
                writeUserVersion(driver, target)
            }
        }

        return LinguaLinkDB(driver)
    }

    private fun readUserVersion(driver: JdbcSqliteDriver): Long =
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0) ?: 0L)
            },
            parameters = 0,
        ).value

    private fun writeUserVersion(driver: JdbcSqliteDriver, version: Long) {
        driver.execute(null, "PRAGMA user_version = $version", 0)
    }
}
