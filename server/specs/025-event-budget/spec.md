# Feature Specification: Event Budget Endpoint

**Feature Branch**: `025-event-budget`
**Created**: 2026-05-21
**Status**: Draft
**Input**: User description: "Create a new endpoint to provide the budget of the event for organisers. We've all partnerships with the applied price according to their validated pack and the progress of the partnership. Return sum of paid partnerships, validated partnerships (paid included), diff between paid and validated to know what will come, non-validated partnerships (paid + validated included), and diff between validated and unvalidated. Then, by pack, return the list of partnerships with company name and the price applied for their pack."

## Clarifications

### Session 2026-05-21

- Q: Which "price" should be summed for each partnership? → A: Full total price: effective base (`packPriceOverride ?: basePrice`) + sum of optional option prices (each with `priceOverride` applied). Required options are already folded into the base price and are not added again.
- Q: Which pack defines the per-pack breakdown bucket? → A: Only `validatedPack()` (suggestion-approved-after-declined OR selected-validated-after-declined). Partnerships without a validated pack are excluded from the per-pack breakdown but still counted in totals.
- Q: What's the population for totals? → A: All non-declined partnerships of the event (mirrors `EventStats`). Declined partnerships are excluded entirely.
- Q: Route shape? → A: `GET /orgs/{orgSlug}/events/{eventSlug}/budget`, with `AuthorizedOrganisationPlugin`, mirroring `/stats`.
- Q: For non-validated partnerships, which pack should we use when computing their contribution to `totals.total`? → A: `validatedPack() → suggestionPack → selectedPack`. Partnerships with no pack at all contribute 0.
- Q: Test scope? → A: Contract test (single route test class) plus an end-to-end integration test exercising a full lifecycle.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View aggregate budget totals (Priority: P1)

As an organiser, I need a single view of the financial state of my event so that I can see at a glance how much has been paid, how much is contractually committed, and how much is still in the pipeline.

**Why this priority**: Aggregate numbers are the headline figure organisers need to manage cash flow and pipeline forecasting. Without them, the per-pack breakdown is just a list with no financial summary.

**Independent Test**: Can be fully tested by creating partnerships in each lifecycle state (non-validated with pack chosen, validated unpaid, validated paid, declined) and verifying that the response totals match expected sums per state.

**Acceptance Scenarios**:

1. **Given** an event with no partnerships, **When** I request the budget endpoint, **Then** the response returns `paid = 0`, `validated = 0`, `total = 0`, and both diffs as `0`.
2. **Given** an event with a single paid partnership at `priceApplied = 1000`, **When** I request the budget, **Then** `paid = 1000`, `validated = 1000`, `total = 1000`, `validated_minus_paid = 0`, `total_minus_validated = 0`.
3. **Given** an event with one paid partnership (1000), one validated-unpaid (500), and one non-validated-with-selected-pack (300), **When** I request the budget, **Then** `paid = 1000`, `validated = 1500`, `total = 1800`, `validated_minus_paid = 500`, `total_minus_validated = 300`.
4. **Given** an event also containing a declined partnership at `priceApplied = 999`, **When** I request the budget, **Then** that partnership is not included in any total.

---

### User Story 2 - View partnerships grouped by pack with lifecycle status (Priority: P2)

As an organiser, I need to see, for each sponsoring pack, the list of non-declined partnerships and the price each is paying along with their current lifecycle status (paid / validated / submitted), so that I can verify per-pack revenue, spot pricing inconsistencies, and see at a glance where each partnership is in the pipeline.

**Why this priority**: Per-pack breakdown is the second half of the user request and makes the aggregates auditable. Lower priority than US1 because the headline totals are usable on their own.

**Independent Test**: Can be fully tested by creating partnerships across two different packs in mixed lifecycle states (paid, validated, submitted) and verifying that each appears under its pricing pack with the correct `price_applied` and `status` values, while declined partnerships are excluded from the per-pack breakdown.

**Acceptance Scenarios**:

