# Xcode 16.0 compatibility migration

The project now targets Kotlin 2.1.20, whose official compatibility matrix lists Xcode 16.0. Compose 1.8.0 is used with its matching compiler plugin, Material 3 and lifecycle dependencies. This is a coordinated toolchain migration, not an Xcode-warning suppression.

## Pinned build stack

| Component | Version |
| --- | --- |
| Kotlin, Compose compiler, serialization plugin | 2.1.20 |
| Compose Multiplatform and Material 3 | 1.8.0 |
| Lifecycle | 2.9.0-beta01 |
| Gradle | 8.11, official distribution checksum pinned |
| Android Gradle Plugin | 8.7.2 |
| Android compile / target / minimum SDK | 35 / 35 / 26 |
| Android Activity | 1.10.1 |
| Coroutines and tests | 1.10.1 |
| Serialization JSON | 1.8.1 |
| kotlinx-datetime | 0.6.2 |
| Koin | 4.0.2 |
| iOS deployment target | 18.0 |

The Android library target uses com.android.library and androidTarget, with org.jetbrains.kotlin.android applied to the Android application. This replaces the newer AGP 9 Kotlin/Android DSL. Existing test command names remain as aliases. iOS device arm64, simulator arm64 and Intel simulator x64 targets share the same UI, business rules and SQLite adapter. The iOS source hierarchy and cinterop commonization are retained.

Clock and Instant imports now use kotlinx-datetime's older-compatible APIs. Dates remain the same ISO strings and timestamps remain epoch milliseconds. No unit, expiry, stock, order or persistence business rules were removed. Android target SDK changes are an OS integration change; existing rules, persistence tests and available runtime checks are rerun rather than assuming equivalence.

The JavaScript canvas target in Compose 1.8 is experimental and requires org.jetbrains.compose.experimental.jscanvas.enabled=true. The app remains a Compose JavaScript browser app; no platform was removed. The web entry point/resources and browser integration test paths are explicitly mapped so they remain included under Kotlin 2.1.20.

## On your Mac

1. Copy/pull the entire updated project, including source adjustments and bundled photographs.
2. Install JDK 21 (or allow the configured Gradle daemon toolchain to provision it).
3. Select your Xcode 16.0 installation. For a per-terminal selection, use its actual path, for example:

```sh
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
xcodebuild -version
sh tools/verify-xcode16.sh
```

The script requires Xcode 16.0 and builds the simulator framework matching your Mac's architecture. Then open iosApp/iosApp.xcodeproj, set your signing team, choose an installed iOS 18.0 simulator and Run. If Xcode is using a previously built framework, use Product > Clean Build Folder and rebuild. A deployment target is the minimum device OS, not the Xcode version; the previous iOS 18.2 target was too high for Xcode 16.0's iOS 18.0 SDK.

Kotlin's official compatibility guide: https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html
Compose 1.8.0 dependencies: https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.8.0
Apple SDK table: https://developer.apple.com/xcode/system-requirements/

An actual Xcode 16.0 build cannot be executed on the Windows development host. Android and web verification results are recorded separately in VERIFICATION.md; Mac compilation and simulator execution remain required before claiming iOS verified.

## Starting data and existing records

If Xcode reports `gradlew: Permission denied` during PhaseScriptExecution, use the updated Compile Kotlin Framework build phase: `sh ./gradlew :shared:embedAndSignAppleFrameworkForXcode`. This does not require an executable permission bit on the wrapper copied from Windows. The Unix wrapper is stored with LF line endings. Alternatively, `chmod +x gradlew` in the project root enables direct execution on your Mac. Clean Build Folder and rebuild after updating the project.

Fresh databases receive predefined pantry/shopping entries exactly once. Existing records are retained and merely gain an initialData marker. Clearing or exhausting a list does not seed it again; confirmed reset leaves it empty. Load demo data controls and the demo-loading repository method were removed. Future new entries continue to save to the same SQLite / IndexedDB stores, and order/stock transactions remain atomic.

Recipe cards form a responsive grid with one column on narrow phones, two at medium widths and three on wide screens. All 12 photographs are bundled locally. PHOTO_CREDITS.md and PHOTO_CREDITS.json identify source pages, photographers and the Pexels license. Photographs show serving inspiration; the written recipe defines the ingredients and quantities.

The Compose 1.8 JS startup loads skiko.js before webApp.js, waits for onWasmReady, and passes the compose-root container explicitly to ComposeViewport. Keep these bootstrap changes when moving the project; changing only version numbers is insufficient. Form edits patch the latest state one field at a time so changing units or receipt dates cannot overwrite another field with an older rendered value.
