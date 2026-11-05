package com.app.meal.planner.domain

import com.app.meal.planner.core.model.*
import kotlinx.datetime.*
import kotlinx.datetime.Clock

interface DateProvider {
    fun today(): LocalDate;
    fun timestamp(): Long
}

class SystemDateProvider : DateProvider {
    override fun today() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    override fun timestamp() = Clock.System.now().toEpochMilliseconds()
}

fun normalize(name: String): String {
    val clean = name.trim().lowercase().split(Regex("\\s+")).joinToString(" ")
    return mapOf(
        "tomatoes" to "tomato",
        "eggs" to "egg",
        "potatoes" to "potato",
        "bell pepper" to "pepper"
    )[clean] ?: clean
}

enum class Expiry { Expired, Soon, Fresh, Undated }

fun expiry(date: String?, today: LocalDate): Expiry = when {
    date == null -> Expiry.Undated
    LocalDate.parse(date) < today -> Expiry.Expired
    LocalDate.parse(date) <= today.plus(3, DateTimeUnit.DAY) -> Expiry.Soon
    else -> Expiry.Fresh
}

fun available(pantry: List<PantryItem>, today: LocalDate) =
    pantry.filter { it.quantity > 0 && expiry(it.expiryDate, today) != Expiry.Expired }
        .map { it.normalizedName }.toSet()

fun missing(recipe: Recipe, pantry: List<PantryItem>, today: LocalDate) =
    recipe.ingredients.filter { it.normalizedName !in available(pantry, today) }

fun rank(recipes: List<Recipe>, pantry: List<PantryItem>, today: LocalDate): List<Recipe> {
    val soon = pantry.filter { it.quantity > 0 && expiry(it.expiryDate, today) == Expiry.Soon }
        .map { it.normalizedName }.toSet()
    return recipes.sortedWith(compareByDescending<Recipe> { r -> r.ingredients.count { it.normalizedName in soon } }.thenBy {
        missing(
            it,
            pantry,
            today
        ).size
    }.thenBy { it.id })
}

fun validation(name: String, quantity: String, date: String): Map<String, String> = buildMap {
    if (name.trim().isEmpty()) put("name", "Enter an ingredient name.")
    val q = quantity.toDoubleOrNull()
    if (q == null || !q.isFinite() || q <= 0) put("quantity", "Enter a positive, finite quantity.")
    if (date.isNotBlank() && runCatching { LocalDate.parse(date) }.isFailure) put(
        "date",
        "Use YYYY-MM-DD or leave blank."
    )
}

fun mergeShopping(existing: List<ShoppingItem>, additions: List<ShoppingItem>): List<ShoppingItem> {
    val result = existing.toMutableList()
    additions.forEach { item ->
        if (result.none { !it.isPurchased && it.normalizedName == item.normalizedName && it.unit == item.unit }) result.add(
            item
        )
    }
    return result
}