1. **Given** two validated partnerships on pack "Gold" and one validated partnership on pack "Silver", **When** I request the budget, **Then** the response contains two pack entries (Gold with two partnerships, Silver with one) and each partnership entry includes `partnership_id`, `company_name`, `price_applied`, and `status`.
2. **Given** a partnership on pack "Gold" with `packPriceOverride = 950000` (catalogue base = 800000) and one optional option of `100000`, **When** I request the budget, **Then** that partnership's `price_applied = 1050000` (override + optional option).
3. **Given** an event with no non-declined partnerships that have any pricing pack, **When** I request the budget, **Then** `packs` is an empty array.
4. **Given** a partnership that has selected a pack but is not yet validated, **When** I request the budget, **Then** that partnership appears in the `packs` array under its `selectedPack` with `status = "submitted"`, and also contributes to `totals.total`.
5. **Given** a partnership with `validatedAt` set and a `BillingEntity` row with `status = PAID`, **When** I request the budget, **Then** that partnership appears with `status = "paid"`.
6. **Given** a partnership with `validatedAt` set but no PAID billing row, **When** I request the budget, **Then** that partnership appears with `status = "validated"`.

---

### User Story 3 - Authorisation enforcement (Priority: P3)

As the system, I must restrict access to budget data to authorised organisation members so that confidential financial information is not leaked.

**Why this priority**: Authorisation is non-negotiable but already covered by the existing `AuthorizedOrganisationPlugin`. Listed here for explicit verification.

**Independent Test**: Can be fully tested by issuing a request without authentication, with authentication for an unrelated organisation, and with authentication for the owning organisation — asserting `401`, `401`, and `200` respectively. (The existing `AuthorizedOrganisationPlugin` returns `401` for both unauthenticated requests and authenticated non-members; the system does not distinguish between the two cases.)

**Acceptance Scenarios**:

1. **Given** a request without authentication, **When** I call the budget endpoint, **Then** the response is `401`.
2. **Given** an authenticated user who is not a member of the event's owning organisation, **When** I call the budget endpoint, **Then** the response is `401`.
3. **Given** an authenticated user who is a member of the owning organisation, **When** I call the budget endpoint, **Then** the response is `200` with the payload.

---

### Edge Cases

