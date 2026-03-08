# Data Model: Morning Organiser Daily Digest

**Branch**: `001-morning-organizer-digest` | **Date**: 2026-03-05

## Overview

This feature introduces **no new database tables** and **no schema migrations**. All readiness checks query fields that already exist in the current schema. The data model changes are limited to new Kotlin domain objects that represent query results.

---

## Existing Tables Used (Read-Only)

### `partnerships` table → `PartnershipsTable` / `PartnershipEntity`

| Column | Kotlin Property | Type | Role in digest |
|--------|----------------|------|----------------|
| `id` | `id` | `UUID` | Partnership identifier — used in the partnership link URL |
| `event_id` | `eventId` | `UUID` | FK to events — scopes queries per event |
| `company_id` | `companyId` | `UUID` | FK to companies — joins company details |
| `contact_name` | `contactName` | `String` | **Agreement readiness** — must be non-blank |
| `contact_role` | `contactRole` | `String` | **Agreement readiness** — must be non-blank |
| `agreement_url` | `agreementUrl` | `String?` | **Agreement readiness** — null = not yet generated |
| `validated_at` | `validatedAt` | `LocalDateTime?` | **Agreement + quote readiness** — non-null = pack confirmed |
| `declined_at` | `declinedAt` | `LocalDateTime?` | **All sections** — non-null = exclude from digest |
| `selected_pack_id` | `selectedPackId` | `UUID?` | **Quote readiness** — FK to `sponsoring_packs`; must be non-null for quote eligibility |
| `communication_publication_date` | `communicationPublicationDate` | `LocalDate?` | **Social media** — matched against today's UTC date |

### `billings` table → `BillingsTable` / `BillingEntity`

| Column | Kotlin Property | Type | Role in digest |
|--------|----------------|------|----------------|
| `partnership_id` | `partnershipId` | `UUID` | FK to partnerships — joins billing for quote check |
| `quote_pdf_url` | `quotePdfUrl` | `String?` | **Quote readiness** — null = not yet generated |

### `companies` table → `CompaniesTable` / `CompanyEntity`

| Column | Kotlin Property | Type | Role in digest |
|--------|----------------|------|----------------|
| `id` | `id` | `UUID` | Joins from partnership.company_id |
| `name` | `name` | `String` | **Agreement + quote readiness + display** — must be non-blank |
| `siret` | `siret` | `String?` | **Agreement + quote readiness** — must be non-null and non-blank |
| `address` | `address` | `String?` | **Agreement + quote readiness** — must be non-null and non-blank |
| `zip_code` | `zipCode` | `String?` | **Agreement + quote readiness** — must be non-null and non-blank |
| `city` | `city` | `String?` | **Agreement + quote readiness** — must be non-null and non-blank |
| `country` | `country` | `String?` | **Agreement + quote readiness** — must be non-null and non-blank |

### `sponsoring_packs` table → `SponsoringPacksTable`

| Column | Kotlin Property | Type | Role in digest |
|--------|----------------|------|----------------|
| `id` | `id` | `UUID` | Joins from partnership.selected_pack_id |
| `base_price` | `basePrice` | `Int` | **Quote readiness** — must be > 0 (pack has a defined price) |

### `events` table → `EventsTable` / `EventEntity`

| Column | Kotlin Property | Type | Role in digest |
|--------|----------------|------|----------------|
| `id` | `id` | `UUID` | Scopes all queries per event |
| `slug` | `slug` | `String` | Used to look up the Slack integration and build partnership links |
| `start_time` | `startTime` | `LocalDateTime` | **Active event filter** — must be in the future |
| `organisation_id` | `organisationId` | `UUID` | FK to organisations — used to look up the org slug when building the Slack notification |

---

## New Domain Objects (Kotlin, No DB)

### `DigestEntry`

A single actionable partnership to display in one digest section.

```kotlin
data class DigestEntry(
    val companyName: String,
    val partnershipLink: String,  // Full URL: {frontendBaseUrl}/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}
)
```

### `EventDigest`

The full digest payload for one event, passed to `NotificationVariables.MorningDigest` and dispatched via `notificationRepository.sendMessageFromMessaging(variables)`.

```kotlin
data class EventDigest(
    val event: EventWithOrganisation,
    val agreementItems: List<DigestEntry>,    // Empty → section omitted from message
    val quoteItems: List<DigestEntry>,
    val socialMediaItems: List<DigestEntry>,
) {
    val hasItems: Boolean
        get() = agreementItems.isNotEmpty() || quoteItems.isNotEmpty() || socialMediaItems.isNotEmpty()
}
```

### `DigestRepository` (interface)

```kotlin
interface DigestRepository {
    /**
     * Returns the digest payload for a single event.
     * Queries all three readiness categories in one pass.
     * Returns null if the event has no actionable items.
     */
    suspend fun queryDigest(eventId: UUID, today: LocalDate): EventDigest
}
```

---

## Readiness Query Logic

### Agreement-ready partnerships

A partnership qualifies when ALL of the following hold:

- `partnerships.declined_at IS NULL`
- `partnerships.validated_at IS NOT NULL`
- `partnerships.agreement_url IS NULL`
- `companies.name` is non-blank
- `companies.siret IS NOT NULL` and non-blank
- `companies.address IS NOT NULL` and non-blank
- `companies.zip_code IS NOT NULL` and non-blank
- `companies.city IS NOT NULL` and non-blank
- `companies.country IS NOT NULL` and non-blank
- `partnerships.contact_name` is non-blank
- `partnerships.contact_role` is non-blank

### Quote-ready partnerships

A partnership qualifies when ALL of the following hold:

- `partnerships.declined_at IS NULL`
- `partnerships.validated_at IS NOT NULL`
- No billing row exists with `billings.quote_pdf_url IS NOT NULL` for this partnership
- `companies.name` is non-blank
- `companies.siret IS NOT NULL` and non-blank
- `companies.address IS NOT NULL` and non-blank
- `companies.zip_code IS NOT NULL` and non-blank
- `companies.city IS NOT NULL` and non-blank
- `companies.country IS NOT NULL` and non-blank
- `partnerships.selected_pack_id IS NOT NULL`
- `sponsoring_packs.base_price > 0`

### Social-media-due partnerships

A partnership qualifies when ALL of the following hold:

- `partnerships.declined_at IS NULL`
- `DATE(partnerships.communication_publication_date) = today` (UTC date comparison)

---

## Validation State Transitions (Unchanged)

This feature is read-only with respect to all existing entities. No state is mutated. The digest job only reads.
