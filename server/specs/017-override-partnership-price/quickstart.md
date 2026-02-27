# Quickstart: Override Partnership Pricing

**Feature**: `017-override-partnership-price`  
**Branch**: `017-override-partnership-price`

> Implement in the order shown below. Each step is independently buildable.

---

## Prerequisites

```bash
cd server
git checkout 017-override-partnership-price
./gradlew build --no-daemon   # verify baseline compiles
```

---

## Step 1 — DB columns (Table + Entity)

### `PartnershipsTable.kt`
Add **after** `organiserId`:
```kotlin
val packPriceOverride = integer("pack_price_override").nullable()
```

### `PartnershipOptionsTable.kt`
Add **after** `selectedValueId`:
```kotlin
val priceOverride = integer("price_override").nullable()
```

### `PartnershipEntity.kt`
Add property delegation (alongside the other `var` fields):
```kotlin
var packPriceOverride by PartnershipsTable.packPriceOverride
```

### `PartnershipOptionEntity.kt`
Add property delegation (alongside the other `var` properties):
```kotlin
var priceOverride by PartnershipOptionsTable.priceOverride
```

---

## Step 2 — Domain models

### `PartnershipPack.kt` — add `packPriceOverride` field

Before `requiredOptions`:
```kotlin
@SerialName("pack_price_override")
val packPriceOverride: Int? = null,
```

### `PartnershipOption.kt` — add abstract `priceOverride` + update all subtypes

In the sealed class, **after** `abstract val totalPrice`:
```kotlin
@SerialName("price_override")
abstract val priceOverride: Int?
```

Add concrete field to all four subtypes (`TextPartnershipOption`, `QuantitativePartnershipOption`, `NumberPartnershipOption`, `SelectablePartnershipOption`):
```kotlin
@SerialName("price_override")
override val priceOverride: Int? = null,
```

### New file: `UpdatePartnershipPricing.kt`

```kotlin
package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request for updating price overrides on a partnership.
 *
 * Partial update semantics:
 * - [packPriceOverride] absent from JSON → existing pack override unchanged
 * - [packPriceOverride] = null → clear existing pack override
 * - [packPriceOverride] = integer → set/replace pack override
 * - [optionsPriceOverrides] absent → all option overrides unchanged
 * - [optionsPriceOverrides] present → only listed option IDs are affected
 */
@Serializable
data class UpdatePartnershipPricing(
    @SerialName("pack_price_override")
    val packPriceOverride: Int? = null,
    @SerialName("options_price_overrides")
    val optionsPriceOverrides: List<OptionPriceOverride>? = null,
)

@Serializable
data class OptionPriceOverride(
    /** UUID of the option — must belong to this partnership. */
    val id: String,
    @SerialName("price_override")
    val priceOverride: Int? = null,
)
```

> ⚠️ **Partial-update note for `packPriceOverride`**: With `kotlinx.serialization`,
> there is no built-in way to distinguish "field omitted" from "field = null".
> For this endpoint the JSON schema validation (`call.receive<T>(schema)`) is the
> primary validation gate. The repository implementation should treat an absent
> `packPriceOverride` (resulting in Kotlin `null`) as "**do not update**" when
> the JSON body contains no `pack_price_override` key.
>
> The recommended approach is to parse the raw request body as `JsonObject` first,
> check for key presence, then deserialize selectively. See the repository step
> for implementation details.

---

## Step 3 — Update mappers

### `PartnershipOptionEntity.ext.kt`

In each `toX` private function, replace every occurrence of:
```kotlin
val price = option.price ?: 0
```
with:
```kotlin
val price = (priceOverride ?: option.price) ?: 0
```

For `SelectablePartnershipOption` (`toSelectablePartnershipOption`), replace:
```kotlin
totalPrice = selectedVal.price,
```
with:
```kotlin
totalPrice = priceOverride ?: selectedVal.price,
```

Pass `priceOverride = priceOverride` to each subtype constructor.

### `SponsoringPackEntity.ext.kt`

Update function signature:
```kotlin
internal fun SponsoringPackEntity.toDomain(
    language: String,
    partnershipId: UUID,
    packPriceOverride: Int? = null,   // NEW
): PartnershipPack
```

Update `totalPrice` computation:
```kotlin
val effectiveBasePrice = packPriceOverride ?: basePrice
val totalPrice = effectiveBasePrice + optionalOptions.sumOf { it.totalPrice }
```

Pass `packPriceOverride = packPriceOverride` to `PartnershipPack(...)`.

### `PartnershipRepositoryExposed.kt`

