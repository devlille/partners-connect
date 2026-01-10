# Data Model: Partnership Email History

**Feature**: 016-partnership-email-history  
**Date**: January 10, 2026

## Entity Relationship Diagram

```
┌─────────────────────────────────┐
│ PartnershipEmailHistory         │
├─────────────────────────────────┤
│ id: UUID (PK)                   │
│ partnershipId: UUID (FK)        │◄──────┐
│ sentAt: LocalDateTime           │       │
│ senderEmail: String             │       │ Many-to-One
│ subject: String                 │       │
│ bodyPlainText: Text             │       │
│ overallStatus: EmailStatus      │       │
│ triggeredBy: String             │       │
└─────────────────────────────────┘       │
        │                                  │
        │ One-to-Many                      │
        ▼                                  │
┌─────────────────────────────────┐       │
│ RecipientDeliveryStatus         │       │
├─────────────────────────────────┤       │
│ id: UUID (PK)                   │       │
│ emailHistoryId: UUID (FK)       │       │
│ recipientEmail: String          │       │
│ deliveryStatus: DeliveryStatus  │       │
│ failureReason: String?          │       │
└─────────────────────────────────┘       │
                                           │
                                           │
                                  ┌────────┴────────┐
                                  │ Partnership      │ (Existing)
                                  ├─────────────────┤
                                  │ id: UUID (PK)   │
                                  │ ...             │
                                  └─────────────────┘
```

## Domain Models

### PartnershipEmailHistory

Represents a single email sent to a partnership.

**Attributes**:
- `id: UUID` - Unique identifier
- `partnershipId: UUID` - Reference to partnership
- `sentAt: LocalDateTime` - When email was sent (UTC)
- `senderEmail: String` - "From" address used
- `subject: String` - Email subject line
- `bodyPlainText: String` - Email body content stored as-is (HTML or plain text)
- `overallStatus: EmailStatus` - Overall delivery status (SENT, FAILED, PARTIAL)
- `triggeredBy: String` - User ID of organiser or "system" for automated emails

**Business Rules**:
- Immutable after creation (no updates or deletes)
- Must have at least one recipient in `RecipientDeliveryStatus`
- Overall status derived from recipient statuses:
  - SENT: All recipients succeeded
  - FAILED: All recipients failed
  - PARTIAL: Mix of success and failure

**Relationships**:
- Belongs to one Partnership (many-to-one)
- Has many RecipientDeliveryStatus records (one-to-many)

---

### RecipientDeliveryStatus

Tracks delivery status for each recipient of an email.

**Attributes**:
- `id: UUID` - Unique identifier
- `emailHistoryId: UUID` - Reference to PartnershipEmailHistory
- `recipientEmail: String` - Email address of recipient
- `deliveryStatus: DeliveryStatus` - Per-recipient status (SENT, FAILED)

**Note**: No failureReason field - per research decision, system does not capture detailed failure reasons initially

**Business Rules**:
- Immutable after creation
- One record per recipient per email

**Relationships**:
- Belongs to one PartnershipEmailHistory (many-to-one)

---

### EmailStatus (Enum)

Overall delivery status for an email.

**Values**:
- `SENT` - All recipients successfully received email
- `FAILED` - All recipients failed to receive email
- `PARTIAL` - Some recipients succeeded, some failed

---

### DeliveryStatus (Enum)

Per-recipient delivery status.

**Values**:
- `SENT` - Recipient successfully received email
- `FAILED` - Delivery to recipient failed

---

### EmailDeliveryResult (Domain Model)

Provider-agnostic model for email delivery results.

**Attributes**:
- `overallStatus: EmailStatus` - Computed overall status
- `recipients: List<RecipientResult>` - Per-recipient results

**RecipientResult (Nested)**:
- `email: String` - Recipient email address
- `status: DeliveryStatus` - Delivery status for this recipient

**Purpose**: Abstraction layer between provider-specific responses (Mailjet) and domain models. `NotificationGateway` implementations map provider responses to this model.

---

## Database Schema (Exposed ORM)

### PartnershipEmailHistoryTable

```kotlin
object PartnershipEmailHistoryTable : UUIDTable("partnership_email_history") {
    val partnershipId = reference(
        "partnership_id",
        PartnershipsTable,
        onDelete = ReferenceOption.NO_ACTION
    )
    val sentAt = datetime("sent_at")
        .clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
    val senderEmail = varchar("sender_email", 255)
    val subject = varchar("subject", 500)
    val bodyPlainText = text("body_plain_text")
    val overallStatus = enumerationByName<EmailStatus>("overall_status", 20)
    val triggeredBy = varchar("triggered_by", 255)
    
    init {
        index(false, partnershipId, sentAt)  // For efficient chronological queries
    }
}
```

**Design Notes**:
- Uses `UUIDTable` per constitution standards
- Uses `datetime()` (NOT `timestamp()`) per standards
- `onDelete = NO_ACTION` preserves history even if partnership deleted (per FR-005)
- Composite index on (partnershipId, sentAt) for paginated retrieval
- Uses `text()` for body content to support unlimited size

---

### RecipientDeliveryStatusTable

```kotlin
object RecipientDeliveryStatusTable : UUIDTable("recipient_delivery_status") {
    val emailHistoryId = reference(
        "email_history_id",
        PartnershipEmailHistoryTable,
        onDelete = ReferenceOption.CASCADE
    )
    val recipientEmail = varchar("recipient_email", 255)
    val deliveryStatus = enumerationByName<DeliveryStatus>("delivery_status", 20)
    
    init {
        index(false, emailHistoryId)  // For efficient joins
        index(false, recipientEmail)  // For querying by recipient
    }
}
```

