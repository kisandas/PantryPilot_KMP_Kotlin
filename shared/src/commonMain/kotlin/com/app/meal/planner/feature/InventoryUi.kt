package com.app.meal.planner.feature

import androidx.compose.foundation.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import com.app.meal.planner.core.designsystem.NavigationIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.meal.planner.core.model.*
import com.app.meal.planner.domain.*
import kotlinx.datetime.*
import kotlinx.datetime.Instant

fun stockNumber(value: Double): String = if (value % 1.0 == 0.0) value.toLong()
    .toString() else (kotlin.math.round(value * 1_000_000) / 1_000_000).toString()

fun stockTime(timestamp: Long): String {
    val local =
        Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.date} ${local.hour.toString().padStart(2, '0')}:${
        local.minute.toString().padStart(2, '0')
    }"
}

fun statusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.Draft -> "Draft"
    OrderStatus.Placed -> "Awaiting delivery"
    OrderStatus.PartiallyReceived -> "Partially received"
    OrderStatus.Received -> "Received"
    OrderStatus.Cancelled -> "Cancelled"
}

internal fun quantityKeyboard(unit: String) = KeyboardOptions(
    keyboardType = if (unit == "piece") KeyboardType.Number else KeyboardType.Decimal
)

@Composable
internal fun ItemEditActions(enabled: Boolean, edit: () -> Unit, delete: () -> Unit) {
    Row {
        CompactItemAction("Edit", Color(0xFFDCEEFF), Color(0xFF185A92), enabled, edit)
        CompactItemAction("Delete", Color(0xFFFDE4E4), Color(0xFFA12828), enabled, delete)
    }
}

@Composable
private fun CompactItemAction(
    action: String,
    background: Color,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick, enabled = enabled, modifier = Modifier.size(48.dp).semantics {
        contentDescription = "$action item"
    }) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .background(if (enabled) background else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            NavigationIcon(
                action,
                if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                18.dp
            )
        }
    }
}

@Composable
internal fun ScreenBackButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        NavigationIcon("Back", MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StockControls(item: PantryItem, s: PilotUiState, dispatch: (Intent) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            enabled = !s.saving && item.quantity > 0 && expiry(
                item.expiryDate,
                s.today
            ) != Expiry.Expired,
            onClick = {
                dispatch(
                    Intent.Stock(
                        StockAction(
                            item.id,
                            quantity = stockNumber(minOf(1.0, item.quantity))
                        )
                    )
                )
            }) { Text("Use stock") }
        FilledTonalButton(
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            enabled = !s.saving && item.quantity > 0,
            onClick = {
                dispatch(
                    Intent.Stock(
                        StockAction(
                            item.id,
                            waste = true,
                            quantity = stockNumber(item.quantity)
                        )
                    )
                )
            }) { Text("Record waste") }
        FilledTonalButton(onClick = { dispatch(Intent.History(item.id)) }) { Text("History") }
        FilledTonalButton(
            enabled = !s.saving,
            onClick = {
                dispatch(
                    Intent.Edit(
                        Editor(
                            name = item.name,
                            unit = item.unit,
                            category = item.category,
                            restockId = item.id,
                            restockUnit = item.unit
                        )
                    )
                )
            }) { Text("Restock") }
    }
}

