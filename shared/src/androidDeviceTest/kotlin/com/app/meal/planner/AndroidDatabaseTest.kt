package com.app.meal.planner

import androidx.test.platform.app.InstrumentationRegistry
import com.app.meal.planner.core.database.AndroidDatabase
import com.app.meal.planner.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class AndroidDatabaseTest {
    @Test fun stockUsageAndOrderReceiptsSurviveReopen() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        verifyStockOrderPersistence {AndroidDatabase(context)}
    }
    @Test fun sqliteCrudSurvivesReopen() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=AndroidDatabase(context);val original=db.read()
        try {
            val p=PantryItem("integration","Tomato","tomato",Category.Vegetables,2.0,"piece",null,1,1)
            val item=ShoppingItem("integration","Rice","rice",100.0,"g",false,1)
            db.write(Records(listOf(p),listOf(item),setOf("01"),"Dark"));db.close()
            val reopened=AndroidDatabase(context)
            try {
                val loaded=reopened.read();assertEquals("Tomato",loaded.pantry.single().name);assertEquals("Dark",loaded.theme);assertTrue("01" in loaded.favorites)
                reopened.write(loaded.copy(pantry=listOf(p.copy(quantity=4.0)),shopping=listOf(item.copy(isPurchased=true))));reopened.close()
                val next=AndroidDatabase(context)
                try { assertEquals(4.0,next.read().pantry.single().quantity,0.0);assertTrue(next.read().shopping.single().isPurchased);next.write(Records());assertTrue(next.read().pantry.isEmpty());assertTrue(next.read().shopping.isEmpty()) }
                finally { next.write(original);next.close() }
            } finally { reopened.close() }
        } finally { db.close() }
    }
}
