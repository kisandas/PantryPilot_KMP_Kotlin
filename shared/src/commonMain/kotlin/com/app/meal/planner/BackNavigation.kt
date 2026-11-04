package com.app.meal.planner
import androidx.compose.runtime.Composable
@Composable expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