**Design Notes**:
- `onDelete = CASCADE` ensures orphan records are cleaned up
- Indexes on both foreign key and recipient email for query performance

---

## Entity Classes (Exposed ORM)

### PartnershipEmailHistoryEntity

```kotlin
class PartnershipEmailHistoryEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEmailHistoryEntity>(
        PartnershipEmailHistoryTable
    )
    
    var partnership by PartnershipEntity referencedOn 
        PartnershipEmailHistoryTable.partnershipId
    var sentAt by PartnershipEmailHistoryTable.sentAt
    var senderEmail by PartnershipEmailHistoryTable.senderEmail
    var subject by PartnershipEmailHistoryTable.subject
    var bodyPlainText by PartnershipEmailHistoryTable.bodyPlainText
    var overallStatus by PartnershipEmailHistoryTable.overallStatus
    var triggeredBy by PartnershipEmailHistoryTable.triggeredBy
    
    val recipients by RecipientDeliveryStatusEntity referrersOn 
        RecipientDeliveryStatusTable.emailHistoryId
}
```

---

### RecipientDeliveryStatusEntity

```kotlin
class RecipientDeliveryStatusEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<RecipientDeliveryStatusEntity>(
        RecipientDeliveryStatusTable
    )
    
    var emailHistory by PartnershipEmailHistoryEntity referencedOn 
        RecipientDeliveryStatusTable.emailHistoryId
    var recipientEmail by RecipientDeliveryStatusTable.recipientEmail
    var deliveryStatus by RecipientDeliveryStatusTable.deliveryStatus
}
```

---

## Domain Model Mapping

### Entity → Domain

```kotlin
fun PartnershipEmailHistoryEntity.toDomain(): PartnershipEmailHistory =
    PartnershipEmailHistory(
        id = id.value,
        partnershipId = partnership.id.value,
        sentAt = sentAt,
        senderEmail = senderEmail,
        subject = subject,
        bodyPlainText = bodyPlainText,
        overallStatus = overallStatus,
        triggeredBy = triggeredBy,
        recipients = recipients.map { it.toDomain() }
    )

fun RecipientDeliveryStatusEntity.toDomain(): RecipientResult =
    RecipientResult(
        email = recipientEmail,
        status = deliveryStatus
    )
```

---

## Data Validation Rules

### PartnershipEmailHistory
- `senderEmail`: Must be valid email format (regex validation)
- `subject`: Max 500 characters, min 1 character (non-blank)
- `bodyPlainText`: Min 1 character (non-blank), no maximum length, stored as-is
- `triggeredBy`: Max 255 characters, either valid UUID or "system"
- `partnershipId`: Must reference existing partnership

### RecipientDeliveryStatus
- `recipientEmail`: Must be valid email format
- `deliveryStatus`: Required enum value
- `emailHistoryId`: Must reference existing email history record

---

## Query Patterns

### Retrieve Email History for Partnership (Paginated)

```kotlin
fun findByPartnershipId(
    partnershipId: UUID,
    page: Int = 0,
    pageSize: Int = DEFAULT_PAGE_SIZE
): List<PartnershipEmailHistory> {
    return PartnershipEmailHistoryEntity
        .find { PartnershipEmailHistoryTable.partnershipId eq partnershipId }
        .orderBy(PartnershipEmailHistoryTable.sentAt to SortOrder.DESC)
        .limit(pageSize, offset = page * pageSize)
        .map { it.toDomain() }
}

fun countByPartnershipId(partnershipId: UUID): Long {
    return PartnershipEmailHistoryEntity
        .find { PartnershipEmailHistoryTable.partnershipId eq partnershipId }
        .count()
}
```

### Create Email History Record

```kotlin
fun create(
    partnershipId: UUID,
    deliveryResult: EmailDeliveryResult,
    senderEmail: String,
    subject: String,
    bodyPlainText: String,
    triggeredBy: String
): PartnershipEmailHistory = transaction {
    val history = PartnershipEmailHistoryEntity.new {
        this.partnership = PartnershipEntity[partnershipId]
        this.sentAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        this.senderEmail = senderEmail
        this.subject = subject
        this.bodyPlainText = bodyPlainText
        this.overallStatus = deliveryResult.overallStatus
        this.triggeredBy = triggeredBy
    }
    
    deliveryResult.recipients.forEach { recipient ->
        RecipientDeliveryStatusEntity.new {
            this.emailHistory = history
            this.recipientEmail = recipient.email
            this.deliveryStatus = recipient.status
        }
    }
    
    history.toDomain()
}
```

---

## Migration Strategy

**Initial Schema Creation**:
- Add tables to `MigrationRegistry` for automatic creation on startup
- Tables will be created in order: `PartnershipEmailHistoryTable`, then `RecipientDeliveryStatusTable`

**No Backward Migration**:
- This is additive-only (new tables, no modifications to existing tables)
- No backward migration needed

**Production Deployment**:
- Tables created automatically on first deployment
- No data migration required (no existing data)
- Zero downtime deployment (new tables don't affect existing functionality)

---

## Storage Estimates

**Per Email Record**:
- PartnershipEmailHistory: ~50KB (mainly body text)
- RecipientDeliveryStatus: ~300 bytes per recipient

**Scale Estimate**:
- 100 partnerships × 10 emails each = 1,000 emails
- 1,000 emails × 50KB ≈ 50MB
- 1,000 emails × 3 recipients avg × 300 bytes ≈ 1MB
- **Total**: ~51MB per 1,000 emails

**Reasonable for PostgreSQL, indefinite retention feasible.**
