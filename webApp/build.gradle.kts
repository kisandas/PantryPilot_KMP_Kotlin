import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))

            implementation(libs.compose.ui)
        }
    }
}

tasks.register<Sync>("packageWebPreview") {
    dependsOn("jsBrowserDevelopmentWebpack")
    // Webpack can already include Compose resources also present in processedResources.
    // Keep the webpack copy when both sources provide the same distribution path.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(layout.buildDirectory.dir("kotlin-webpack/js/developmentExecutable"))
    from(layout.buildDirectory.dir("processedResources/js/main"))
    into(layout.buildDirectory.dir("dist/js/developmentExecutable"))
}
