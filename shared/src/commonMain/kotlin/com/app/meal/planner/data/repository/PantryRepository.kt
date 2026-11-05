package com.app.meal.planner.data.repository

import com.app.meal.planner.core.database.LocalDatabase
import com.app.meal.planner.core.model.*
import com.app.meal.planner.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*
import kotlin.random.Random

class PantryRepository(private val database: LocalDatabase, private val clock: DateProvider) {
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(Records())
    val records = mutable.asStateFlow()
    suspend fun load() = mutex.withLock {
        val stored = database.read()
        val loaded = stored
        val migrated = if (loaded.stockTrackingInitialized) loaded else loaded.copy(
            stockTrackingInitialized = true,
            movements = loaded.movements + loaded.pantry.map { p ->
                StockMovement(
                    "opening:${p.id}",
                    p.id,
                    p.name,
                    p.unit,
                    MovementType.Opening,
                    p.quantity,
                    p.quantity,
                    clock.timestamp(),
                    "Stock balance when tracking was enabled",
                    expiryDate = p.expiryDate
                )
            },
        )
        if (migrated != stored) database.write(migrated)
        mutable.value = migrated
    }

    suspend fun update(transform: (Records) -> Records) = mutex.withLock {
        val next = transform(mutable.value).copy(stockTrackingInitialized = true)
        database.write(next)
        mutable.value = next
    }

    fun id() = "${clock.timestamp()}-${Random.nextInt().toUInt()}"
    suspend fun saveRecipe(recipe: Recipe) = update { r ->
        require(recipe.title.isNotBlank()) { "Enter a recipe title." }
        require(recipe.preparationMinutes in 1..1440 && recipe.servings in 1..100) { "Enter a preparation time from 1–1440 minutes and 1–100 servings." }
        require(recipe.ingredients.isNotEmpty() && recipe.ingredients.all {
            it.displayName.isNotBlank() && it.unit in units && validStockQuantity(
                it.quantity,
                it.unit
            )
        }) { "Add ingredients with valid quantities and units." }
        require(recipe.instructions.isNotEmpty() && recipe.instructions.all { it.isNotBlank() }) { "Add at least one instruction." }
        r.copy(recipes = r.recipes.filterNot { it.id == recipe.id } + recipe)
    }

    suspend fun deleteRecipe(recipeId: String) = update { r ->
        r.copy(
            recipes = r.recipes.filterNot { it.id == recipeId },
            favorites = r.favorites - recipeId
        )
    }

    private fun movement(
        item: PantryItem,
        type: MovementType,
        delta: Double,
        balance: Double,
        note: String,
        orderId: String? = null
    ) =
        StockMovement(
            id(),
            item.id,
            item.name,
            item.unit,
            type,
            delta,
            balance,
            clock.timestamp(),
            note,
            orderId,
            item.expiryDate
        )

    suspend fun savePantry(item: PantryItem) = update { r ->
        require(
            item.name.isNotBlank() && validStockQuantity(
                item.quantity,
                item.unit
            )
        ) { "Enter a positive quantity; pieces must be whole numbers." }
        val old = r.pantry.find { it.id == item.id }
        require(old == null || old.unit == item.unit) { "A batch's unit cannot be changed. Add a separate batch in the new unit." }
        val delta = item.quantity - (old?.quantity ?: 0.0)
        r.copy(
            pantry = r.pantry.filterNot { it.id == item.id } + item,
            movements = if (old == null || delta != 0.0) r.movements + movement(
                item,
                if (old == null) MovementType.Opening else MovementType.Adjustment,
                delta,
                item.quantity,
                if (old == null) "Added stock batch" else "Manual stock count correction"
            ) else r.movements
        )
    }

