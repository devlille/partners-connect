# Data Model: Option Usage Count

**Feature**: 022-option-usage-count
**Date**: 2026-03-25

## New Entities

### SponsoringOptionWithCount (NEW)

**File**: `sponsoring/domain/SponsoringOptionWithCount.kt`
**Purpose**: Wrapper that pairs a sponsoring option with its validated partnership count for the list endpoint.

```kotlin
@Serializable
data class SponsoringOptionWithCount(
    val option: SponsoringOptionWithTranslations,
    @SerialName("partnership_count") val partnershipCount: Int,
)
```

**Fields**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `option` | `SponsoringOptionWithTranslations` | Yes | The full option with all translations (polymorphic sealed class) |
| `partnership_count` | `Int` | Yes | Number of validated partnerships whose validated pack contains this option. Always >= 0. |

**Validation rules**:
- `partnership_count` MUST be >= 0
- `option` MUST NOT be null
- No default value for `partnership_count` (must always be serialized — `encodeDefaults=false` in Json config)

**Serialization notes**:
- `@SerialName("partnership_count")` maps to snake_case JSON key
- `option` field serializes polymorphically via `@JsonClassDiscriminator("type")` on `SponsoringOptionWithTranslations`

## Existing Entities (No Changes)

### SponsoringOptionWithTranslations (reused as-is)

**File**: `sponsoring/domain/SponsoringOptionWithTranslations.kt`

Sealed class with 4 subtypes: `Text`, `TypedQuantitative`, `TypedNumber`, `TypedSelectable`.
Base fields: `id: String`, `translations: Map<String, OptionTranslation>`, `price: Int?`
**NOT modified** — shared by sponsoring pack endpoint.

### PackOptionsTable (queried, not modified)

**File**: `sponsoring/infrastructure/db/PackOptionsTable.kt`

Links packs to options. Used to find which packs contain a given option, and in the new counting logic, to build a map of which options appear in which packs.

### PartnershipEntity.validatedPack() (called, not modified)

**File**: `partnership/infrastructure/db/PartnershipEntity.kt`

Returns the validated `SponsoringPackEntity?` for a partnership. Used to determine which partnerships have a confirmed/validated pack for counting.

## Repository Interface Changes

### OptionRepository (MODIFIED)

**File**: `sponsoring/domain/OptionRepository.kt`

New method added:

```kotlin
fun listOptionsWithPartnershipCounts(eventSlug: String): List<SponsoringOptionWithCount>
```

**Behavior**: Returns all options for the event, each wrapped with the count of validated partnerships using that option. Options with no associated partnerships have `partnershipCount = 0`.

## Repository Implementation Changes

### OptionRepositoryExposed (MODIFIED)

**File**: `sponsoring/application/OptionRepositoryExposed.kt`

New method implementation using single-pass algorithm:

1. Query all options for the event (existing `allByEvent()`)
2. Query all pack-option associations for the event from `PackOptionsTable`
3. Load all event partnerships and filter by `validatedPack() != null`
4. For each validated partnership, map its validated pack ID to the options in that pack
5. Build `Map<UUID, Int>` of option ID → partnership count
6. Wrap each option with its count

## Relationships

```
SponsoringOptionWithCount
├── option: SponsoringOptionWithTranslations (1:1, embedded)
└── partnershipCount: Int (computed from):
    └── PartnershipEntity (N partnerships per event)
        └── validatedPack() → SponsoringPackEntity?
            └── PackOptionsTable → SponsoringOptionEntity (M:N)
```

## State Transitions

No state transitions — this is a read-only computed field.
