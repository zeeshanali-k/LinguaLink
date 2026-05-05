package com.devscion.lingualink.data.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.devscion.lingualink.db.LinguaLinkDB
import java.io.File

object DatabaseFactory {
    fun create(): LinguaLinkDB {
        val dbDir = File(System.getProperty("user.home"), ".lingualink").also { it.mkdirs() }
        val dbFile = File(dbDir, "lingualink.db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        LinguaLinkDB.Schema.create(driver)
        return LinguaLinkDB(driver)
    }
}