@Composable
fun StockTotals(pantry: List<PantryItem>) {
    val totals =
        pantry.groupBy { it.normalizedName to compatibleStockUnits(it.unit) }.values.map { batches ->
            val displayUnit = when {
                batches.any { it.unit == "kg" } -> "kg"
                batches.any { it.unit == "L" } -> "L"
                else -> batches.first().unit
            }
            Triple(
                batches.first().name,
                displayUnit,
                batches.sumOf { convertStockQuantity(it.quantity, it.unit, displayUnit) })
        }
    if (totals.isNotEmpty()) {
        Text("Totals across batches", style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            totals.sortedBy { it.first }.forEach { (name, unit, quantity) ->
                Card {
                    Text(
                        "$name · ${stockNumber(quantity)} $unit",
                        Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShoppingStockControls(item: ShoppingItem, s: PilotUiState, dispatch: (Intent) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (item.orderId == null) {
            Button(
                enabled = !s.saving,
                onClick = {
                    dispatch(
                        Intent.Receipt(
                            ReceiptEditor(
                                shoppingItemId = item.id,
                                quantity = stockNumber(item.quantity)
                            )
                        )
                    )
                }) { Text("Receive into pantry") }
        } else {
            val order = s.records.orders.find { it.id == item.orderId }
            FilledTonalButton(onClick = {
                dispatch(Intent.Navigate("Orders")); dispatch(
                Intent.OrdersFilter(
                    s.orders.copy(selected = item.orderId)
                )
            )
            }) { Text("${order?.number ?: "Order"} · ${order?.let { statusLabel(it.status) } ?: "View"}") }
        }
        if (item.isPurchased) FilledTonalButton(
            enabled = !s.saving,
            onClick = { dispatch(Intent.BuyAgain(item.id)) }) { Text("Buy again") }
    }
}

@Composable
fun ShoppingInventoryCard(item: ShoppingItem, s: PilotUiState, dispatch: (Intent) -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    item.isPurchased,
                    { dispatch(Intent.Purchased(item.id)) },
                    enabled = !s.saving && item.orderId == null,
                    modifier = Modifier.semantics {
                        contentDescription = "Purchased: ${item.name}"
                    })
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        fontWeight = FontWeight.SemiBold
                    ); Text(
                    "${stockNumber(item.quantity)} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall
                )
                }
                if (item.orderId == null) {
                    ItemEditActions(
                        enabled = !s.saving,
                        edit = {
                            dispatch(
                                Intent.Edit(
                                    Editor(
                                        item.id,
                                        true,
                                        item.name,
                                        stockNumber(item.quantity),
                                        item.unit
                                    )
                                )
                            )
                        },
                        delete = { dispatch(Intent.Confirm("shopping:${item.id}")) }
                    )
                }
            }
            ShoppingStockControls(item, s, dispatch)
        }
    }
}

