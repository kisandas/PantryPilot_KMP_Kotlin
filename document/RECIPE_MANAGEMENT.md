# User-managed recipes and interface update

Recipes are no longer populated from a built-in catalog. Fresh and upgraded installations show only user-created recipes. Pantry and shopping start empty on fresh installations. The former catalog is now a test fixture and is not compiled into the application.

## Recipe workflow

Open Recipes and choose Create recipe. Enter a title, description, category, preparation minutes, servings, ingredient names/quantities/units, and instructions (one step per line). Optionally choose one of the locally bundled photographs. Save opens the recipe detail. Edit recipe updates the existing entry; Delete recipe asks for confirmation, removes the recipe and its favorite marker, and leaves pantry balances and historical cooking movements intact.

Data uses the common serializable Recipe model and `recipe` row kind in SQLite on Android/iOS and IndexedDB in the browser. No destructive database migration is needed. Existing stock/order/history data is preserved. Reset all local data also clears user recipes.

Favorite hearts expose checked state and accessible add/remove descriptions. Feedback is shown by the shared SnackbarHost: Added to your favorites / Removed from your favorites. Recipe card content uses consistent 16 dp padding and 8 dp spacing, compact titles, and a single View recipe action.

## Forms and system UI

Empty-state cards no longer repeat the screen's add action. Scaffold applies system insets once; the former outer safeContentPadding is removed. Header padding is 6 dp and list bottom spacing is 12 dp. Actual system safe areas remain respected.

Expiry fields open Android DatePickerDialog, iOS UIDatePicker wheels, and an HTML date input on web. The date is optional and can be cleared. Both ingredient editing and pantry receipt forms use the same field contract. Browser appearance depends on its native date control.

Android disables autofill for the activity hierarchy and provides no Compose autofill services, including dialogs. This is scoped to the app and does not modify the user's system-wide keyboard configuration. Third-party keyboard toolbars and word suggestions remain controlled by the keyboard app.

## Verification

See VERIFICATION.md for current results. iOS native compilation and date-wheel interaction must be checked on a Mac running the supported Xcode version; Windows cannot validate Apple platform binaries.

The installed Android startup check exposed the older Koin Compose remember-loader reading its context before initialization. App now creates an isolated Koin application explicitly before composing children and closes that context when it leaves composition. This keeps the shared dependency module and avoids relying on global Koin initialization.
