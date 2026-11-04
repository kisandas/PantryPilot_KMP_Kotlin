package com.app.meal.planner.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.app.meal.planner.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

actual fun createDatabase(): LocalDatabase = error("Android requires a Context; inject AndroidDatabase from the activity.")
class AndroidDatabase(context: Context) : LocalDatabase {
    private val appContext = context.applicationContext
    private val db by lazy { appContext.openOrCreateDatabase("pantry-pilot.db", Context.MODE_PRIVATE, null).apply {
        execSQL("CREATE TABLE IF NOT EXISTS records (kind TEXT NOT NULL, id TEXT NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(kind,id))")
        execSQL("PRAGMA user_version = 1")
    } }
    override suspend fun read(): Records = withContext(Dispatchers.IO) {
        val rows = mutableListOf<DatabaseRow>()
        db.rawQuery("SELECT kind,id,payload FROM records", null).use { c -> while(c.moveToNext()) {
            rows.add(DatabaseRow(c.getString(0),c.getString(1),c.getString(2)))
        } }
        decodeRows(rows)
    }
    override suspend fun write(records: Records) = withContext(Dispatchers.IO) {
        db.beginTransaction()
        try {
            db.delete("records", null, null)
            fun put(kind: String, id: String, payload: String) { db.execSQL("INSERT INTO records VALUES(?,?,?)", arrayOf(kind,id,payload)) }
            encodeRows(records).forEach { put(it.kind,it.id,it.payload) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    override fun close() = db.close()
}

