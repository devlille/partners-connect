# Data Model: Email Partnership Contacts via Mailing Integration

**Feature**: 014-email-partnerships  
**Date**: 2025-12-19  
**Status**: Complete

## Overview

This feature does NOT introduce new database tables. It reuses existing schema for partnerships, company emails, events, and users. The data model focuses on the relationships and data flow for bulk email sending.

## Existing Entities (Reused)

### Partnership (PartnershipsTable)
**Purpose**: Core partnership record linking company to event

**Relevant Fields**:
- `id` (UUID, PK): Partnership identifier
- `company_id` (UUID, FK → companies): Reference to company
- `event_id` (UUID, FK → events): Reference to event
- `validated_pack_id` (UUID, FK → sponsoring_packs, nullable): Current validated pack
- `suggestion_sent_at` (datetime, nullable): Suggestion workflow timestamp
- `suggestion_approved_at` (datetime, nullable): Approval timestamp
- `billing_id` (UUID, FK → invoices, nullable): Associated billing record
- `agreement_id` (UUID, FK → partnership_agreements, nullable): Agreement record
- `organiser_user_id` (UUID, FK → users, nullable): **Assigned organizer for this partnership**

**Relationships**:
- One Partnership → Many PartnershipEmails (company contact emails)
- One Partnership → Optional User (organiser)
- One Partnership → One Event
- One Partnership → One Company

**Filtering Logic** (reused from PartnershipRepository.listByEvent):
- `filter[validated]`: `validated_pack_id IS NOT NULL`
- `filter[suggestion]`: `suggestion_sent_at IS NOT NULL`
- `filter[paid]`: `billing_id IS NOT NULL AND invoices.status = 'PAID'`
- `filter[agreement-generated]`: `agreement_id IS NOT NULL`
- `filter[agreement-signed]`: `agreements.signed_at IS NOT NULL`
- `filter[pack_id]`: `validated_pack_id = <pack_uuid>`

---

### PartnershipEmail (PartnershipEmailsTable / company_emails)
**Purpose**: Contact email addresses for each partnership

**Schema**:
```kotlin
object PartnershipEmailsTable : UUIDTable("company_emails") {
    val partnershipId = reference("partnership_id", PartnershipsTable)
    val email = text("email")
}
```

**Fields**:
- `id` (UUID, PK): Email record identifier
- `partnership_id` (UUID, FK → partnerships): Parent partnership
- `email` (text): Contact email address

**Cardinality**: One Partnership → Many Emails (1:N relationship)

**Usage**: All emails for matching partnerships become potential recipients (after deduplication)

---

### Event (EventsTable)
**Purpose**: Event context for email sender information

**Relevant Fields**:
- `id` (UUID, PK): Event identifier
- `slug` (text, unique): Event URL slug (e.g., "devlille-2025")
- `name` (text): Event name (e.g., "DevLille 2025")
- `contact_email` (text): Event contact email (used for From when no organizer, CC when organizer assigned)
- `organisation_id` (UUID, FK → organisations): Parent organization

**Relationships**:
- One Event → Many Partnerships
- One Event → One Organisation (for Mailjet integration lookup)

**Usage**:
- Event name prefixes email subject: `[${event.name}] ${subject}`
- Event contact email used as From (when no organizer) or CC (when organizer assigned)

---

### User (UsersTable)
**Purpose**: Partnership organizer information (when assigned)

**Relevant Fields**:
- `id` (UUID, PK): User identifier
- `email` (text): User email address
- `firstname` (text): User first name
- `lastname` (text): User last name

**Derived Field**:
- `name`: Computed as `"$firstname $lastname"` for email From name

**Cardinality**: One Partnership → Optional User (0:1 relationship)

**Usage**:
- When partnership has assigned organizer (`organiser_user_id` not null):
  - From: `organiser.email` with name `"${organiser.firstname} ${organiser.lastname}"`
  - CC: `event.contact.email`
- When partnership has NO organizer (`organiser_user_id` is null):
  - From: `event.contact.email` with name `event.name`
  - CC: (none)

---

### MailjetIntegration (MailjetIntegrationsTable)
**Purpose**: Organization-specific Mailjet API credentials

**Relevant Fields**:
- `integration_id` (UUID, FK → integrations): Integration record
- `api_key` (text, encrypted): Mailjet API key
- `secret` (text, encrypted): Mailjet API secret

**Relationships**:
- One Organisation → One Mailjet Integration

