package com.app.meal.planner.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pantrypilot.shared.generated.resources.*

/** Real photographs are bundled in shared resources; rendering never fetches remote images. */
@Composable fun RecipeArtwork(title: String,seed: Int,height: Dp=160.dp) {
    val photo=when(seed) {
        1 -> Res.drawable.recipe_01
        2 -> Res.drawable.recipe_02
        3 -> Res.drawable.recipe_03
        4 -> Res.drawable.recipe_04
        5 -> Res.drawable.recipe_05
        6 -> Res.drawable.recipe_06
        7 -> Res.drawable.recipe_07
        8 -> Res.drawable.recipe_08
        9 -> Res.drawable.recipe_09
        10 -> Res.drawable.recipe_10
        11 -> Res.drawable.recipe_11
        else -> Res.drawable.recipe_12
    }
    Image(painterResource(photo),contentDescription=title,contentScale=ContentScale.Crop,
        modifier=Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(16.dp)))
}
