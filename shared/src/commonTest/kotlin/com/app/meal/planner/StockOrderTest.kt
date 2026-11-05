package com.app.meal.planner

import androidx.lifecycle.viewModelScope
import com.app.meal.planner.core.database.*
import com.app.meal.planner.core.model.*
import com.app.meal.planner.data.repository.*
import com.app.meal.planner.domain.*
import com.app.meal.planner.feature.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class StockOrderTest {
    private val clock=StockClock()
    private fun egg(id: String="old",q: Double=2.0,date: String?="2026-11-05") = PantryItem(id,"Egg","egg",Category.Protein,q,"piece",date,1,1)
    private fun shopping(q: Double=6.0,unit: String="piece") = ShoppingItem("shopping","Eggs","egg",q,unit,createdAt=1)
    private suspend fun fixture(records: Records=Records()): Pair<PantryRepository,StockDatabase> {
        val db=StockDatabase(records.copy(initialDataSeeded=true)); val repo=PantryRepository(db,clock);repo.load();return repo to db
    }
    @Test fun upgradePreservesBalancesAndCreatesOpeningHistoryOnlyOnce() = runTest {
        val (repo,db)=fixture(Records(pantry=listOf(egg())))
        assertEquals(2.0,repo.records.value.pantry.single().quantity)
        assertEquals(1,repo.records.value.movements.size)
        val reopened=PantryRepository(db,clock);reopened.load();assertEquals(repo.records.value,reopened.records.value)
        reopened.update {Records()};reopened.savePantry(egg());reopened.load()
        assertEquals(1,reopened.records.value.movements.size)
    }
    @Test fun restockAddsConvertedSugarBatchesWithoutChangingOriginalExpiry() = runTest {
        val sugar = egg().copy(name = "Sugar", normalizedName = "sugar", quantity = 10.5, unit = "kg")
        val (repo, db) = fixture(Records(pantry = listOf(sugar)))
        repo.reduceStock(sugar.id, 0.1, false, "")
        repo.restock(sugar.id, 100.0, "g", null)
        assertEquals(10.5, repo.records.value.pantry.sumOf { it.quantity }, 0.000001)
        repo.restock(sugar.id, 0.1, "kg", null)
        assertEquals(10.6, db.read().pantry.sumOf { it.quantity }, 0.000001)
        assertEquals(sugar.expiryDate, db.read().pantry.first { it.id == sugar.id }.expiryDate)
        assertTrue(db.read().shopping.isEmpty())
        assertFailsWith<IllegalArgumentException> { repo.restock(sugar.id, 1.0, "L", null) }
        assertEquals(10.6, db.read().pantry.sumOf { it.quantity }, 0.000001)
    }

    @Test fun usingTwoEggsDepletesThatBatchAndRemovesItFromRecipeAvailability() = runTest {
        val (repo,_)=fixture(Records(pantry=listOf(egg())))
        repo.reduceStock("old",2.0,false,"Breakfast")
        assertEquals(0.0,repo.records.value.pantry.single().quantity)
        assertTrue(available(repo.records.value.pantry,clock.today()).isEmpty())
        assertEquals(MovementType.Used,repo.records.value.movements.last().type)
        assertEquals(-2.0,repo.records.value.movements.last().delta)
        assertEquals("Breakfast",repo.records.value.movements.last().note)
    }
    @Test fun invalidUsageCannotMutateStock() = runTest {
        val (repo,_)=fixture(Records(pantry=listOf(egg())))
        val before=repo.records.value
        for(q in listOf(3.0,-1.0,0.0,Double.NaN,Double.POSITIVE_INFINITY,0.5)) assertFailsWith<IllegalArgumentException> {repo.reduceStock("old",q,false,"")}
        assertEquals(before,repo.records.value)
    }
    @Test fun expiredStockCanBeWastedButNotUsed() = runTest {
        val (repo,_)=fixture(Records(pantry=listOf(egg(date="2026-11-03"))))
        assertFailsWith<IllegalArgumentException> {repo.reduceStock("old",1.0,false,"")}
        repo.reduceStock("old",2.0,true,"Expired")
        assertEquals(0.0,repo.records.value.pantry.single().quantity)
        assertEquals(MovementType.Waste,repo.records.value.movements.last().type)
    }
    @Test fun receiptAddsNewBatchWithoutChangingOlderExpiryAndCannotReceiveTwice() = runTest {
        val (repo,db)=fixture(Records(pantry=listOf(egg()),shopping=listOf(shopping())))
        val orderId=repo.createOrder("Local store","Weekly shop");repo.placeOrder(orderId)
        val line=repo.records.value.orders.single().lines.single()
        repo.receiveOrderLine(orderId,line.id,6.0,"2026-11-20",Category.Protein)
        assertEquals(8.0,repo.records.value.pantry.sumOf {it.quantity})
        assertEquals("2026-11-05",repo.records.value.pantry.first {it.id=="old"}.expiryDate)
        assertEquals(OrderStatus.Received,repo.records.value.orders.single().status)
        assertTrue(repo.records.value.shopping.single().isPurchased)
        val saved=repo.records.value
        assertFailsWith<IllegalArgumentException>{repo.receiveOrderLine(orderId,line.id,6.0,"2026-11-20",Category.Protein)}
        assertEquals(saved,repo.records.value)
        val reopened=PantryRepository(db,clock);reopened.load();assertEquals(saved,reopened.records.value)
    }
    @Test fun partialReceiptsPreserveSeparateDatesAndCompleteOnlyAtFullQuantity() = runTest {
        val (repo,_)=fixture(Records(shopping=listOf(shopping())))
        val id=repo.createOrder("Store","");repo.placeOrder(id);val line=repo.records.value.orders.single().lines.single()
        repo.receiveOrderLine(id,line.id,2.0,"2026-11-09",Category.Protein)
        assertEquals(OrderStatus.PartiallyReceived,repo.records.value.orders.single().status)
        assertFalse(repo.records.value.shopping.single().isPurchased)
        val before=repo.records.value
        assertFailsWith<IllegalArgumentException>{repo.receiveOrderLine(id,line.id,5.0,null,Category.Protein)}
        assertEquals(before,repo.records.value)
        repo.receiveOrderLine(id,line.id,4.0,"2026-11-20",Category.Protein)
        assertEquals(2,repo.records.value.pantry.size);assertEquals(6.0,repo.records.value.pantry.sumOf {it.quantity})
        assertEquals(OrderStatus.Received,repo.records.value.orders.single().status)
    }
    @Test fun bulkReceiptCommitsSelectedOutstandingLinesTogether() = runTest {
        val eggs = shopping()
        val rice = eggs.copy(id = "rice", name = "Rice", normalizedName = "rice", quantity = 500.0, unit = "g")
        val milk = eggs.copy(id = "milk", name = "Milk", normalizedName = "milk", quantity = 1.0, unit = "L")
        val (repo, db) = fixture(Records(shopping = listOf(eggs, rice, milk)))
        val id = repo.createOrder("Store", "")
        repo.placeOrder(id)
        val lines = repo.records.value.orders.single().lines.associateBy { it.name }
        val eggLine = lines.values.first { it.shoppingItemId == eggs.id }
        val riceLine = lines.values.first { it.shoppingItemId == rice.id }
        repo.receiveOrderLine(id, eggLine.id, 2.0, null, Category.Protein)

        val selected = setOf(eggLine.id, riceLine.id)
        val before = repo.records.value
        db.fail = true
        assertFailsWith<IllegalStateException> { repo.receiveOutstanding(id, selected) }
        assertEquals(before, repo.records.value)

        db.fail = false
        repo.receiveOutstanding(id, selected)
        val saved = repo.records.value
        assertEquals(OrderStatus.PartiallyReceived, saved.orders.single().status)
        assertEquals(6.0, saved.pantry.filter { it.normalizedName == "egg" }.sumOf { it.quantity })
        assertEquals(500.0, saved.pantry.single { it.normalizedName == "rice" }.quantity)
        assertFalse(saved.shopping.single { it.id == milk.id }.isPurchased)
        assertFailsWith<IllegalArgumentException> { repo.receiveOutstanding(id, selected) }
        assertEquals(saved, repo.records.value)
    }

    @Test fun cancellationReturnsOnlyOutstandingQuantitiesToShopping() = runTest {
        val (repo,_)=fixture(Records(shopping=listOf(shopping())))
        val id=repo.createOrder("Store","");repo.placeOrder(id)
        repo.receiveOrderLine(id,repo.records.value.orders.single().lines.single().id,2.0,null,Category.Protein)
        repo.cancelOrder(id)
        assertEquals(2.0,repo.records.value.pantry.single().quantity)
        assertEquals(4.0,repo.records.value.shopping.single().quantity)
        assertNull(repo.records.value.shopping.single().orderId)
        assertEquals(OrderStatus.Cancelled,repo.records.value.orders.single().status)
        val next=repo.createOrder("Other store","")
        assertEquals(4.0,repo.records.value.orders.first {it.id==next}.lines.single().quantity)
    }
    @Test fun draftsCanBeEditedButPlacedOrdersAreLockedAndShoppingCannotBeOrderedTwice() = runTest {
        val (repo,_)=fixture(Records(shopping=listOf(shopping())))
        val id=repo.createOrder("Store","");val original=repo.records.value.orders.single()
        assertFailsWith<IllegalArgumentException>{repo.createOrder("Duplicate","")}
        val edited=original.copy(lines=original.lines.map {it.copy(quantity=3.0)})
        repo.saveDraft(edited);assertEquals(3.0,repo.records.value.shopping.single().quantity)
        repo.placeOrder(id)
        assertFailsWith<IllegalArgumentException>{repo.saveDraft(edited)}
        assertFailsWith<IllegalArgumentException>{repo.togglePurchased("shopping")}
        assertFailsWith<IllegalArgumentException>{repo.deleteItem("shopping",true)}
    }
    @Test fun failedReceiptDoesNotPublishStockOrderOrHistoryChanges() = runTest {
        val (repo,db)=fixture(Records(shopping=listOf(shopping())))
        val id=repo.createOrder("Store","");repo.placeOrder(id)
        val before=repo.records.value;db.fail=true
        assertFailsWith<IllegalStateException>{repo.receiveOrderLine(id,before.orders.single().lines.single().id,2.0,null,Category.Protein)}
        assertEquals(before,repo.records.value)
    }
    @Test fun quickReceiptRecordsAnOrderAndBuyAgainCreatesAnUnlinkedShoppingItem() = runTest {
        val (repo,_)=fixture(Records(shopping=listOf(shopping().copy(isPurchased=true))))
        repo.receiveShopping("shopping",6.0,null,Category.Protein)
        assertEquals(OrderStatus.Received,repo.records.value.orders.single().status)
        repo.buyAgain("shopping")
        val next=repo.records.value.shopping.first {it.id!="shopping"}
        assertNull(next.orderId);assertFalse(next.isPurchased)
    }
    private fun recipe(ingredients: List<RecipeIngredient>) = Recipe("test","Egg breakfast","","Breakfast",10,1,ingredients,listOf("Cook"),"")
    @Test fun cookingConsumesEarliestExpiryFirstAndCannotPartiallyDeductOnShortage() = runTest {
        val (repo,_)=fixture(Records(pantry=listOf(egg("fresh",3.0,"2026-11-20"),egg("soon",1.0,"2026-11-05"),egg("expired",4.0,"2026-11-01"))))
        val recipe=recipe(listOf(RecipeIngredient("Egg",2.0,"piece")))
        repo.cookRecipe(recipe)
        assertEquals(0.0,repo.records.value.pantry.first {it.id=="soon"}.quantity)
        assertEquals(2.0,repo.records.value.pantry.first {it.id=="fresh"}.quantity)
        assertEquals(4.0,repo.records.value.pantry.first {it.id=="expired"}.quantity)
        val before=repo.records.value
        assertFailsWith<IllegalArgumentException>{repo.cookRecipe(recipe.copy(ingredients=recipe.ingredients+RecipeIngredient("Milk",100.0,"ml")))}
        assertEquals(before,repo.records.value)
    }
    @Test fun shortfallShoppingUsesMissingQuantityAndDoesNotConvertUnits() = runTest {
        val (repo,_)=fixture(Records(pantry=listOf(egg(q=1.0))))
        val recipe=recipe(listOf(RecipeIngredient("Egg",3.0,"piece"),RecipeIngredient("Egg",20.0,"g")))
        repo.addMissing(recipe)
        assertEquals(2.0,repo.records.value.shopping.first {it.unit=="piece"}.quantity)
        assertEquals(20.0,repo.records.value.shopping.first {it.unit=="g"}.quantity)
        assertEquals(0,repo.addMissing(recipe))
    }
    @Test fun correctionsAndRemovalAreAuditedAndHistorySurvivesDeletion() = runTest {
        val (repo,_)=fixture(Records(pantry=listOf(egg())))
        repo.savePantry(egg(q=4.0));assertEquals(MovementType.Adjustment,repo.records.value.movements.last().type)
        assertFailsWith<IllegalArgumentException>{repo.savePantry(egg(q=4.0).copy(unit="g"))}
        repo.deleteItem("old",false)
        assertTrue(repo.records.value.pantry.isEmpty())
        assertEquals(0.0,repo.records.value.movements.sumOf {it.delta})
        assertEquals(MovementType.Removed,repo.records.value.movements.last().type)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun rapidQuantityAndUnitEditsPreserveTheFullSavedValue() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (repo,_)=fixture()
        val model=PilotViewModel(repo,clock)
        try {
            runCurrent()
            model.accept(Intent.Edit(Editor(shopping=true)))
            model.accept(Intent.ChangeForm(FormType.Ingredient,FormField.Name,"Coffee"))
            model.accept(Intent.ChangeForm(FormType.Ingredient,FormField.Quantity,"2"))
            model.accept(Intent.ChangeForm(FormType.Ingredient,FormField.Quantity,"20"))
            model.accept(Intent.ChangeForm(FormType.Ingredient,FormField.Quantity,"200"))
            model.accept(Intent.ChangeForm(FormType.Ingredient,FormField.Unit,"g"))
            model.accept(Intent.Save);runCurrent()
            assertEquals("Coffee",repo.records.value.shopping.single().name)
            assertEquals(200.0,repo.records.value.shopping.single().quantity)
            assertEquals("g",repo.records.value.shopping.single().unit)
        } finally {model.viewModelScope.cancel();Dispatchers.resetMain()}
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun receiptFieldEditsKeepQuantityDateAndCategoryTogether() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (repo,_)=fixture(Records(shopping=listOf(shopping())))
        val orderId=repo.createOrder("Store","");repo.placeOrder(orderId)
        val lineId=repo.records.value.orders.single().lines.single().id
        val model=PilotViewModel(repo,clock)
        try {
            runCurrent()
            model.accept(Intent.Receipt(ReceiptEditor(orderId,lineId,quantity="6")))
            model.accept(Intent.ChangeForm(FormType.Receipt,FormField.Quantity,"2"))
            model.accept(Intent.ChangeForm(FormType.Receipt,FormField.Date,"2026-11-20"))
            model.accept(Intent.ChangeForm(FormType.Receipt,FormField.Category,"Protein"))
            model.accept(Intent.SaveReceipt);runCurrent()
            val batch=repo.records.value.pantry.single()
            assertEquals(2.0,batch.quantity)
            assertEquals("2026-11-20",batch.expiryDate)
            assertEquals(Category.Protein,batch.category)
            assertEquals(OrderStatus.PartiallyReceived,repo.records.value.orders.single().status)
        } finally {model.viewModelScope.cancel();Dispatchers.resetMain()}
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun mviInvalidUsageRetainsFormAndSuccessfulUsagePublishesReducedBalance() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (repo,_)=fixture(Records(pantry=listOf(egg())))
        val model=PilotViewModel(repo,clock)
        try {
            runCurrent();model.accept(Intent.Stock(StockAction("old",quantity="3")));model.accept(Intent.SaveStock);runCurrent()
            assertNotNull(model.state.value.stockAction?.error);assertEquals(2.0,model.state.value.records.pantry.single().quantity)
            model.accept(Intent.Stock(StockAction("old",quantity="2")));model.accept(Intent.SaveStock);runCurrent()
            assertNull(model.state.value.stockAction);assertEquals(0.0,model.state.value.records.pantry.single().quantity)
        } finally {model.viewModelScope.cancel();Dispatchers.resetMain()}
    }
}
private class StockClock: DateProvider {override fun today()=LocalDate.parse("2026-11-04");override fun timestamp()=100L}
private class StockDatabase(records: Records): LocalDatabase {
    private var rows=encodeRows(records)
    var fail=false
    override suspend fun read()=decodeRows(rows)
    override suspend fun write(records: Records) {check(!fail){"Simulated disk failure"};rows=encodeRows(records)}
}

