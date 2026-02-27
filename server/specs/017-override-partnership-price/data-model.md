# Data Model: Override Partnership Pricing

**Feature**: `017-override-partnership-price`  
**Date**: 2026-02-27

---

## Database Schema Changes

### `PartnershipsTable` — new column

```kotlin
val packPriceOverride = integer("pack_price_override").nullable()
```

| Column | Type | Nullable | Default | Constraint |
|---|---|---|---|---|
| `pack_price_override` | `INTEGER` | ✅ | `NULL` | `CHECK (pack_price_override >= 0)` |

`NULL` → no override active; catalogue `base_price` is used.  
`0` → valid override (complimentary pack).

---

### `PartnershipOptionsTable` — new column

```kotlin
val priceOverride = integer("price_override").nullable()
```

| Column | Type | Nullable | Default | Constraint |
|---|---|---|---|---|
| `price_override` | `INTEGER` | ✅ | `NULL` | `CHECK (price_override >= 0)` |

`NULL` → no override active; catalogue `option.price` is used.

---

## Entity Changes

### `PartnershipEntity` — new property

```kotlin
var packPriceOverride by PartnershipsTable.packPriceOverride
```

### `PartnershipOptionEntity` — new property

```kotlin
var priceOverride by PartnershipOptionsTable.priceOverride
```

---

## Domain Model Changes

### `PartnershipPack` — new field + effective `totalPrice`

```kotlin
@Serializable
class PartnershipPack(
    val id: String,
    val name: String,
    @SerialName("base_price")
    val basePrice: Int,                          // catalogue price (never mutated)
    @SerialName("pack_price_override")
    val packPriceOverride: Int? = null,          // NEW – null if no override active
    @SerialName("required_options")
    val requiredOptions: List<PartnershipOption>,
    @SerialName("optional_options")
    val optionalOptions: List<PartnershipOption>,
    @SerialName("total_price")
    val totalPrice: Int,                         // (packPriceOverride ?: basePrice) + Σ optionalOptions.totalPrice
)
```

**Effective pack price** = `packPriceOverride ?: basePrice`

---

### `PartnershipOption` sealed class — new abstract field + effective `totalPrice`

New abstract field added to the sealed class:

```kotlin
@SerialName("price_override")
abstract val priceOverride: Int?          // NEW – null if no override active
```

Added to all four concrete subtypes:
- `TextPartnershipOption`
- `QuantitativePartnershipOption`
- `NumberPartnershipOption`
- `SelectablePartnershipOption`

**Effective option price** = `priceOverride ?: price`

`totalPrice` for each subtype is recomputed:

| Subtype | Old `totalPrice` | New `totalPrice` |
|---|---|---|
| `TextPartnershipOption` | `option.price ?: 0` | `(priceOverride ?: option.price) ?: 0` |
| `QuantitativePartnershipOption` | `price * quantity` | `(priceOverride ?: price) * quantity` |
| `NumberPartnershipOption` | `price * fixedQty` | `(priceOverride ?: price) * fixedQty` |
| `SelectablePartnershipOption` | `selectedValue.price` | `priceOverride ?: selectedValue.price` |

---

### New domain request: `UpdatePartnershipPricing`

```kotlin
@Serializable
data class UpdatePartnershipPricing(
    @SerialName("pack_price_override")
    val packPriceOverride: Int? = null,           // omit = no change; null = clear; integer = set
    @SerialName("options_price_overrides")
    val optionsPriceOverrides: List<OptionPriceOverride>? = null,  // omit = no change
)

@Serializable
data class OptionPriceOverride(
    val id: String,                               // option UUID (must belong to this partnership)
    @SerialName("price_override")
    val priceOverride: Int? = null,               // null = clear this option's override
)
```

**Partial-update logic in repository:**
- `packPriceOverride` field in request JSON → set/clear pack override.  
  Absent from JSON (default `null` used) → **no change** to existing value — handled via an explicit "was the field present in the request?" sentinel. (Implementation uses a wrapper type or JSON element inspection, see quickstart.md.)
- `optionsPriceOverrides` absent → no change to any option override.  
  Present but empty list → no changes.  
  Present with entries → only listed option IDs are updated.

---

## Mapper Changes

### `SponsoringPackEntity.ext.kt` — accept override parameter

New signature:

```kotlin
internal fun SponsoringPackEntity.toDomain(
    language: String,
    partnershipId: UUID,
    packPriceOverride: Int? = null,       // NEW
): PartnershipPack
```

`totalPrice` computation:

```kotlin
val effectiveBasePrice = packPriceOverride ?: basePrice
val totalPrice = effectiveBasePrice + optionalOptions.sumOf { it.totalPrice }
```

### `PartnershipOptionEntity.ext.kt` — use effective price

All private mapping functions updated to derive price as:

```kotlin
val price = (priceOverride ?: option.price) ?: 0
```

`totalPrice` uses `price` (the effective value) everywhere.

---

## Billing Integration

### `QontoInvoiceItem.mapper.kt` — `invoiceItems()`

Before:
```kotlin
unitPrice = QontoMoneyAmount(value = "${pack.basePrice}", ...)
// option:
unitPrice = QontoMoneyAmount(value = "${option.price}", ...)
```

After:
```kotlin
unitPrice = QontoMoneyAmount(value = "${pack.packPriceOverride ?: pack.basePrice}", ...)
// option:
unitPrice = QontoMoneyAmount(value = "${option.priceOverride ?: option.price}", ...)
```

No changes required elsewhere in the billing pipeline.

---

## Validation Rules

| Field | Rule |
|---|---|
| `packPriceOverride` | Non-negative integer when present (`>= 0`) |
| `optionsPriceOverrides[].id` | Valid UUID; must correspond to an option belonging to this partnership |
| `optionsPriceOverrides[].priceOverride` | Non-negative integer OR null (clear override) |
| Pack must exist | If `packPriceOverride` is set and partnership has no validated pack → 409 Conflict |

---

## State Transitions

```
PartnershipOption.priceOverride
  NULL ──[set override]──► Integer
  Integer ──[clear override]──► NULL
  Integer ──[update override]──► new Integer

PartnershipsTable.pack_price_override
  NULL ──[set override]──► Integer
  Integer ──[clear override]──► NULL
  Integer ──[update override]──► new Integer
```

No partnership lifecycle status constraint (FR-014: overrides allowed regardless of status).
