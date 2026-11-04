import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("sqlite") {
            definitionFile.set(project.file("src/nativeInterop/cinterop/sqlite.def"))
        }
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    androidTarget {
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    js(IR) {
        browser { testTask { useKarma { useChromeHeadless() } } }
        binaries.executable()
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation("io.insert-koin:koin-compose:4.0.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        jsTest { kotlin.srcDir("src/webTest/kotlin") }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
        }
        val androidUnitTest by getting {
            kotlin.srcDir("src/androidHostTest/kotlin")
            dependencies { implementation(libs.kotlin.testJunit) }
        }
        val androidInstrumentedTest by getting {
            kotlin.srcDir("src/androidDeviceTest/kotlin")
            dependencies {
                implementation(libs.androidx.testRunner)
                implementation(libs.androidx.testExt.junit)
            }
        }
    }
}

android {
    namespace = "com.app.meal.planner.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

// Keep the commands used by existing documentation and local workflows.
// Tasks themselves are implemented by the legacy Android plugin required by Kotlin 2.1.20.
tasks.register("testAndroidHostTest") { dependsOn("testDebugUnitTest") }
tasks.register("connectedAndroidDeviceTest") { dependsOn("connectedDebugAndroidTest") }



