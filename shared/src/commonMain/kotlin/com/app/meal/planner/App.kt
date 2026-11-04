package com.app.meal.planner

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.meal.planner.core.database.*
import com.app.meal.planner.core.di.pilotModule
import com.app.meal.planner.core.model.*
import com.app.meal.planner.data.repository.*
import com.app.meal.planner.domain.*
import com.app.meal.planner.feature.*
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.getKoin
import com.app.meal.planner.core.designsystem.RecipeArtwork
import com.app.meal.planner.core.designsystem.NavigationIcon

@Composable fun App(databaseFactory: () -> LocalDatabase = { createDatabase() }) {
    val local = remember { databaseFactory() }
    KoinApplication(application = { modules(pilotModule(local)) }) {

        val koin = getKoin()
        val vm = viewModel { koin.get<PilotViewModel>() }
        PilotApp(vm)
    }
}
@Composable private fun PilotApp(vm: PilotViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    PlatformBackHandler(s.editor != null || s.stockAction!=null || s.receipt!=null || s.orderEditor!=null || s.settings.confirmation != null || s.recipes.selected != null || s.tab != "Home") {
        when {
            s.stockAction!=null -> if(!s.saving) vm.accept(Intent.Stock(null))
            s.receipt!=null -> if(!s.saving) vm.accept(Intent.Receipt(null))
            s.orderEditor!=null -> if(!s.saving) vm.accept(Intent.OrderEdit(null))
            s.editor != null -> if(!s.saving) vm.accept(Intent.Edit(null))
            s.settings.confirmation != null -> if(!s.saving) vm.accept(Intent.Confirm(null))
            s.recipes.selected != null -> vm.accept(Intent.RecipeFilter(s.recipes.copy(selected=null)))
            s.tab=="Orders" && s.orders.selected!=null -> vm.accept(Intent.OrdersFilter(s.orders.copy(selected=null)))
            else -> vm.accept(Intent.Navigate("Home"))
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.accept(Intent.RefreshDate) }
    LaunchedEffect(vm,lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.effects.collect { snackbar.showSnackbar(it) } } }
    val dark = s.records.theme == "Dark" || (s.records.theme=="System" && isSystemInDarkTheme())
    val colors = if(dark) darkColorScheme(primary=Color(0xFFA8D5A2),secondary=Color(0xFFFFBA83)) else lightColorScheme(primary=Color(0xFF315C3A),onPrimary=Color.White,primaryContainer=Color(0xFFE3EBD9),background=Color(0xFFFAF8F0),surface=Color(0xFFFAF8F0),surfaceVariant=Color(0xFFECEEE1),surfaceContainer=Color(0xFFF1F0E6),surfaceContainerLow=Color(0xFFF5F3EA),surfaceContainerHigh=Color(0xFFEFEDE2),surfaceContainerHighest=Color(0xFFECEBDF),onPrimaryContainer=Color(0xFF233E29),secondary=Color(0xFF99511F))
    MaterialTheme(colorScheme=colors) {
        BoxWithConstraints(Modifier.fillMaxSize().background(colors.background).safeContentPadding()) {
            val wide = maxWidth >= 850.dp
            val recipeColumns = when { maxWidth >= 1200.dp -> 3; maxWidth >= 600.dp -> 2; else -> 1 }
            Scaffold(snackbarHost={ SnackbarHost(snackbar) },bottomBar={ if(!wide) NavigationBar { listOf("Home","Pantry","Recipes","Shopping","Orders").forEach { tab -> NavigationBarItem(selected=s.tab==tab,onClick={vm.accept(Intent.Navigate(tab))},icon={NavigationIcon(tab)},label={Text(tab)}) } } }) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if(wide) Column(Modifier.width(200.dp).fillMaxHeight().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("PantryPilot",fontSize=24.sp,fontWeight=FontWeight.Bold,color=colors.primary)
                        Text("A little less waste.\nA lot more possibility.",style=MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(24.dp))
                        listOf("Home","Pantry","Recipes","Shopping","Orders","History","Settings").forEach { tab -> FilledTonalButton({vm.accept(Intent.Navigate(tab))},Modifier.fillMaxWidth(),colors=ButtonDefaults.filledTonalButtonColors(containerColor=if(s.tab==tab) colors.primaryContainer else colors.surface)) { NavigationIcon(tab); Spacer(Modifier.width(10.dp)); Text(tab) } }
                        Spacer(Modifier.weight(1f)); Text("LOCAL BY DESIGN\nYour kitchen, your device.",style=MaterialTheme.typography.labelSmall)
                    }
                    Box(Modifier.weight(1f),contentAlignment=Alignment.TopCenter) {
                    Column(Modifier.widthIn(max=1100.dp).fillMaxSize().padding(horizontal=if(wide) 28.dp else 16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(if(wide) s.tab else "PantryPilot",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                            TextButton({vm.accept(Intent.Navigate(if(s.tab=="Settings") "Home" else "Settings"))}) { Text(if(s.tab=="Settings") "Home" else "Settings") }
                        }
                        if(s.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                        when {
                            s.loading -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
                            s.error != null -> Empty("Could not open your pantry",s.error ?: "", "Retry") { vm.accept(Intent.Retry) }
                            else -> LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)) {
                                when(s.tab) {
                                    "Home" -> {
                                        item { Hero("Good food starts here.","Keep track of what you have. Make something delicious.") }
                                        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Stat("Stock batches",s.records.pantry.count {it.quantity>0},Modifier.weight(1f)); Stat("Expiring soon",s.records.pantry.count { it.quantity>0 && expiry(it.expiryDate,s.today)==Expiry.Soon },Modifier.weight(1f)); Stat("To pick up",s.records.shopping.count { !it.isPurchased },Modifier.weight(1f)) } }
                                        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Button({vm.accept(Intent.Edit(Editor()))}) { Text("+ Ingredient") }; OutlinedButton({vm.accept(Intent.Navigate("Shopping"))}) { Text("Shopping list") } } }
                                        if(s.records.pantry.isEmpty()) item { Empty("A fresh start for your kitchen","Add an ingredient to start tracking your stock.","Add ingredient") { vm.accept(Intent.Edit(Editor())) } }
                                        item { Heading("Use these first", "A little care today means less waste tomorrow.") }
                                        val soon = s.records.pantry.filter { it.quantity>0 && expiry(it.expiryDate,s.today)==Expiry.Soon }.sortedBy { it.expiryDate }
                                        if(soon.isEmpty()) item { Text("Nothing expiring in the next three days.") }
                                        items(soon.take(4),key={"soon-${it.id}"}) { item -> PantryRow(item,s,{vm.accept(Intent.Edit(it))},{vm.accept(Intent.Confirm("pantry:${item.id}"))},vm::accept) }
                                        item { Heading("Made for your pantry","Recipes ranked by what needs using first.") }
                                        items(rank(bundledRecipes,s.records.pantry,s.today).take(3)) { recipe -> RecipeCard(recipe,s,{ vm.accept(Intent.Navigate("Recipes")); vm.accept(Intent.RecipeFilter(s.recipes.copy(selected=recipe.id))) },{ vm.accept(Intent.Favorite(recipe.id)) }) }
                                    }
                                    "Pantry" -> {
                                        item { Heading("Your pantry","Know what you have. Use what you love.") }
                                        item { StockTotals(s.records.pantry) }
                                        item { TextButton({vm.accept(Intent.History())}) {Text("View stock history")};Text("Each row is a stock batch. Use stock to subtract what you ate; record waste for discarded food.",style=MaterialTheme.typography.bodySmall) }
                                        item { OutlinedTextField(s.pantry.search,{vm.accept(Intent.PantryFilter(s.pantry.copy(search=it)))},label={Text("Search ingredients")},modifier=Modifier.fillMaxWidth(),singleLine=true) }
                                        item { ChoiceRow(listOf("All")+Category.entries.map { it.name },s.pantry.category?.name ?: "All") { vm.accept(Intent.PantryFilter(s.pantry.copy(category=Category.entries.find { c -> c.name==it }))) } }
                                        item { ChoiceRow(listOf("All","Expiring soon","Expired"),s.pantry.expiry) { vm.accept(Intent.PantryFilter(s.pantry.copy(expiry=it))) } }
                                        item { Row(verticalAlignment=Alignment.CenterVertically) { Button({vm.accept(Intent.Edit(Editor()))}) { Text("+ Add ingredient") }; TextButton({vm.accept(Intent.PantryFilter(s.pantry.copy(sortByName=!s.pantry.sortByName)))}) { Text(if(s.pantry.sortByName) "Sort: Name" else "Sort: Expiry") } } }
                                        val filtered = s.records.pantry.filter { it.name.contains(s.pantry.search,true) && (s.pantry.category==null || it.category==s.pantry.category) && when(s.pantry.expiry) { "Expired" -> it.quantity>0 && expiry(it.expiryDate,s.today)==Expiry.Expired; "Expiring soon" -> it.quantity>0 && expiry(it.expiryDate,s.today)==Expiry.Soon; else -> true } }.let { if(s.pantry.sortByName) it.sortedBy { p -> p.name.lowercase() } else it.sortedWith(compareBy<PantryItem> { p -> p.expiryDate ?: "9999" }.thenBy { p -> p.name }) }
                                        if(filtered.isEmpty()) item { Empty("No ingredients to show","Add an ingredient or adjust your filters.","Add ingredient") {vm.accept(Intent.Edit(Editor()))} }
                                        items(filtered,key={it.id}) { item -> PantryRow(item,s,{vm.accept(Intent.Edit(it))},{vm.accept(Intent.Confirm("pantry:${item.id}"))},vm::accept) }
                                    }
                                    "Recipes" -> {
                                        val selected = bundledRecipes.find { it.id==s.recipes.selected }
                                        if(selected!=null) {
                                            item { TextButton({vm.accept(Intent.RecipeFilter(s.recipes.copy(selected=null)))}) {Text("‹ All recipes")} }
                                            item { Hero(selected.title,"${selected.preparationMinutes} min · ${selected.servings} servings · ${selected.category}") }
                                            item { RecipeArtwork(selected.title,selected.id.toInt(),height=240.dp); Text(selected.description); FavoriteButton(selected,s) { vm.accept(Intent.Favorite(selected.id)) } }
                                            item { Heading("Ingredients","Stock checks quantities in matching units. Expired batches are excluded.") }
                                            items(selected.ingredients) { ingredient -> val held=s.records.pantry.filter {it.normalizedName==ingredient.normalizedName && it.unit==ingredient.unit && it.quantity>0 && expiry(it.expiryDate,s.today)!=Expiry.Expired}.sumOf {it.quantity}; val has=held+STOCK_EPSILON>=ingredient.quantity; Card { Row(Modifier.fillMaxWidth().padding(16.dp)) { Text("${ingredient.displayName} · ${number(ingredient.quantity)} ${ingredient.unit}",Modifier.weight(1f)); Text("${if(has) "Enough" else "+ Short"} · ${stockNumber(held)} ${ingredient.unit} in stock",color=if(has) colors.primary else colors.secondary) } } }
                                            item { if(recipeShortages(selected,s.records.pantry,s.today).isEmpty()) Text("All required quantities are in stock.") else Button({vm.accept(Intent.Missing(selected.id))},enabled=!s.saving) { Text("Add stock shortfalls to shopping") } }
                                            item {
                                                val plan=runCatching {recipeDeductions(selected,s.records.pantry,s.today)}
                                                Button(enabled=!s.saving && plan.isSuccess,onClick={vm.accept(Intent.Confirm("cook:${selected.id}"))}) {Text("Cook & deduct stock")}
                                                Text("This action checks exact quantities and units for ${selected.servings} servings, then uses the earliest-expiring valid batches first.",style=MaterialTheme.typography.bodySmall)
                                                if(plan.isFailure) Text(plan.exceptionOrNull()?.message ?: "Insufficient stock",style=MaterialTheme.typography.bodySmall,color=colors.secondary)
                                            }
                                            item { Heading("Let's make it","Follow these steps, one at a time.") }
                                            items(selected.instructions.withIndex().toList()) { step -> Card { Row(Modifier.padding(18.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) { Text("${step.index+1}",fontWeight=FontWeight.Bold,color=colors.primary); Text(step.value) } } }
                                        } else {
                                            item { Heading("A little kitchen inspiration","12 everyday recipes, ready whenever you are.") }
                                            item { OutlinedTextField(s.recipes.search,{vm.accept(Intent.RecipeFilter(s.recipes.copy(search=it)))},label={Text("Search recipes or ingredients")},modifier=Modifier.fillMaxWidth(),singleLine=true) }
                                            item { ChoiceRow(listOf("All","Can cook","Favorites"),s.recipes.filter) { vm.accept(Intent.RecipeFilter(s.recipes.copy(filter=it))) } }
                                            item { Text("Can cook checks unexpired quantities in matching units. Other cards show ingredient names present.",style=MaterialTheme.typography.bodySmall) }
                                            val recipes=rank(bundledRecipes,s.records.pantry,s.today).filter { (it.title.contains(s.recipes.search,true) || it.ingredients.any { i -> i.displayName.contains(s.recipes.search,true) }) && when(s.recipes.filter) { "Can cook" -> recipeShortages(it,s.records.pantry,s.today).isEmpty(); "Favorites" -> it.id in s.records.favorites; else -> true } }
                                            if(recipes.isEmpty()) item { Text("No recipes match. Try another filter or add pantry ingredients.") }
                                            items(recipes.chunked(recipeColumns)) { row -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { row.forEach { recipe -> Box(Modifier.weight(1f)) { RecipeCard(recipe,s,{vm.accept(Intent.RecipeFilter(s.recipes.copy(selected=recipe.id)))},{vm.accept(Intent.Favorite(recipe.id))}) } }; repeat(recipeColumns-row.size) { Spacer(Modifier.weight(1f)) } } }
                                        }
                                    }
                                    "Shopping" -> {
                                        item { Heading("A thoughtful shopping list","${s.records.shopping.count { !it.isPurchased }} items left to pick up") }
                                        item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { Button({vm.accept(Intent.Edit(Editor(shopping=true)))}) { Text("+ Add item") }; OutlinedButton(enabled=!s.saving && s.records.shopping.any {!it.isPurchased && it.orderId==null},onClick={vm.accept(Intent.OrderEdit(OrderEditor()))}) {Text("Create order")}; if(s.records.shopping.any { it.isPurchased }) TextButton({vm.accept(Intent.Confirm("clear"))}) { Text("Clear purchased") } } }
                                        if(s.records.shopping.isEmpty()) item { Empty("Everything's picked up","Add items here, or send missing ingredients from a recipe.","Add item") {vm.accept(Intent.Edit(Editor(shopping=true)))} }
                                        listOf(false,true).forEach { purchased ->
                                            val rows=s.records.shopping.filter { it.isPurchased==purchased }
                                            if(rows.isNotEmpty()) item { Text(if(purchased) "PURCHASED" else "TO PICK UP",style=MaterialTheme.typography.labelLarge) }
                                            items(rows,key={it.id}) { item -> ShoppingInventoryCard(item,s,vm::accept) }
                                        }
                                        item { Text("A checkmark alone does not change stock. Receive into pantry records a purchase and adds its batch; linked orders track partial receipts.",style=MaterialTheme.typography.bodySmall) }
                                    }
                                    "Orders" -> ordersContent(s,vm::accept)
                                    "History" -> historyContent(s,vm::accept)
                                    "Settings" -> {
                                        item { Hero("Your kitchen, your way.","PantryPilot · Version 1.0.0") }
                                        item { Heading("Appearance","Choose what feels right."); ChoiceRow(listOf("System","Light","Dark"),s.records.theme) {vm.accept(Intent.Theme(it))} }
                                        item { Heading("Local data","Everything is stored on this device. No account or cross-device synchronization.") }
                                        item { OutlinedButton({vm.accept(Intent.Confirm("reset"))}) {Text("Reset all local data")} }
                                        item { Text("Made for less food waste and easier everyday cooking. Recipes are bundled; no API keys or server needed. Clearing this site's browser data removes its database.") }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
            InventoryDialogs(s,vm::accept)
            s.editor?.let { editor -> EditorDialog(editor,s.saving,{field,value -> vm.accept(Intent.ChangeForm(FormType.Ingredient,field,value))},{vm.accept(Intent.Save)},{vm.accept(Intent.Edit(null))}) }
            s.settings.confirmation?.let { action ->
                val recipe=bundledRecipes.find {"cook:${it.id}"==action}
                AlertDialog(onDismissRequest={if(!s.saving) vm.accept(Intent.Confirm(null))},title={Text(when {recipe!=null -> "Cook & deduct stock?";action.startsWith("cancel-order:") -> "Cancel outstanding purchase?";action=="reset" -> "Reset all local data?";action=="clear" -> "Clear purchased items?";else -> "Delete this item?"})},text={Text(when {recipe!=null -> "${recipe.title} · ${recipe.servings} servings\n\n"+recipe.ingredients.joinToString("\n") {"${it.displayName}: ${stockNumber(it.quantity)} ${it.unit}"}+"\n\nThese quantities will be subtracted from unexpired batches and recorded in stock history. Viewing the recipe never uses stock.";action.startsWith("cancel-order:") -> "Received stock stays in your pantry. Only outstanding quantities return to Shopping. This order will be closed.";action.startsWith("pantry:") -> "The batch will be removed and its remaining quantity recorded as removed stock. Its history is kept.";else -> "This change cannot be undone."})},confirmButton={TextButton(enabled=!s.saving,onClick={when {action.startsWith("pantry:") -> vm.accept(Intent.Delete(action.substringAfter(":"),false));action.startsWith("shopping:") -> vm.accept(Intent.Delete(action.substringAfter(":"),true));else -> vm.accept(Intent.ExecuteConfirmation)}}) {Text("Confirm")}},dismissButton={TextButton(enabled=!s.saving,onClick={vm.accept(Intent.Confirm(null))}) {Text("Cancel")}})
            }
        }
    }
}
private fun number(q: Double) = if(q % 1.0==0.0) q.toLong().toString() else q.toString()
@Composable private fun Heading(title: String, subtitle: String) { Column(verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun Hero(title: String,subtitle: String) { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(24.dp)) { Column(Modifier.fillMaxWidth().padding(26.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) { Text("FRESH IDEAS, LESS WASTE",style=MaterialTheme.typography.labelSmall); Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold); Text(subtitle) } } }
@Composable private fun Stat(label: String,value: Int,modifier: Modifier) { Card(modifier) { Column(Modifier.padding(14.dp)) {Text(value.toString(),style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary);Text(label,style=MaterialTheme.typography.labelMedium)} } }
@Composable private fun Empty(title: String,description: String,action: String,onClick: ()->Unit) { Card { Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Text(title,style=MaterialTheme.typography.titleLarge); Text(description); Button(onClick) {Text(action)} } } }
@Composable private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String)->Unit) { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {options.forEach { FilterChip(selected==it,{onSelect(it)},label={Text(it)}) }} }
@Composable private fun PantryRow(item: PantryItem,s: PilotUiState,edit: (Editor)->Unit,delete: ()->Unit,dispatch: (Intent)->Unit) {
    Card { Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {Text(item.name,fontWeight=FontWeight.Bold);Text("${stockNumber(item.quantity)} ${item.unit} · ${item.category}",style=MaterialTheme.typography.bodySmall)}
            TextButton(enabled=!s.saving,onClick={edit(Editor(item.id,false,item.name,number(item.quantity),item.unit,item.category,item.expiryDate ?: ""))}){Text("Edit")}
            TextButton(enabled=!s.saving,onClick=delete){Text("Delete")}
        }
        val status=expiry(item.expiryDate,s.today)
        Text(if(item.quantity<=0) "Out of stock · batch retained for history" else when(status){Expiry.Expired -> "Expired · ${item.expiryDate}";Expiry.Soon -> "Use soon · ${item.expiryDate}";Expiry.Fresh -> "Fresh · ${item.expiryDate}";Expiry.Undated -> "No expiry date"},color=if(status==Expiry.Soon || status==Expiry.Expired) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium)
        StockControls(item,s,dispatch)
    } }
}
@Composable private fun FavoriteButton(recipe: Recipe,s: PilotUiState,click: ()->Unit) { TextButton(click,enabled=!s.saving) {Text(if(recipe.id in s.records.favorites) "Saved" else "Favorite")} }
@Composable private fun RecipeCard(recipe: Recipe,s: PilotUiState,open: ()->Unit,favorite: ()->Unit) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            RecipeArtwork(recipe.title,recipe.id.toInt())
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(recipe.category.uppercase(),Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                FavoriteButton(recipe,s,favorite)
            }
            Text(recipe.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,minLines=2)
            Text("${recipe.preparationMinutes} min · ${recipe.servings} servings",style=MaterialTheme.typography.bodySmall)
            Text("${recipe.ingredients.size-missing(recipe,s.records.pantry,s.today).size}/${recipe.ingredients.size} ingredient names in your pantry",style=MaterialTheme.typography.bodySmall)
            TextButton(open,Modifier.fillMaxWidth()) {Text("View recipe")}
        }
    }
}
@Composable private fun EditorDialog(e: Editor,saving: Boolean,change: (FormField,String)->Unit,save: ()->Unit,cancel: ()->Unit) {
    AlertDialog(onDismissRequest={if(!saving) cancel()},title={Text("${if(e.id==null) "Add" else "Edit"} ${if(e.shopping) "shopping item" else "ingredient"}")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(e.name,{change(FormField.Name,it)},label={Text("Name")},singleLine=true,enabled=!saving,isError="name" in e.errors,supportingText={e.errors["name"]?.let {Text(it)}})
        OutlinedTextField(e.quantity,{change(FormField.Quantity,it)},label={Text("Quantity")},singleLine=true,enabled=!saving,isError="quantity" in e.errors,supportingText={e.errors["quantity"]?.let {Text(it)}})
        Text("Unit"); if(e.id!=null && !e.shopping) Text("${e.unit} · fixed for this stock batch") else ChoiceRow(units,e.unit) { if(!saving) change(FormField.Unit,it) }
        if(!e.shopping && e.id!=null) Text("Editing quantity records a stock count correction. Use stock records consumption; a new purchase should be received as a new batch.",style=MaterialTheme.typography.bodySmall)
        if(!e.shopping) { Text("Category");ChoiceRow(Category.entries.map {it.name},e.category.name) {if(!saving) change(FormField.Category,it)};OutlinedTextField(e.date,{change(FormField.Date,it)},label={Text("Expiry YYYY-MM-DD (optional)")},singleLine=true,enabled=!saving,isError="date" in e.errors,supportingText={Text(e.errors["date"] ?: "Past dates are allowed and shown as expired.")}) }
    }},confirmButton={Button(save,enabled=!saving){Text(if(saving) "Saving…" else "Save")}},dismissButton={TextButton(cancel,enabled=!saving){Text("Cancel")}})
}