fun LazyListScope.ordersContent(
    s: PilotUiState,
    checked: Set<String>,
    toggle: (String) -> Unit,
    dispatch: (Intent) -> Unit
) {
    val selected = s.records.orders.find { it.id == s.orders.selected }
    if (selected == null) {
        item {
            Text(
                "Household purchases",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ); Text("Plan a purchase, receive each delivery, and track where your stock came from.")
        }
        item {
            Button(
                enabled = !s.saving && s.records.shopping.any { !it.isPurchased && it.orderId == null },
                onClick = { dispatch(Intent.OrderEdit(OrderEditor())) }) { Text("Create order from Shopping") }
        }
        item {
            Text(
                "Includes all unpurchased shopping items not already linked to an order. Add or edit Shopping items first.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            InventoryChoices(
                listOf("All", "Open", "Received", "Cancelled"),
                s.orders.filter
            ) { dispatch(Intent.OrdersFilter(s.orders.copy(filter = it))) }
        }
        val orders = s.records.orders.filter {
            when (s.orders.filter) {
                "Open" -> it.status in listOf(
                    OrderStatus.Draft,
                    OrderStatus.Placed,
                    OrderStatus.PartiallyReceived
                ); "Received" -> it.status == OrderStatus.Received; "Cancelled" -> it.status == OrderStatus.Cancelled; else -> true
            }
        }.sortedByDescending { it.createdAt }
        if (orders.isEmpty()) item {
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp)
                ) {
                    Text(
                        "No purchase orders here yet",
                        style = MaterialTheme.typography.titleMedium
                    ); Text(
                    "Add the things you need to Shopping, then create a draft order."
                ); FilledTonalButton({ dispatch(Intent.Navigate("Shopping")) }) { Text("Open Shopping") }
                }
            }
        }
        items(orders, key = { it.id }) { order ->
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${order.number} · ${order.store.ifBlank { "Household purchase" }}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "${statusLabel(order.status)} · ${order.lines.size} lines · ${
                            stockTime(
                                order.createdAt
                            )
                        }"
                    )
                    Text(order.lines.joinToString {
                        "${it.name} ${stockNumber(it.receivedQuantity)}/${
                            stockNumber(
                                it.quantity
                            )
                        } ${it.unit}"
                    }, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton({ dispatch(Intent.OrdersFilter(s.orders.copy(selected = order.id))) }) {
                        Text(
                            "View order"
                        )
                    }
                }
            }
        }
    } else {
        item {
            ScreenBackButton("Back to orders") {
                dispatch(Intent.OrdersFilter(s.orders.copy(selected = null)))
            }
        }
        val canReceive =
            selected.status in listOf(OrderStatus.Placed, OrderStatus.PartiallyReceived)
        val selectedLines =
            selected.lines.filter { it.id in checked && it.quantity - it.receivedQuantity > STOCK_EPSILON }
        if (canReceive) item {
            var confirm by remember(selected.id) { mutableStateOf(false) }
            Button({ confirm = true }, enabled = selectedLines.isNotEmpty() && !s.saving) {
                Text("Receive selected (${selectedLines.size})")
            }
            if (confirm) AlertDialog(
                onDismissRequest = { confirm = false },
                title = { Text("Receive selected items?") },
                text = { Text("Record the outstanding quantities for ${selectedLines.size} selected items without expiry dates. Use the individual receive button to set a date or quantity.") },
                confirmButton = {
                    Button(
                        {
                            confirm = false
                            dispatch(
                                Intent.ReceiveSelected(
                                    selected.id,
                                    selectedLines.map { it.id }.toSet()
                                )
                            )
                        },
                        enabled = selectedLines.isNotEmpty() && !s.saving
                    ) { Text("Receive delivery") }
                },
                dismissButton = { OutlinedButton({ confirm = false }) { Text("Cancel") } }
            )
        }
        item {
            Text(
                "${selected.number} · ${selected.store.ifBlank { "Household purchase" }}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ); Text(statusLabel(selected.status)); Text(
            "Created ${stockTime(selected.createdAt)}",
            style = MaterialTheme.typography.bodySmall
        ); if (selected.notes.isNotBlank()) Text(selected.notes)
        }
        if (selected.status == OrderStatus.Draft) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !s.saving,
                    onClick = {
                        dispatch(
                            Intent.OrderEdit(
                                OrderEditor(
                                    selected,
                                    selected.store,
                                    selected.notes,
                                    selected.lines.associate { it.id to stockNumber(it.quantity) })
                            )
                        )
                    }) { Text("Edit draft") }
                Button(
                    enabled = !s.saving,
                    onClick = { dispatch(Intent.PlaceOrder(selected.id)) }) { Text("Place order") }
            }
            Text(
                "Placing an order does not add pantry stock. Receive items when they arrive.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (selected.status in listOf(
                OrderStatus.Draft,
                OrderStatus.Placed,
                OrderStatus.PartiallyReceived
            )
        ) item {
            OutlinedButton(
                enabled = !s.saving,
                onClick = { dispatch(Intent.Confirm("cancel-order:${selected.id}")) }) { Text("Cancel outstanding purchase") }
        }
        items(selected.lines, key = { it.id }) { line ->
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canReceive && line.quantity - line.receivedQuantity > STOCK_EPSILON) {
                            Checkbox(
                                line.id in checked, { toggle(line.id) }, enabled = !s.saving,
                                modifier = Modifier.semantics {
                                    contentDescription = "Select ${line.name} for delivery"
                                })
                        }
                        Text(line.name, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Ordered ${stockNumber(line.quantity)} ${line.unit} · Received ${
                            stockNumber(
                                line.receivedQuantity
                            )
                        } ${line.unit}"
                    )
                    val remaining = (line.quantity - line.receivedQuantity).coerceAtLeast(0.0)
                    Text(
                        if (remaining <= STOCK_EPSILON) "Fully received" else "${if (selected.status == OrderStatus.Cancelled) "Cancelled" else "Outstanding"}: ${
                            stockNumber(
                                remaining
                            )
                        } ${line.unit}"
                    )
                    if (remaining > STOCK_EPSILON && selected.status in listOf(
                            OrderStatus.Placed,
                            OrderStatus.PartiallyReceived
                        )
                    ) Button(
                        enabled = !s.saving,
                        onClick = {
                            dispatch(
                                Intent.Receipt(
                                    ReceiptEditor(
                                        selected.id,
                                        line.id,
                                        quantity = stockNumber(remaining),
                                        category = line.category
                                    )
                                )
                            )
                        }) { Text("Receive delivery") }
                }
            }
        }
        val receipts = s.records.movements.filter { it.orderId == selected.id }.asReversed()
        if (receipts.isNotEmpty()) item {
            Text(
                "Receipt history",
                style = MaterialTheme.typography.titleLarge
            ); Text("Each receipt is a separate pantry batch with its own expiry date.")
        }
        items(receipts, key = { it.id }) { MovementCard(it) }
    }
}

