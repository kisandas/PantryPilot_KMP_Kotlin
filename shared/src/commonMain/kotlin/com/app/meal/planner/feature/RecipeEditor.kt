package com.app.meal.planner.feature

import androidx.compose.foundation.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.app.meal.planner.domain.validStockQuantity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.app.meal.planner.encodeRecipePhoto
import com.app.meal.planner.rememberRecipePhotoPicker
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.app.meal.planner.core.model.*
import com.app.meal.planner.core.designsystem.RecipeArtwork

data class DraftIngredient(val name: String="",val quantity: String="1",val unit: String="piece")
data class RecipeEditor(
    val id: String?=null, val title: String="", val description: String="", val category: String="Dinner",
    val minutes: String="20", val servings: String="2", val ingredients: List<DraftIngredient> = listOf(DraftIngredient()),
    val instructions: String="", val visual: String="", val error: String?=null,
    val showValidation: Boolean = false,
) {
    fun change(field: String,value: String): RecipeEditor = when(field) {
        "title" -> copy(title=value,error=null)
        "description" -> copy(description=value,error=null)
        "category" -> copy(category=value,error=null)
        "minutes" -> copy(minutes=value,error=null)
        "servings" -> copy(servings=value,error=null)
        "instructions" -> copy(instructions=value,error=null)
        "visual" -> copy(visual=value,error=null)
        "add" -> copy(ingredients=ingredients+DraftIngredient(),error=null)
        "remove" -> copy(ingredients=ingredients.filterIndexed { index,_ -> index!=value.toInt() },error=null)
        else -> {
            val parts=field.split(":")
            if(parts.size!=3 || parts[0]!="ingredient") this else copy(ingredients=ingredients.mapIndexed { index,row ->
                if(index!=parts[1].toInt()) row else when(parts[2]) { "name" -> row.copy(name=value); "quantity" -> row.copy(quantity=value); "unit" -> row.copy(unit=value); else -> row }
            },error=null)
        }
    }
    fun validationErrors(): Map<String, String> = buildMap {
        if (title.isBlank()) put("title", "Enter a recipe title.")
        if ((minutes.trim().toIntOrNull() ?: 0) !in 1..1440) put("minutes", "Enter 1–1440 whole minutes.")
        if ((servings.trim().toIntOrNull() ?: 0) !in 1..100) put("servings", "Enter 1–100 whole servings.")
        if (instructions.isBlank()) put("instructions", "Enter at least one instruction.")
        ingredients.forEachIndexed { index, ingredient ->
            if (ingredient.name.isBlank()) put("ingredient:$index:name", "Enter an ingredient name.")
            if (!validStockQuantity(ingredient.quantity.trim().toDoubleOrNull() ?: Double.NaN, ingredient.unit)) {
                put("ingredient:$index:quantity", if (ingredient.unit == "piece") "Enter a positive whole number." else "Enter a positive quantity.")
            }
        }
    }
    fun toRecipe(recipeId: String)=Recipe(recipeId,title.trim(),description.trim(),category,minutes.trim().toIntOrNull() ?: 0,servings.trim().toIntOrNull() ?: 0,
        ingredients.map {RecipeIngredient(it.name.trim(),it.quantity.trim().toDoubleOrNull() ?: Double.NaN,it.unit)},
        instructions.lines().map {it.trim()}.filter {it.isNotEmpty()},visual)
    companion object {
        fun from(recipe: Recipe)=RecipeEditor(recipe.id,recipe.title,recipe.description,recipe.category,recipe.preparationMinutes.toString(),recipe.servings.toString(),recipe.ingredients.map {DraftIngredient(it.displayName,stockNumber(it.quantity),it.unit)},recipe.instructions.joinToString("\n"),recipe.visual)
    }
}

