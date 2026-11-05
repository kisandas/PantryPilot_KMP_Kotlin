package com.app.meal.planner

import androidx.lifecycle.viewModelScope
import com.app.meal.planner.core.database.*
import com.app.meal.planner.core.model.*
import com.app.meal.planner.data.repository.PantryRepository
import com.app.meal.planner.domain.DateProvider
import com.app.meal.planner.feature.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class UserRecipesTest {
    private val clock=object: DateProvider {override fun today()=LocalDate.parse("2026-11-04");override fun timestamp()=100L}
    private fun recipe()=Recipe("mine","Egg bowl","My lunch","Lunch",10,1,listOf(RecipeIngredient("Egg",2.0,"piece")),listOf("Cook eggs thoroughly."),"1")
    @Test fun freshDatabaseHasNoRecipes()=runTest {
        val db=RecipeDatabase();val repo=PantryRepository(db,clock);repo.load()
        assertTrue(repo.records.value.recipes.isEmpty())
        assertTrue(repo.records.value.pantry.isEmpty())
    }
    @Test fun createUpdateFavoriteAndDeletePersistWithoutChangingStock()=runTest {
        val db=RecipeDatabase();val repo=PantryRepository(db,clock);repo.load()
        val pantry=repo.records.value.pantry
        repo.saveRecipe(recipe());repo.update {it.copy(favorites=setOf("mine"))}
        val reopened=PantryRepository(db,clock);reopened.load()
        assertEquals(recipe(),reopened.records.value.recipes.single())
        reopened.saveRecipe(recipe().copy(title="Updated bowl",servings=2))
        assertEquals("Updated bowl",db.read().recipes.single().title)
        assertEquals(setOf("mine"),db.read().favorites)
        reopened.deleteRecipe("mine")
        assertTrue(db.read().recipes.isEmpty());assertTrue(db.read().favorites.isEmpty())
        assertEquals(pantry,db.read().pantry)
    }
    @Test fun invalidRecipeAndFailedSaveDoNotPublishPartialChanges()=runTest {
        val db=RecipeDatabase();val repo=PantryRepository(db,clock);repo.load()
        for(invalid in listOf(recipe().copy(title=""),recipe().copy(servings=0),recipe().copy(instructions=emptyList()),recipe().copy(ingredients=listOf(RecipeIngredient("Egg",0.5,"piece"))))) {
            assertFailsWith<IllegalArgumentException> {repo.saveRecipe(invalid)}
        }
        val before=repo.records.value;db.fail=true
        assertFailsWith<IllegalStateException> {repo.saveRecipe(recipe())}
        assertEquals(before,repo.records.value)
    }
    @Test fun createdRecipeSupportsShoppingShortfallsAndCooking()=runTest {
        val db=RecipeDatabase();val repo=PantryRepository(db,clock);repo.load();repo.saveRecipe(recipe())
        repo.savePantry(PantryItem("eggs", "Egg", "egg", Category.Protein, 6.0, "piece", null, 1, 1))
        val eggs=repo.records.value.pantry.single {it.normalizedName=="egg"}
        repo.cookRecipe(repo.records.value.recipes.single())
        assertEquals(eggs.quantity-2,repo.records.value.pantry.single {it.id==eggs.id}.quantity)
        repo.saveRecipe(recipe().copy(ingredients=listOf(RecipeIngredient("Mushroom",200.0,"g"))))
        repo.addMissing(repo.records.value.recipes.single())
        assertTrue(repo.records.value.shopping.any {it.normalizedName=="mushroom" && it.quantity==200.0})
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun recipeEditorSavesLatestFieldsAndFavoriteFeedbackMatchesState()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo=PantryRepository(RecipeDatabase(),clock)
        val model=PilotViewModel(repo,clock)
        val messages=mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {model.effects.collect {messages.add(it)}}
        try {
            runCurrent();model.accept(Intent.EditRecipe(RecipeEditor.from(recipe())))
            model.accept(Intent.RecipeField("title","Personal bowl"));model.accept(Intent.RecipeField("servings","3"))
            model.accept(Intent.SaveRecipe);runCurrent()
            assertEquals("Personal bowl",model.state.value.records.recipes.single().title)
            assertEquals(3,model.state.value.records.recipes.single().servings)
            model.accept(Intent.Favorite("mine"));runCurrent()
            assertEquals("Added to your favorites",messages.last())
            model.accept(Intent.Favorite("mine"));runCurrent()
            assertEquals("Removed from your favorites",messages.last())
        } finally {model.viewModelScope.cancel();Dispatchers.resetMain()}
    }
}
private class RecipeDatabase: LocalDatabase {
    private var rows=emptyList<DatabaseRow>();var fail=false
    override suspend fun read()=decodeRows(rows)
    override suspend fun write(records: Records) {check(!fail);rows=encodeRows(records)}
}
