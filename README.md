# Kilgi

Kilgi is an Android app for vegetable vendors. The current implementation starts **Module 1: Landed Cost & Procurement (Inventory)** with an **interactive in-memory prototype** and no database yet.

## Implemented domain layer

The module lives under `app/src/main/java/com/example/kilgi/inventory/`.

### Models
- `ProcurementBatch` - represents one purchased vegetable batch / lot.
- `SpoilageEntry` - records spoilage or waste in kilograms.
- `MeasuredWholesaleUnits` - stores the actual kilogram weight of each purchased sack, crate, or pack.
- `UnitConversion` - converts sacks, crates, or other wholesale units into kilograms using actual measured unit weights.

### Calculation engines
- `LandedCostCalculator` - adds base item price, shipping, store delivery, raw sorting labor, and packaging/material cost.
- `TrueCostPerKiloEngine` - recalculates per-kilo cost based on remaining sellable kilograms after spoilage.

## Covered business rules
- Batch creation stores provider, product, wholesale quantity, and cost inputs.
- Batch weight is calculated from the actual measured weight of each purchased sack/crate, not from a fixed same-weight assumption.
- Landed cost is computed automatically from all procurement-related costs.
- Spoilage can be logged multiple times on the same batch.
- Spoilage cannot exceed the original batch weight.
- True cost per kilo increases automatically as sellable kilograms decrease.
- Wholesale unit conversion is configurable per vendor batch and supports different weights per purchased unit.

## Current screen behavior

- The launcher screen includes a **Batch Creation Form**.
- Users can enter actual sack/crate weights as comma-separated or newline-separated kilograms.
- Creating a batch recalculates the active batch summary in memory only.
- The same screen includes a **Spoilage / Waste Log** for the current active batch.
- Logging spoilage updates remaining sellable weight and true cost per kilo immediately.
- No values are persisted yet; everything resets when the app restarts.

## Run unit tests

```powershell
.\gradlew.bat test
```

## Next likely step
- Add a dynamic per-unit weight entry UI instead of a comma-separated text field.
- Keep persistence out for now, or later connect the same models to Room/database storage.

