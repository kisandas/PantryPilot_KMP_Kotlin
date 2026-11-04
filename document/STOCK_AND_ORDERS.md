# Household purchases and pantry stock

PantryPilot now tracks household stock movements as well as expiry dates. It does not know what you physically used until you record usage or explicitly confirm cooking a recipe.

## Your two eggs example

1. Open Pantry and find the batch showing 2 eggs.
2. Choose **Use stock**, enter **2**, optionally enter `Breakfast`, then Save.
3. That batch becomes **0 piece — Out of stock**. It no longer counts toward recipe availability or use-soon suggestions.
4. Open its **History** to see the dated `Used -2 piece` entry and the resulting zero balance.
5. Choose **Restock** and enter 6 eggs in Shopping.
6. When the eggs arrive, choose **Receive into pantry**, enter 6 and their new expiry date, then Save.
7. A purchase order and receipt are recorded. A new batch contains 6 eggs with the new expiry. The old batch and its history remain separate. Pantry's **Totals across batches** shows the combined balance.

## Planned household purchases

- Add what you intend to buy in Shopping. Use separate units deliberately; no automatic unit conversion occurs.
- Choose **Create order**. All unpurchased, unlinked shopping items are included. Enter the store/supplier and optional notes.
- A **Draft** order lets you edit its quantities, store and notes. To change products, cancel the draft, edit Shopping and create a new draft.
- Choose **Place order**. This moves it to **Awaiting delivery**; it does not change pantry stock.
- Open **Orders**, select the order, and choose **Receive delivery** for a line. Enter the amount actually received, category and expiry date.
- If only part arrives, the order becomes **Partially received**. The rest remains outstanding. Receive later deliveries separately so their expiry dates stay accurate.
- When every line is fully received, the order becomes **Received** and its shopping items are marked purchased. Received quantities cannot be received again.
- **Cancel outstanding purchase** closes an unfinished order. Stock already received stays in the pantry; only outstanding quantities return to unlinked Shopping items.

There is no seller-facing sales workflow, accounting, online store integration, payments or automatic ordering. This is a local household procurement and stock system. Purchase orders contain products, units, quantities, store, notes, dates, statuses and receipts; prices, taxes, refunds, returns and supplier payments are outside this implementation.

## Daily usage, waste and corrections

- **Use stock** subtracts what you consumed from the selected batch. It cannot exceed that batch's balance or use expired food.
- **Record waste** subtracts discarded food, including expired stock, with an optional reason.
- **Edit** quantity is a manual count correction and creates an Adjustment entry. A batch's unit is fixed. A new purchase belongs in a new batch rather than changing an old batch's expiry.
- Exhausted batches remain at zero for history; they do not count as available ingredients. You can remove a batch if desired; its stock history is retained.
- Removing a batch records its remaining quantity as Removed, so it does not silently disappear from the stock ledger.
- **History** shows dated opening balances, purchase receipts, usage, waste, corrections and removals. Each entry stores the batch balance after that movement.
- **Restock** opens a new shopping-item form. **Buy again** on a purchased shopping item creates a new unlinked purchase item.

A purchased checkbox alone is still a shopping reminder; it does not invent a receipt date, expiry or pantry batch. Use Receive into pantry to record the actual arrival. Shopping items linked to orders are managed through their order so they cannot be checked, edited or deleted inconsistently.

## Cooking a recipe

Recipe details show required quantities and stock held in the exact same unit. **Can cook** now checks sufficient unexpired quantities. Recipe overview cards still summarize ingredient names present; ranking prioritizes soon-expiring ingredient names.

**Add stock shortfalls to shopping** adds only the missing quantity for each ingredient/unit. It preserves an existing active item's quantity rather than silently changing it. Review that shopping item if your planned quantity needs increasing.

**Cook & deduct stock** is an explicit confirmation for the recipe's displayed serving count. It lists the amounts to subtract and uses earliest-expiring unexpired batches first (undated batches last). All required ingredients are checked before any deduction. If there is not enough stock in the same unit, nothing is deducted. For a different serving count or substitutions, record actual usage manually. Simply viewing a recipe never reduces stock.

## Persistence and upgrades

The UI, order/stock models, rules, row codec, repository and state owner are shared by Android, iOS and web. SQLite and IndexedDB adapters use the common record codec. Each purchase receipt updates order state, shopping status, stock and history together in a single database transaction.

Existing records are loaded without resetting their balances. The first successful upgraded load records their opening balances once. Subsequent loading does not duplicate opening history. New records use additional `order`, `movement` and `stockTracking` record kinds in the existing database table/object store; no destructive table or database recreation occurs.

Do not run an older app version against an upgraded database: the old version does not understand order/history records. Reset local data intentionally removes orders and stock history as well as pantry and shopping data. Predefined pantry/shopping entries are inserted once on a fresh database. No Load demo data action remains. Existing records are preserved; resets and depletion do not replenish starter stock.

Data remains local to the current app/device/browser origin. Keep one active editing browser tab; concurrent multi-tab edits and device synchronization are not supported. The app has no automatic consumption sensors or external order integration.