fun LazyListScope.historyContent(s: PilotUiState, dispatch: (Intent) -> Unit) {
    item {
        ScreenBackButton("Back to pantry") { dispatch(Intent.Navigate("Pantry")) }; Text(
        "Stock history",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    ); Text("See what was received, used, wasted, or corrected. History survives removing a batch.")
    }
    if (s.historyItemId != null) item { FilledTonalButton({ dispatch(Intent.History()) }) { Text("Show all stock movements") } }
    val movements =
        s.records.movements.filter { s.historyItemId == null || it.pantryItemId == s.historyItemId }
            .asReversed().sortedByDescending { it.occurredAt }
    if (movements.isEmpty()) item { Text("No movements yet. Add stock, receive a purchase, or record usage.") }
    items(movements, key = { it.id }) { MovementCard(it) }
}

@Composable
private fun MovementCard(movement: StockMovement) {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("${movement.name} · ${movement.type}", fontWeight = FontWeight.Bold)
            Text(
                "${if (movement.delta > 0) "+" else ""}${stockNumber(movement.delta)} ${movement.unit} · Batch balance after: ${
                    stockNumber(
                        movement.balanceAfter
                    )
                } ${movement.unit}"
            )
            Text("${stockTime(movement.occurredAt)} · ${movement.expiryDate?.let { "Expiry $it" } ?: "No expiry date"}",
                style = MaterialTheme.typography.bodySmall)
            if (movement.note.isNotBlank()) Text(
                movement.note,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun InventoryDialogs(s: PilotUiState, dispatch: (Intent) -> Unit) {
    s.stockAction?.let { form ->
        val item = s.records.pantry.find { it.id == form.itemId }
        InventoryDialog(
            if (form.waste) "Record food waste" else "Record stock usage",
            s.saving,
            form.error,
            { dispatch(Intent.Stock(null)) },
            { dispatch(Intent.SaveStock) }) {
            Text("${item?.name ?: "Batch removed"} · ${item?.let { "${stockNumber(it.quantity)} ${it.unit} remaining" } ?: ""}")
            InventoryChoices(
                listOf("Used", "Waste"),
                if (form.waste) "Waste" else "Used"
            ) {
                if (!s.saving) dispatch(
                    Intent.ChangeForm(
                        FormType.Stock,
                        FormField.Waste,
                        (it == "Waste").toString()
                    )
                )
            }
            FormTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.quantity,
                onValueChange = {
                    dispatch(
                        Intent.ChangeForm(
                            FormType.Stock,
                            FormField.Quantity,
                            it
                        )
                    )
                },
                enabled = !s.saving,
                label = { Text("Quantity to subtract (${item?.unit ?: ""})") },
                singleLine = true
            )
            FormTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.note,
                onValueChange = { dispatch(Intent.ChangeForm(FormType.Stock, FormField.Note, it)) },
                enabled = !s.saving,
                label = { Text("Reason / meal (optional)") })
            Text(
                "Saving reduces this batch immediately. It cannot make the balance negative. Expired food can be recorded as waste.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    s.receipt?.let { form ->
        val line =
            s.records.orders.find { it.id == form.orderId }?.lines?.find { it.id == form.lineId }
        val shopping = s.records.shopping.find { it.id == form.shoppingItemId }
        InventoryDialog(
            "Receive into pantry",
            s.saving,
            form.error,
            { dispatch(Intent.Receipt(null)) },
            { dispatch(Intent.SaveReceipt) }) {
            Text("${line?.name ?: shopping?.name} · ${line?.unit ?: shopping?.unit}")
            Text(
                "A new batch will be added. Older stock and expiry dates stay separate.",
                style = MaterialTheme.typography.bodySmall
            )
            FormTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.quantity,
                onValueChange = {
                    dispatch(
                        Intent.ChangeForm(
                            FormType.Receipt,
                            FormField.Quantity,
                            it
                        )
                    )
                },
                enabled = !s.saving,
                label = { Text("Quantity received now") },
                singleLine = true
            )
            ExpiryDateField(
                form.date,
                enabled = !s.saving
            ) { dispatch(Intent.ChangeForm(FormType.Receipt, FormField.Date, it)) }
            Text("Category")
            InventoryChoices(
                Category.entries.map { it.name },
                form.category.name
            ) {
                if (!s.saving) dispatch(
                    Intent.ChangeForm(
                        FormType.Receipt,
                        FormField.Category,
                        it
                    )
                )
            }
            Text(
                "Partial delivery? Enter only the quantity received today. The rest stays outstanding.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    s.orderEditor?.let { form ->
        InventoryDialog(
            if (form.order == null) "New household purchase" else "Edit ${form.order.number}",
            s.saving,
            form.error,
            { dispatch(Intent.OrderEdit(null)) },
            { dispatch(Intent.SaveOrder) }) {
            FormTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.store,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                onValueChange = {
                    dispatch(
                        Intent.ChangeForm(
                            FormType.Order,
                            FormField.Store,
                            it
                        )
                    )
                },
                enabled = !s.saving,
                label = { Text("Store / supplier (optional)") },
                singleLine = true
            )
            FormTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.notes,
                onValueChange = { dispatch(Intent.ChangeForm(FormType.Order, FormField.Note, it)) },
                enabled = !s.saving,
                label = { Text("Notes (optional)") })
            if (form.order == null) {
                Text("These unpurchased shopping items will be linked to the draft:")
                s.records.shopping.filter { !it.isPurchased && it.orderId == null }
                    .forEach { Text("• ${it.name} · ${stockNumber(it.quantity)} ${it.unit}") }
            } else form.order.lines.forEach { line ->
                FormTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = form.quantities[line.id] ?: "",
                    keyboardOptions = quantityKeyboard(line.unit),
                    onValueChange = {
                        dispatch(
                            Intent.ChangeForm(
                                FormType.Order,
                                FormField.OrderLineQuantity,
                                it,
                                line.id
                            )
                        )
                    },
                    enabled = !s.saving,
                    label = { Text("${line.name} (${line.unit})") },
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun InventoryDialog(
    title: String,
    saving: Boolean,
    error: String?,
    cancel: () -> Unit,
    save: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!saving) cancel() },
        title = { Text(title) },
        text = {
            FormKeyboardScope {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) { content(); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            Button(
                save,
                enabled = !saving
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { OutlinedButton(cancel, enabled = !saving) { Text("Cancel") } })
}

@Composable
private fun InventoryChoices(options: List<String>, selected: String, select: (String) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) { options.forEach { FilterChip(it == selected, { select(it) }, label = { Text(it) }) } }
}



