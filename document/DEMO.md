# Three minute interview demo

0:00–0:25 — Open Home. A fresh installation starts with predefined pantry/shopping entries; existing installations retain their records. Explain shared Compose UI, local-only storage and expiry-based recommendations. There is no Load demo data action.

0:25–1:05 — Open Pantry. Add an ingredient, demonstrate invalid quantity validation, save and edit it. Select a batch, choose Use stock, and subtract the quantity actually used. Show its reduced balance and Stock history. Explain that editing a count is an audited correction and new purchases create separate expiry batches.

1:05–1:40 — Open Recipes. Show the responsive photograph grid, search, Can cook and Favorites filters. Open details, inspect exact-unit stock checks, ordered instructions, and Cook & deduct stock confirmation. Add stock shortfalls to Shopping without duplicate active items.

1:40–2:20 — Open Shopping, add a manual item and Receive into pantry, or create a draft purchase order from the pending shopping lines. Place the order, receive a partial delivery with an expiry date, and show outstanding quantities in Orders. Each receipt adds a new stock batch; consumption and receipts are recorded in History.

2:20–2:45 — Refresh the browser or force-stop/reopen Android. Verify the newly created item, balances, order status, favorites and theme remain. Checking purchased alone does not invent a pantry receipt; receive explicitly.

2:45–3:00 — Show Settings themes and explain that reset leaves an empty database without reseeding. Show the shared repository/record codec and SQLite/IndexedDB boundaries. State that Xcode 16.0 compatibility is configured but must be verified on a Mac.

## Screenshot and capture instructions

1. Capture Home on a fresh app origin or use existing personal data only in private demonstrations.
2. Capture Recipes at 1280px or wider: three photograph columns. At medium widths the grid has two columns; narrow phones have one.
3. Capture a recipe detail showing the bundled photograph, quantities and instructions.
4. Capture Pantry stock totals and History after usage or a receipt.
5. Capture Orders with a partial receipt and its separate expiry batch.
6. Capture a mobile layout around 390×844 and one dark-theme screen.
7. Use browser DevTools' Capture screenshot or Android Studio Running Devices' screenshot/record controls.

Do not clear or overwrite personal records to prepare screenshots. Predefined entries are created only on a fresh database. No record/publish service is required.
