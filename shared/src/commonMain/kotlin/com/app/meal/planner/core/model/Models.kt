package com.app.meal.planner.core.model

import kotlinx.serialization.Serializable

enum class Category { Vegetables, Fruits, Dairy, Grains, Protein, Spices, Other }
val units = listOf("piece", "g", "kg", "ml", "L")
@Serializable data class PantryItem(val id: String, val name: String, val normalizedName: String, val category: Category, val quantity: Double, val unit: String, val expiryDate: String? = null, val createdAt: Long, val updatedAt: Long)
@Serializable data class ShoppingItem(val id: String, val name: String, val normalizedName: String, val quantity: Double, val unit: String, val isPurchased: Boolean = false, val createdAt: Long, val orderId: String? = null)
@Serializable data class RecipeIngredient(val displayName: String, val quantity: Double, val unit: String) { val normalizedName get() = com.app.meal.planner.domain.normalize(displayName) }
@Serializable data class Recipe(val id: String, val title: String, val description: String, val category: String, val preparationMinutes: Int, val servings: Int, val ingredients: List<RecipeIngredient>, val instructions: List<String>, val visual: String)
@Serializable data class Records(
    val pantry: List<PantryItem> = emptyList(),
    val shopping: List<ShoppingItem> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val theme: String = "System",
    val orders: List<PurchaseOrder> = emptyList(),
    val movements: List<StockMovement> = emptyList(),
    val stockTrackingInitialized: Boolean = false,
    val initialDataSeeded: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
)

@Serializable enum class OrderStatus { Draft, Placed, PartiallyReceived, Received, Cancelled }
@Serializable data class OrderLine(
    val id: String, val name: String, val normalizedName: String,
    val quantity: Double, val unit: String, val category: Category = Category.Other,
    val receivedQuantity: Double = 0.0, val shoppingItemId: String? = null,
)
@Serializable data class PurchaseOrder(
    val id: String, val number: String, val store: String, val notes: String,
    val lines: List<OrderLine>, val status: OrderStatus = OrderStatus.Draft,
    val createdAt: Long, val updatedAt: Long,
)
@Serializable enum class MovementType { Opening, Purchase, Used, Waste, Adjustment, Removed }
@Serializable data class StockMovement(
    val id: String, val pantryItemId: String, val name: String, val unit: String,
    val type: MovementType, val delta: Double, val balanceAfter: Double,
    val occurredAt: Long, val note: String = "", val orderId: String? = null,
    val expiryDate: String? = null,
)
