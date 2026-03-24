---
name: test-factories
description: 'Test data factories for Exposed entities and domain objects in partners-connect. Use when creating insertMocked* or create* factory functions, adding test data setup for new entities, or following factory conventions. Covers entity factories, domain object factories, join table factories, composite factories, foreign key handling, and unique default patterns.'
---

# Test Data Factories

## Location & Naming

| Item | Convention |
|------|-----------|
| Source set | `application/src/test/kotlin/` |
| Package | `fr.devlille.partners.connect.<feature>.factories` |
| File name | `<Entity>.factory.kt` — e.g., `Company.factory.kt`, `Partnership.factory.kt` |

Every Exposed entity that tests need **MUST** have a corresponding factory file.
One file can contain multiple related factory functions (main entity + child/join entities).

---

## Two Factory Types

| Type | Naming | Returns | Purpose |
|------|--------|---------|---------|
| Entity factory | `insertMocked<Entity>()` | `<Entity>Entity` | Insert a row in H2 via Exposed |
| Domain object factory | `create<Domain>()` | Domain data class | Build an in-memory domain object |

---

## Entity Factory — Standard Pattern

```kotlin
package fr.devlille.partners.connect.companies.factories

import fr.devlille.partners.connect.companies.domain.CompanyStatus
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedCompany(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(),
    address: String = "123 Mock St",
    city: String = "Mock City",
    zipCode: String = "12345",
    country: String = "MO",
    siret: String = "12345678901234",
    vat: String = "FR12345678901",
    description: String? = "This is a mock company for testing purposes.",
    siteUrl: String = "https://www.mockcompany.com",
    status: CompanyStatus = CompanyStatus.ACTIVE,
): CompanyEntity = CompanyEntity.new(id) {
    this.name = name
    this.address = address
    this.city = city
    this.zipCode = zipCode
    this.country = country
    this.siret = siret
    this.vat = vat
    this.description = description
    this.siteUrl = siteUrl
    this.logoUrlOriginal = null
    this.logoUrl1000 = null
    this.logoUrl500 = null
    this.logoUrl250 = null
    this.status = status
}
```

### Rules (NON-NEGOTIABLE)

1. **All parameters MUST have defaults** — callers only override what they care about.
2. **Unique fields MUST use UUID-based defaults** — prevents constraint violations:
   - `name = id.toString()`
   - `email = "$id@mail.com"`
   - `slug = name` (derived from unique name)
   - `externalId = "speaker-${id.take(8)}"`
3. **Enum fields default to the most common/active state** — `CompanyStatus.ACTIVE`, `PromotionStatus.PENDING`.
4. **Nullable fields default to `null`** unless a sensible value helps most tests.
5. **NO `transaction {}` inside factories** — the caller manages the transaction.
6. **`@Suppress("LongParameterList")`** on functions with 5+ parameters.
7. **Return the Entity** — `CompanyEntity`, not `UUID`.

---

## Foreign Key Handling

### Required foreign key (with default UUID)

When the parent entity is expected to already exist:

```kotlin
fun insertMockedPartnership(
    id: UUID = UUID.randomUUID(),
    eventId: UUID = UUID.randomUUID(),
    companyId: UUID = UUID.randomUUID(),
): PartnershipEntity = PartnershipEntity.new(id) {
    this.event = EventEntity[eventId]       // Bracket lookup syntax
    this.company = CompanyEntity[companyId]
}
```

### Required foreign key (no default — caller MUST provide)

When the factory makes no sense without the parent:

```kotlin
fun insertMockedJobOffer(
    id: UUID = UUID.randomUUID(),
    companyId: UUID,                         // No default — required
    title: String = id.toString(),
): CompanyJobOfferEntity = CompanyJobOfferEntity.new(id) {
    this.company = CompanyEntity.findById(companyId)!!
}
```

### Optional foreign key (nullable with `?.let {}`)

```kotlin
fun insertMockedPartnership(
    selectedPackId: UUID? = null,
    organiserId: UUID? = null,
): PartnershipEntity = PartnershipEntity.new(id) {
    this.selectedPack = selectedPackId?.let { SponsoringPackEntity[it] }
    this.organiser = organiserId?.let { UserEntity[it] }
}
```

### Nested factory call as default

When one entity always needs a parent and creating it inline is acceptable:

```kotlin
fun insertMockedOrganisationEntity(
    id: UUID = UUID.randomUUID(),
    representativeUser: UserEntity = insertMockedUser(),  // Calls another factory
): OrganisationEntity = OrganisationEntity.new(id) {
    this.representativeUser = representativeUser
}
```

---

## DateTime Handling

### Current timestamp default

```kotlin
fun insertMockedPartnershipEmailHistory(
    sentAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
)
```

### Calculated timestamps (future/past events)