    suspend fun restock(batchId: String, quantity: Double, unit: String, date: String?) =
        update { records ->
            val original = records.pantry.firstOrNull { it.id == batchId }
                ?: error("Stock batch no longer exists.")
            require(
                validStockQuantity(
                    quantity,
                    unit
                )
            ) { "Enter a positive quantity in a compatible unit." }
            val converted = convertStockQuantity(quantity, unit, original.unit)
            require(
                validStockQuantity(
                    converted,
                    original.unit
                )
            ) { "Quantity is outside the supported range." }
            val timestamp = clock.timestamp()
            val batch = original.copy(
                id = id(), quantity = converted, expiryDate = date,
                createdAt = timestamp, updatedAt = timestamp
            )
            records.copy(
                pantry = records.pantry + batch,
                movements = records.movements + movement(
                    batch,
                    MovementType.Purchase,
                    converted,
                    converted,
                    "Restocked"
                )
            )
        }

    suspend fun saveShopping(item: ShoppingItem) = update { r ->
        require(
            item.name.isNotBlank() && validStockQuantity(
                item.quantity,
                item.unit
            )
        ) { "Enter a positive quantity; pieces must be whole numbers." }
        require(r.shopping.find { it.id == item.id }?.orderId == null) { "This item is linked to an order. Manage it from Orders." }
        r.copy(shopping = r.shopping.filterNot { it.id == item.id } + item)
    }

    suspend fun deleteItem(itemId: String, shopping: Boolean) = update { r ->
        if (shopping) {
            require(r.shopping.find { it.id == itemId }?.orderId == null) { "Cancel the linked order before deleting this shopping item." }
            r.copy(shopping = r.shopping.filterNot { it.id == itemId })
        } else {
            val item =
                r.pantry.firstOrNull { it.id == itemId } ?: error("Stock batch no longer exists.")
            r.copy(
                pantry = r.pantry.filterNot { it.id == itemId },
                movements = r.movements + movement(
                    item,
                    MovementType.Removed,
                    -item.quantity,
                    0.0,
                    "Stock batch removed"
                )
            )
        }
    }

    suspend fun reduceStock(itemId: String, quantity: Double, waste: Boolean, note: String) =
        update { r ->
            val item =
                r.pantry.firstOrNull { it.id == itemId } ?: error("Stock batch no longer exists.")
            require(
                validStockQuantity(
                    quantity,
                    item.unit
                )
            ) { "Enter a positive quantity; pieces must be whole numbers." }
            require(quantity <= item.quantity) { "Only ${item.quantity} ${item.unit} remain in this batch." }
            require(
                waste || expiry(
                    item.expiryDate,
                    clock.today()
                ) != Expiry.Expired
            ) { "This batch is expired. Record it as waste instead." }
            val balance = (item.quantity - quantity).coerceAtLeast(0.0)
            r.copy(
                pantry = r.pantry.map {
                    if (it.id == itemId) it.copy(
                        quantity = balance,
                        updatedAt = clock.timestamp()
                    ) else it
                },
                movements = r.movements + movement(
                    item,
                    if (waste) MovementType.Waste else MovementType.Used,
                    -quantity,
                    balance,
                    note.trim()
                )
            )
        }

    suspend fun cookRecipe(recipe: Recipe) = update { r ->
        val deductions = recipeDeductions(recipe, r.pantry, clock.today())
        val moves = r.pantry.mapNotNull { p ->
            deductions[p.id]?.let { amount ->
                movement(
                    p,
                    MovementType.Used,
                    -amount,
                    (p.quantity - amount).coerceAtLeast(0.0),
                    "Cooked: ${recipe.title} (${recipe.servings} servings)"
                )
            }
        }
        r.copy(pantry = r.pantry.map { p ->
            deductions[p.id]?.let {
                p.copy(
                    quantity = (p.quantity - it).coerceAtLeast(
                        0.0
                    ), updatedAt = clock.timestamp()
                )
            } ?: p
        }, movements = r.movements + moves)
    }

    suspend fun togglePurchased(itemId: String) = update { r ->
        require(r.shopping.find { it.id == itemId }?.orderId == null) { "Receive this item from its order; linked receipts cannot be unchecked." }
        r.copy(shopping = r.shopping.map { if (it.id == itemId) it.copy(isPurchased = !it.isPurchased) else it })
    }

