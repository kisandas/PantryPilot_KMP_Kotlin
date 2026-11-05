package com.app.meal.planner.domain

import com.app.meal.planner.core.model.*
import kotlinx.datetime.LocalDate

const val STOCK_EPSILON = 0.000000001
fun validStockQuantity(quantity: Double, unit: String): Boolean =
    quantity.isFinite() && quantity > 0 && unit in units && (unit != "piece" || quantity % 1.0 == 0.0)

fun compatibleStockUnits(unit: String): List<String> = when (unit) {
    "g", "kg" -> listOf("g", "kg")
    "ml", "L" -> listOf("ml", "L")
    else -> listOf(unit)
}

fun convertStockQuantity(quantity: Double, from: String, to: String): Double {
    require(to in compatibleStockUnits(from)) { "Choose a compatible stock unit." }
    if (from == to) return quantity
    return if (from == "g" || from == "ml") quantity / 1000 else quantity * 1000
}

fun orderStatus(lines: List<OrderLine>): OrderStatus = when {
    lines.all { it.receivedQuantity >= it.quantity - STOCK_EPSILON } -> OrderStatus.Received
    lines.any { it.receivedQuantity > 0 } -> OrderStatus.PartiallyReceived
    else -> OrderStatus.Placed
}

fun recipeShortages(recipe: Recipe,pantry: List<PantryItem>,today: LocalDate): List<RecipeIngredient> =
    recipe.ingredients.groupBy {it.normalizedName to it.unit}.mapNotNull { (key,ingredients) ->
        val stocked=pantry.filter {it.normalizedName==key.first && it.unit==key.second && it.quantity>0 && expiry(it.expiryDate,today)!=Expiry.Expired}.sumOf {it.quantity}
        val shortage=ingredients.sumOf {it.quantity}-stocked
        if(shortage>STOCK_EPSILON) ingredients.first().copy(quantity=shortage) else null
    }

/** Plan all deductions first: an insufficient ingredient fails without deducting anything. */
fun recipeDeductions(recipe: Recipe, pantry: List<PantryItem>, today: LocalDate): Map<String, Double> {
    val deductions = linkedMapOf<String, Double>()
    recipe.ingredients.groupBy { it.normalizedName to it.unit }.forEach { (key, ingredients) ->
        var needed = ingredients.sumOf { it.quantity }
        val batches = pantry.filter {
            it.normalizedName == key.first && it.unit == key.second && it.quantity > 0 && expiry(it.expiryDate,today) != Expiry.Expired
        }.sortedWith(compareBy<PantryItem> { it.expiryDate ?: "9999-12-31" }.thenBy { it.createdAt }.thenBy { it.id })
        batches.forEach { batch ->
            if(needed > STOCK_EPSILON) {
                val take = minOf(batch.quantity, needed)
                deductions[batch.id] = take
                needed -= take
            }
        }
        require(needed <= STOCK_EPSILON) { "Not enough ${ingredients.first().displayName} in ${key.second}. Receive more stock or record actual usage manually. No quantities were reduced." }
    }
    return deductions
}
