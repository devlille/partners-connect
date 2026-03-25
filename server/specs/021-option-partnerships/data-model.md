# Data Model: Option Partnerships

**Feature**: 021-option-partnerships
**Date**: 2026-03-24

## Existing Entities (No Changes)

### PartnershipItem (reused as-is)

**File**: `partnership/domain/PartnershipItem.kt`

```kotlin
@Serializable
data class PartnershipItem(
    val id: String,
    val contact: Contact,
    @SerialName("company_name") val companyName: String,
    @SerialName("event_name") val eventName: String,
    @SerialName("selected_pack_id") val selectedPackId: String? = null,
    @SerialName("selected_pack_name") val selectedPackName: String? = null,
    @SerialName("suggested_pack_id") val suggestedPackId: String? = null,
    @SerialName("suggested_pack_name") val suggestedPackName: String? = null,
    @SerialName("validated_pack_id") val validatedPackId: String? = null,
    val language: String,
    val phone: String? = null,
    val emails: List<String> = emptyList(),
    val organiser: User? = null,
    @SerialName("created_at") val createdAt: LocalDateTime,
)
```

### SponsoringOptionWithTranslations (reused as-is, sealed class with 4 subtypes)

**File**: `sponsoring/domain/SponsoringOptionWithTranslations.kt`

Base fields: `id: String`, `translations: Map<String, OptionTranslation>`, `price: Int?`
Subtypes: `Text`, `TypedQuantitative`, `TypedNumber`, `TypedSelectable`

### PackOptionsTable (queried, not modified)

**File**: `sponsoring/infrastructure/db/PackOptionsTable.kt`

Links packs to options. Used to find which packs contain a given option.

### PartnershipEntity.validatedPack() (called, not modified)

**File**: `partnership/infrastructure/db/PartnershipEntity.kt`

Returns the validated `SponsoringPackEntity?` based on suggestion approval and validation timestamps.

## New Domain Model

### SponsoringOptionDetailWithPartners

**File**: `sponsoring/domain/SponsoringOptionDetailWithPartners.kt` (NEW)

Wraps a `SponsoringOptionWithTranslations` with the list of validated partnerships.

```kotlin
@Serializable
data class SponsoringOptionDetailWithPartners(
    val option: SponsoringOptionWithTranslations,
    val partnerships: List<PartnershipItem> = emptyList(),
)
```

**JSON output** (example):
```json
{
  "option": {
    "type": "text",
    "id": "abc-123",
    "translations": { "en": { "language": "en", "name": "Logo on website", "description": "..." } },
    "price": 500
  },
  "partnerships": [
    {
      "id": "def-456",
      "contact": { "display_name": "Jane Doe", "role": "CTO" },
      "company_name": "Acme Corp",
      "event_name": "DevLille 2026",
      "selected_pack_id": "pack-1",
      "selected_pack_name": "Gold",
      "validated_pack_id": "pack-1",
      "language": "en",
      "emails": ["jane@acme.com"],
      "organiser": null,
      "created_at": "2026-03-01T10:00:00"
    }
  ]
}
```

## New Repository Method

### OptionRepository.getOptionByIdWithPartners

**File**: `sponsoring/domain/OptionRepository.kt` (MODIFIED — add method)

```kotlin
fun getOptionByIdWithPartners(
    eventSlug: String,
    optionId: UUID,
): SponsoringOptionDetailWithPartners
```

### Implementation in OptionRepositoryExposed (MODIFIED — add method)

**File**: `sponsoring/application/OptionRepositoryExposed.kt`

```kotlin
override fun getOptionByIdWithPartners(
    eventSlug: String,
    optionId: UUID,
): SponsoringOptionDetailWithPartners = transaction {
    val event = EventEntity.findBySlug(eventSlug)
        ?: throw NotFoundException("Event with slug $eventSlug not found")

    val option = optionEntity.findById(optionId)
        ?: throw NotFoundException("Option not found")

    if (option.event.id.value != event.id.value) {
        throw NotFoundException("Option not found")
    }

    // Find all pack IDs that contain this option
    val packIdsWithOption = PackOptionsTable
        .selectAll()
        .where { PackOptionsTable.option eq optionId }
        .map { it[PackOptionsTable.pack].value }
        .toSet()

    // Find all partnerships for this event whose validated pack contains this option
    val partnerships = PartnershipEntity
        .find { PartnershipsTable.eventId eq event.id.value }
        .filter { partnership ->
            val validatedPack = partnership.validatedPack()
            validatedPack != null && validatedPack.id.value in packIdsWithOption
        }
        .map { partnership ->
            partnership.toDomain(PartnershipEmailEntity.emails(partnership.id.value))
        }

    SponsoringOptionDetailWithPartners(
        option = option.toDomainWithAllTranslations(),
        partnerships = partnerships,
    )
}
```

## Database Changes

**None** — no new tables, columns, or migrations required.

## Entity Relationships (Query Path)

```
Option (optionId)
  ↓ PackOptionsTable (option → pack)
Pack IDs containing option
  ↓ PartnershipEntity.validatedPack()
Partnerships whose validated pack ∈ Pack IDs
  ↓ toDomain(emails)
List<PartnershipItem>
```