    suspend fun buyAgain(itemId: String) = update { r ->
        val source = r.shopping.first { it.id == itemId }
        r.copy(
            shopping = r.shopping + source.copy(
                id = id(),
                isPurchased = false,
                orderId = null,
                createdAt = clock.timestamp()
            )
        )
    }

    suspend fun createOrder(store: String, notes: String): String {
        var orderId = ""
        update { r ->
            val items = r.shopping.filter { !it.isPurchased && it.orderId == null }
            require(items.isNotEmpty()) { "Add unpurchased items to Shopping before creating an order." }
            orderId = id()
            val order = PurchaseOrder(
                orderId,
                "PO-${r.orders.size + 1}",
                store.trim(),
                notes.trim(),
                items.map {
                    OrderLine(
                        id(),
                        it.name,
                        it.normalizedName,
                        it.quantity,
                        it.unit,
                        shoppingItemId = it.id
                    )
                },
                createdAt = clock.timestamp(),
                updatedAt = clock.timestamp()
            )
            r.copy(
                orders = r.orders + order,
                shopping = r.shopping.map { if (it.id in items.map { i -> i.id }) it.copy(orderId = orderId) else it })
        }
        return orderId
    }

    suspend fun saveDraft(order: PurchaseOrder) = update { r ->
        val old = r.orders.first { it.id == order.id }
        require(old.status == OrderStatus.Draft) { "Only draft orders can be edited." }
        require(order.lines.size == old.lines.size && order.lines.map { it.id } == old.lines.map { it.id }) { "Order lines changed. Reopen the order." }
        require(
            order.lines.zip(old.lines)
                .all { (line, previous) -> line.name == previous.name && line.normalizedName == previous.normalizedName && line.unit == previous.unit && line.shoppingItemId == previous.shoppingItemId }) { "Cancel the draft and edit Shopping to change products or units." }
        require(order.lines.all {
            it.name.isNotBlank() && validStockQuantity(
                it.quantity,
                it.unit
            ) && it.receivedQuantity == 0.0
        }) { "Each order line needs a positive quantity; pieces must be whole numbers." }
        val updated = old.copy(
            store = order.store.trim(),
            notes = order.notes.trim(),
            lines = order.lines,
            updatedAt = clock.timestamp()
        )
        r.copy(
            orders = r.orders.map { if (it.id == old.id) updated else it },
            shopping = r.shopping.map { item ->
                updated.lines.find { it.shoppingItemId == item.id }
                    ?.let { item.copy(quantity = it.quantity) } ?: item
            })
    }

    suspend fun placeOrder(orderId: String) = update { r ->
        val order = r.orders.first { it.id == orderId }
        require(order.status == OrderStatus.Draft) { "Order is already placed or closed." }
        require(order.lines.isNotEmpty() && order.lines.all {
            validStockQuantity(
                it.quantity,
                it.unit
            )
        }) { "Check order quantities before placing." }
        r.copy(orders = r.orders.map {
            if (it.id == orderId) it.copy(
                status = OrderStatus.Placed,
                updatedAt = clock.timestamp()
            ) else it
        })
    }

    suspend fun cancelOrder(orderId: String) = update { r ->
        val order = r.orders.first { it.id == orderId }
        require(order.status != OrderStatus.Received && order.status != OrderStatus.Cancelled) { "This order is already closed." }
        r.copy(
            orders = r.orders.map {
                if (it.id == orderId) it.copy(
                    status = OrderStatus.Cancelled,
                    updatedAt = clock.timestamp()
                ) else it
            },
            shopping = r.shopping.map { item ->
                order.lines.find { it.shoppingItemId == item.id }?.let { line ->
                    if (line.receivedQuantity < line.quantity - STOCK_EPSILON) item.copy(
                        orderId = null,
                        isPurchased = false,
                        quantity = line.quantity - line.receivedQuantity
                    )
                    else item
                } ?: item
            })
    }

