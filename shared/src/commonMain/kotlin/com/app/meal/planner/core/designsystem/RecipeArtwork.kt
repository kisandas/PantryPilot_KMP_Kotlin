package com.app.meal.planner.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Vector illustration drawn locally on every platform; no image requests or font glyph dependency. */
@Composable fun RecipeArtwork(title: String, seed: Int) {
    Canvas(Modifier.fillMaxWidth().height(112.dp).semantics { contentDescription = "Illustrated bowl for $title" }) {
        drawRoundRect(Color(0xFFE8EDD9),cornerRadius=androidx.compose.ui.geometry.CornerRadius(24f,24f))
        val center=Offset(size.width/2,size.height/2)
        val radius=size.height*.41f
        drawCircle(Color(0xFFCBD5BA),radius,center+Offset(0f,4f))
        drawCircle(Color(0xFFFFFCF1),radius,center)
        drawCircle(Color(0xFFE1C58E),radius*.75f,center)
        repeat(8) { i ->
            val x = center.x + ((i*37+seed*11)%71-35)*radius/52
            val y = center.y + ((i*23+seed*7)%61-30)*radius/52
            drawOval(if(i%3==0) Color(0xFFD86846) else Color(0xFF527D46),Offset(x-radius*.16f,y-radius*.11f),Size(radius*.36f,radius*.24f))
        }
    }
}
