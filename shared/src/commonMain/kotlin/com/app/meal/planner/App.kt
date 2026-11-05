package com.app.meal.planner

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import org.jetbrains.compose.resources.painterResource
import pantrypilot.shared.generated.resources.Res
import pantrypilot.shared.generated.resources.pantry_logo
import androidx.compose.foundation.*
import com.app.meal.planner.feature.FormKeyboardScope
import com.app.meal.planner.feature.FormTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
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
import org.koin.compose.KoinIsolatedContext
import org.koin.dsl.koinApplication
import org.koin.compose.koinInject
import org.koin.compose.getKoin
import com.app.meal.planner.core.designsystem.RecipeArtwork
import com.app.meal.planner.core.designsystem.NavigationIcon

@Composable
fun App(databaseFactory: () -> LocalDatabase = { createDatabase() }) {
    val local = remember { databaseFactory() }
    // Initialize before composing children: the older Koin Compose remember-loader
    // can expose an uninitialized context during the first Android composition.
    val context = remember(local) { koinApplication { modules(pilotModule(local)) } }
    DisposableEffect(context) { onDispose { context.close() } }
    KoinIsolatedContext(context) {

        val koin = getKoin()
        val vm = viewModel { koin.get<PilotViewModel>() }
        PilotApp(vm)
    }
}

private data class ScreenDestination(val tab: String, val detailId: String?) {
    val depth: Int
        get() = when {
            tab == "Home" -> 0
            tab == "History" && detailId != null -> 3
            tab == "History" || tab == "Settings" || detailId != null -> 2
            else -> 1
        }
}