```kotlin
fun insertMockedFutureEvent(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(),
    slug: String = name,
    orgId: UUID = UUID.randomUUID(),
): EventEntity {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val yesterday = now.toInstant(TimeZone.UTC).minus(duration = 1.days).toLocalDateTime(TimeZone.UTC)
    val tomorrow = now.toInstant(TimeZone.UTC).plus(duration = 1.days).toLocalDateTime(TimeZone.UTC)
    return EventEntity.new(id) {
        this.name = name
        this.slug = slug
        this.startTime = LocalDateTime.parse("${now.year + 1}-12-01T00:00:00")
        this.endTime = LocalDateTime.parse("${now.year + 1}-12-31T23:59:59")
        this.submissionStartTime = submissionStartTime ?: yesterday
        this.submissionEndTime = submissionEndTime ?: tomorrow
        this.organisation = OrganisationEntity[orgId]
    }
}
```

When a domain needs both future and past entities, create **two factory functions**: `insertMockedFutureEvent()` and `insertMockedPastEvent()`.

### Optional datetime fields

```kotlin
fun insertMockedBoothActivity(
    startTime: LocalDateTime? = null,
    endTime: LocalDateTime? = null,
): BoothActivityEntity = BoothActivityEntity.new(id) {
    this.startTime = startTime
    this.endTime = endTime
}
```

---

## Join Table Factory

For join tables that extend `Table` (not `UUIDTable`), use DSL `insert`:

```kotlin
package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import org.jetbrains.exposed.v1.jdbc.insert
import java.util.UUID

fun insertMockedPackOptions(
    packId: UUID = UUID.randomUUID(),
    optionId: UUID = UUID.randomUUID(),
    required: Boolean = true,
) =
    PackOptionsTable.insert {
        it[this.pack] = packId
        it[this.option] = optionId
        it[this.required] = required
    }
```

For join tables that extend `UUIDTable`, use the standard entity pattern:

```kotlin
fun insertMockedSpeakerPartnership(
    id: UUID = UUID.randomUUID(),
    partnershipId: UUID = UUID.randomUUID(),
    speakerId: UUID = UUID.randomUUID(),
): SpeakerPartnershipEntity = SpeakerPartnershipEntity.new(id) {
    this.partnership = PartnershipEntity[partnershipId]
    this.speaker = SpeakerEntity[speakerId]
}
```

---

## Composite Factory

When inserting an entity also requires creating child/related records, create a helper function in the same file:

```kotlin
// In BoothActivity.factory.kt — creates option + links it to partnership
fun insertMockedBoothOption(
    partnershipId: UUID,
    packId: UUID,
    eventId: UUID,
    optionId: UUID = UUID.randomUUID(),
): SponsoringOptionEntity {
    val option = insertMockedSponsoringOption(
        optionId = optionId,
        eventId = eventId,
        optionType = OptionType.TYPED_SELECTABLE,
    )
    PartnershipOptionEntity.new {
        this.partnership = PartnershipEntity[partnershipId]
        this.pack = SponsoringPackEntity[packId]
        this.option = option
    }
    return option
}
```

```kotlin
// In PartnershipEmailHistory.factory.kt — creates history + recipient records
fun insertMockedPartnershipEmailHistory(
    id: UUID = UUID.randomUUID(),
    partnershipId: UUID = UUID.randomUUID(),
    recipientEmails: List<String> = listOf("partner@company.com"),
    recipientStatuses: Map<String, DeliveryStatus> =
        recipientEmails.associateWith { DeliveryStatus.SENT },
): PartnershipEmailHistoryEntity {
    val emailHistory = PartnershipEmailHistoryEntity.new(id) {
        this.partnership = PartnershipEntity[partnershipId]
        // ... fields
    }
    recipientEmails.forEach { email ->
        RecipientDeliveryStatusEntity.new {
            this.emailHistory = emailHistory
            this.recipientEmail = email
            this.deliveryStatus = recipientStatuses[email] ?: DeliveryStatus.SENT
        }
    }
    return emailHistory
}
```

---

## Polymorphic Entity Factory

When an entity has type-specific fields, use a `when` block:

```kotlin
fun insertMockedSponsoringOption(
    optionId: UUID = UUID.randomUUID(),
    eventId: UUID = UUID.randomUUID(),
    optionType: OptionType = OptionType.TEXT,
    name: String = optionId.toString(),
    selectableValues: List<SelectableValue> = emptyList(),
): SponsoringOptionEntity {
    val option = SponsoringOptionEntity.new(optionId) {
        this.event = EventEntity[eventId]
        this.optionType = optionType
        when (optionType) {
            OptionType.TYPED_QUANTITATIVE -> this.quantitativeDescriptor = QuantitativeDescriptor.JOB_OFFER
            OptionType.TYPED_NUMBER -> {
                this.numberDescriptor = NumberDescriptor.NB_TICKET
                this.fixedQuantity = 1
            }
            OptionType.TYPED_SELECTABLE -> this.selectableDescriptor = SelectableDescriptor.BOOTH
            OptionType.TEXT -> { }
        }
    }
    selectableValues.forEach {
        SelectableValueEntity.new(it.id.toUUID()) {
            this.option = option
            this.value = it.value
            this.price = it.price
        }
    }
    insertMockedOptionTranslation(optionId = optionId, name = name)
    return option
}
```