**Usage**: Fetched internally by `NotificationGateway` (Mailjet implementation) from `mailjet_integrations` table using `orgSlug` to authenticate with Mailjet API

---

## Data Flow Diagram

```
Request: POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/email
  ↓
  ├─ Parameters: orgSlug, eventSlug
  ├─ Query Filters: filter[validated], filter[paid], etc. (optional)
  └─ Body: { subject, body }

[AuthorizedOrganisationPlugin validates JWT + permissions]
  ↓
[Route Handler Orchestration]
  ↓
  ├─ 1. Fetch Partnerships with Emails & Organizers
  │    └─ PartnershipEmailRepository.getPartnershipsWithEmails(eventSlug, filters)
  │         ↓
  │         Query: SELECT partnerships.id, partnerships.organiser_user_id, users.email, users.firstname, users.lastname, company_emails.email
  │                FROM partnerships
  │                LEFT JOIN users ON partnerships.organiser_user_id = users.id
  │                JOIN company_emails ON company_emails.partnership_id = partnerships.id
  │                WHERE partnerships.event_id = (SELECT id FROM events WHERE slug = eventSlug)
  │                  AND [apply filters: validated, paid, etc.]
  │         ↓
  │         Returns: List<PartnershipWithEmails>
  │           └─ PartnershipWithEmails(
  │                partnershipId: UUID,
  │                organiser: User?,  // null if no organizer assigned
  │                emails: List<String>  // all contact emails for this partnership
  │              )
  │
  ├─ 2. Fetch Event Details
  │    └─ EventRepository.findBySlug(eventSlug)
  │         └─ Returns: Event(name, contact.email, organisation_id)
  │
  ├─ 3. Group Partnerships by Organizer (route logic)
  │    └─ partnershipsWithEmails.groupBy { it.organiser }
  │         └─ Map<User?, List<PartnershipWithEmails>>
  │              ├─ Key = User (assigned organizer)
  │              └─ Key = null (no organizer assigned)
  │
  ├─ 4. Send Emails via NotificationRepository (for each organizer group)
  │    └─ For Each Organizer Group:
  │              ├─ Determine From/CC:
  │              │    ├─ If organiser != null:
  │              │    │    ├─ From: organiser.email, organiser.name
  │              │    │    └─ CC: event.contact.email
  │              │    └─ If organiser == null:
  │              │         ├─ From: event.contact.email, event.name
  │              │         └─ CC: (none)
  │              │
  │              ├─ Collect & Deduplicate Recipients:
  │              │    └─ group.flatMap { it.emails }.distinct()
  │              │         └─ Result: List<EmailContact> (unique email addresses)
  │              │
  │              └─ Send via NotificationRepository:
  │                   └─ NotificationRepository.sendMail(
  │                        orgSlug = orgSlug,
  │                        from = EmailContact(email=fromEmail, name=fromName),
  │                        to = recipients.map { EmailContact(email=it) },
  │                        cc = ccEmail?.let { [EmailContact(email=it)] },
  │                        subject = "[${event.name}] $subject",
  │                        htmlBody = body
  │                      )
  │                        ↓
  │                        [NotificationRepository converts to generic format]
  │                        └─ Converts EmailContact → Destination, subject → header, etc.
  │                        ↓
  │                        [NotificationGateway fetches config and sends]
  │                        └─ Mailjet gateway fetches config from mailjet_integrations table
  │                        └─ Converts generic format to Mailjet-specific API format
  │                        └─ Sends via Mailjet API POST /v3.1/send
  │                             ↓
  │                             HTTP POST https://api.mailjet.com/v3.1/send
  │                             ↓
  │                             Returns: Boolean (success if Mailjet accepts batch)
  │                        ↓
  │                        Returns: Boolean (success)
  │
  └─ 5. Return Response:
       └─ HTTP 200 OK { recipients: <total_unique_count> }

Error Scenarios:
  ├─ Zero partnerships match filters → NotFoundException (404)
  ├─ No Mailjet integration → NotFoundException (404) - thrown by NotificationRepository
  ├─ Mailjet API failure → ServiceUnavailableException (503) - thrown by NotificationRepository
  ├─ Invalid request body → BadRequestException (400, handled by JSON schema)
  └─ No edit permission → UnauthorizedException (401, handled by plugin)

**Repository Responsibilities**:
- **PartnershipEmailRepository**: Data fetching only (no email sending)
- **EventRepository**: Event details retrieval
- **NotificationRepository**: Email sending abstraction (converts EmailContact to generic Destination format, calls NotificationGateway)
- **NotificationGateway**: Generic notification interface (Mailjet implementation fetches own config from mailjet_integrations table, handles sending)
- **Route Orchestration**: Groups partnerships by organizer, loops groups, calls NotificationRepository.sendMail() per group
```

