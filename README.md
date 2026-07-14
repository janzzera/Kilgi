# Kilgi

Kilgi is an Android app for vegetable vendors. Module 1 now runs as a **Room-backed landed cost procurement and inventory ledger prototype** for Carbon Market operations.

## What Module 1 now does

- Creates vegetable lots in a normalized local Room database.
- Tracks dynamic additional batch expenses in a separate child table.
- Tracks spoilage logs with `NORMAL` and `ABNORMAL` valuation behavior.
- Recomputes:
  - **Total Capitalized Cost**
  - **Net Usable Kilograms**
  - **True Cost Per Kilo**
- Persists automated **double-entry journal entries** for:
  - initial lot creation
  - later capitalized expenses
  - abnormal spoilage write-offs

## Room schema

The database layer lives under `app/src/main/java/com/example/kilgi/inventory/data/`.

- `lots`
- `batch_expenses`
- `spoilage_logs`
- `journal_entries`
- `journal_lines`

## Accounting rules implemented

### Accounts
- `10100` Cash / Mobile Wallet
- `12000` Merchandise Inventory - Vegetables
- `20100` Accounts Payable - Providers
- `50100` Cost of Goods Sold (reserved for Module 2)
- `50200` Inventory Loss - Spoilage/Waste

### Automated journal behavior
- **Initial lot creation**
  - Debit inventory for purchase cost plus standard freight
  - Credit purchase payment source (`AP` or `CASH`)
  - Credit freight payment source (`AP` or `CASH`)
- **Additional expense**
  - Debit inventory
  - Credit selected payment source
- **Normal spoilage**
  - No journal entry
  - Remaining good kilos absorb the same capitalized cost, increasing true cost per kilo
- **Abnormal spoilage**
  - Debit `50200 Inventory Loss - Spoilage/Waste`
  - Credit `12000 Merchandise Inventory - Vegetables`
  - Write-off amount is calculated at the current pre-loss unit cost

## Main packages

- `inventory/data` - Room entities, DAOs, enums, and database singleton
- `inventory/service` - valuation engine and repository orchestration
- `inventory/accounting` - journal draft builders and chart-of-accounts constants

Legacy in-memory domain classes remain in the project for reference, but the launcher screen now uses the Room-backed flow.

## Current screens

`MainActivity` now serves as the dedicated **Lot** screen and supports:

- a top menu with `Lot` and `Journal` entry points that can grow with future modules
- baseline lot creation
- selecting/loading a persisted lot by `lot_id`
- appending dynamic batch expenses
- logging normal or abnormal spoilage
- viewing live lot valuation
- viewing all saved lots

`JournalActivity` now provides a dedicated **Journal** screen that:

- loads the latest or selected lot journal
- displays journal entries grouped by event
- shows debit and credit lines in entry-style cards with account, amount, memo, payment source, and provider details when available

## Run tests

```powershell
.\gradlew.bat test --console=plain
```

## Next likely improvements

- Add a dedicated providers master table
- Add RecyclerView-based lot and journal screens
- Add Module 2 sale posting into `50100 COGS`
- Add Room migrations once the schema starts evolving