In each call to `pack.toDomain(...)` (4 locations — `getById`, `getByIdDetailed`, and wherever else it's called), add:
```kotlin
packPriceOverride = partnership.packPriceOverride,
```

---

## Step 4 — Repository interface + implementation

### `PartnershipRepository.kt` — add method

```kotlin
/**
 * Updates price overrides for the pack and/or options of a partnership.
 *
 * Partial update: only fields present in [pricing] are applied.
 *
 * @throws NotFoundException if event or partnership not found
 * @throws NotFoundException if any option ID in [pricing] does not belong to
 *   this partnership
 * @throws ConflictException if [pricing.packPriceOverride] is set but partnership
 *   has no validated sponsoring pack
 */
fun updatePricing(
    eventSlug: String,
    partnershipId: UUID,
    pricing: UpdatePartnershipPricing,
    packOverridePresent: Boolean,
): UUID
```

> The `packOverridePresent: Boolean` parameter distinguishes "field was in the
> JSON body" (true) from "field was absent" (false), enabling true partial-update
> semantics for the pack override.

### `PartnershipRepositoryExposed.kt` — implement

```kotlin
override fun updatePricing(
    eventSlug: String,
    partnershipId: UUID,
    pricing: UpdatePartnershipPricing,
    packOverridePresent: Boolean,
): UUID = transaction {
    val event = EventEntity.findBySlug(eventSlug)
        ?: throw NotFoundException("Event with slug $eventSlug not found")
    val partnership = PartnershipEntity.singleByEventAndPartnership(event.id.value, partnershipId)
        ?: throw NotFoundException("Partnership not found")

    // Pack override — only update if key was present in request JSON
    if (packOverridePresent) {
        if (pricing.packPriceOverride != null && partnership.validatedPack() == null) {
            throw ConflictException(
                "Cannot set pack price override: partnership has no validated sponsoring pack",
            )
        }
        partnership.packPriceOverride = pricing.packPriceOverride
    }

    // Option overrides — update only the listed option IDs
    pricing.optionsPriceOverrides?.forEach { override ->
        val optionId = override.id.toUUID()
        val optionEntity = PartnershipOptionEntity
            .find {
                (PartnershipOptionsTable.partnershipId eq partnershipId) and
                    (PartnershipOptionsTable.optionId eq optionId)
            }
            .singleOrNull()
            ?: throw NotFoundException(
                "Option $optionId is not associated with partnership $partnershipId",
            )
        optionEntity.priceOverride = override.priceOverride
    }

    partnership.id.value
}
```

---

## Step 5 — Route

In `PartnershipRoutes.kt`, add inside `orgsPartnershipRoutes()` or as a new private function. Recommended: new private function `orgsPartnershipPricingRoutes()`, registered in `partnershipRoutes()`.

```kotlin
private fun Route.orgsPartnershipPricingRoutes() {
    val repository by inject<PartnershipRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/pricing") {
        install(AuthorizedOrganisationPlugin)

        put {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId

            // Parse raw JSON to detect key presence for partial-update semantics
            val jsonBody = call.receiveText()
            val jsonObject = Json.parseToJsonElement(jsonBody).jsonObject
            val packOverridePresent = jsonObject.containsKey("pack_price_override")

            // Validate against schema then deserialize
            val pricing = call.receive<UpdatePartnershipPricing>(
                schema = "update_partnership_pricing_request.schema.json",
            )

            // Note: receive() above consumes the body; use jsonBody for detection
            // Alternative: use a custom schema validation step on jsonBody first,
            // then deserialize from jsonBody directly.

            repository.updatePricing(
                eventSlug = eventSlug,
                partnershipId = partnershipId.toUUID(),
                pricing = pricing,
                packOverridePresent = packOverridePresent,
            )

            val partnershipDetail = repository.getByIdDetailed(eventSlug, partnershipId.toUUID())
            call.respond(HttpStatusCode.OK, partnershipDetail)
        }
    }
}
```

> **Body double-read note**: `call.receiveText()` and `call.receive<T>()` both
> consume the body. Use `receiveText()` once, parse manually with
> `Json.decodeFromString<UpdatePartnershipPricing>(jsonBody)` after schema
> validation, rather than calling both `receiveText()` and `receive<T>()`.

Also register in `partnershipRoutes()`:
```kotlin
orgsPartnershipPricingRoutes()
```

---

## Step 6 — Billing integration

In `QontoInvoiceItem.mapper.kt`, update `invoiceItems()`:

```kotlin
// Pack line item
unitPrice = QontoMoneyAmount(
    value = "${pack.packPriceOverride ?: pack.basePrice}",
    currency = partnership.currency,
),

// Option line items
unitPrice = QontoMoneyAmount(
    value = "${option.priceOverride ?: option.price}",
    currency = partnership.currency,
),
```

---

## Step 7 — Schema files

Copy `contracts/update_partnership_pricing_request.schema.json` to:
```
application/src/main/resources/schemas/update_partnership_pricing_request.schema.json
```

Add `pack_price_override` to `partnership_pack.schema.json`:
```json
"pack_price_override": {
  "type": ["integer", "null"],
  "minimum": 0,
  "description": "Organiser-set price override for this pack. Null when no override is active."
}
```

Add `price_override` to `partnership_option.schema.json` (in the base/shared properties):
```json
"price_override": {
  "type": ["integer", "null"],
  "minimum": 0,
  "description": "Organiser-set price override for this option. Null when no override is active."
}
```

---

## Step 8 — Tests

### Contract test (write FIRST per TDD):
`application/src/test/kotlin/.../partnership/infrastructure/api/PartnershipPricingRoutePutTest.kt`

Tests to cover:
- `PUT` with valid pack override → 200 with updated `pack_price_override` in response
- `PUT` with invalid (negative) price → 400
- `PUT` with unknown option ID → 404
- `PUT` without auth → 401
- `PUT` with wrong org → 403
- `PUT` with pack override when no validated pack → 409

### Integration test:
`application/src/test/kotlin/.../partnership/PartnershipPricingRoutesTest.kt`

Tests to cover:
- Set pack override → billing invoice uses override price
- Set option override → billing invoice uses override price
- Clear pack override → billing uses catalogue price again
- Omit pack override field → existing override preserved

---

## Verification

```bash
cd server
./gradlew ktlintFormat --no-daemon
./gradlew detekt --no-daemon
./gradlew test --no-daemon
npm install && npm run validate
./gradlew build --no-daemon
```