---

## Domain Data Structures (New)

### SendPartnershipEmailRequest
**Purpose**: Request body for POST endpoint

```kotlin
@Serializable
data class SendPartnershipEmailRequest(
    @SerialName("subject")
    val subject: String,  // Required, max 500 chars (validated by JSON schema)
    
    @SerialName("body")
    val body: String  // Required, HTML format (validated by JSON schema)
)
```

**Validation Rules** (enforced by JSON schema):
- `subject`: Required, minLength=1, maxLength=500
- `body`: Required, minLength=1

---

### SendPartnershipEmailResponse
**Purpose**: Response body for successful send

```kotlin
@Serializable
data class SendPartnershipEmailResponse(
    @SerialName("recipients")
    val recipients: Int  // Total unique email recipients across all batches (minimum 1)
)
```

**Example**:
```json
{
  "recipients": 42
}
```

---

### PartnershipWithEmails
**Purpose**: Internal data structure for repository query results

```kotlin
data class PartnershipWithEmails(
    val partnershipId: UUID,
    val organiser: User?,  // null if partnership has no assigned organizer
    val emails: List<String>  // Contact emails from company_emails table
)
```

**Usage**: Returned by `PartnershipEmailRepository.getPartnershipsWithEmails()` for route handler processing.

---

## Query Patterns

### Primary Query (PartnershipEmailRepository)

```sql
-- Fetch partnerships with emails and optional organizer
SELECT 
    p.id AS partnership_id,
    u.id AS organiser_id,
    u.email AS organiser_email,
    u.firstname AS organiser_firstname,
    u.lastname AS organiser_lastname,
    ce.email AS contact_email
FROM partnerships p
LEFT JOIN users u ON p.organiser_user_id = u.id
JOIN company_emails ce ON ce.partnership_id = p.id
JOIN events e ON p.event_id = e.id
WHERE e.slug = :eventSlug
  AND [apply filters based on PartnershipFilters]
ORDER BY [direction parameter: p.created_at ASC/DESC]
```

**Filter Application**:
```sql
-- filter[validated]=true
AND p.validated_pack_id IS NOT NULL

-- filter[paid]=true
AND p.billing_id IS NOT NULL
AND EXISTS (
    SELECT 1 FROM invoices i 
    WHERE i.id = p.billing_id 
    AND i.status = 'PAID'
)

-- filter[suggestion]=true
AND p.suggestion_sent_at IS NOT NULL

-- filter[agreement-generated]=true
AND p.agreement_id IS NOT NULL

-- filter[agreement-signed]=true
AND p.agreement_id IS NOT NULL
AND EXISTS (
    SELECT 1 FROM partnership_agreements pa
    WHERE pa.id = p.agreement_id
    AND pa.signed_at IS NOT NULL
)

-- filter[pack_id]={uuid}
AND p.validated_pack_id = :packId
```

**Result Grouping**:
- One partnership may have multiple rows (one per contact email)
- Route handler groups results by `partnership_id` to collect all emails
- Then groups by `organiser` for batch sending

---

## Exposed ORM Mapping

### Accessing Partnership Organizer
```kotlin
// In PartnershipEntity
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)
    
    var organiserUserId by PartnershipsTable.organiserUserId  // nullable UUID
    
    // Relationship to User entity
    val organiser by UserEntity optionalReferencedOn PartnershipsTable.organiserUserId
}
```

### Accessing Partnership Emails
```kotlin
// Query pattern in repository
val partnershipEmails = PartnershipEmailEntity
    .find { PartnershipEmailsTable.partnershipId eq partnershipId }
    .map { it.email }
    .toList()
```

---

## Data Validation & Constraints

### Request Validation (JSON Schema)
- Subject: 1-500 characters, required
- Body: ≥1 character, required, HTML content

### Business Logic Validation (Route Handler)
- At least one partnership must match filters (404 if zero)
- Mailjet integration must exist for organization (404 if not found)
- Matched partnerships must have at least one email address (404 if zero total)

