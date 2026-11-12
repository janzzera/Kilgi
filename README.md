# 🥬 Kilgi - Offline-First Accounting & Landed Cost Inventory Management System

**Kilgi** is a specialized, Room-backed, offline-first mobile accounting and landed-cost inventory management application designed for vegetable vendors, wholesalers, and merchandising businesses. It combines real-time batch valuation, FIFO waterfall payment settlements, automated double-entry bookkeeping, background sync with a REST API, and secure hybrid online/offline authentication into a clean, modern Material 3 interface.

---

## 🌟 Key Application Features

### 👤 1. Authentication & Security
- **Initial Account Setup (`UserSetupActivity`)**:
  - Requires an active internet connection (`NetworkUtils`) to register the user directly on the cloud REST API / Supabase server.
  - Collects username, display name, business name, optional email/mobile, and password with local password confirmation matching.
  - Server-side password hashing and salt generation stored back to the local database.
- **Hybrid Online/Offline Login (`LoginActivity`)**:
  - **24-Hour Token Expiry**: Tracks JWT token issuance timestamps in `TokenManager`.
  - **Online Login**: If online and the JWT token is missing or expired (>= 24 hours old), performs REST API authentication (`POST /api/auth/login`), saving the returned JWT token.
  - **Offline Login**: If offline or if the token is still valid (< 24 hours old), performs fast local password verification using Base64 HMAC-SHA512 salt verification (`PasswordHasher`).
- **Encrypted Preferences (`TokenManager`)**:
  - Securely stores JWT tokens using Android `EncryptedSharedPreferences` with AES256-GCM encryption.
  - `AuthInterceptor` automatically attaches HTTP `Authorization: Bearer <token>` headers to all outgoing Retrofit REST API calls.

---

### 📦 2. Procurement & Landed Cost Inventory (`MainActivity`)
- **Procurement Batch Tracking**:
  - Record vegetable purchase lots with supplier, vegetable type, sack count, raw kilograms, base unit price, and payment source (Cash or Accounts Payable).
  - Track standard freight and shipping payment source.
- **Dynamic Landed Cost Valuation Engine (`BatchValuationEngine`)**:
  - Calculates **Total Capitalized Cost** (Purchase Price + Standard Freight + Additional Landed Cost Expenses).
  - Recomputes **Net Usable Kilograms** (Raw Kilos - Normal Shrinkage - Abnormal Losses).
  - Computes the true **Cost Per Kilo** dynamically in real time.
- **Landed Cost Expense Allocation**:
  - Add landed expenses (Freight-In, Storage, Handling) directly to procurement lots.
- **Dual-Mode Spoilage & Waste Tracking**:
  - **Normal Shrinkage**: Absorbs cost into remaining inventory, inflating unit cost to reflect expected weight loss.
  - **Abnormal Spoilage**: Calculates explicit inventory loss and posts an immediate write-off entry to Account `60800` (Inventory Loss).
- **Interactive Lot Management**:
  - Date range filtering, page navigation, sorting by date, and detailed lot summaries.

---

### 💵 3. Merchandising Sales & FIFO Settlements (`SalesActivity`)
- **Retail EOD Sales**:
  - Log daily retail cash sales with instant revenue and cash ledger postings.
- **Wholesale On-Account Invoicing**:
  - Create wholesale customer invoices with auto-generated invoice numbers (`INV-YYYYMMDD-HHMMSS`).
- **FIFO Waterfall Settlement Engine (`PaymentAllocationEngine`)**:
  - **Customer Collections**: Collect payments from buyers and automatically allocate them across open invoices using FIFO (First-In, First-Out) waterfall logic.
  - **Provider Settlements**: Pay supplier accounts payable and automatically allocate payments across open procurement lots using FIFO waterfall.
- **Receivables & Payables Snapshot**:
  - Real-time visibility into total outstanding Accounts Receivable (A/R) and Accounts Payable (A/P) balances per customer and provider.

---

### 📖 4. Automated Double-Entry Bookkeeping (`JournalActivity`)
- **Automated Entry Generation (`JournalEntryFactory`)**:
  - Automatically posts balanced debit and credit journal entries for Lot Purchases, Landed Cost Expenses, Abnormal Spoilage Write-Offs, Retail Sales, Wholesale Invoices, Customer Collections, Provider Settlements, and Manual Adjustments.
- **Comprehensive Ledger Transparency**:
  - Monthly period filtering and page navigation.
  - Detailed debit and credit line breakdown for every event.
- **Interactive T-Accounts & Trial Balance**:
  - View period activity and net balances for all active accounts.
  - Real-time Trial Balance verifying total debits equal total credits.
- **Manual Adjusting Entries**:
  - Post custom manual adjustments with debit account, credit account, amount, and memo.

---

### 📊 5. Financial Reporting (`ReportsActivity`)
Generates real-time, professional financial statements for any selected accounting period:
- **Income Statement**:
  - Total Revenues (Sales)
  - Cost of Goods Sold (COGS) & Inventory Loss
  - Operating Expenses (Rent, Salaries, Utilities, etc.)
  - Net Income / Net Loss calculation
- **Statement of Changes in Owner's Equity**:
  - Beginning Capital, Net Income/Loss, Drawings, and Ending Capital
- **Balance Sheet**:
  - Current Assets (Cash, A/R, Merchandise Inventory)
  - Non-Current Assets (Equipment, Accumulated Depreciation)
  - Current Liabilities (Accounts Payable)
  - Owner's Equity

