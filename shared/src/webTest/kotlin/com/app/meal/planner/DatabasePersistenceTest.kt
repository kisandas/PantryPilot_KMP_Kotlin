package com.app.meal.planner

import com.app.meal.planner.core.database.createDatabase
import com.app.meal.planner.core.model.*
import com.app.meal.planner.data.repository.PantryRepository
import com.app.meal.planner.domain.SystemDateProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class DatabasePersistenceTest {
    @Test fun stockUsageAndOrderReceiptsSurviveReopen() = runTest {verifyStockOrderPersistence {createDatabase()}}
    @Test fun databaseSurvivesCloseReopenAndCrud() = runTest {
        val db=createDatabase();val original=db.read()
        try {
            val pantry=PantryItem("integration-pantry","Persistence tomato","persistence tomato",Category.Vegetables,2.0,"piece",null,1,1)
            val shopping=ShoppingItem("integration-shop","Persistence rice","persistence rice",100.0,"g",false,1)
            db.write(Records(listOf(pantry),listOf(shopping),setOf("01"),"Dark",recipes=listOf(Recipe("user","My recipe","Personal","Lunch",10,1,listOf(RecipeIngredient("Egg",1.0,"piece")),listOf("Cook thoroughly"),""))));db.close()
            val reopened=createDatabase()
            try {
                assertEquals("Persistence tomato",reopened.read().pantry.single().name)
                assertEquals("Dark",reopened.read().theme)
                assertEquals("My recipe",reopened.read().recipes.single().title)
                assertEquals(setOf("01"),reopened.read().favorites)
                val repository=PantryRepository(reopened,SystemDateProvider());repository.load()
                repository.update { it.copy(pantry=it.pantry.map { p -> p.copy(quantity=3.0) },shopping=it.shopping.map { item -> item.copy(isPurchased=true) }) }
                reopened.close()
                val next=createDatabase()
                try {
                    assertEquals(3.0,next.read().pantry.single().quantity)
                    assertTrue(next.read().shopping.single().isPurchased)
                    next.write(Records()); assertTrue(next.read().pantry.isEmpty());assertTrue(next.read().shopping.isEmpty())
                } finally { next.write(original);next.close() }
            } finally { reopened.close() }
        } finally { db.close() }
    }
}
