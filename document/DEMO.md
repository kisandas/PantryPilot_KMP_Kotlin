# Three minute interview demo

0:00–0:25 — Open Home. Explain that the cream/green Compose UI, models, business rules and feature state are shared across Android, iOS and web. Choose Load demo data and confirm. Show summary counts and Use these first.

0:25–1:05 — Open Pantry. Add "Tomatoes", quantity 3, piece, Vegetables, expiry tomorrow. Try an empty name or negative quantity first to show field validation. Save, search, edit the quantity and save. Delete the test ingredient with confirmation. Show expired and expiring-soon filters.

1:05–1:45 — Open a recommended recipe. Explain use-soon ranking and presence-only matching. Inspect numbered instructions and Available/Missing statuses. Toggle favorite. Add missing ingredients twice. Show that the second action does not duplicate active ingredients or change their quantities.

1:45–2:20 — Open Shopping. Add a manual item; edit it; check it purchased. Explain that checking does not add to the pantry. Clear purchased items with confirmation if desired.

2:20–2:45 — Refresh browser, or force-stop and reopen Android. Show pantry, shopping, favorites and theme surviving. On Android use `adb shell am force-stop com.app.meal.planner`, then reopen from the launcher. Show repository StateFlow updates without reload after a new edit.

2:45–3:00 — Show Settings light/dark and explain local-only storage. In source show LocalDatabase and platform adapters: system SQLite on mobile, IndexedDB on web. State honestly whether iOS has been run on macOS.

## Screenshot and capture instructions

1. Load demo data explicitly on the app instance used for capture.
2. Capture Home at desktop width (sidebar, summaries, use-first section).
3. Capture Recipes at desktop width (two columns), then a recipe detail with missing ingredients.
4. Capture Shopping with both active and purchased rows.
5. Capture Pantry at a 390×844 browser viewport or Android emulator; include bottom navigation.
6. Capture one dark-theme screen, then restore your preferred theme.
7. Use browser DevTools' Capture screenshot or Android Studio's Running Devices screenshot button. On an attached emulator, `adb shell screencap -p /sdcard/pantry.png` followed by `adb pull /sdcard/pantry.png document/pantry-android.png` saves a PNG.
8. For a recording use Android Studio Running Devices' Record control, or the operating system's browser screen recorder. No record/publish service is required.

Do not include personal records in public interview captures. Demo data is local to the current device/origin.

## Stock and household-order demo

1. Pantry: select the 2-egg batch, Use stock, quantity 2, note Breakfast, Save. Show zero balance and its Used history entry.
2. Restock: add 6 eggs to Shopping. Create order, enter a store, save draft and place the order.
3. Orders: receive only 2 eggs, with a new expiry. Show Partially received and the new stock batch.
4. Receive the remaining 4 with another expiry; show Received and separate batches, then refresh and verify balances/order/history remain.
5. Show Record waste, the stock correction explanation and the recipe Cook & deduct stock confirmation. State that physical usage must be recorded explicitly.