@Composable
private fun PilotApp(vm: PilotViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    PlatformBackHandler(s.recipeEditor != null || s.editor != null || s.stockAction != null || s.receipt != null || s.orderEditor != null || s.settings.confirmation != null || s.recipes.selected != null || s.tab != "Home") {
        when {
            s.recipeEditor != null -> if (!s.saving) vm.accept(Intent.EditRecipe(null))
            s.stockAction != null -> if (!s.saving) vm.accept(Intent.Stock(null))
            s.receipt != null -> if (!s.saving) vm.accept(Intent.Receipt(null))
            s.orderEditor != null -> if (!s.saving) vm.accept(Intent.OrderEdit(null))
            s.editor != null -> if (!s.saving) vm.accept(Intent.Edit(null))
            s.settings.confirmation != null -> if (!s.saving) vm.accept(Intent.Confirm(null))
            s.tab == "Recipes" && s.recipes.selected != null -> vm.accept(
                Intent.RecipeFilter(
                    s.recipes.copy(
                        selected = null
                    )
                )
            )

            s.tab == "Orders" && s.orders.selected != null -> vm.accept(
                Intent.OrdersFilter(
                    s.orders.copy(
                        selected = null
                    )
                )
            )

            s.tab == "History" -> vm.accept(Intent.Navigate("Pantry"))
            else -> vm.accept(Intent.Navigate("Home"))
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.accept(Intent.RefreshDate) }
    LaunchedEffect(
        vm,
        lifecycle
    ) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.effects.collect {
                snackbar.showSnackbar(
                    it
                )
            }
        }
    }
    val dark = s.records.theme == "Dark" || (s.records.theme == "System" && isSystemInDarkTheme())
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFFA8D5A2),
        primaryContainer = Color(0xFF294632),
        onPrimaryContainer = Color(0xFFD3EDC9),
        secondary = Color(0xFFFFBA83),
        secondaryContainer = Color(0xFF334639),
        onSecondaryContainer = Color(0xFFDAE8D9)
    ) else lightColorScheme(
        primary = Color(0xFF315C3A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE3EBD9),
        background = Color(0xFFFAF8F0),
        surface = Color(0xFFFAF8F0),
        surfaceVariant = Color(0xFFECEEE1),
        surfaceContainer = Color(0xFFF1F0E6),
        surfaceContainerLow = Color(0xFFF5F3EA),
        surfaceContainerHigh = Color(0xFFEFEDE2),
        surfaceContainerHighest = Color(0xFFECEBDF),
        onPrimaryContainer = Color(0xFF233E29),
        secondary = Color(0xFF99511F),
        secondaryContainer = Color(0xFFE5EDE2),
        onSecondaryContainer = Color(0xFF294632)
    )
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(18.dp))
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
            if (s.loading) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painterResource(Res.drawable.pantry_logo),
                        null,
                        Modifier.size(96.dp).clip(RoundedCornerShape(24.dp))
                    )
                    Text("PantryPilot", style = MaterialTheme.typography.headlineMedium)
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
                return@BoxWithConstraints
            }
            val wide = maxWidth >= 850.dp || (maxWidth >= 650.dp && maxHeight < 500.dp)
            val recipeColumns = if (isWebPlatform && maxWidth >= 720.dp) 3 else 2
            var checkedOrderLines by remember(s.orders.selected) { mutableStateOf(emptySet<String>()) }
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                if (!wide) NavigationBar(
                    containerColor = colors.surfaceContainer,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    listOf("Home", "Pantry", "Recipes", "Shopping", "Orders").forEach { tab ->
                        NavigationBarItem(
                            selected = s.tab == tab,
                            onClick = { vm.accept(Intent.Navigate(tab)) },
                            icon = {
                                NavigationIcon(
                                    tab,
                                    if (s.tab == tab) colors.onPrimaryContainer else colors.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    tab,
                                    fontWeight = if (s.tab == tab) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = colors.primaryContainer,
                                selectedTextColor = colors.primary,
                                unselectedTextColor = colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (wide) Column(
                        Modifier.width(180.dp).fillMaxHeight().verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painterResource(Res.drawable.pantry_logo),
                            null,
                            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Text(
                            "PantryPilot",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        Text(
                            "A little less waste.\nA lot more possibility.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(24.dp))
                        listOf(
                            "Home",
                            "Pantry",
                            "Recipes",
                            "Shopping",
                            "Orders",
                            "History",
                            "Settings"
                        ).forEach { tab ->
                            FilledTonalButton(
                                { vm.accept(Intent.Navigate(tab)) },
                                Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (s.tab == tab) colors.primaryContainer else colors.surfaceContainer)
                            ) { NavigationIcon(tab); Spacer(Modifier.width(10.dp)); Text(tab) }
                        }

                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        Column(
                            Modifier.widthIn(max = 1100.dp).fillMaxSize()
                                .padding(horizontal = if (wide) 28.dp else 16.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!wide) {
                                    Image(
                                        painterResource(Res.drawable.pantry_logo),
                                        null,
                                        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    if (wide) s.tab else "PantryPilot",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (s.tab == "Settings") {
                                    ScreenBackButton("Back to Home") { vm.accept(Intent.Navigate("Home")) }
                                } else {
                                    FilledTonalButton({ vm.accept(Intent.Navigate("Settings")) }) {
                                        Text(
                                            "Settings"
                                        )
                                    }
                                }
                            }
                            if (s.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                            when {
                                s.error != null -> Empty(
                                    "Could not open your pantry",
                                    s.error ?: "",
                                    "Retry"
                                ) { vm.accept(Intent.Retry) }

                                else -> AnimatedContent(
                                    targetState = ScreenDestination(
                                        s.tab, when (s.tab) {
                                            "Recipes" -> s.recipes.selected
                                            "Orders" -> s.orders.selected
                                            "History" -> s.historyItemId
                                            else -> null
                                        }
                                    ),
                                    transitionSpec = {
                                        val direction =
                                            targetState.depth.compareTo(initialState.depth)
                                        if (direction == 0) {
                                            fadeIn(tween(180)) togetherWith fadeOut(tween(160))
                                        } else {
                                            val duration = 240
                                            (slideInHorizontally(tween(duration)) { direction * it } + fadeIn(
                                                tween(duration)
                                            )) togetherWith
                                                    (slideOutHorizontally(tween(duration)) { -direction * it / 3 } + fadeOut(
                                                        tween(duration)
                                                    ))
                                        }
                                    },
                                    label = "Screen transition"
                                ) { destination ->
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(bottom = 12.dp)
                                    ) {
                                        when (destination.tab) {
                                            "Home" -> {
                                                item {
                                                    Hero(
                                                        "Good food starts here.",
                                                        "Keep track of what you have. Make something delicious."
                                                    )
                                                }
                                                item {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                    ) {
                                                        Stat(
                                                            "Stock batches",
                                                            s.records.pantry.count { it.quantity > 0 },
                                                            Modifier.weight(1f)
                                                        ); Stat(
                                                        "Expiring soon",
                                                        s.records.pantry.count {
                                                            it.quantity > 0 && expiry(
                                                                it.expiryDate,
                                                                s.today
                                                            ) == Expiry.Soon
                                                        },
                                                        Modifier.weight(1f)
                                                    ); Stat(
                                                        "To pick up",
                                                        s.records.shopping.count { !it.isPurchased },
                                                        Modifier.weight(1f)
                                                    )
                                                    }
                                                }
                                                item {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                    ) {
                                                        Button({ vm.accept(Intent.Edit(Editor())) }) {
                                                            Text(
                                                                "+ Ingredient"
                                                            )
                                                        }; OutlinedButton({
                                                        vm.accept(
                                                            Intent.Navigate(
                                                                "Shopping"
                                                            )
                                                        )
                                                    }) {
                                                        Text(
                                                            "Shopping list"
                                                        )
                                                    }
                                                    }
                                                }
                                                if (s.records.pantry.isEmpty()) item {
                                                    Empty(
                                                        "A fresh start for your kitchen",
                                                        "Add an ingredient to start tracking your stock.",
                                                        ""
                                                    ) { vm.accept(Intent.Edit(Editor())) }
                                                }
                                                item {
                                                    Heading(
                                                        "Use these first",
                                                        "A little care today means less waste tomorrow."
                                                    )
                                                }
                                                val soon = s.records.pantry.filter {
                                                    it.quantity > 0 && expiry(
                                                        it.expiryDate,
                                                        s.today
                                                    ) == Expiry.Soon
                                                }.sortedBy { it.expiryDate }
                                                if (soon.isEmpty()) item { Text("Nothing expiring in the next three days.") }
                                                items(
                                                    soon.take(4),
                                                    key = { "soon-${it.id}" }) { item ->
                                                    PantryRow(
                                                        item,
                                                        s,
                                                        { vm.accept(Intent.Edit(it)) },
                                                        { vm.accept(Intent.Confirm("pantry:${item.id}")) },
                                                        vm::accept
                                                    )
                                                }
                                                item {
                                                    Heading(
                                                        "Made for your pantry",
                                                        if (s.records.recipes.isEmpty()) "Create your first recipe in Recipes." else "Recipes ranked by what needs using first."
                                                    )
                                                }
                                                items(
                                                    rank(
                                                        s.records.recipes,
                                                        s.records.pantry,
                                                        s.today
                                                    ).take(3)
                                                ) { recipe ->
                                                    RecipeCard(
                                                        recipe,
                                                        s,
                                                        {
                                                            vm.accept(Intent.Navigate("Recipes")); vm.accept(
                                                            Intent.RecipeFilter(
                                                                s.recipes.copy(
                                                                    selected = recipe.id
                                                                )
                                                            )
                                                        )
                                                        },
                                                        { vm.accept(Intent.Favorite(recipe.id)) })
                                                }
                                            }

                                            "Pantry" -> {
                                                item {
                                                    Heading(
                                                        "Your pantry",
                                                        "Know what you have. Use what you love."
                                                    )
                                                }
                                                item { StockTotals(s.records.pantry) }
                                                item {
                                                    FilledTonalButton({ vm.accept(Intent.History()) }) {
                                                        Text(
                                                            "View stock history"
                                                        )
                                                    }; Text(
                                                    "Each row is a stock batch. Use stock to subtract what you ate; record waste for discarded food.",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                }
                                                item {
                                                    FormTextField(
                                                        s.pantry.search,
                                                        {
                                                            vm.accept(
                                                                Intent.PantryFilter(
                                                                    s.pantry.copy(search = it)
                                                                )
                                                            )
                                                        },
                                                        label = { Text("Search ingredients") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true
                                                    )
                                                }
                                                item {
                                                    ChoiceRow(
                                                        listOf("All") + Category.entries.map { it.name },
                                                        s.pantry.category?.name ?: "All"
                                                    ) {
                                                        vm.accept(
                                                            Intent.PantryFilter(
                                                                s.pantry.copy(
                                                                    category = Category.entries.find { c -> c.name == it })
                                                            )
                                                        )
                                                    }
                                                }
                                                item {
                                                    ChoiceRow(
                                                        listOf(
                                                            "All",
                                                            "Expiring soon",
                                                            "Expired"
                                                        ), s.pantry.expiry
                                                    ) {
                                                        vm.accept(
                                                            Intent.PantryFilter(
                                                                s.pantry.copy(
                                                                    expiry = it
                                                                )
                                                            )
                                                        )
                                                    }
                                                }
                                                item {
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                    ) {
                                                        Button({ vm.accept(Intent.Edit(Editor())) }) {
                                                            Text(
                                                                "+ Add ingredient"
                                                            )
                                                        }; FilledTonalButton({
                                                        vm.accept(
                                                            Intent.PantryFilter(
                                                                s.pantry.copy(sortByName = !s.pantry.sortByName)
                                                            )
                                                        )
                                                    }) { Text(if (s.pantry.sortByName) "Sort: Name" else "Sort: Expiry") }
                                                    }
                                                }
                                                val filtered = s.records.pantry.filter {
                                                    it.name.contains(
                                                        s.pantry.search,
                                                        true
                                                    ) && (s.pantry.category == null || it.category == s.pantry.category) && when (s.pantry.expiry) {
                                                        "Expired" -> it.quantity > 0 && expiry(
                                                            it.expiryDate,
                                                            s.today
                                                        ) == Expiry.Expired; "Expiring soon" -> it.quantity > 0 && expiry(
                                                            it.expiryDate,
                                                            s.today
                                                        ) == Expiry.Soon; else -> true
                                                    }
                                                }.let {
                                                    if (s.pantry.sortByName) it.sortedBy { p -> p.name.lowercase() } else it.sortedWith(
                                                        compareBy<PantryItem> { p ->
                                                            p.expiryDate ?: "9999"
                                                        }.thenBy { p -> p.name })
                                                }
                                                if (filtered.isEmpty()) item {
                                                    Empty(
                                                        "No ingredients to show",
                                                        "Add an ingredient or adjust your filters.",
                                                        ""
                                                    ) { vm.accept(Intent.Edit(Editor())) }
                                                }
                                                items(
                                                    filtered,
                                                    key = { it.id }) { item ->
                                                    PantryRow(
                                                        item,
                                                        s,
                                                        { vm.accept(Intent.Edit(it)) },
                                                        { vm.accept(Intent.Confirm("pantry:${item.id}")) },
                                                        vm::accept
                                                    )
                                                }
                                            }

                                            "Recipes" -> {
                                                val selected =
                                                    s.records.recipes.find { it.id == destination.detailId }
                                                if (selected != null) {
                                                    item {
                                                        ScreenBackButton("Back to recipes") {
                                                            vm.accept(
                                                                Intent.RecipeFilter(
                                                                    s.recipes.copy(
                                                                        selected = null
                                                                    )
                                                                )
                                                            )
                                                        }
                                                    }
                                                    item {
                                                        Hero(
                                                            selected.title,
                                                            "${selected.preparationMinutes} min · ${selected.servings} servings · ${selected.category}"
                                                        )
                                                    }
                                                    item {
                                                        Column(
                                                            verticalArrangement = Arrangement.spacedBy(
                                                                12.dp
                                                            )
                                                        ) {
                                                            RecipeArtwork(
                                                                selected.title,
                                                                selected.visual,
                                                                height = 240.dp
                                                            )
                                                            Text(selected.description)
                                                            FlowRow(
                                                                horizontalArrangement = Arrangement.spacedBy(
                                                                    8.dp
                                                                )
                                                            ) {
                                                                FavoriteButton(
                                                                    selected,
                                                                    s
                                                                ) {
                                                                    vm.accept(
                                                                        Intent.Favorite(
                                                                            selected.id
                                                                        )
                                                                    )
                                                                }
                                                                OutlinedButton(
                                                                    {
                                                                        vm.accept(
                                                                            Intent.EditRecipe(
                                                                                RecipeEditor.from(
                                                                                    selected
                                                                                )
                                                                            )
                                                                        )
                                                                    },
                                                                    enabled = !s.saving,
                                                                    modifier = Modifier.height(48.dp)
                                                                ) { Text("Edit recipe") }
                                                                OutlinedButton(
                                                                    {
                                                                        vm.accept(
                                                                            Intent.Confirm(
                                                                                "recipe:${selected.id}"
                                                                            )
                                                                        )
                                                                    },
                                                                    enabled = !s.saving,
                                                                    modifier = Modifier.height(48.dp),
                                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                                        contentColor = colors.error
                                                                    )
                                                                ) { Text("Delete recipe") }
                                                            }
                                                        }
                                                    }
                                                    item {
                                                        Heading(
                                                            "Ingredients",
                                                            "Stock checks quantities in matching units. Expired batches are excluded."
                                                        )
                                                    }
                                                    items(selected.ingredients) { ingredient ->
                                                        val held = s.records.pantry.filter {
                                                            it.normalizedName == ingredient.normalizedName && it.unit == ingredient.unit && it.quantity > 0 && expiry(
                                                                it.expiryDate,
                                                                s.today
                                                            ) != Expiry.Expired
                                                        }.sumOf { it.quantity };
                                                        val has =
                                                            held + STOCK_EPSILON >= ingredient.quantity; Card {
                                                        Row(
                                                            Modifier.fillMaxWidth().padding(16.dp)
                                                        ) {
                                                            Text(
                                                                "${ingredient.displayName} · ${
                                                                    number(
                                                                        ingredient.quantity
                                                                    )
                                                                } ${ingredient.unit}",
                                                                Modifier.weight(1f)
                                                            ); Text(
                                                            "${if (has) "Enough" else "+ Short"} · ${
                                                                stockNumber(
                                                                    held
                                                                )
                                                            } ${ingredient.unit} in stock",
                                                            color = if (has) colors.primary else colors.secondary
                                                        )
                                                        }
                                                    }
                                                    }
                                                    item {
                                                        if (recipeShortages(
                                                                selected,
                                                                s.records.pantry,
                                                                s.today
                                                            ).isEmpty()
                                                        ) Text("All required quantities are in stock.") else Button(
                                                            { vm.accept(Intent.Missing(selected.id)) },
                                                            enabled = !s.saving
                                                        ) { Text("Add stock shortfalls to shopping") }
                                                    }
                                                    item {
                                                        val plan = runCatching {
                                                            recipeDeductions(
                                                                selected,
                                                                s.records.pantry,
                                                                s.today
                                                            )
                                                        }
                                                        Button(
                                                            enabled = !s.saving && plan.isSuccess,
                                                            onClick = { vm.accept(Intent.Confirm("cook:${selected.id}")) }) {
                                                            Text(
                                                                "Cook & deduct stock"
                                                            )
                                                        }
                                                        Text(
                                                            "This action checks exact quantities and units for ${selected.servings} servings, then uses the earliest-expiring valid batches first.",
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                        if (plan.isFailure) Text(
                                                            plan.exceptionOrNull()?.message
                                                                ?: "Insufficient stock",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = colors.secondary
                                                        )
                                                    }
                                                    item {
                                                        Heading(
                                                            "Let's make it",
                                                            "Follow these steps, one at a time."
                                                        )
                                                    }
                                                    items(
                                                        selected.instructions.withIndex().toList()
                                                    ) { step ->
                                                        Card {
                                                            Row(
                                                                Modifier.padding(18.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(
                                                                    16.dp
                                                                )
                                                            ) {
                                                                Text(
                                                                    "${step.index + 1}",
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = colors.primary
                                                                ); Text(step.value)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    item {
                                                        Heading(
                                                            "Your recipes",
                                                            "Create your own collection and cook with what you have."
                                                        ); Button(
                                                        {
                                                            vm.accept(
                                                                Intent.EditRecipe(
                                                                    RecipeEditor()
                                                                )
                                                            )
                                                        },
                                                        modifier = Modifier.padding(top = 12.dp)
                                                    ) { Text("+ Create recipe") }
                                                    }
                                                    item {
                                                        FormTextField(
                                                            s.recipes.search,
                                                            {
                                                                vm.accept(
                                                                    Intent.RecipeFilter(
                                                                        s.recipes.copy(search = it)
                                                                    )
                                                                )
                                                            },
                                                            label = { Text("Search recipes or ingredients") },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )
                                                    }
                                                    item {
                                                        ChoiceRow(
                                                            listOf(
                                                                "All",
                                                                "Can cook",
                                                                "Favorites"
                                                            ), s.recipes.filter
                                                        ) {
                                                            vm.accept(
                                                                Intent.RecipeFilter(
                                                                    s.recipes.copy(
                                                                        filter = it
                                                                    )
                                                                )
                                                            )
                                                        }
                                                    }
                                                    item {
                                                        Text(
                                                            "Can cook checks unexpired quantities in matching units. Other cards show ingredient names present.",
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                    val recipes = rank(
                                                        s.records.recipes,
                                                        s.records.pantry,
                                                        s.today
                                                    ).filter {
                                                        (it.title.contains(
                                                            s.recipes.search,
                                                            true
                                                        ) || it.ingredients.any { i ->
                                                            i.displayName.contains(
                                                                s.recipes.search,
                                                                true
                                                            )
                                                        }) && when (s.recipes.filter) {
                                                            "Can cook" -> recipeShortages(
                                                                it,
                                                                s.records.pantry,
                                                                s.today
                                                            ).isEmpty(); "Favorites" -> it.id in s.records.favorites; else -> true
                                                        }
                                                    }
                                                    if (recipes.isEmpty()) item { Text(if (s.records.recipes.isEmpty()) "No recipes yet. Create a recipe to get started." else "No recipes match. Try another search or filter.") }
                                                    items(recipes.chunked(recipeColumns)) { row ->
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                12.dp
                                                            )
                                                        ) {
                                                            row.forEach { recipe ->
                                                                Box(
                                                                    Modifier.weight(
                                                                        1f
                                                                    )
                                                                ) {
                                                                    RecipeCard(
                                                                        recipe,
                                                                        s,
                                                                        {
                                                                            vm.accept(
                                                                                Intent.RecipeFilter(
                                                                                    s.recipes.copy(
                                                                                        selected = recipe.id
                                                                                    )
                                                                                )
                                                                            )
                                                                        },
                                                                        {
                                                                            vm.accept(
                                                                                Intent.Favorite(
                                                                                    recipe.id
                                                                                )
                                                                            )
                                                                        })
                                                                }
                                                            }; repeat(recipeColumns - row.size) {
                                                            Spacer(
                                                                Modifier.weight(1f)
                                                            )
                                                        }
                                                        }
                                                    }
                                                }
                                            }

                                            "Shopping" -> {
                                                item {
                                                    Heading(
                                                        "A thoughtful shopping list",
                                                        "${s.records.shopping.count { !it.isPurchased }} items left to pick up"
                                                    )
                                                }
                                                item {
                                                    Row(
                                                        Modifier.horizontalScroll(
                                                            rememberScrollState()
                                                        ),
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                    ) {
                                                        Button({
                                                            vm.accept(
                                                                Intent.Edit(
                                                                    Editor(
                                                                        shopping = true
                                                                    )
                                                                )
                                                            )
                                                        }) {
                                                            Text(
                                                                "+ Add item"
                                                            )
                                                        }; OutlinedButton(
                                                        enabled = !s.saving && s.records.shopping.any { !it.isPurchased && it.orderId == null },
                                                        onClick = {
                                                            vm.accept(
                                                                Intent.OrderEdit(OrderEditor())
                                                            )
                                                        }) { Text("Create order") }; if (s.records.shopping.any { it.isPurchased }) OutlinedButton(
                                                        { vm.accept(Intent.Confirm("clear")) }) {
                                                        Text(
                                                            "Clear purchased"
                                                        )
                                                    }
                                                    }
                                                }
                                                if (s.records.shopping.isEmpty()) item {
                                                    Empty(
                                                        "Everything's picked up",
                                                        "Add items here, or send missing ingredients from a recipe.",
                                                        ""
                                                    ) { vm.accept(Intent.Edit(Editor(shopping = true))) }
                                                }
                                                listOf(false, true).forEach { purchased ->
                                                    val rows =
                                                        s.records.shopping.filter { it.isPurchased == purchased }
                                                    if (rows.isNotEmpty()) item {
                                                        Text(
                                                            if (purchased) "PURCHASED" else "TO PICK UP",
                                                            style = MaterialTheme.typography.labelLarge
                                                        )
                                                    }
                                                    items(
                                                        rows,
                                                        key = { it.id }) { item ->
                                                        ShoppingInventoryCard(
                                                            item,
                                                            s,
                                                            vm::accept
                                                        )
                                                    }
                                                }
                                                item {
                                                    Text(
                                                        "A checkmark alone does not change stock. Receive into pantry records a purchase and adds its batch; linked orders track partial receipts.",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }

                                            "Orders" -> ordersContent(
                                                s.copy(orders = s.orders.copy(selected = destination.detailId)),
                                                checkedOrderLines,
                                                { id ->
                                                    checkedOrderLines =
                                                        if (id in checkedOrderLines) checkedOrderLines - id else checkedOrderLines + id
                                                },
                                                vm::accept
                                            )

                                            "History" -> historyContent(
                                                s.copy(historyItemId = destination.detailId),
                                                vm::accept
                                            )

                                            "Settings" -> {
                                                item {
                                                    Hero(
                                                        "Your kitchen, your way.",
                                                        "PantryPilot · Version 1.0.0"
                                                    )
                                                }
                                                item {
                                                    Heading(
                                                        "Appearance",
                                                        "Choose what feels right."
                                                    ); ChoiceRow(
                                                    listOf("System", "Light", "Dark"),
                                                    s.records.theme
                                                ) { vm.accept(Intent.Theme(it)) }
                                                }
                                                item {
                                                    Heading(
                                                        "Local data",
                                                        "Everything is stored on this device. No account or cross-device synchronization."
                                                    )
                                                }
                                                item {
                                                    OutlinedButton({ vm.accept(Intent.Confirm("reset")) }) {
                                                        Text(
                                                            "Reset all local data"
                                                        )
                                                    }
                                                }
                                                item { Text("Made for less food waste and easier everyday cooking. Your recipes are saved locally; no API keys or server needed. Clearing this site's browser data removes its database.") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            s.recipeEditor?.let { RecipeEditorDialog(it, s.saving, vm::accept) }
            InventoryDialogs(s, vm::accept)
            s.editor?.let { editor ->
                EditorDialog(
                    editor,
                    s.saving,
                    { field, value ->
                        vm.accept(
                            Intent.ChangeForm(
                                FormType.Ingredient,
                                field,
                                value
                            )
                        )
                    },
                    { vm.accept(Intent.Save) },
                    { vm.accept(Intent.Edit(null)) })
            }
            s.settings.confirmation?.let { action ->
                val recipe = s.records.recipes.find { "cook:${it.id}" == action }
                AlertDialog(
                    onDismissRequest = { if (!s.saving) vm.accept(Intent.Confirm(null)) },
                    title = {
                        Text(
                            when {
                                recipe != null -> "Cook & deduct stock?"; action.startsWith("recipe:") -> "Delete recipe?"; action.startsWith(
                                "cancel-order:"
                            ) -> "Cancel outstanding purchase?"; action == "reset" -> "Reset all local data?"; action == "clear" -> "Clear purchased items?"; else -> "Delete this item?"
                            }
                        )
                    },
                    text = {
                        Text(
                            when {
                                recipe != null -> "${recipe.title} · ${recipe.servings} servings\n\n" + recipe.ingredients.joinToString(
                                    "\n"
                                ) { "${it.displayName}: ${stockNumber(it.quantity)} ${it.unit}" } + "\n\nThese quantities will be subtracted from unexpired batches and recorded in stock history. Viewing the recipe never uses stock."; action.startsWith(
                                "cancel-order:"
                            ) -> "Received stock stays in your pantry. Only outstanding quantities return to Shopping. This order will be closed."; action.startsWith(
                                "pantry:"
                            ) -> "The batch will be removed and its remaining quantity recorded as removed stock. Its history is kept."; else -> "This change cannot be undone."
                            }
                        )
                    },
                    confirmButton = {
                        Button(enabled = !s.saving, onClick = {
                            when {
                                action.startsWith("pantry:") -> vm.accept(
                                    Intent.Delete(
                                        action.substringAfter(
                                            ":"
                                        ), false
                                    )
                                ); action.startsWith("shopping:") -> vm.accept(
                                Intent.Delete(
                                    action.substringAfter(
                                        ":"
                                    ), true
                                )
                            ); else -> vm.accept(Intent.ExecuteConfirmation)
                            }
                        }) { Text("Confirm") }
                    },
                    dismissButton = {
                        OutlinedButton(
                            enabled = !s.saving,
                            onClick = { vm.accept(Intent.Confirm(null)) }) { Text("Cancel") }
                    })
            }
        }
    }
}

private fun number(q: Double) = if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()
@Composable
private fun Heading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        ); Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    }
}

@Composable
private fun Hero(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("FRESH IDEAS, LESS WASTE", style = MaterialTheme.typography.labelSmall); Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        ); Text(subtitle)
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            ); Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Empty(title: String, description: String, action: String, onClick: () -> Unit) {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge
            ); Text(description); if (action.isNotBlank()) Button(onClick) { Text(action) }
        }
    }
}

@Composable
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { options.forEach { FilterChip(selected == it, { onSelect(it) }, label = { Text(it) }) } }
}

@Composable
private fun PantryRow(
    item: PantryItem,
    s: PilotUiState,
    edit: (Editor) -> Unit,
    delete: () -> Unit,
    dispatch: (Intent) -> Unit
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        fontWeight = FontWeight.Bold
                    ); Text(
                    "${stockNumber(item.quantity)} ${item.unit} · ${item.category}",
                    style = MaterialTheme.typography.bodySmall
                )
                }
                ItemEditActions(
                    enabled = !s.saving,
                    edit = {
                        edit(
                            Editor(
                                item.id,
                                false,
                                item.name,
                                number(item.quantity),
                                item.unit,
                                item.category,
                                item.expiryDate.orEmpty()
                            )
                        )
                    },
                    delete = delete
                )
            }
            val status = expiry(item.expiryDate, s.today)
            Text(
                if (item.quantity <= 0) "Out of stock · batch retained for history" else when (status) {
                    Expiry.Expired -> "Expired · ${item.expiryDate}"; Expiry.Soon -> "Use soon · ${item.expiryDate}"; Expiry.Fresh -> "Fresh · ${item.expiryDate}"; Expiry.Undated -> "No expiry date"
                },
                color = if (status == Expiry.Soon || status == Expiry.Expired) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
            StockControls(item, s, dispatch)
        }
    }
}

@Composable
private fun FavoriteButton(recipe: Recipe, s: PilotUiState, click: () -> Unit) {
    val filled = recipe.id in s.records.favorites
    IconToggleButton(
        checked = filled, onCheckedChange = { click() }, enabled = !s.saving,
        modifier = Modifier.semantics {
            contentDescription = if (filled) "Remove from favorites" else "Add to favorites"
        }) {
        val color =
            if (filled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.size(24.dp)) {
            val path = Path().apply {
                moveTo(size.width * .5f, size.height * .88f)
                cubicTo(
                    -size.width * .28f,
                    size.height * .36f,
                    size.width * .1f,
                    -size.height * .1f,
                    size.width * .5f,
                    size.height * .24f
                )
                cubicTo(
                    size.width * .9f,
                    -size.height * .1f,
                    size.width * 1.28f,
                    size.height * .36f,
                    size.width * .5f,
                    size.height * .88f
                )
                close()
            }
            if (filled) drawPath(path, color) else drawPath(
                path,
                color,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, s: PilotUiState, open: () -> Unit, favorite: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeArtwork(recipe.title, recipe.visual, height = 120.dp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    recipe.category,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                FavoriteButton(recipe, s, favorite)
            }
            Text(
                recipe.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${recipe.preparationMinutes} min · ${recipe.servings} servings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${
                    recipe.ingredients.size - missing(
                        recipe,
                        s.records.pantry,
                        s.today
                    ).size
                } of ${recipe.ingredients.size} ingredients available",
                style = MaterialTheme.typography.bodySmall
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    open,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) { Text("View recipe") }
            }
        }
    }
}

@Composable
private fun EditorDialog(
    e: Editor,
    saving: Boolean,
    change: (FormField, String) -> Unit,
    save: () -> Unit,
    cancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!saving) cancel() },
        title = { Text(if (e.restockId != null) "Restock ${e.name}" else "${if (e.id == null) "Add" else "Edit"} ${if (e.shopping) "shopping item" else "ingredient"}") },
        text = {
            FormKeyboardScope {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (e.restockId == null) FormTextField(
                        e.name,
                        { change(FormField.Name, it) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        label = { Text("Name") },
                        singleLine = true,
                        enabled = !saving,
                        isError = "name" in e.errors,
                        supportingText = { e.errors["name"]?.let { Text(it) } })
                    FormTextField(
                        e.quantity,
                        { change(FormField.Quantity, it) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = quantityKeyboard(e.unit),
                        label = { Text("Quantity") },
                        singleLine = true,
                        enabled = !saving,
                        isError = "quantity" in e.errors,
                        supportingText = { e.errors["quantity"]?.let { Text(it) } })
                    Text("Unit"); if (e.id != null && !e.shopping) Text("${e.unit} · fixed for this stock batch") else ChoiceRow(
                    e.restockUnit?.let(::compatibleStockUnits) ?: units,
                    e.unit
                ) { if (!saving) change(FormField.Unit, it) }
                    if (!e.shopping && e.id != null) Text(
                        "Editing quantity records a stock count correction. Use stock records consumption; a new purchase should be received as a new batch.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!e.shopping) {
                        if (e.restockId == null) {
                            Text("Category"); ChoiceRow(
                                Category.entries.map { it.name },
                                e.category.name
                            ) { if (!saving) change(FormField.Category, it) }
                        }; ExpiryDateField(
                            e.date,
                            enabled = !saving,
                            error = e.errors["date"]
                        ) { change(FormField.Date, it) }
                    }
                }
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














