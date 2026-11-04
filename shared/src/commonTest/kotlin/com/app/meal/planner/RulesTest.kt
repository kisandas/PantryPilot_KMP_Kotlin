package com.app.meal.planner

import com.app.meal.planner.core.model.*
import com.app.meal.planner.domain.*
import com.app.meal.planner.data.repository.*
import com.app.meal.planner.core.database.LocalDatabase
import com.app.meal.planner.feature.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*
import androidx.lifecycle.viewModelScope

class RulesTest {
    private val today = LocalDate.parse("2026-11-04")
    private fun pantry(name: String,date: String?=null) = PantryItem(name,name,normalize(name),Category.Other,1.0,"piece",date,0,0)
    private fun shopping(name: String,unit: String="g",purchased: Boolean=false) = ShoppingItem("$name$unit$purchased",name,normalize(name),10.0,unit,purchased,0)
    @Test fun expiryBoundaries() {
        assertEquals(Expiry.Expired,expiry("2026-11-03",today))
        assertEquals(Expiry.Soon,expiry("2026-11-04",today))
        assertEquals(Expiry.Soon,expiry("2026-11-07",today))
        assertEquals(Expiry.Fresh,expiry("2026-11-08",today))
        assertEquals(Expiry.Undated,expiry(null,today))
    }
    @Test fun normalizationIsExplicit() {
        assertEquals("tomato",normalize("  TOMATOES  "))
        assertEquals("olive oil",normalize(" Olive   Oil "))
        assertEquals("egg",normalize("Eggs"))
        assertEquals("couscous",normalize("couscous"))
    }
    @Test fun expiredIngredientsAreUnavailable() {
        assertEquals(setOf("tomato"),available(listOf(pantry("Egg","2026-11-03"),pantry("Tomato","2026-11-04")),today))
    }
    @Test fun rankingPrioritizesUseSoonAndIsDeterministic() {
        val records=listOf(pantry("Spinach","2026-11-04"))
        val first=rank(bundledRecipes,records,today)
        assertTrue(first.first().ingredients.any { it.normalizedName=="spinach" })
        assertEquals(first.map {it.id},rank(bundledRecipes.reversed(),records,today).map {it.id})
    }
    @Test fun shoppingMergePreservesQuantitiesAndUnits() {
        val original=listOf(shopping("Tomato"))
        val additions=listOf(shopping("Tomatoes").copy(quantity=50.0),shopping("Tomato","piece"))
        val merged=mergeShopping(original,additions)
        assertEquals(2,merged.size); assertEquals(10.0,merged.first().quantity)
        assertEquals(merged,mergeShopping(merged,additions))
        assertEquals(2,mergeShopping(listOf(shopping("Tomato",purchased=true)),listOf(shopping("Tomato"))).size)
    }
    @Test fun validationRejectsInvalidAndAllowsPastDates() {
        assertTrue(validation(" ","NaN","invalid").keys.containsAll(listOf("name","quantity","date")))
        listOf("0","-1","Infinity","nope").forEach { assertTrue("quantity" in validation("Tomato",it,"")) }
        assertTrue(validation("Tomato","1.5","2000-01-01").isEmpty())
    }
    @Test fun recipesAreCompleteAndHaveUniqueIds() {
        assertEquals(12,bundledRecipes.size)
        assertEquals(12,bundledRecipes.map {it.id}.toSet().size)
        assertTrue(bundledRecipes.all {it.ingredients.isNotEmpty() && it.instructions.size>=3 && it.ingredients.all { i -> i.quantity>0 }})
    }
    @Test fun failedWritesDoNotPublishUnsavedState() = runTest {
        val db=FakeDatabase(); val repository=PantryRepository(db,FixedClock())
        repository.load(); db.fail=true
        assertFailsWith<IllegalStateException> { repository.update { it.copy(theme="Dark") } }
        assertEquals("System",repository.records.value.theme)
    }
    @Test fun repositoryMissingInsertionIsIdempotent() = runTest {
        val db=FakeDatabase();val repository=PantryRepository(db,FixedClock()); repository.load()
        assertTrue(repository.addMissing(bundledRecipes.first())>0)
        assertEquals(0,repository.addMissing(bundledRecipes.first()))
        val restored=PantryRepository(db,FixedClock());restored.load()
        assertEquals(repository.records.value,restored.records.value)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun mviValidationAndReadFailureRecovery() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val db=FakeDatabase();db.fail=true
        val model=PilotViewModel(PantryRepository(db,FixedClock()),FixedClock())
        try {
            runCurrent(); assertNotNull(model.state.value.error);assertFalse(model.state.value.loading)
            db.fail=false;model.accept(Intent.Retry);runCurrent();assertNull(model.state.value.error)
            model.accept(Intent.Edit(Editor(name="",quantity="-2")));model.accept(Intent.Save)
            assertTrue(model.state.value.editor!!.errors.keys.containsAll(listOf("name","quantity")))
            model.accept(Intent.Edit(Editor(name="Rice",quantity="100",unit="g")));model.accept(Intent.Save);runCurrent()
            assertNull(model.state.value.editor);assertEquals("Rice",model.state.value.records.pantry.single().name)
            db.fail=true
            model.accept(Intent.Edit(Editor(name="Retained form",quantity="2")));model.accept(Intent.Save);runCurrent()
            assertEquals("Retained form",model.state.value.editor?.name)
            assertFalse(model.state.value.saving)
            assertEquals(1,model.state.value.records.pantry.size)
            model.accept(Intent.PantryFilter(PantryUiState(search="rice")))
            model.accept(Intent.Navigate("Shopping"));model.accept(Intent.Navigate("Pantry"))
            assertEquals("rice",model.state.value.pantry.search)
        } finally { model.viewModelScope.cancel();Dispatchers.resetMain() }
    }
}
private class FixedClock: DateProvider { override fun today()=LocalDate.parse("2026-11-04");override fun timestamp()=1L }
private class FakeDatabase: LocalDatabase {
    var data=Records();var fail=false
    override suspend fun read(): Records { check(!fail){"Disk unavailable"};return data }
    override suspend fun write(records: Records) {check(!fail){"Disk unavailable"}; data=records}
}


