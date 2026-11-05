package com.app.meal.planner.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.meal.planner.core.model.*
import com.app.meal.planner.data.repository.*
import com.app.meal.planner.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class DashboardUiState(val settings: Boolean = false)
data class PantryUiState(
    val search: String = "",
    val category: Category? = null,
    val expiry: String = "All",
    val sortByName: Boolean = false
)

data class RecipesUiState(
    val search: String = "",
    val filter: String = "All",
    val selected: String? = null
)

data class ShoppingUiState(val showPurchased: Boolean = true)
data class SettingsUiState(val confirmation: String? = null)
data class Editor(
    val id: String? = null,
    val shopping: Boolean = false,
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "piece",
    val category: Category = Category.Other,
    val date: String = "",
    val errors: Map<String, String> = emptyMap(),
    val restockId: String? = null,
    val restockUnit: String? = null
)

data class StockAction(
    val itemId: String,
    val waste: Boolean = false,
    val quantity: String = "1",
    val note: String = "",
    val error: String? = null
)

data class ReceiptEditor(
    val orderId: String? = null,
    val lineId: String? = null,
    val shoppingItemId: String? = null,
    val quantity: String,
    val date: String = "",
    val category: Category = Category.Other,
    val error: String? = null
)

data class OrderEditor(
    val order: PurchaseOrder? = null,
    val store: String = "",
    val notes: String = "",
    val quantities: Map<String, String> = emptyMap(),
    val error: String? = null
)

