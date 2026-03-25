# Research: Option Partnerships

**Feature**: 021-option-partnerships
**Date**: 2026-03-24

## R1: Partnership-Option Relationship Path

**Decision**: Derive partnership-option association through validated pack membership, not direct `PartnershipOptionsTable` selection.

**Rationale**: The user confirmed that a partnership is associated with an option when its validated pack (determined by `validatedPack()`) contains that option (via `PackOptionsTable`). This represents the business-correct relationship: the validated pack is the authoritative source of what the partner committed to.

**Alternatives considered**:
- Direct `PartnershipOptionsTable` lookup: Rejected because it captures the partner's initial selection, which may differ from the validated/approved state.

**Implementation path**:
1. For a given option, find all packs that contain this option via `PackOptionsTable`
2. For each partnership in the event, call `validatedPack()` to get the validated pack
3. If the validated pack is in the set of packs containing the option, include the partnership

## R2: Response Structure for Option Detail with Partnerships

**Decision**: Create a new wrapper domain model `SponsoringOptionDetailWithPartners` that composes `SponsoringOptionWithTranslations` fields with an additional `partnerships: List<PartnershipItem>` field.

**Rationale**: The existing `SponsoringOptionWithTranslations` is a sealed class with polymorphic JSON serialization (`@JsonClassDiscriminator("type")`). Adding a field to the sealed class would require modifying all 4 subtypes and break the clean separation. A wrapper that includes the option data plus a partnerships list is simpler and non-breaking.

**Alternatives considered**:
- Modifying `SponsoringOptionWithTranslations` sealed class: Rejected — requires changing all 4 subtypes and pollutes the base option model with partnership concerns.
- Returning a generic map/pair: Rejected — not type-safe and doesn't produce clean JSON.

## R3: Query Approach for Validated Partnerships by Option

**Decision**: Query all event partnerships, filter by `validatedPack()` returning non-null, then match against packs containing the option.

**Rationale**: `validatedPack()` is an in-memory function on `PartnershipEntity` that evaluates suggestion approval/decline timestamps. It cannot be expressed as a pure SQL query. The approach is:
1. Get the list of pack IDs that contain the option (SQL via `PackOptionsTable`)
2. Get all partnerships for the event (SQL via `PartnershipEntity.find`)
3. For each partnership, call `validatedPack()` and check if the result's pack ID is in the set from step 1
4. Map matching partnerships to `PartnershipItem` using existing `toDomain()` mapper

**Performance note**: This scans all event partnerships in memory. For typical event sizes (10-200 partnerships), this is acceptable. For very large events, a future optimization could add a denormalized `validated_pack_id` column, but this is out of scope.

**Alternatives considered**:
- Pure SQL query with validated_at/declined_at logic: Rejected — duplicates the complex `validatedPack()` logic and risks drift between the two implementations.

## R4: Endpoint Scope

**Decision**: Add partnerships only to the single option detail endpoint (`GET /orgs/{orgSlug}/events/{eventSlug}/options/{optionId}`), not to the list endpoint.

**Rationale**: User confirmed this scope. The list endpoint returns a summary view where loading partnerships per option would be an N+1 concern. The detail endpoint is the appropriate place for enriched data.

**Alternatives considered**:
- Adding to both list and detail endpoints: Rejected by user — unnecessary for list view.

## R5: Existing `toDomain()` Mapper Reuse

**Decision**: Reuse `PartnershipEntity.toDomain(emails)` mapper from `partnership/application/mappers/PartnershipEntity.ext.kt` to produce `PartnershipItem` objects.

**Rationale**: This mapper already produces the exact `PartnershipItem` structure used by the partnership list endpoint, including deriving `validatedPackId` from `validatedPack()`. Reusing it ensures 100% field parity (FR-002).

**Implementation note**: The mapper requires the partnership's email list, obtained via `PartnershipEmailEntity.emails(partnershipId)`. This must be called in the same transaction.

## R6: OpenAPI / JSON Schema Updates

**Decision**: Add a new JSON schema `sponsoring_option_with_partnerships.schema.json` and a GET operation to OpenAPI for the `/{optionId}` path.

**Rationale**: The existing `sponsoring_option_with_translations.schema.json` does not include a `partnerships` field. The GET operation for `/{optionId}` is missing from the OpenAPI spec (only DELETE and PUT exist). Both need to be added.

**Alternatives considered**:
- Modifying existing schema: Rejected — would affect the list endpoint response schema.
