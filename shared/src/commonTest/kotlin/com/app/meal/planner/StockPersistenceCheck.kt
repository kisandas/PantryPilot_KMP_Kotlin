package com.app.meal.planner

import com.app.meal.planner.core.database.LocalDatabase
import com.app.meal.planner.core.model.*
import com.app.meal.planner.data.repository.PantryRepository
import com.app.meal.planner.domain.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

/** Run the same real transaction/reopen workflow on IndexedDB and Android SQLite. */
suspend fun verifyStockOrderPersistence(factory: ()->LocalDatabase) {
    var db=factory()
    val original=db.read()
    val clock=object: DateProvider {override fun today()=LocalDate.parse("2026-11-04");override fun timestamp()=1000L}
    try {
        var repo=PantryRepository(db,clock);repo.load();repo.update {Records()}
        repo.savePantry(PantryItem("stock-integration","Egg","egg",Category.Protein,2.0,"piece","2026-11-05",1,1))
        repo.reduceStock("stock-integration",2.0,false,"Breakfast")
        repo.saveShopping(ShoppingItem("stock-shopping","Eggs","egg",6.0,"piece",createdAt=1))
        val orderId=repo.createOrder("Integration store","Test receipt");repo.placeOrder(orderId)
        val lineId=repo.records.value.orders.single().lines.single().id
        repo.receiveOrderLine(orderId,lineId,2.0,"2026-11-10",Category.Protein)
        db.close();db=factory();repo=PantryRepository(db,clock);repo.load()
        assertEquals(OrderStatus.PartiallyReceived,repo.records.value.orders.single().status)
        assertEquals(3,repo.records.value.movements.size)
        assertEquals(0.0,repo.records.value.pantry.first {it.id=="stock-integration"}.quantity)
        assertEquals(2.0,repo.records.value.pantry.sumOf {it.quantity})
        assertEquals(orderId,repo.records.value.shopping.single().orderId)
        repo.receiveOrderLine(orderId,lineId,4.0,"2026-11-20",Category.Protein)
        repo.update {it.copy(shopping=emptyList())}
        db.close();db=factory();repo=PantryRepository(db,clock);repo.load()
        assertEquals(OrderStatus.Received,repo.records.value.orders.single().status)
        assertEquals(6.0,repo.records.value.pantry.sumOf {it.quantity})
        assertEquals(4,repo.records.value.movements.size)
        assertEquals(setOf("2026-11-05","2026-11-10","2026-11-20"),repo.records.value.pantry.map {it.expiryDate}.toSet())
        assertTrue(repo.records.value.shopping.isEmpty())
    } finally { db.write(original);db.close() }
}
