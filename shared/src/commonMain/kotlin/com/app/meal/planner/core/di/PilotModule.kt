package com.app.meal.planner.core.di

import com.app.meal.planner.core.database.LocalDatabase
import com.app.meal.planner.data.repository.PantryRepository
import com.app.meal.planner.domain.*
import com.app.meal.planner.feature.PilotViewModel
import org.koin.dsl.module

fun pilotModule(database: LocalDatabase) = module {
    single<LocalDatabase> { database }
    single<DateProvider> { SystemDateProvider() }
    single { PantryRepository(get(),get()) }
    factory { PilotViewModel(get(),get()) }
}
