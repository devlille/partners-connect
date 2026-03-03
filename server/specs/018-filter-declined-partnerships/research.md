# Research: Filter Partnerships by Declined Status

**Feature Branch**: `018-filter-declined-partnerships`
**Phase**: 0 - Research & Unknown Resolution
**Date**: March 2, 2026

All NEEDS CLARIFICATION items from the spec have been resolved through direct codebase inspection.

---

## RES-001: Declined partnership data model

**Unknown**: How is "declined" represented in the database — status enum or dedicated field?

**Decision**: `declinedAt` nullable datetime column in `PartnershipsTable`

**Rationale**: Inspecting `PartnershipsTable.kt` reveals `val declinedAt = datetime("declined_at").nullable()`. Partnership decline is recorded as a timestamp, not an enum. A partnership is declined when `declinedAt IS NOT NULL`.

**Alternatives considered**: Status enum — not present in codebase.

**Critical note**: `suggestionDeclinedAt` also exists on the same table and represents the decline of a pack suggestion (a separate concept). The filter MUST operate exclusively on `declinedAt` and NOT on `suggestionDeclinedAt`.

---

## RES-002: Current default behaviour for declined partnerships

**Unknown**: Does the existing GET partnerships endpoint currently exclude or include declined partnerships?

**Decision**: Currently included — declining is a breaking change in default behaviour.

**Rationale**: Inspecting `PartnershipEntity.filters()` in `PartnershipEntity.kt` confirms there is no `declinedAt` filter clause. All partnerships for the event are returned regardless of `declinedAt`. The spec intentionally makes "exclude declined" the new default, which is a deliberate breaking change.

**Alternatives considered**: Adding the filter as purely opt-in (no default exclusion) — rejected per user requirement.

---

## RES-003: PartnershipFilters field type design

**Unknown**: Should `declined` be a `Boolean?` (defaulting to null = no filter) like other filters, or `Boolean` (defaulting to false = exclude declined)?

**Decision**: `Boolean` with default `false` (non-nullable)

**Rationale**: All other boolean filters use `Boolean?` where `null` means "apply no constraint." For `declined`, the desired default behaviour is to actively exclude declined partnerships (i.e., always add a `declinedAt IS NULL` constraint unless `declined = true`). Using a non-nullable `Boolean` with `default = false` cleanly encodes this intent and avoids null ambiguity. This makes the PartnershipFilters default constructor `PartnershipFilters()` automatically exclude declined, matching the spec.

**Alternatives considered**: `Boolean?` with `null` → exclude (treated same as `false` in the repository) — workable but less explicit. Rejected in favour of the more explicit non-nullable approach.

---

## RES-004: Boolean query parameter validation

**Unknown**: Does the existing codebase validate that boolean query parameters contain only "true" or "false"? Can we reuse existing infrastructure?

**Decision**: No existing validation — add a shared parsing helper.

**Rationale**: Inspecting `PartnershipRoutes.kt` shows boolean params are parsed as `queryParam?.toBoolean()`. Kotlin's `String.toBoolean()` returns `false` for any string that is not case-insensitively "true" — so `"garbage"` silently becomes `false`, violating FR-005. A helper function that accepts only `"true"` and `"false"` (case-insensitive) must be added. The logical place is `StringValues.ext.kt` in the partnership `infrastructure/api` package (where other parameter helpers live) or a shared utility, alongside adding the `filter[declined]` parsing.

**Alternatives considered**: Manual inline validation in each route — rejected (duplication). Schema-based validation — not applicable for query parameters in this codebase's current setup.

---

## RES-005: Metadata (filters) contract

**Unknown**: Must `filter[declined]` be added to the `PaginationMetadata.filters` array?

**Decision**: Yes — add `FilterDefinition("declined", FilterType.BOOLEAN)` to `buildMetadata()`.

**Rationale**: `PartnershipRepositoryExposed.buildMetadata()` already constructs a `PaginationMetadata` object listing every supported filter (validated, suggestion, paid, agreement-generated, agreement-signed, organiser). FR-011 in the spec requires `filter[declined]` to be listed there. The existing `FilterType.BOOLEAN` type is the correct value.

**Alternatives considered**: Skip metadata update — rejected (violates spec 015 contract and FR-011).

---

## RES-006: Email endpoint NotFoundException vs HTTP 204

**Unknown**: The email route currently throws `NotFoundException` when no partnerships match. The spec says HTTP 204 for zero recipients. How should the `filter[declined]` default interact with this?

**Decision**: The empty-results response behaviour for the email endpoint is unchanged by this feature. The filter simply narrows (or widens with `declined=true`) the recipient set. If the result is empty, the existing `NotFoundException` → HTTP 404 still applies.

**Rationale**: Looking at `PartnershipEmailRoutes.kt`, the line `throw NotFoundException("No partnerships match...")` maps to HTTP 404 via `StatusPages`. The spec's FR-010 states HTTP 204 but the existing endpoint returns 404. To avoid scope creep, we align with the existing behaviour. (The spec 014 clarification noted "Return HTTP 204 No Content when no partnerships match" but the current implementation throws NotFoundException → 404. This discrepancy predates this feature.)

**Important**: This was noted in the spec as FR-010 / SC — the plan will implement the `declined` filter only and leave the 204 vs 404 discrepancy as a known pre-existing nuance, not a regression introduced by this feature.

---

## RES-007: Impact on PartnershipEntity.filters() signature

**Unknown**: Since `PartnershipEntity.filters()` is a shared companion method used by both the list repository and the email repository, can we add a `declined` parameter safely?

**Decision**: Add `declined: Boolean = false` parameter with default value.

**Rationale**: The `filters()` companion method signature in `PartnershipEntity.kt` currently has 7 parameters. Adding `declined: Boolean = false` as an 8th parameter with a default value is backward-compatible — existing call sites continue to work with the old behaviour (exclude declined by default, which is the new desired default). Both `PartnershipRepositoryExposed` and `PartnershipEmailRepositoryExposed` call this method and will be updated to pass `filters.declined`.

---

## Summary of files to change

| File | Change |
|------|--------|
| `partnership/domain/PartnershipItem.kt` | Add `declined: Boolean = false` to `PartnershipFilters` |
| `partnership/infrastructure/db/PartnershipEntity.kt` | Add `declined: Boolean = false` to `filters()` + declinedAt constraint |
| `partnership/application/PartnershipRepositoryExposed.kt` | Pass `declined = filters.declined`; add `FilterDefinition("declined", ...)` |
| `partnership/application/PartnershipEmailRepositoryExposed.kt` | Pass `declined = filters.declined` |
| `partnership/infrastructure/api/PartnershipRoutes.kt` | Parse `filter[declined]` with validation, default `false` |
| `partnership/infrastructure/api/PartnershipEmailRoutes.kt` | Parse `filter[declined]` with validation, default `false` |
| `resources/openapi/openapi.yaml` | Add `filter[declined]` param to GET partnerships and POST email endpoints |
| New: `PartnershipDeclinedFilterRoutesTest.kt` | Integration test |
| New: `PartnershipListDeclinedFilterRouteGetTest.kt` | Contract test |
