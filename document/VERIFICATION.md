# Verification results

## Latest user-recipe and form update

Verified on Windows on 4 November 2026. This section supersedes older catalog-based walkthroughs below.

- 37 Android host tests passed, zero failures/errors.
- 39 headless-browser tests passed, zero failures/errors.
- 38 connected-device tests passed on physical Android 16, including recipe rows persisted by actual SQLite across close/reopen.
- Actual IndexedDB persistence checks now include user recipe payloads. New common tests cover an empty initial recipe collection, create/update/delete, favorite cleanup, validation and failed saves, recipe cooking/shortfalls, and editor/favorite feedback.
- Android APK and web preview packaging passed in the combined regression run. The follow-up host/browser tests and Android/web rebuild also passed after isolating native date-dialog events and aligning form fields. The updated Android APK installed successfully.
- Browser UI on the isolated localhost:8098 origin: created a personal recipe with a selected local photo and instruction, toggled filled/outlined hearts and verified both snackbar messages, edited its title, refreshed, and verified the saved title and favorite. The deletion confirmation was inspected and cancelled; actual deletion/favorite cleanup is covered by automated tests.
- Responsive recipe card inspected at 390x844. Screenshots: user-recipe-card.png, user-recipe-mobile.png, recipe-favorite-feedback.png. Web expiry input opened the browser calendar and returned a selected date to the ingredient form; that unsaved ingredient was cancelled. Final DOM-native confirmation selected 2026-11-21 while preserving the piece unit (expiry-date-picker.png). Legacy canvas accessibility clicks were unreliable for the native date popup, so its DOM controls were used for this check.
- iOS date-wheel source is implemented but its compilation and runtime remain unverified on Windows. Android native date-dialog interaction and absence of third-party keyboard bubbles still require device UI checks; disabling app autofill does not change keyboard-owned suggestions.

The final Android launch check exposed an older Koin Compose initialization failure. App now initializes an isolated Koin context before composing children. The subsequent host/browser tests and Android/web builds passed, and final web startup rendered successfully. The physical device disconnected before the repaired APK could be installed. An emulator check was started, then stopped at the user's request; installation did not finish. Final Android runtime verification of this startup fix remains pending.

See RECIPE_MANAGEMENT.md for behavior and data preservation details. No user database reset was performed.


## Latest Xcode 16.0 compatibility and recipe update

Verified on Windows on 4 November 2026 with JDK 21, Kotlin 2.1.20, Compose 1.8.0, AGP 8.7.2 and Gradle 8.11. This section supersedes the historical results below.

| Check | Result |
| --- | --- |
| Android host tests | 32 passed, zero failures/errors |
| Android connected-device tests | 33 passed, zero failures/errors on a physical Android 16 device |
| Headless browser tests | 34 passed, zero failures/errors |
| Final packaging | Combined regression command completed successfully; Android APK and web preview built |
| Android startup | Updated APK installed with existing data preserved and MainActivity launched successfully |
| Browser UI | Final bundle rendered; new shopping entry retained 200 g after refresh; three-column local-photo recipe grid verified |
| iOS | Configured for Xcode 16.0 and iOS 18.0; compilation/runtime still require verification on a Mac |

Coverage includes SQLite/IndexedDB persistence, stock/order workflows, one-time predefined data, existing-data preservation, no reseeding after reset, failed-initialization retry, and rapid quantity/unit edits retaining the full value. The browser runner emitted a Chrome shutdown warning after its tests; XML reports show zero failed tests.

Current reports: `shared/build/reports/tests/testDebugUnitTest/index.html`, `shared/build/reports/tests/jsBrowserTest/index.html`, and `shared/build/outputs/androidTest-results/connected/debug/`.

The regression command uses JDK 21 and Chrome:

```powershell
.\gradlew.bat :shared:testAndroidHostTest :shared:jsBrowserTest :shared:connectedAndroidDeviceTest :androidApp:assembleDebug :webApp:packageWebPreview --no-configuration-cache --max-workers=2
```

Fresh installations receive eight pantry batches and two shopping entries automatically, with no Load Data action. All twelve recipe photographs are project resources. Existing user databases are preserved. See XCODE16_MIGRATION.md for the Mac verification command and PHOTO_CREDITS.md for image sources.

Screenshots: `document/recipes-photo-grid.png` and `document/saved-entry-after-refresh.png`. UI checks used localhost:8097, separate from the user's localhost:8094 storage. The legacy Compose JS canvas has limited accessibility exposure; visual interaction was used for this walkthrough. Only the tested regression cases are claimed, rather than a guarantee of every possible platform behavior.

## Historical verification before the compatibility update

Verified on Windows on 4 November 2026.