data class OrdersUiState(val filter: String = "All", val selected: String? = null)
enum class FormType { Ingredient, Stock, Receipt, Order }
enum class FormField { Name, Quantity, Unit, Category, Date, Note, Waste, Store, OrderLineQuantity }
data class PilotUiState(
    val records: Records = Records(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val tab: String = "Home",
    val pantry: PantryUiState = PantryUiState(),
    val recipes: RecipesUiState = RecipesUiState(),
    val shopping: ShoppingUiState = ShoppingUiState(),
    val dashboard: DashboardUiState = DashboardUiState(),
    val settings: SettingsUiState = SettingsUiState(),
    val editor: Editor? = null,
    val today: kotlinx.datetime.LocalDate,
    val stockAction: StockAction? = null,
    val receipt: ReceiptEditor? = null,
    val orderEditor: OrderEditor? = null,
    val orders: OrdersUiState = OrdersUiState(),
    val historyItemId: String? = null,
    val recipeEditor: RecipeEditor? = null
)

sealed interface Intent {
    data class EditRecipe(val value: RecipeEditor?) : Intent
    data class RecipeField(val field: String, val value: String) : Intent
    data object SaveRecipe : Intent
    data object Retry : Intent
    data object RefreshDate : Intent
    data class Navigate(val tab: String) : Intent
    data class PantryFilter(val value: PantryUiState) : Intent
    data class RecipeFilter(val value: RecipesUiState) : Intent
    data class Edit(val value: Editor?) : Intent
    data object Save : Intent
    data class Delete(val id: String, val shopping: Boolean) : Intent
    data class Purchased(val id: String) : Intent
    data class Favorite(val id: String) : Intent
    data class Missing(val id: String) : Intent
    data class Theme(val value: String) : Intent
    data class Confirm(val action: String?) : Intent
    data class ReceiveSelected(val orderId: String, val lineIds: Set<String>) : Intent
    data object ExecuteConfirmation : Intent
    data class Stock(val value: StockAction?) : Intent
    data object SaveStock : Intent
    data class Receipt(val value: ReceiptEditor?) : Intent
    data object SaveReceipt : Intent
    data class OrderEdit(val value: OrderEditor?) : Intent
    data object SaveOrder : Intent
    data class OrdersFilter(val value: OrdersUiState) : Intent
    data class PlaceOrder(val id: String) : Intent
    data class History(val itemId: String? = null) : Intent
    data class BuyAgain(val id: String) : Intent
    data class ChangeForm(
        val form: FormType,
        val field: FormField,
        val value: String,
        val lineId: String? = null
    ) : Intent
}

class PilotViewModel(private val repository: PantryRepository, private val clock: DateProvider) :
    ViewModel() {
    private val mutable = MutableStateFlow(PilotUiState(today = clock.today()))
    val state = mutable.asStateFlow()
    private val events = Channel<String>(Channel.BUFFERED)
    val effects = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.records.collect { records ->
                mutable.update {
                    it.copy(
                        records = records
                    )
                }
            }
        }
        load()
        viewModelScope.launch {
            while (isActive) {
                delay(30_000); accept(Intent.RefreshDate)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null) }; try {
            repository.load(); mutable.update { it.copy(loading = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mutable.update {
                it.copy(
                    loading = false,
                    error = e.message ?: "Unable to load local data. Retry."
                )
            }
        }
        }
    }

    private fun operation(onFailure: (String) -> Unit = {}, block: suspend () -> Unit) {
        if (state.value.saving || state.value.loading || state.value.error != null) return
        mutable.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message =
                    e.message ?: "Could not save. Please retry."; onFailure(message); events.send(
                    message
                )
            } finally {
                mutable.update { it.copy(saving = false) }
            }
        }
    }

    fun accept(intent: Intent) {
        when (intent) {
            is Intent.EditRecipe -> mutable.update { it.copy(recipeEditor = intent.value) }
            is Intent.RecipeField -> mutable.update { current ->
                if (current.saving) current else current.copy(
                    recipeEditor = current.recipeEditor?.change(intent.field, intent.value)
                )
            }

            Intent.SaveRecipe -> {
                val form = state.value.recipeEditor ?: return
                if (form.validationErrors().isNotEmpty()) {
                    mutable.update { it.copy(recipeEditor = form.copy(showValidation = true)) }
                    return
                }
                operation(onFailure = { message ->
                    mutable.update {
                        it.copy(
                            recipeEditor = form.copy(
                                error = message
                            )
                        )
                    }
                }) {
                    val recipe = form.toRecipe(form.id ?: repository.id())
                    repository.saveRecipe(recipe)
                    mutable.update {
                        it.copy(
                            recipeEditor = null,
                            tab = "Recipes",
                            recipes = it.recipes.copy(selected = recipe.id)
                        )
                    }
                    events.send("Recipe saved")
                }
            }

            is Intent.ChangeForm -> mutable.update { current ->
                if (current.saving) current else when (intent.form) {
                    FormType.Ingredient -> current.copy(editor = current.editor?.let { form ->
                        when (intent.field) {
                            FormField.Name -> form.copy(
                                name = intent.value,
                                errors = form.errors - "name"
                            )

                            FormField.Quantity -> form.copy(
                                quantity = intent.value,
                                errors = form.errors - "quantity"
                            )

                            FormField.Unit -> form.copy(
                                unit = intent.value,
                                errors = form.errors - "quantity"
                            )

                            FormField.Category -> form.copy(category = Category.valueOf(intent.value))
                            FormField.Date -> form.copy(
                                date = intent.value,
                                errors = form.errors - "date"
                            )

                            else -> form
                        }
                    })

                    FormType.Stock -> current.copy(stockAction = current.stockAction?.let { form ->
                        when (intent.field) {
                            FormField.Quantity -> form.copy(quantity = intent.value, error = null)
                            FormField.Note -> form.copy(note = intent.value, error = null)
                            FormField.Waste -> form.copy(
                                waste = intent.value == "true",
                                error = null
                            )

                            else -> form
                        }
                    })

                    FormType.Receipt -> current.copy(receipt = current.receipt?.let { form ->
                        when (intent.field) {
                            FormField.Quantity -> form.copy(quantity = intent.value, error = null)
                            FormField.Date -> form.copy(date = intent.value, error = null)
                            FormField.Category -> form.copy(
                                category = Category.valueOf(intent.value),
                                error = null
                            )

                            else -> form
                        }
                    })

                    FormType.Order -> current.copy(orderEditor = current.orderEditor?.let { form ->
                        when (intent.field) {
                            FormField.Store -> form.copy(store = intent.value, error = null)
                            FormField.Note -> form.copy(notes = intent.value, error = null)
                            FormField.OrderLineQuantity -> form.copy(
                                quantities = form.quantities + (requireNotNull(
                                    intent.lineId
                                ) to intent.value), error = null
                            )

                            else -> form
                        }
                    })
                }
            }

            Intent.Retry -> load()
            Intent.RefreshDate -> mutable.update { it.copy(today = clock.today()) }
            is Intent.Navigate -> mutable.update {
                it.copy(
                    tab = intent.tab,
                    recipes = it.recipes.copy(selected = null)
                )
            }

            is Intent.PantryFilter -> mutable.update { it.copy(pantry = intent.value) }
            is Intent.RecipeFilter -> mutable.update { it.copy(recipes = intent.value) }
            is Intent.Edit -> mutable.update { it.copy(editor = intent.value) }
            Intent.Save -> {
                val editor = state.value.editor ?: return
                val errors = validation(
                    editor.name,
                    editor.quantity,
                    if (editor.shopping) "" else editor.date
                ).toMutableMap()
                if ("quantity" !in errors && editor.unit == "piece" && editor.quantity.toDoubleOrNull()
                        ?.let { it % 1.0 != 0.0 } == true
                ) errors["quantity"] = "Pieces must be whole numbers."
                if (errors.isNotEmpty()) {
                    mutable.update { it.copy(editor = editor.copy(errors = errors)) }; return
                }
                operation {
                    val r = repository.records.value
                    val id = editor.id ?: repository.id();
                    val name = editor.name.trim();
                    val q = editor.quantity.toDouble()
                    if (editor.restockId != null) {
                        repository.restock(
                            editor.restockId,
                            q,
                            editor.unit,
                            editor.date.ifBlank { null })
                    } else if (editor.shopping) {
                        val old = r.shopping.find { it.id == id }
                        repository.saveShopping(
                            ShoppingItem(
                                id,
                                name,
                                normalize(name),
                                q,
                                editor.unit,
                                old?.isPurchased ?: false,
                                old?.createdAt ?: clock.timestamp()
                            )
                        )
                    } else {
                        val old = r.pantry.find { it.id == id }
                        repository.savePantry(
                            PantryItem(
                                id,
                                name,
                                normalize(name),
                                editor.category,
                                q,
                                editor.unit,
                                editor.date.ifBlank { null },
                                old?.createdAt ?: clock.timestamp(),
                                clock.timestamp()
                            )
                        )
                    }
                    mutable.update { it.copy(editor = null) }; events.send(if (editor.restockId != null) "Stock added to pantry" else "Saved to your device")
                }
            }

            is Intent.Delete -> operation {
                repository.deleteItem(
                    intent.id,
                    intent.shopping
                ); mutable.update { it.copy(settings = SettingsUiState()) }; events.send("Item deleted; stock history retained")
            }

            is Intent.Purchased -> operation { repository.togglePurchased(intent.id) }
            is Intent.Favorite -> operation {
                val added = intent.id !in state.value.records.favorites
                repository.update { it.copy(favorites = if (added) it.favorites + intent.id else it.favorites - intent.id) }
                events.send(if (added) "Added to your favorites" else "Removed from your favorites")
            }

            is Intent.Missing -> operation {
                val count =
                    repository.addMissing(state.value.records.recipes.first { it.id == intent.id }); events.send(
                if (count == 0) "Shopping list already up to date" else "Added $count ingredients to shopping list"
            )
            }

            is Intent.Theme -> operation { repository.update { it.copy(theme = intent.value) } }
            is Intent.Confirm -> mutable.update { it.copy(settings = SettingsUiState(intent.action)) }
            is Intent.Stock -> mutable.update { it.copy(stockAction = intent.value) }
            Intent.SaveStock -> {
                val form = state.value.stockAction ?: return
                operation(onFailure = { message ->
                    mutable.update {
                        it.copy(
                            stockAction = form.copy(
                                error = message
                            )
                        )
                    }
                }) {
                    repository.reduceStock(
                        form.itemId,
                        form.quantity.toDoubleOrNull() ?: Double.NaN,
                        form.waste,
                        form.note
                    )
                    mutable.update { it.copy(stockAction = null) }; events.send(if (form.waste) "Waste recorded; stock reduced" else "Usage recorded; stock reduced")
                }
            }

            is Intent.Receipt -> mutable.update { it.copy(receipt = intent.value) }
            Intent.SaveReceipt -> {
                val form = state.value.receipt ?: return
                operation(onFailure = { message ->
                    mutable.update {
                        it.copy(
                            receipt = form.copy(
                                error = message
                            )
                        )
                    }
                }) {
                    require(
                        validation(
                            "Receipt",
                            form.quantity,
                            form.date
                        ).isEmpty()
                    ) { "Enter a positive quantity and a valid YYYY-MM-DD expiry date, or leave the date blank." }
                    val q = form.quantity.toDouble();
                    val date = form.date.ifBlank { null }
                    if (form.shoppingItemId != null) repository.receiveShopping(
                        form.shoppingItemId,
                        q,
                        date,
                        form.category
                    )
                    else repository.receiveOrderLine(
                        requireNotNull(form.orderId),
                        requireNotNull(form.lineId),
                        q,
                        date,
                        form.category
                    )
                    mutable.update { it.copy(receipt = null) }; events.send("Received into pantry as a new expiry batch")
                }
            }

            is Intent.OrderEdit -> mutable.update { it.copy(orderEditor = intent.value) }
            Intent.SaveOrder -> {
                val form = state.value.orderEditor ?: return
                val invalidLine = form.order?.lines?.firstOrNull { line ->
                    !validStockQuantity(
                        form.quantities[line.id]?.trim()?.toDoubleOrNull() ?: Double.NaN, line.unit
                    )
                }
                val error = when {
                    form.store.length > 100 -> "Use 100 characters or fewer for the supplier."
                    form.notes.length > 500 -> "Use 500 characters or fewer for notes."
                    invalidLine != null -> "Enter a valid positive quantity for ${invalidLine.name}. Pieces must be whole numbers."
                    form.order == null && state.value.records.shopping.none { !it.isPurchased && it.orderId == null } ->
                        "Add an unpurchased shopping item before creating an order."

                    else -> null
                }
                if (error != null) {
                    mutable.update { it.copy(orderEditor = form.copy(error = error)) }
                    return
                }
                operation(onFailure = { message ->
                    mutable.update {
                        it.copy(
                            orderEditor = form.copy(
                                error = message
                            )
                        )
                    }
                }) {
                    val orderId = if (form.order == null) repository.createOrder(
                        form.store,
                        form.notes
                    ) else {
                        val order = form.order.copy(
                            store = form.store,
                            notes = form.notes,
                            lines = form.order.lines.map { line ->
                                line.copy(
                                    quantity = form.quantities[line.id]?.toDoubleOrNull()
                                        ?: Double.NaN
                                )
                            })
                        repository.saveDraft(order); order.id
                    }
                    mutable.update {
                        it.copy(
                            orderEditor = null,
                            tab = "Orders",
                            orders = it.orders.copy(selected = orderId)
                        )
                    }; events.send("Draft order saved")
                }
            }

            is Intent.OrdersFilter -> mutable.update { it.copy(orders = intent.value) }
            is Intent.PlaceOrder -> operation { repository.placeOrder(intent.id); events.send("Order placed; ready for receipts") }
            is Intent.History -> mutable.update {
                it.copy(
                    tab = "History",
                    historyItemId = intent.itemId
                )
            }

            is Intent.BuyAgain -> operation { repository.buyAgain(intent.id); events.send("Added a new purchase to Shopping") }
            is Intent.ReceiveSelected -> operation {
                repository.receiveOutstanding(intent.orderId, intent.lineIds)
                events.send("Selected items received into pantry")
            }

            Intent.ExecuteConfirmation -> operation {
                val action = state.value.settings.confirmation
                when (action) {
                    "reset" -> repository.update { Records() }
                    "clear" -> repository.update { it.copy(shopping = it.shopping.filterNot { item -> item.isPurchased }) }
                    else -> when {
                        action?.startsWith("recipe:") == true -> {
                            repository.deleteRecipe(action.substringAfter(":"))
                            mutable.update { it.copy(recipes = it.recipes.copy(selected = null)) }
                        }

                        action?.startsWith("cancel-order:") == true -> repository.cancelOrder(
                            action.substringAfter(
                                ":"
                            )
                        )

                        action?.startsWith("cook:") == true -> repository.cookRecipe(state.value.records.recipes.first {
                            it.id == action.substringAfter(
                                ":"
                            )
                        })
                    }
                }
                mutable.update { it.copy(settings = SettingsUiState()) }; events.send(
                if (action?.startsWith(
                        "recipe:"
                    ) == true
                ) "Recipe deleted" else "Local data updated"
            )
            }
        }
    }
}