    private fun receipt(
        r: Records,
        orderId: String,
        lineId: String,
        quantity: Double,
        date: String?,
        category: Category
    ): Records {
        val order = r.orders.first { it.id == orderId }
        require(order.status == OrderStatus.Placed || order.status == OrderStatus.PartiallyReceived) { "Place the order before receiving it." }
        val line = order.lines.first { it.id == lineId }
        require(
            validStockQuantity(
                quantity,
                line.unit
            )
        ) { "Enter a positive receipt quantity; pieces must be whole numbers." }
        require(quantity <= line.quantity - line.receivedQuantity + STOCK_EPSILON) { "Cannot receive more than the outstanding quantity." }
        require(
            date == null || expiry(
                date,
                clock.today()
            ) != Expiry.Expired
        ) { "New receipts cannot have an expired date. Check the date or return the purchase." }
        val received = minOf(quantity, line.quantity - line.receivedQuantity)
        val batch = PantryItem(
            id(),
            line.name,
            line.normalizedName,
            category,
            received,
            line.unit,
            date,
            clock.timestamp(),
            clock.timestamp()
        )
        val lines = order.lines.map {
            if (it.id == lineId) it.copy(
                receivedQuantity = (it.receivedQuantity + received).coerceAtMost(it.quantity),
                category = category
            ) else it
        }
        val updated =
            order.copy(lines = lines, status = orderStatus(lines), updatedAt = clock.timestamp())
        return r.copy(
            pantry = r.pantry + batch,
            orders = r.orders.map { if (it.id == orderId) updated else it },
            shopping = r.shopping.map { item ->
                if (item.id == line.shoppingItemId) item.copy(
                    isPurchased = lines.first { it.id == lineId }.receivedQuantity >= line.quantity - STOCK_EPSILON
                ) else item
            },
            movements = r.movements + movement(
                batch,
                MovementType.Purchase,
                received,
                received,
                "Received ${order.number}${if (order.store.isBlank()) "" else " · ${order.store}"}",
                orderId
            )
        )
    }

    suspend fun receiveOrderLine(
        orderId: String,
        lineId: String,
        quantity: Double,
        date: String?,
        category: Category
    ) = update { receipt(it, orderId, lineId, quantity, date, category) }

    suspend fun receiveOutstanding(orderId: String, lineIds: Set<String>) = update { records ->
        require(lineIds.isNotEmpty()) { "Select at least one item." }
        val order = records.orders.first { it.id == orderId }
        val lines = order.lines.filter { it.id in lineIds }
        require(lines.size == lineIds.size) { "Some selected items are no longer in this order." }
        lines.fold(records) { current, line ->
            receipt(
                current,
                orderId,
                line.id,
                line.quantity - line.receivedQuantity,
                null,
                line.category
            )
        }
    }

    suspend fun receiveShopping(
        itemId: String,
        quantity: Double,
        date: String?,
        category: Category
    ) = update { r ->
        val item = r.shopping.first { it.id == itemId }
        require(item.orderId == null) { "This item already belongs to an order. Receive it from Orders." }
        val line = OrderLine(
            id(),
            item.name,
            item.normalizedName,
            item.quantity,
            item.unit,
            category,
            shoppingItemId = item.id
        )
        val order = PurchaseOrder(
            id(),
            "PO-${r.orders.size + 1}",
            "",
            "Quick receipt from Shopping",
            listOf(line),
            OrderStatus.Placed,
            clock.timestamp(),
            clock.timestamp()
        )
        val prepared = r.copy(
            orders = r.orders + order,
            shopping = r.shopping.map { if (it.id == itemId) it.copy(orderId = order.id) else it })
        receipt(prepared, order.id, line.id, quantity, date, category)
    }

    suspend fun addMissing(recipe: Recipe): Int {
        var count = 0
        update { current ->
            val additions = recipeShortages(recipe, current.pantry, clock.today()).map {
                ShoppingItem(
                    id(),
                    it.displayName,
                    it.normalizedName,
                    it.quantity,
                    it.unit,
                    createdAt = clock.timestamp()
                )
            }
            val merged = mergeShopping(current.shopping, additions); count =
            merged.size - current.shopping.size
            current.copy(shopping = merged)
        }
        return count
    }
}

