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

### Chart of accounts

The app now keeps a centralized merchandising-oriented chart of accounts in `inventory/accounting/AccountingCatalog.java` so the same account codes and names are reused consistently across automated journal entries and the journal screen.

#### Assets
- `10100` Cash in Bank
- `10300` Accounts Receivable
- `11500` Office Equipment
- `12000` Merchandise Inventory - Vegetables

#### Liabilities
- `20100` Accounts Payable
- `20200` Notes Payable

#### Owner's Equity
- `30100` Capital
- `30200` Drawing

#### Revenues
- `40100` Sales
- `40200` Sales Discount
- `40300` Sales Returns and Allowances

#### Costs
- `50100` Purchases
- `50200` Freight-In
- `50300` Purchase Discount
- `50400` Purchase Returns and Allowances
- `50500` Cost of Goods Sold (reserved for Module 2)

#### Expenses
- `60100` Taxes and Licenses Expense
- `60200` Freight-Out
- `60300` Rent Expense
- `60400` Utilities Expense
- `60500` Advertising Expense
- `60600` Salaries Expense
- `60700` Supplies Expense
- `60800` Inventory Loss - Spoilage/Waste

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
  - Debit `60800 Inventory Loss - Spoilage/Waste`
  - Credit `12000 Merchandise Inventory - Vegetables`
  - Write-off amount is calculated at the current pre-loss unit cost

## Main packages

- `inventory/data` - Room entities, DAOs, enums, and database singleton
- `inventory/service` - valuation engine and repository orchestration
- `inventory/accounting` - journal draft builders and chart-of-accounts constants

Legacy in-memory domain classes remain in the project for reference, but the launcher screen now uses the Room-backed flow.

## Current screens

`MainActivity` now serves as the dedicated **Lot** screen and supports:

- a cleaner lot-first layout with less instructional text
- lot creation from a modal opened above the table section
- a simple lot table with month and day-of-month filtering
- tapping a lot row to view its details in a compact table
- adding expenses or logging spoilage directly from the selected lot detail area
- keeping the `Journal` screen available from the top menu for the selected lot

`JournalActivity` now provides a dedicated **Journal** screen that:

- loads the latest or selected lot journal
- shows the current chart of accounts grouped by accounting classification for easier journalizing reference
- displays journal entries grouped by event
- shows debit and credit lines in entry-style cards with account, amount, memo, payment source, and provider details when available

## Run tests

```powershell
.\gradlew.bat test --console=plain
```

## Next likely improvements

- Add a dedicated providers master table
- Add RecyclerView-based lot and journal screens if the dataset becomes large
- Add Module 2 sale posting into `50100 COGS`
- Add Room migrations once the schema starts evolving