- A partnership has no `selectedPack`, no `suggestionPack`, and no `validatedPack()` (brand-new registration in transit) → contributes 0 to all totals and is absent from `packs`.
- A partnership is paid (`billing.status = PAID`) but `validatedAt` is null (data inconsistency) → the contract follows `validatedAt` for the `validated` bucket, so this partnership would be counted in `paid` but not in `validated`. This is acceptable: the invariant `paid ⊆ validated` is a business invariant, not a structural one. Tests will document the literal behaviour.
- A partnership has `packPriceOverride = 0` → contributes the sum of its optional options to its `priceApplied` (zero is a valid override per spec 017).
- A partnership has `validatedPack()` set but the partner has no optional options → `priceApplied = effectiveBasePrice` only.
- Two partnerships share the same company name (different companies) → both appear independently; sort order between them is undefined but stable within a request.
- The event slug does not exist → respond `404` with a `NotFoundException`-shaped message (consistent with `/stats`).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose `GET /orgs/{orgSlug}/events/{eventSlug}/budget` returning JSON with two top-level groups: `totals` and `packs`.
- **FR-002**: System MUST compute `price_applied` for a partnership as `(packPriceOverride ?: pack.basePrice) + Σ optionalOptions.totalPrice`, where each option's effective price uses `priceOverride ?: cataloguePrice` and quantity rules already defined in `PartnershipOptionEntity.toDomain` (text → 1; quantitative → selectedQuantity; number → fixedQuantity; selectable → selectedValue.price). This matches the existing `PartnershipPack.totalPrice` rule and MUST NOT diverge from it.
- **FR-003**: System MUST determine the pricing pack for a partnership using the order `validatedPack() → suggestionPack → selectedPack`; if all three are null the partnership contributes 0 to all totals.
- **FR-004**: `totals.paid` MUST equal the sum of `price_applied` over partnerships whose `BillingEntity.status == PAID`.
- **FR-005**: `totals.validated` MUST equal the sum of `price_applied` over partnerships with `validatedAt != null` AND `declinedAt == null`.
- **FR-006**: `totals.total` MUST equal the sum of `price_applied` over all non-declined partnerships that have a pricing pack (per FR-003).
- **FR-007**: `totals.validated_minus_paid` MUST equal `totals.validated - totals.paid`; `totals.total_minus_validated` MUST equal `totals.total - totals.validated`. The diffs are computed server-side and included in the response (not left to the client).
- **FR-008**: The `packs` array MUST contain one entry per distinct pricing pack (per FR-003) that has at least one non-declined partnership using it. Pack entries MUST include `pack_id`, `pack_name`, `base_price` (catalogue base price for reference), `totals` (same shape as the event-level `totals` per FR-004 through FR-007 but scoped to that pack's partnerships only), and `partnerships`. The sum of `pack.totals.total` across all packs equals the event-level `totals.total` (and likewise for `paid`, `validated`, and the two diff fields).
- **FR-009**: Each entry in `pack.partnerships` MUST contain `partnership_id`, `company_name`, `price_applied` (computed per FR-002 using that partnership's pricing pack), and `status` ∈ {`"paid"`, `"validated"`, `"submitted"`}. The status MUST be derived as: `"paid"` if a `BillingEntity` row exists with `status == PAID`; else `"validated"` if `validatedAt != null`; else `"submitted"`.
- **FR-010**: `packs` MUST be sorted by `pack_name` ascending; within each pack, `partnerships` MUST be sorted by `company_name` ascending (case-insensitive).
- **FR-011**: Declined partnerships (`declinedAt != null`) MUST be excluded from all totals and from `packs`.
- **FR-012**: The endpoint MUST require organisation-level authorisation via `AuthorizedOrganisationPlugin`, identically to `/orgs/{orgSlug}/events/{eventSlug}/stats`.
- **FR-013**: The endpoint MUST return `404` when the event slug does not exist.
- **FR-014**: All monetary values MUST be returned as integers in the same unit as `SponsoringPack.basePrice` (no decimals, no currency conversion). The response MUST include a top-level `currency` field with value `"EUR"`, matching the existing `PartnershipDetail.currency` default for forward compatibility.
- **FR-015**: The implementation MUST avoid N+1 queries by batch-loading billing rows, partnership options, and option translations for the full set of partnerships in a single transaction (mirroring the batched approach used in `EventStatsRepositoryExposed`).

### Key Entities

- **Event**: Identified by slug; the budget is scoped to a single event.
- **Partnership**: A relationship between a company and an event; carries lifecycle flags (`validatedAt`, `declinedAt`), pack references (`selectedPack`, `suggestionPack`, `packPriceOverride`), and a parent company.
- **Sponsoring Pack**: The pricing unit; has `basePrice`. The "validated pack" is determined by the existing `validatedPack()` helper.
- **Partnership Option**: A line item attached to a partnership; carries a possible `priceOverride` and a quantity/selectable value used to compute its effective price.
- **Billing**: Per-partnership invoice/quote state; only `status == PAID` counts as "paid" for this endpoint.
- **Budget Totals**: Aggregate of `priceApplied` across the event's non-declined partnerships, broken into `paid`, `validated`, `total`, and the two derived diffs.

## Assumptions

- The existing pricing rule (`PartnershipPack.totalPrice` and `PartnershipOptionEntity.toDomain`) is the source of truth; this endpoint mirrors it rather than introducing a parallel formula.
- Currency is uniform across the event and is `"EUR"`; multi-currency events are out of scope.
- Authorisation is satisfied entirely by `AuthorizedOrganisationPlugin`; no new permission concept is introduced.
- The endpoint is read-only and idempotent; no caching or pagination is required in v1 — events are expected to have under a few hundred partnerships, well within a single transaction.
- A partnership's `BillingEntity.status` is the only signal for "paid"; financial reconciliation outside billing is out of scope.
- The `total - validated` value can be negative only via the documented data-inconsistency edge case above; in normal operation the invariant `paid ⊆ validated ⊆ total` holds.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An organiser can retrieve the budget for any event they have access to in under 1 second for events with up to 200 partnerships.
- **SC-002**: 100% of partnerships in any of the five lifecycle states (non-validated-no-pack, non-validated-with-pack, validated-unpaid, validated-paid, declined) are categorised correctly per FR-004 through FR-011.
- **SC-003**: The two diff fields always equal their definitions (`validated - paid` and `total - validated`) for every response.
- **SC-004**: The implementation issues O(1) transaction calls per request (no N+1 patterns) for events with ≤ 200 partnerships, verified by review.
- **SC-005**: Unauthorised callers (no auth or non-member of owning organisation) receive `401` 100% of the time; authorised callers receive `200`.