---

### 📅 6. Accounting Period Management (`PeriodManagementActivity`)
- **Period Lifecycle**: Create accounting periods with start and end dates.
- **Pre-Close Verification Checklist**: Ensure spoilage logs and bank reconciliations are completed prior to period closure.
- **Period Locking**: Lock closed accounting periods to freeze past ledger transactions and prevent unauthorized modifications.

---

### 🔄 7. Offline-First Architecture & Sync Engine
- **Local Room Caching**:
  - All 15 database entities are saved locally first in SQLite via Room (`KilgiDatabase`).
  - Fast, responsive UI with zero offline lag.
- **Background Sync Engine (`DataSyncWorker` & `SyncManager`)**:
  - Offline writes are marked with `isSynced = false`.
  - Android `WorkManager` monitors network connectivity (`NetworkType.CONNECTED`).
  - When internet is available, `DataSyncWorker` automatically uploads unsynced offline records to the REST API via `POST` requests and updates local `isSynced = true` flags.
- **Network Data Fetching (`NetworkBoundResource`)**:
  - Abstract state wrapper returning `Resource<T>` (`LOADING`, `SUCCESS`, `ERROR`).
  - Automatically fetches fresh data from the REST API, updates local Room cache, and emits fresh state to ViewModels.
- **Pull-To-Refresh (`SwipeRefreshLayout`)**:
  - Swipe down from the top on any screen (`MainActivity`, `SalesActivity`, `JournalActivity`, `ReportsActivity`, `PeriodManagementActivity`) to trigger instant background sync with the REST API and refresh local UI data.

---

## 🏛️ System Architecture

```
com.example.kilgi
├── MainActivity.java                  # Lot Management & Procurement Dashboard
├── SalesActivity.java                 # Sales, Invoicing & FIFO Settlement Dashboard
├── JournalActivity.java               # Automated Ledger, T-Accounts & Trial Balance
├── ReportsActivity.java               # Income Statement, Equity & Balance Sheet
├── PeriodManagementActivity.java      # Accounting Period Management & Period Locking
├── UserSetupActivity.java             # Internet-Required Initial Setup & Registration
├── LoginActivity.java                 # Hybrid Online/Offline Login
└── inventory
    ├── data                           # Room Entities, DAOs, API Request/Response Models
    │   ├── api                        # ApiService, AuthResponse, LoginRequest
    │   ├── common                     # Resource, Status, AppExecutors
    │   └── TokenManager.java          # EncryptedSharedPreferences & JWT Storage
    ├── repository                     # Domain Repositories (User, Provider, Customer, Lot, Sales, Journal, Period)
    ├── viewmodel                      # ViewModels exposing LiveData<Resource<T>>
    ├── network                        # ApiClient, AuthInterceptor, NetworkBoundResource, NetworkUtils
    ├── sync                           # DataSyncWorker & SyncManager (WorkManager)
    ├── security                       # PasswordHasher (Base64 HMAC-SHA512)
    ├── service                        # BatchValuationEngine, PaymentAllocationEngine, LotFilterUtils
    └── accounting                     # AccountingCatalog, JournalEntryFactory, LedgerEntryDraft
```

---

## 📋 Chart of Accounts (`AccountingCatalog.java`)

| Account Code | Account Name | Account Type | Normal Balance |
| :--- | :--- | :--- | :--- |
| **10100** | Cash in Bank | Current Asset | Debit |
| **10200** | Accounts Receivable | Current Asset | Debit |
| **10300** | Merchandise Inventory | Current Asset | Debit |
| **15100** | Equipment | Non-Current Asset | Debit |
| **15200** | Accumulated Depreciation | Contra Asset | Credit |
| **20100** | Accounts Payable | Current Liability | Credit |
| **30100** | Owner's Capital | Equity | Credit |
| **30200** | Owner's Drawing | Equity | Debit |
| **40100** | Sales Revenue | Revenue | Credit |
| **50100** | Cost of Goods Sold | Expense | Debit |
| **60100** | Freight-In | Landed Expense | Debit |
| **60200** | Storage & Warehousing | Landed Expense | Debit |
| **60300** | Handling & Porterage | Landed Expense | Debit |
| **60800** | Inventory Loss (Spoilage) | Operating Expense | Debit |
| **60900** | Operating Expenses | Operating Expense | Debit |

---

## 🛠️ Tech Stack & Dependencies

- **Language & SDK**: Java (Android SDK Target 36, Min SDK 24)
- **Local Persistence**: Room (`androidx.room:room-runtime:2.6.1`)
- **Networking**: Retrofit 2 (`com.squareup.retrofit2:retrofit:2.11.0`) + OkHttp 4 (`com.squareup.okhttp3:okhttp:4.12.0`)
- **Background Work**: WorkManager (`androidx.work:work-runtime:2.9.1`)
- **Security**: EncryptedSharedPreferences (`androidx.security:security-crypto:1.1.0`)
- **UI Components**: Material Components (`com.google.android.material:material`), SwipeRefreshLayout (`androidx.swiperefreshlayout:swiperefreshlayout:1.1.0`), ConstraintLayout
- **Architecture Components**: ViewModel, LiveData, MediatorLiveData

---

## 🧪 Running Unit Tests

To execute the test suite (verifying batch landed-cost calculations, FIFO payment allocation engines, and accounting rules):

```powershell
.\gradlew.bat testDebugUnitTest --console=plain
```
