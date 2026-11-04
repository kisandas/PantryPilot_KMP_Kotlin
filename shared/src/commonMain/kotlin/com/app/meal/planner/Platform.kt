package com.app.meal.planner

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform