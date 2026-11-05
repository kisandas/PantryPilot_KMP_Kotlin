package com.app.meal.planner.core.database

import com.app.meal.planner.core.model.*
import kotlinx.serialization.json.Json

/** Common row format for SQLite and IndexedDB. New record kinds need no destructive migration. */
data class DatabaseRow(val kind: String, val id: String, val payload: String)

fun encodeRows(records: Records): List<DatabaseRow> = buildList {
    records.pantry.forEach { add(DatabaseRow("pantry", it.id, Json.encodeToString(it))) }
    records.shopping.forEach { add(DatabaseRow("shopping", it.id, Json.encodeToString(it))) }
    records.recipes.forEach { add(DatabaseRow("recipe", it.id, Json.encodeToString(it))) }
    records.favorites.forEach { add(DatabaseRow("favorite", it, "")) }
    add(DatabaseRow("theme", "theme", records.theme))
    records.orders.forEach { add(DatabaseRow("order", it.id, Json.encodeToString(it))) }
    records.movements.forEach { add(DatabaseRow("movement", it.id, Json.encodeToString(it))) }
    add(DatabaseRow("stockTracking", "version", if (records.stockTrackingInitialized) "1" else "0"))
    add(DatabaseRow("initialData", "version", if (records.initialDataSeeded) "1" else "0"))
}

fun decodeRows(rows: List<DatabaseRow>): Records {
    val pantry = mutableListOf<PantryItem>()
    val shopping = mutableListOf<ShoppingItem>()
    val favorites = mutableSetOf<String>()
    val orders = mutableListOf<PurchaseOrder>()
    val movements = mutableListOf<StockMovement>()
    val recipes = mutableListOf<Recipe>()
    var theme = "System"
    var initialized = false
    var seeded = false
    rows.forEach { row ->
        when (row.kind) {
            "pantry" -> pantry.add(Json.decodeFromString<PantryItem>(row.payload))
            "shopping" -> shopping.add(Json.decodeFromString<ShoppingItem>(row.payload))
            "recipe" -> recipes.add(Json.decodeFromString<Recipe>(row.payload))
            "favorite" -> favorites.add(row.id)
            "theme" -> theme = row.payload
            "order" -> orders.add(Json.decodeFromString<PurchaseOrder>(row.payload))
            "movement" -> movements.add(Json.decodeFromString<StockMovement>(row.payload))
            "stockTracking" -> initialized = row.payload == "1"
            "initialData" -> seeded = row.payload == "1"
            else -> error("Database contains a newer record type. Update PantryPilot before editing data.")
        }
    }
    return Records(
        pantry,
        shopping,
        favorites,
        theme,
        orders,
        movements,
        initialized,
        seeded,
        recipes
    )
}
