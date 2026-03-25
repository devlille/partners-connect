# Research: Option Usage Count

**Feature**: 022-option-usage-count
**Date**: 2026-03-25

## R1: Wrapper vs Inline Field Approach

**Decision**: Wrapper object per list item (`{"option":{...}, "partnership_count": N}`)

**Rationale**: `SponsoringOptionWithTranslations` is a sealed class with `@JsonClassDiscriminator("type")` and 4 subtypes (Text, TypedQuantitative, TypedNumber, TypedSelectable). This class is shared by the sponsoring pack endpoint (`GET /orgs/{orgSlug}/events/{eventSlug}/packs`), where partnership counts should not appear. Modifying the sealed class would leak the count field into unrelated endpoints.

**Alternatives considered**:
- **Inline field on sealed class**: Rejected — would pollute pack endpoint responses and require all 4 subtypes to add the field. Clarified with user during `/speckit.clarify`.
- **Separate count endpoint**: Rejected — requires extra HTTP round-trip; wrapping is simpler and more efficient.

## R2: Reuse of Feature 021 Counting Logic

**Decision**: Reuse the same `validatedPack()` → `PackOptionsTable` → option association pattern from feature 021's `getOptionByIdWithPartners()`.

**Rationale**: Feature 021 already established the canonical logic for determining which partnerships are associated with an option:
1. Query `PackOptionsTable` to find pack IDs that contain the option
2. Load all event partnerships via `PartnershipEntity.find { PartnershipsTable.eventId eq event.id.value }`
3. Filter in-memory using `partnership.validatedPack()` (returns validated pack or null)
4. Check if the validated pack ID is in the set of pack IDs for the option

For the list endpoint, this is adapted to compute counts for ALL options in a single pass rather than per-option.

**Alternatives considered**:
- **SQL-only count**: Rejected — `validatedPack()` is an in-memory function on `PartnershipEntity` that involves complex pack validation logic; it cannot be expressed as a single SQL query.
- **Per-option query (N+1)**: Rejected — would load partnerships N times for N options. Single-pass approach loads partnerships once and builds a count map.

## R3: Efficient Counting Strategy (Single-Pass)

**Decision**: Load all validated partnerships once, build a map of `optionId → count`, then merge with options.

**Algorithm**:
1. Load all event options via existing `allByEvent()` query
2. Load all event partnerships
3. For each partnership with a `validatedPack()`, look up which options are in that pack via `PackOptionsTable`
4. Build a `Map<UUID, Int>` counting partnerships per option
5. For each option, wrap it with its count (defaulting to 0)

This requires:
- 1 query for options (existing)
- 1 query for partnerships (existing pattern)
- 1 query for all pack-option associations for the event (new bulk query)
- In-memory join to build counts

**Rationale**: Avoids N+1 queries. The number of partnerships and options per event is moderate (tens to low hundreds), making in-memory computation efficient.

**Alternatives considered**:
- **Lazy count (only compute when accessed)**: Not applicable — REST endpoint must return all data in a single response.
- **Cached counts**: Over-engineering — counts are computed on read, data volume is small, and cache invalidation would be complex.

## R4: JSON Schema Pattern

**Decision**: New `sponsoring_option_with_count.schema.json` wrapping `sponsoring_option_with_translations.schema.json` with a `partnership_count` integer field.

**Rationale**: Follows the same pattern as feature 021's `sponsoring_option_with_partnerships.schema.json`, which wraps the option schema with a `partnerships` array. The new schema is structurally similar but replaces the partnerships list with a count integer.

**Schema shape**:
```json
{
  "type": "object",
  "properties": {
    "option": { "$ref": "sponsoring_option_with_translations.schema.json" },
    "partnership_count": { "type": "integer", "minimum": 0 }
  },
  "required": ["option", "partnership_count"]
}
```

**Alternatives considered**:
- **Reuse existing schema with optional fields**: Rejected — the detail schema has `partnerships` (array), while list needs `partnership_count` (integer). Different shapes warrant separate schemas.

## R5: New Repository Method

**Decision**: Add `listOptionsWithPartnershipCounts(eventSlug: String): List<SponsoringOptionWithCount>` to `OptionRepository`.

**Rationale**: The existing `listOptionsByEventWithAllTranslations()` returns `List<SponsoringOptionWithTranslations>`. The new method returns the enriched wrapper list. The route handler calls the new method. The old method remains available for other consumers (if any).

**Alternatives considered**:
- **Modify existing method signature**: Rejected — would break the existing contract and potentially affect other callers.
- **Compute count in route layer**: Rejected — counting logic involves database queries (`PackOptionsTable`, `PartnershipEntity`), which belong in the repository layer.

## R6: Response Shape Change (Breaking Change Consideration)

**Decision**: The list endpoint response changes from `List<SponsoringOptionWithTranslations>` to `List<SponsoringOptionWithCount>`. This is a breaking change for API consumers.

**Rationale**: The spec explicitly requires a wrapper (FR-004, clarification session). The option data is preserved inside the `option` field, so clients need to update their response parsing. This is acceptable because:
- The endpoint is authenticated (organizer-only)
- The frontend and backend are co-developed
- The change is clearly documented in OpenAPI

**Migration path**: Update frontend to read `item.option.*` fields instead of `item.*` directly, and use `item.partnership_count` for the new count display.
