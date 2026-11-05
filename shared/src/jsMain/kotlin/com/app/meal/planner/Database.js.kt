@file:Suppress("UnsafeCastFromDynamic")

package com.app.meal.planner.core.database

import com.app.meal.planner.core.model.*
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlin.js.Promise

actual fun createDatabase(): LocalDatabase = BrowserDatabase()
class BrowserDatabase : LocalDatabase {
    private var opened: dynamic = null
    private suspend fun database(): dynamic {
        if (opened != null) return opened
        opened = Promise<dynamic> { resolve, reject ->
            val request = js("globalThis.indexedDB.open('PantryPilot', 1)")
            request.onupgradeneeded = { _: dynamic ->
                request.result.createObjectStore(
                    "records",
                    js("({keyPath:'key'})")
                ); Unit
            }
            request.onsuccess = { _: dynamic -> resolve(request.result) }
            request.onerror =
                { _: dynamic -> reject(Throwable("Cannot open browser database: ${request.error}")) }
            request.onblocked =
                { _: dynamic -> reject(Throwable("Close other PantryPilot tabs and retry.")) }
        }.await()
        opened.onversionchange = { _: dynamic -> opened.close(); opened = null }
        return opened
    }

    override suspend fun read(): Records {
        val db = database()
        return Promise<Records> { resolve, reject ->
            val tx = db.transaction(arrayOf("records"), "readonly")
            val request = tx.objectStore("records").getAll()
            tx.oncomplete = { _: dynamic ->
                try {
                    val decoded = mutableListOf<DatabaseRow>()
                    val rows = request.result
                    for (i in 0 until (rows.length as Int)) {
                        val row = rows[i]
                        decoded.add(
                            DatabaseRow(
                                row.kind as String,
                                row.id as String,
                                row.payload as String
                            )
                        )
                    }
                    resolve(decodeRows(decoded))
                } catch (e: Throwable) {
                    reject(e)
                }
            }
            tx.onerror = { _: dynamic -> reject(Throwable("Browser database read failed.")) }
            tx.onabort = { _: dynamic -> reject(Throwable("Browser database read aborted.")) }
        }.await()
    }

    override suspend fun write(records: Records) {
        val db = database()
        Promise<Unit> { resolve, reject ->
            val tx = db.transaction(arrayOf("records"), "readwrite");
            val store = tx.objectStore("records")
            store.clear()
            fun put(kind: String, id: String, payload: String) {
                val row = js("({})"); row.key = "$kind:$id"; row.kind = kind; row.id =
                    id; row.payload = payload; store.put(row)
            }
            encodeRows(records).forEach { put(it.kind, it.id, it.payload) }
            tx.oncomplete = { _: dynamic -> resolve(Unit) }
            tx.onerror =
                { _: dynamic -> reject(Throwable("Browser database write failed. Check available storage.")) }
            tx.onabort =
                { _: dynamic -> reject(Throwable("Browser database write aborted; changes were not saved.")) }
        }.await()
    }

    override fun close() {
        if (opened != null) opened.close(); opened = null
    }
}
