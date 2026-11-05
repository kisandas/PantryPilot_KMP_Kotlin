package com.app.meal.planner

import androidx.compose.runtime.Composable

// Shared screens provide visible back controls on iOS.
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