@Composable fun RecipeEditorDialog(form: RecipeEditor,saving: Boolean,dispatch: (Intent)->Unit) {
    val errors = if (form.showValidation) form.validationErrors() else emptyMap()
    fun change(field: String,value: String) { if(!saving) dispatch(Intent.RecipeField(field,value)) }
    Dialog(onDismissRequest={if(!saving) dispatch(Intent.EditRecipe(null))}) {
        Surface(shape=MaterialTheme.shapes.extraLarge) {
            Column(Modifier.widthIn(max=600.dp).fillMaxWidth().heightIn(max=700.dp).imePadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(if(form.id==null) "Create recipe" else "Edit recipe",style=MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(form.title,{change("title",it)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text),isError="title" in errors,supportingText=errors["title"]?.let { message -> { Text(message) } },label={Text("Recipe title")},singleLine=true,enabled=!saving,modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(form.description,{change("description",it)},label={Text("Description")},enabled=!saving,modifier=Modifier.fillMaxWidth())
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("Breakfast","Lunch","Dinner","Snack").forEach { FilterChip(form.category==it,{change("category",it)},label={Text(it)},enabled=!saving) } }
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(form.minutes,{change("minutes",it)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),isError="minutes" in errors,supportingText=errors["minutes"]?.let { message -> { Text(message) } },label={Text("Minutes")},singleLine=true,enabled=!saving,modifier=Modifier.weight(1f))
                        OutlinedTextField(form.servings,{change("servings",it)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),isError="servings" in errors,supportingText=errors["servings"]?.let { message -> { Text(message) } },label={Text("Servings")},singleLine=true,enabled=!saving,modifier=Modifier.weight(1f))
                    }
                    Text("Photo (optional)",style=MaterialTheme.typography.titleMedium)
                    var photoError by remember { mutableStateOf(false) }
                    val pickPhoto = rememberRecipePhotoPicker { bytes ->
                        val photo = bytes?.let(::encodeRecipePhoto)
                        photoError = photo == null
                        if (photo != null) change("visual", photo)
                    }
                    if (form.visual.isNotEmpty()) RecipeArtwork(form.title, form.visual, height = 120.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(pickPhoto, enabled = !saving) { Text("Choose photo") }
                        if (form.visual.isNotEmpty()) TextButton({ change("visual", "") }, enabled = !saving) {
                            Text("Remove")
                        }
                    }
                    if (photoError) Text("Choose a supported image under 2 MB.", color = MaterialTheme.colorScheme.error)
                    Text("Ingredients",style=MaterialTheme.typography.titleMedium)
                    form.ingredients.forEachIndexed { index,row ->
                        OutlinedCard { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(row.name,{change("ingredient:$index:name",it)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text),isError="ingredient:$index:name" in errors,supportingText=errors["ingredient:$index:name"]?.let { message -> { Text(message) } },label={Text("Ingredient ${index+1}")},singleLine=true,enabled=!saving,modifier=Modifier.fillMaxWidth())
                            OutlinedTextField(row.quantity,{change("ingredient:$index:quantity",it)},keyboardOptions=quantityKeyboard(row.unit),isError="ingredient:$index:quantity" in errors,supportingText=errors["ingredient:$index:quantity"]?.let { message -> { Text(message) } },label={Text("Quantity")},singleLine=true,enabled=!saving,modifier=Modifier.fillMaxWidth())
                            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) { units.forEach { unit -> FilterChip(row.unit==unit,{change("ingredient:$index:unit",unit)},label={Text(unit)},enabled=!saving) } }
                            TextButton({change("remove",index.toString())},enabled=!saving && form.ingredients.size>1) {Text("Remove ingredient")}
                        } }
                    }
                    OutlinedButton({change("add","")},enabled=!saving) {Text("Add ingredient")}
                    OutlinedTextField(form.instructions,{change("instructions",it)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text),isError="instructions" in errors,label={Text("Instructions")},supportingText={Text(errors["instructions"] ?: "Enter one step per line.")},minLines=3,enabled=!saving,modifier=Modifier.fillMaxWidth())
                    form.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    TextButton({dispatch(Intent.EditRecipe(null))},enabled=!saving) {Text("Cancel")}
                    Button({dispatch(Intent.SaveRecipe)},enabled=!saving) {Text(if(saving) "Saving…" else "Save recipe")}
                }
            }
        }
    }
}
