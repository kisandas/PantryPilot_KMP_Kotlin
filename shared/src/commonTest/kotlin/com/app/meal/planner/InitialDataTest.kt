package com.app.meal.planner

import com.app.meal.planner.core.database.*
import com.app.meal.planner.core.model.*
import com.app.meal.planner.data.repository.PantryRepository
import com.app.meal.planner.domain.DateProvider
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

class InitialDataTest {
    private val clock=object: DateProvider {
        override fun today()=LocalDate.parse("2026-11-04")
        override fun timestamp()=100L
    }
    @Test fun freshDatabaseStartsEmptyAndPreservesUserEntriesOnReopen() = runTest {
        val db = InitialDatabase()
        val repository = PantryRepository(db, clock)
        repository.load()
        val records = repository.records.value
        assertTrue(records.pantry.isEmpty())
        assertTrue(records.shopping.isEmpty())
        assertTrue(records.orders.isEmpty())
        assertTrue(records.recipes.isEmpty())
        assertTrue(records.movements.isEmpty())
        repository.saveShopping(ShoppingItem("personal", "Coffee", "coffee", 250.0, "g", createdAt = 100))
        val reopened = PantryRepository(db, clock)
        reopened.load()
        assertEquals(repository.records.value, reopened.records.value)
    }
    @Test fun resetAndDepletionDoNotBringBackPredefinedStock() = runTest {
        val db=InitialDatabase();val repository=PantryRepository(db,clock);repository.load()
        repository.update {Records()}
        val reopened=PantryRepository(db,clock);reopened.load()
        assertTrue(reopened.records.value.pantry.isEmpty())
        assertTrue(reopened.records.value.shopping.isEmpty())
        assertTrue(reopened.records.value.movements.isEmpty())
    }
    @Test fun upgradingExistingDataNeverAddsStarterItems() = runTest {
        val old=PantryItem("personal","Egg","egg",Category.Protein,2.0,"piece",null,1,1)
        val db=InitialDatabase(Records(pantry=listOf(old)))
        val repository=PantryRepository(db,clock);repository.load()
        assertEquals(listOf(old),repository.records.value.pantry)
        assertEquals(1,repository.records.value.movements.size)
    }
    @Test fun failedInitializationIsRecoverableAndLeavesStockEmpty() = runTest {
        val db=InitialDatabase();db.fail=true
        val repository=PantryRepository(db,clock)
        assertFailsWith<IllegalStateException> {repository.load()}
        assertTrue(repository.records.value.pantry.isEmpty())
        db.fail=false;repository.load();assertTrue(repository.records.value.pantry.isEmpty())
        repository.load();assertTrue(repository.records.value.movements.isEmpty())
    }
}
private class InitialDatabase(records: Records=Records()): LocalDatabase {
    private var rows=encodeRows(records)
    var fail=false
    override suspend fun read()=decodeRows(rows)
    override suspend fun write(records: Records) {check(!fail){"Storage unavailable"};rows=encodeRows(records)}
}
