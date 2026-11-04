package com.app.meal.planner.data.repository

import com.app.meal.planner.core.model.*
import com.app.meal.planner.domain.*
import kotlinx.datetime.*

/** Starting stock is inserted once on a fresh database, never after reset or depletion. */
fun predefinedRecords(clock: DateProvider): Records {
    val now=clock.timestamp()
    fun item(id: String,name: String,category: Category,quantity: Double,unit: String,days: Int?) =
        PantryItem("starter-$id",name,normalize(name),category,quantity,unit,days?.let {clock.today().plus(it,DateTimeUnit.DAY).toString()},now,now)
    return Records(
        pantry=listOf(
            item("tomato","Tomato",Category.Vegetables,4.0,"piece",3),
            item("spinach","Spinach",Category.Vegetables,150.0,"g",1),
            item("egg","Egg",Category.Protein,6.0,"piece",5),
            item("rice","Rice",Category.Grains,500.0,"g",30),
            item("milk","Milk",Category.Dairy,1000.0,"ml",4),
            item("chickpeas","Chickpeas",Category.Protein,400.0,"g",20),
            item("oil","Olive oil",Category.Other,250.0,"ml",null),
            item("yogurt","Yogurt",Category.Dairy,250.0,"g",7),
        ),
        shopping=listOf(
            ShoppingItem("starter-pasta","Pasta","pasta",250.0,"g",createdAt=now),
            ShoppingItem("starter-lemon","Lemon","lemon",2.0,"piece",createdAt=now),
        ),
        initialDataSeeded=true,
    )
}
