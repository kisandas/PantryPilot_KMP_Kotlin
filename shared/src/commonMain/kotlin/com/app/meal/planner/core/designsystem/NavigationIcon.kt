package com.app.meal.planner.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlin.math.*

/** Small shared vectors avoid depending on platform symbol/emoji fonts. */
@Composable fun NavigationIcon(destination: String, color: Color = MaterialTheme.colorScheme.primary, iconSize: Dp = 22.dp) {
    Canvas(Modifier.size(iconSize)) {
        scale(size.width/24f,size.height/24f,Offset.Zero) {
            val outline=Stroke(1.8f,cap=StrokeCap.Round,join=StrokeJoin.Round)
            fun line(x1: Float,y1: Float,x2: Float,y2: Float) = drawLine(color,Offset(x1,y1),Offset(x2,y2),strokeWidth=1.8f,cap=StrokeCap.Round)
            fun path(vararg points: Pair<Float,Float>) {
                val p=Path();points.forEachIndexed {i,point -> if(i==0) p.moveTo(point.first,point.second) else p.lineTo(point.first,point.second)}
                drawPath(p,color,style=outline)
            }
            when(destination) {
                "Back" -> { path(12f to 5f,5f to 12f,12f to 19f); line(5f,12f,21f,12f) }
                "Edit" -> {
                    path(4f to 16f, 16f to 4f, 20f to 8f, 8f to 20f, 3f to 21f, 4f to 16f)
                    line(13f,7f,17f,11f)
                }
                "Delete" -> {
                    path(6f to 7f,7f to 21f,17f to 21f,18f to 7f)
                    path(9f to 7f,9f to 3f,15f to 3f,15f to 7f)
                    line(4f,7f,20f,7f)
                    line(10f,11f,10f,17f)
                    line(14f,11f,14f,17f)
                }
                "Home" -> {path(3f to 10f,12f to 3f,21f to 10f);path(5f to 9f,5f to 21f,10f to 21f,10f to 14f,14f to 14f,14f to 21f,19f to 21f,19f to 9f)}
                "Pantry" -> {drawRoundRect(color,Offset(4f,3f),Size(16f,18f),CornerRadius(2f),style=outline);line(4f,9f,20f,9f);line(4f,15f,20f,15f);line(11f,6f,13f,6f);line(11f,12f,13f,12f);line(11f,18f,13f,18f)}
                "Recipes" -> {path(12f to 5f,8f to 3f,3f to 4f,3f to 19f,8f to 18f,12f to 20f,16f to 18f,21f to 19f,21f to 4f,16f to 3f,12f to 5f,12f to 20f);line(6f,8f,9f,9f);line(15f,9f,18f,8f)}
                "Shopping" -> {path(2f to 4f,5f to 4f,8f to 16f,19f to 16f,22f to 7f,6f to 7f);drawCircle(color,1.4f,Offset(9f,21f));drawCircle(color,1.4f,Offset(18f,21f))}
                "Orders" -> {drawRoundRect(color,Offset(5f,4f),Size(14f,18f),CornerRadius(2f),style=outline);drawRoundRect(color,Offset(9f,2f),Size(6f,4f),CornerRadius(1f),style=outline);line(8f,10f,16f,10f);line(8f,14f,16f,14f);line(8f,18f,13f,18f)}
                "History" -> {drawArc(color,210f,300f,false,Offset(3f,3f),Size(18f,18f),style=outline);path(2f to 4f,2f to 10f,8f to 10f);line(12f,7f,12f,12f);line(12f,12f,16f,14f)}
                else -> {drawCircle(color,5.5f,Offset(12f,12f),style=outline);drawCircle(color,2f,Offset(12f,12f),style=outline);repeat(8) {i -> val angle=i*PI/4;line(12f+cos(angle).toFloat()*7,12f+sin(angle).toFloat()*7,12f+cos(angle).toFloat()*10,12f+sin(angle).toFloat()*10)}}
            }
        }
    }
}