| Check | Result |
| --- | --- |
| Android debug APK | Build passed; installed and launched on Medium_Phone_API_36 emulator (Android 16) |
| Android host tests | 26 passed, zero failures/errors |
| Android device tests | 27 passed, zero failures/errors, including real SQLite CRUD, stock usage, partial/full order receipts and reopen persistence |
| Browser development webpack | Build passed; served locally and rendered in Chromium |
| Browser tests | 28 passed, zero failures/errors, including real IndexedDB CRUD, stock usage, partial/full order receipts and reopen persistence |
| Browser UI walkthrough | Demo load; refresh retained 8 pantry items, 3 soon-expiring items and shopping data; recipe missing lentils added to shopping; purchased status changed; desktop sidebar and 390×844 mobile layout inspected |
| iOS arm64 / simulator targets | Configured, NOT compiled or run: Windows lacks macOS/Xcode and Apple cinterop toolchains |
| Production/release signing and publication | Not performed |
| Offline web launch | Not implemented or claimed: no service worker |

The combined verification command succeeded:

```powershell
.\gradlew.bat :shared:jsBrowserTest :shared:connectedAndroidDeviceTest :androidApp:assembleDebug :shared:testAndroidHostTest :webApp:jsBrowserDevelopmentWebpack --no-configuration-cache --max-workers=2
```

Environment used `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`, `GRADLE_USER_HOME=C:\Users\Krishna\.gradle`, `CHROME_BIN=C:\Program Files\Google\Chrome\Application\chrome.exe`.

Reports:

- `shared/build/reports/tests/testAndroidHostTest/index.html`
- `shared/build/reports/tests/jsBrowserTest/index.html`
- `shared/build/outputs/androidTest-results/connected/androidMain/TEST-Medium_Phone_API_36(AVD) - 16-_shared-.xml`

Database integration tests restore the original data after their checks. Browser tests use their test runner origin, separate from the preview's local origin. SQLite integration runs in the instrumentation test package, separate from the actual Android app's private data.

Initial checks exposed an offline-cache miss for aapt2 and a back-handler dependency added during a running build. Both were fixed; the subsequent combined build and tests above passed. Kotlin reports Apple targets disabled on Windows as expected. Webpack reports upstream source-map warnings; bundling succeeds. Native Android symbol-stripping warnings package the existing library unchanged. The configuration-cache path is intentionally bypassed in documented verification commands.

Known verification limits: browser refresh and database reopen are verified; abrupt OS termination/power loss, browser quota exhaustion, multiple simultaneous tabs, full screen-reader coverage and iOS runtime have not been tested. Android app startup is verified, while full pantry/shopping end-to-end UI automation was performed in the browser. iOS must be checked on macOS before release.

Final UI/accessibility packaging check also passed: :androidApp:assembleDebug :webApp:packageWebPreview. Development web distribution: webApp/build/dist/js/developmentExecutable. The final palette uses cream surfaces and labeled shopping checkboxes.


## Stock and household-order update

The final combined build passed with 26 Android-host tests, 27 Android emulator tests, and 28 browser tests (zero failures/errors). It built the updated Android debug APK and packaged the browser preview:

```powershell
.\gradlew.bat :shared:testAndroidHostTest :shared:jsBrowserTest :shared:connectedAndroidDeviceTest :androidApp:assembleDebug :webApp:packageWebPreview --no-configuration-cache --max-workers=2
```

New focused coverage: one-time opening-balance upgrade, using two eggs to zero, no negative/invalid/fractional-piece usage, expired-food waste, receipts with independent expiry dates, duplicate/over-receipt rejection, partial/full statuses, cancellation returning only outstanding quantities, draft edits and linked-shopping locks, atomic failure behavior, quick receipts, buy-again, FEFO recipe deductions, quantity shortfalls without conversion, audited corrections/removal, form error retention and real database persistence. A common integration workflow runs against both actual SQLite and IndexedDB and verifies order, stock, shopping links and history after closing/reopening the database.

Browser UI walkthrough on an isolated localhost:8096 origin: added 2 eggs expiring 2026-11-06; used both for Breakfast; confirmed zero batch balance and no expiry reminder; restocked 6 eggs in Shopping; created PO-1 for Local store; placed it; received 2 expiring 2026-11-12 and then 4 expiring 2026-11-20; verified Partially received then Received; refreshed; verified Pantry total 6 eggs, original zero batch, and persistent purchase/usage/opening history. Checked the 390x844 mobile layout with five-destination bottom navigation and stock totals. Captures: `document/order-partial.png`, `document/stock-mobile.png`, `document/stock-history.png`.

Screen-reader coverage remains incomplete: Compose browser accessibility snapshots were sometimes stale, so visual coordinate checks were used when necessary. iOS remains configured but unverified on Windows. The UI walkthrough used a separate test origin and did not reset the user's existing localhost:8094 database.
