# Research: Override Partnership Pricing

**Feature**: `017-override-partnership-price`  
**Date**: 2026-02-27

---

## 1. Existing Price Flow (as-is)

### Decision
Catalogue prices live exclusively in the sponsoring domain (`SponsoringPacksTable`, `SponsoringOptionsTable`). They are read-only from the partnership domain's perspective.

### Rationale
The two places where prices enter the partnership domain are:

1. **Mapper `SponsoringPackEntity.ext.kt`** — builds `PartnershipPack` using `entity.basePrice` and delegates to option mappers for option prices. `totalPrice` = `basePrice + sum(optionalOptions.totalPrice)`.  
2. **Mapper `PartnershipOptionEntity.ext.kt`** — builds each `PartnershipOption` subtype using `option.price ?: 0`. `totalPrice` = `price * quantity` (or `price * fixedQuantity`, or `selectedValue.price` for selectable).

### Billing touchpoint
`QontoInvoiceItem.mapper.kt` (`invoiceItems()`) reads prices from the `PartnershipDetail.validatedPack` domain object:
- Pack line item uses `pack.basePrice`
- Each optional option line item uses `option.price`

These are the **only two places** that emit prices to Qonto.

---

## 2. Storage Strategy for Overrides

### Decision
Store overrides on the partnership side, not by mutating catalogue data:

| Override type | Table | Column |
|---|---|---|
| Pack price override | `PartnershipsTable` | `pack_price_override INTEGER NULLABLE` |
| Option price override | `PartnershipOptionsTable` | `price_override INTEGER NULLABLE` |

### Rationale
- **Catalogue prices are shared** across multiple partnerships; they must never be mutated.
- Storing per-partnership overrides alongside the existing partnership rows keeps the model simple — no new junction tables required.
- `NULL` = "no override; use catalogue price". Explicit `0` = valid free-of-charge override.

### Alternatives considered
- **New `PartnershipPriceOverride` junction table**: More normalised but adds unnecessary join complexity for what is essentially a single nullable column per entity.
- **Stored effective price (snapshot)**: Breaks auditability and reversal capability; ruled out.

---

## 3. Domain Model Changes

### Decision
Both `PartnershipPack` and `PartnershipOption` (sealed class + subtypes) gain an optional `priceOverride` field exposing the raw override to front-end consumers. `totalPrice` is recomputed using the effective price.

| Field added to | Kotlin field | JSON field | Nullable |
|---|---|---|---|
| `PartnershipPack` | `packPriceOverride: Int?` | `pack_price_override` | yes |
| `PartnershipOption` (all subtypes) | `priceOverride: Int?` | `price_override` | yes |

The **effective prices** consumed by billing are:
- Pack: `packPriceOverride ?: basePrice`
- Option: `priceOverride ?: price`

`totalPrice` on `PartnershipPack` is updated to use effective pack price + effective option prices.  
`totalPrice` on each `PartnershipOption` subtype is updated to use `(priceOverride ?: price) * quantity`.

### Rationale
FR-010 requires both values to be visible to front-end. FR-013 requires billing to use only the effective price. Carrying both fields in the domain model satisfies both consumers without a separate DTO.

---

## 4. API Endpoint Design

### Decision
New dedicated endpoint under the org-secured namespace:

```
PUT /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/pricing
```

Protected by `AuthorizedOrganisationPlugin`. Returns `PartnershipDetail` (same as the public GET).

### Rationale
- The existing `PUT /events/{eventSlug}/partnerships/{partnershipId}` is **public** (no authentication). Mixing organiser-only pricing fields into it would break FR-011.  
- The existing `DELETE /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}` sets the precedent for org-scoped operations on a partnership.
- A dedicated `/pricing` sub-resource follows the existing pattern of `/organiser`, `/billing`, `/agreement`, etc.

### Alternatives considered
- Extending the public PUT: Security violation — price overrides are organiser-only (FR-011).
- PATCH on the existing org DELETE route: Inelegant and mixes unrelated concerns.

---

## 5. Partial Update Semantics for Pricing

### Decision
- **`packPriceOverride`**: Omitting the field in the request body leaves the existing DB override unchanged. Sending `null` explicitly clears the override. Sending a non-negative integer sets/replaces it.
- **`optionsPriceOverrides`**: Omitting the list entirely leaves all option overrides unchanged. Including the list replaces overrides only for the specified option IDs. To clear a specific option's override, include that option ID with `priceOverride: null`.

### Rationale
This matches the partial-update pattern established by `UpdatePartnershipContactInfo` (FR-006).

---

## 6. Billing Integration (Qonto)

### Decision
Update `QontoInvoiceItem.mapper.kt` (`invoiceItems()`) to use effective prices:
- Pack: `pack.packPriceOverride ?: pack.basePrice`
- Option: `option.priceOverride ?: option.price`

No changes to `QontoProvider`, `QontoBillingGateway`, or `BillingRepositoryExposed` — the effective price is resolved in the domain object before it reaches the mapper.

### Rationale
The billing layer already receives a fully hydrated `PartnershipDetail` from the partnership domain. Resolving effective prices in the mapper is a one-line change per line item with zero structural impact upstream. Keeps billing code oblivious to the concept of "overrides".

---

## 7. No Notification Required

### Decision
No Slack / Mailjet notification is triggered by price override operations.

### Rationale
Price overrides are an internal organiser operation (clarification Q5). No `NotificationVariables` subtype needs to be added.

---

## 8. Test Strategy

| Test class | Type | Location |
|---|---|---|
| `PartnershipPricingRoutePutTest` | Contract | `partnership.infrastructure.api` |
| `PartnershipPricingRoutesTest` | Integration | `partnership` (root) |

Factories updated: `Partnership.factory.kt` (add `packPriceOverride` param), `PartnershipOption.factory.kt` (add `priceOverride` param).
