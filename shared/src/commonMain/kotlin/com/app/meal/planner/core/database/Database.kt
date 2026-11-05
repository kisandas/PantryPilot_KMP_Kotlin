package com.app.meal.planner.core.database

import com.app.meal.planner.core.model.Records

/** Atomic snapshot transaction over real SQLite tables / IndexedDB object stores. */
interface LocalDatabase {
    suspend fun read(): Records;
    suspend fun write(records: Records);
    fun close() {}
}

expect fun createDatabase(): LocalDatabase