### Database Constraints (Existing Schema)
- Partnership emails: `partnership_id` must reference valid partnership
- Partnership organiser: `organiser_user_id` must reference valid user (or null)
- Event slug: Must be unique and match existing event
- Organisation: Must have valid Mailjet integration record

---

## Example Data Scenarios

### Scenario 1: Partnerships with Mixed Organizer Assignment

**Database State**:
```
Partnerships:
  - ID: P1, Event: devlille-2025, Company: CompanyA, Organiser: User1 (alice@example.com), Emails: [contact1@companyA.com]
  - ID: P2, Event: devlille-2025, Company: CompanyB, Organiser: User1 (alice@example.com), Emails: [contact2@companyB.com, contact3@companyB.com]
  - ID: P3, Event: devlille-2025, Company: CompanyC, Organiser: NULL, Emails: [contact4@companyC.com]
  - ID: P4, Event: devlille-2025, Company: CompanyD, Organiser: User2 (bob@example.com), Emails: [contact5@companyD.com]

Event: devlille-2025, Name: "DevLille 2025", Contact: event@devlille.com
```

**Email Grouping Result**:
```
Group 1 (Organiser: User1 alice@example.com):
  - From: alice@example.com (Alice Smith)
  - CC: event@devlille.com
  - To: [contact1@companyA.com, contact2@companyB.com, contact3@companyB.com]  # 3 unique recipients

Group 2 (Organiser: User2 bob@example.com):
  - From: bob@example.com (Bob Jones)
  - CC: event@devlille.com
  - To: [contact5@companyD.com]  # 1 recipient

Group 3 (Organiser: NULL):
  - From: event@devlille.com (DevLille 2025)
  - CC: (none)
  - To: [contact4@companyC.com]  # 1 recipient

Total Recipients: 5
```

**Mailjet API Calls**: 3 separate POST requests to `/v3.1/send`

---

### Scenario 2: Email Deduplication

**Database State**:
```
Partnerships:
  - ID: P1, Company: CompanyA, Organiser: NULL, Emails: [shared@example.com, contact1@example.com]
  - ID: P2, Company: CompanyB, Organiser: NULL, Emails: [shared@example.com, contact2@example.com]
  - ID: P3, Company: CompanyC, Organiser: NULL, Emails: [contact3@example.com]
```

**Email Collection** (before deduplication):
```
All emails: [
  shared@example.com,   # from P1
  contact1@example.com, # from P1
  shared@example.com,   # from P2 (DUPLICATE)
  contact2@example.com, # from P2
  contact3@example.com  # from P3
]
```

**After Deduplication** (`.distinct()`):
```
Unique recipients: [
  shared@example.com,
  contact1@example.com,
  contact2@example.com,
  contact3@example.com
]
```

**Result**: 4 recipients (not 5) - `shared@example.com` receives only one email

---

## Performance Considerations

### Query Optimization
- **Index on partnerships.event_id**: Already exists for event-based filtering
- **Index on partnerships.organiser_user_id**: Recommended for organizer grouping performance
- **Index on company_emails.partnership_id**: Already exists for foreign key relationship

### In-Memory Processing
- Email deduplication uses Kotlin `distinct()` - O(n) time, O(n) space
- Organizer grouping uses `groupBy {}` - O(n) time, O(n) space
- Acceptable for ≤500 partnerships (spec constraint)

### Database Impact
- Single query fetches all partnerships + emails + organizers (JOIN operation)
- No N+1 query problem (all data fetched upfront)
- Read-only operation (no database writes)

---

## Security & Privacy

### Data Access Control
- `AuthorizedOrganisationPlugin` enforces user has edit permission for organization
- Only partnerships for specified event are accessible
- No cross-organization data leakage

### Email Address Exposure
- Contact emails already stored in database (existing data)
- No new PII storage
- Emails sent only to registered partnership contacts
- No external email address injection (all from database)

### Mailjet API Security
- API credentials encrypted at rest in database
- Transmitted via HTTPS to Mailjet
- Basic Auth header (Base64 encoded API key:secret)

---

## Migration Requirements

**None** - This feature requires NO database schema changes. All necessary tables and columns already exist.

---

## Summary

**Entities Involved**: 5 existing (Partnerships, PartnershipEmails, Events, Users, MailjetIntegrations)  
**New Tables**: 0  
**New Columns**: 0  
**Query Complexity**: Single JOIN query with optional filters  
**Data Volume**: ≤500 partnerships per request (spec constraint)  
**Performance**: Optimized via existing indexes, in-memory grouping/deduplication
