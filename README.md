# 🥬 Kilgi

**Kilgi** is a specialized, Room-backed accounting and inventory ledger designed for **Carbon Market** vegetable vendors. It streamlines complex procurement, spoilage valuation, and wholesale settlements into a single mobile interface.

---

## 🚀 Core Functionalities

### 📦 Module 1: Inventory & Landed Cost
*   **Lot Management:** Normalized local database for tracking vegetable batches.
*   **Valuation Engine:** 
    *   Recomputes **Total Capitalized Cost** and **Net Usable Kilograms**.
    *   Calculates **True Cost Per Kilo** dynamically.
*   **Waste Tracking:** Logs spoilage with `NORMAL` (cost absorption) and `ABNORMAL` (write-off) valuation behaviors.
*   **Automated Accounting:** Persists double-entry journal entries for lot creation, additional expenses, and losses.

### 💰 Module 2: Sales & Settlements (In Progress)
*   **Unified Sales Engine:** Bulk retail cash entries and Wholesale on-account invoices.
*   **A/R & A/P Management:** 
    *   Customer payments and provider settlements.
    *   **FIFO Waterfall Allocator:** Automated matching of payments to invoices via `PaymentAllocationEngine`.
*   **Master Data:** Comprehensive records for `Users`, `Providers`, and `Customers`.

---

## 🏛️ Accounting Architecture

Kilgi maintains a centralized, merchandising-oriented **Chart of Accounts** (`AccountingCatalog.java`) to ensure consistency across automated postings and manual reviews.

### Primary Accounts
| Category | Accounts |
| :--- | :--- |
| **Assets** | Cash in Bank, A/R, Merchandise Inventory |
| **Liabilities** | Accounts Payable, Notes Payable |
| **OE** | Owner's Capital, Drawing |
| **Revenues** | Sales, Sales Discounts, Returns |
| **Expenses** | Rent, Utilities, Salaries, Inventory Loss (Spoilage) |

> [!NOTE]
> **Automated Journal Behavior:**
> - **Initial Lot:** Debits Inventory; Credits AP/Cash.
> - **Abnormal Spoilage:** Debits Inventory Loss; Credits Inventory (calculated at current unit cost).

---

## 🛠️ Technical Stack

- **Platform:** Android (Min SDK 24, Target SDK 36)
- **Database:** Room Persistence Library (Local First)
- **UI:** Material 3 with CoordinatorLayout & BottomNavigationView
- **Architecture:** 
    - `inventory/data`: Room Entities & DAOs
    - `inventory/service`: Valuation & Allocation Logic
    - `inventory/accounting`: Journal Draft Builders

---

## 🖥️ Screen Overview

1.  **Lot Screen (Main):** Compact table view with date filtering, modal-based creation, and integrated spoilage/expense logging.
2.  **Journal Screen:** Entry-style cards showing debits/credits grouped by event with full ledger transparency.
3.  **Financial Reports:** Real-time visibility into Income Statements, Balance Sheets, and Equity.

---

## 🔨 Development

### Running Tests
Execute the local unit tests to verify the valuation engine and FIFO allocator:
```powershell
.\gradlew.bat test --console=plain
```

### Roadmap
- [ ] Dedicated Customer/Provider ledger screens.
- [ ] SKU/Lot-specific attribution for Wholesale Invoices.
- [ ] Room migrations for schema evolution.
- [ ] COGS & Inventory depletion posting.