---

## Domain Object Factory

For domain models used as request/input DTOs — no database involved:

```kotlin
package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack

fun createSponsoringPack(
    name: String = "Silver",
    price: Int = 2000,
    maxQuantity: Int = 10,
): CreateSponsoringPack = CreateSponsoringPack(
    name = name,
    price = price,
    maxQuantity = maxQuantity,
)
```

Domain object factories follow the same rules: all parameters have defaults, unique fields use UUID-based names.

---

## Multi-Function Files

A single factory file can contain several related functions. Group them in the same file when they share the same main entity:

```kotlin
// Partnership.factory.kt contains:
fun insertMockedPartnership(...)          // Main entity
fun insertMockedOptionPartnership(...)    // Child join entity
fun insertMockedPartnershipEmail(...)     // Child entity
```

Child/relation factories that are **only meaningful with the parent** live in the parent's factory file. Standalone entities get their own file.

---

## Permission Factory

For authorization setup in tests:

```kotlin
fun insertMockedOrgaPermission(
    orgId: UUID,         // Required — no default
    userId: UUID,        // Required — no default
    canEdit: Boolean = true,
): OrganisationPermissionEntity = OrganisationPermissionEntity.new {
    this.organisation = OrganisationEntity[orgId]
    this.user = UserEntity[userId]
    this.canEdit = canEdit
}
```

---

## Quick Reference — All Existing Factories

| Function | File | Domain | Returns |
|----------|------|--------|---------|
| `insertMockedCompany()` | `Company.factory.kt` | companies | `CompanyEntity` |
| `insertMockedJobOffer()` | `JobOffer.factory.kt` | companies | `CompanyJobOfferEntity` |
| `insertMockCompanyJobOfferPromotion()` | `CompanyJobOfferPromotion.factory.kt` | companies | `CompanyJobOfferPromotionEntity` |
| `insertMockedPartnership()` | `Partnership.factory.kt` | partnership | `PartnershipEntity` |
| `insertMockedOptionPartnership()` | `Partnership.factory.kt` | partnership | `PartnershipOptionEntity` |
| `insertMockedPartnershipEmail()` | `Partnership.factory.kt` | partnership | `PartnershipEmailEntity` |
| `insertMockedBilling()` | `Billing.factory.kt` | partnership | `BillingEntity` |
| `insertMockedSpeakerPartnership()` | `SpeakerPartnershipEntity.factory.kt` | partnership | `SpeakerPartnershipEntity` |
| `insertMockedBoothActivity()` | `BoothActivity.factory.kt` | partnership | `BoothActivityEntity` |
| `insertMockedBoothOption()` | `BoothActivity.factory.kt` | partnership | `SponsoringOptionEntity` |
| `insertMockedCommunicationPlan()` | `CommunicationPlan.factory.kt` | partnership | `CommunicationPlanEntity` |
| `createCompanyBillingData()` | `CompanyBillingData.factory.kt` | partnership | `CompanyBillingData` |
| `insertMockedPartnershipTicket()` | `PartnershipTicket.factory.kt` | partnership | `PartnershipTicketEntity` |
| `insertMockedPartnershipEmailHistory()` | `PartnershipEmailHistory.factory.kt` | partnership | `PartnershipEmailHistoryEntity` |
| `insertMockedSponsoringPack()` | `SponsoringPackEntity.factory.kt` | sponsoring | `SponsoringPackEntity` |
| `insertMockedSponsoringOption()` | `SponsoringOptionEntity.factory.kt` | sponsoring | `SponsoringOptionEntity` |
| `insertMockedOptionTranslation()` | `OptionTranslationEntity.factory.kt` | sponsoring | `OptionTranslationEntity` |
| `insertMockedPackOptions()` | `PackOptionsTable.factory.kt` | sponsoring | DSL insert |
| `createSponsoringPack()` | `CreateSponsoringPack.factory.kt` | sponsoring | `CreateSponsoringPack` |
| `insertMockedFutureEvent()` | `EventEntity.factory.kt` | events | `EventEntity` |
| `insertMockedPastEvent()` | `EventEntity.factory.kt` | events | `EventEntity` |
| `createEvent()` | `Event.factory.kt` | events | `Event` |
| `insertMockedOrganisationEntity()` | `OrganisationEntity.factory.kt` | organisations | `OrganisationEntity` |
| `createOrganisation()` | `Organisation.factory.kt` | organisations | `Organisation` |
| `insertMockedUser()` | `UserEntity.factory.kt` | users | `UserEntity` |
| `insertMockedOrgaPermission()` | `EventPermissionEntity.factory.kt` | users | `OrganisationPermissionEntity` |
| `insertMockedProvider()` | `Provider.factory.kt` | provider | `ProviderEntity` |
| `insertMockedIntegration()` | `Integration.factory.kt` | integrations | `UUID` |
| `insertSlackIntegration()` | `Integration.factory.kt` | integrations | `UUID` |
| `insertMockedSpeaker()` | `SpeakerEntity.factory.kt` | agenda | `SpeakerEntity` |
