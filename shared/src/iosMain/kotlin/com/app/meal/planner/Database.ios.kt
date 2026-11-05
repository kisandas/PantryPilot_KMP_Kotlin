@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.app.meal.planner.core.database

import com.app.meal.planner.core.model.*
// SQLite handles are opaque C structs, generated outside the sqlite function package.
import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.*
import platform.Foundation.*
import sqlite.*

actual fun createDatabase(): LocalDatabase = AppleDatabase()
class AppleDatabase : LocalDatabase {
    private val db: CPointer<sqlite3>

    init {
        val directory = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String
        db = memScoped {
            val pointer = alloc<CPointerVar<sqlite3>>()
            check(
                sqlite3_open(
                    "$directory/pantry-pilot.db",
                    pointer.ptr
                ) == SQLITE_OK
            ) { "Cannot open SQLite database" }
            pointer.value!!
        }
        execute("CREATE TABLE IF NOT EXISTS records(kind TEXT NOT NULL,id TEXT NOT NULL,payload TEXT NOT NULL,PRIMARY KEY(kind,id))")
        execute("PRAGMA user_version = 1")
    }

    private fun execute(sql: String) {
        check(
            sqlite3_exec(
                db,
                sql,
                null,
                null,
                null
            ) == SQLITE_OK
        ) { sqlite3_errmsg(db)?.toKString() ?: "SQLite operation failed" }
    }

    override suspend fun read(): Records = memScoped {
        val statement = alloc<CPointerVar<sqlite3_stmt>>()
        check(
            sqlite3_prepare_v2(
                db,
                "SELECT kind,id,payload FROM records",
                -1,
                statement.ptr,
                null
            ) == SQLITE_OK
        )
        val s = statement.value!!
        val rows = mutableListOf<DatabaseRow>()
        try {
            var result = sqlite3_step(s)
            while (result == SQLITE_ROW) {
                fun column(index: Int) =
                    sqlite3_column_text(s, index)!!.reinterpret<ByteVar>().toKString()
                rows.add(DatabaseRow(column(0), column(1), column(2)))
                result = sqlite3_step(s)
            }
            check(result == SQLITE_DONE) { "SQLite read failed" }
        } finally {
            sqlite3_finalize(s)
        }
        decodeRows(rows)
    }

    override suspend fun write(records: Records) {
        fun quote(value: String) = "'${value.replace("'", "''")}'"
        fun put(kind: String, id: String, payload: String) =
            execute("INSERT INTO records VALUES(${quote(kind)},${quote(id)},${quote(payload)})")
        execute("BEGIN IMMEDIATE")
        try {
            execute("DELETE FROM records")
            encodeRows(records).forEach { put(it.kind, it.id, it.payload) }
            execute("COMMIT")
        } catch (e: Throwable) {
            execute("ROLLBACK"); throw e
        }
    }

    override fun close() {
        sqlite3_close(db)
    }
}
