# Data Model: Job Offer Promotion

## Overview
This document defines the data entities, relationships, and validation rules for the job offer promotion workflow. The model extends existing entities (CompanyJobOffer, Partnership, Event) with a new promoted job offer entity that tracks promotion lifecycle and approval workflow.

**Technical Note**: All date/time fields use `DATETIME` column type (PostgreSQL), which maps to `LocalDateTime` in Kotlin via Exposed ORM's `datetime()` function. This is consistent with the project's standard practice (no `TIMESTAMP` type used).

---

## Entity Definitions

### PromotedJobOffer (NEW)

**Purpose**: Tracks job offer promotions to event partnerships with approval workflow state.

**Table**: `company_job_offer_promotions`

**Fields**:
| Field Name | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Unique promotion identifier |
| job_offer_id | UUID | FOREIGN KEY → company_job_offers.id, NOT NULL, ON DELETE CASCADE | Source job offer being promoted |
| partnership_id | UUID | FOREIGN KEY → partnerships.id, NOT NULL, ON DELETE NO ACTION | Target partnership for promotion |
| event_id | UUID | FOREIGN KEY → events.id, NOT NULL, ON DELETE NO ACTION | Target event (denormalized for query performance) |
| status | VARCHAR(20) | NOT NULL, CHECK IN ('pending', 'approved', 'declined') | Current promotion state |
| promoted_at | DATETIME | NOT NULL | When promotion was submitted/re-submitted |
| reviewed_at | DATETIME | NULL | When approval/decline occurred |
| reviewed_by | UUID | FOREIGN KEY → users.id, NULL | User who approved/declined |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Record creation timestamp |
| updated_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last modification timestamp |

**Indexes**:
- `idx_promotions_job_offer` ON (job_offer_id) - Query promotions for specific job offer (FR-028)
- `idx_promotions_partnership` ON (partnership_id) - Query promotions for partnership
- `idx_promotions_event_status` ON (event_id, status) - Filter by event and status (FR-026, FR-027)
- `unique_job_offer_partnership` ON (job_offer_id, partnership_id) - Prevent true duplicates (though we upsert for re-promotion)

**Validation Rules**:
- `status` must transition: NULL → pending → (approved | declined)
- Re-promotion allowed: declined → pending (FR-031)
- `reviewed_at` and `reviewed_by` must be NULL when status=pending
- `reviewed_at` and `reviewed_by` must be NOT NULL when status IN (approved, declined)
- `promoted_at` must be ≤ CURRENT_TIMESTAMP (datetime comparison)
- `reviewed_at` must be ≥ `promoted_at` when not NULL (datetime comparison)

**Relationships**:
- **MANY-TO-ONE** with CompanyJobOffer (CASCADE delete when job offer deleted per FR-024)
- **MANY-TO-ONE** with Partnership (NO ACTION on partnership delete per FR-032)
- **MANY-TO-ONE** with Event (denormalized, NO ACTION on event delete)
- **MANY-TO-ONE** with User (reviewer, NULL for pending promotions)

**Exposed Table Implementation** (FR-033, FR-034, FR-036):
```kotlin
object CompanyJobOfferPromotionsTable : UUIDTable("company_job_offer_promotions") {
    val jobOfferId = reference("job_offer_id", CompanyJobOfferTable, onDelete = ReferenceOption.CASCADE)
    val partnershipId = reference("partnership_id", PartnershipsTable, onDelete = ReferenceOption.NO_ACTION)
    val eventId = reference("event_id", EventsTable, onDelete = ReferenceOption.NO_ACTION)
    val status = enumerationByName<PromotionStatus>("status", 20)
    val promotedAt = datetime("promoted_at")
    val reviewedAt = datetime("reviewed_at").nullable()
    val reviewedBy = reference("reviewed_by", UsersTable).nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    val updatedAt = datetime("updated_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        index(false, jobOfferId)
        index(false, partnershipId)
        index(false, eventId, status)
        uniqueIndex(jobOfferId, partnershipId)
    }
}
```

**Exposed Entity Implementation** (FR-033, FR-035):
```kotlin
class CompanyJobOfferPromotionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyJobOfferPromotionEntity>(CompanyJobOfferPromotionsTable)

    var jobOffer by CompanyJobOfferEntity referencedOn CompanyJobOfferPromotionsTable.jobOfferId
    var partnership by PartnershipEntity referencedOn CompanyJobOfferPromotionsTable.partnershipId
    var event by EventEntity referencedOn CompanyJobOfferPromotionsTable.eventId
    var status by CompanyJobOfferPromotionsTable.status
    var promotedAt by CompanyJobOfferPromotionsTable.promotedAt
    var reviewedAt by CompanyJobOfferPromotionsTable.reviewedAt
    var reviewedBy by UserEntity optionalReferencedOn CompanyJobOfferPromotionsTable.reviewedBy
    val createdAt by CompanyJobOfferPromotionsTable.createdAt
    var updatedAt by CompanyJobOfferPromotionsTable.updatedAt
}
```

---

### PromotionStatus (NEW ENUM)

**Purpose**: Type-safe status enumeration for promotion lifecycle.

**Values**:
| Status | Description | Transitions From | Transitions To |
|--------|-------------|------------------|----------------|
| pending | Awaiting organizer review | - (initial), declined | approved, declined |
| approved | Accepted by organizer | pending | - (terminal) |
| declined | Rejected by organizer | pending | pending (via re-promotion) |

**Implementation**:
```kotlin
enum class PromotionStatus {
    PENDING,
    APPROVED,
    DECLINED
}
```

---

## Request/Response Models

### PromoteJobOfferRequest (NEW)

**Purpose**: Company owner request to promote job offer to partnership.

**Fields**:
| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| job_offer_id | UUID (string) | Yes | Must exist in company_job_offers for this company | Job offer to promote |
| partnership_id | UUID (string) | Yes | Must be active partnership for this company | Target partnership |

**JSON Schema**: `promote_job_offer.schema.json`
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["job_offer_id", "partnership_id"],
  "properties": {
    "job_offer_id": {
      "type": "string",
      "format": "uuid",
      "description": "UUID of the job offer to promote"
    },
    "partnership_id": {
      "type": "string",
      "format": "uuid",
      "description": "UUID of the target partnership"
    }
  },
  "additionalProperties": false
}
```

**Validation**:
- Job offer must exist and belong to company (FR-001)
- Partnership must exist and link company to event (FR-002)
- Event must not have ended (FR-030)
- Allow re-promotion if status=declined (FR-031, upsert behavior)

---

### ApproveJobOfferPromotionRequest (NEW)

**Purpose**: Event organizer request to approve pending promotion.

**Fields**: Empty body (action endpoint, promotion ID in URL path)

**JSON Schema**: `approve_job_offer_promotion.schema.json`
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {},
  "additionalProperties": false
}
```

**Validation**:
- Promotion must exist (FR-006)
- Promotion must have status=pending
- User must have event organization canEdit=true permission (FR-008)

---

### DeclineJobOfferPromotionRequest (NEW)

**Purpose**: Event organizer request to decline pending promotion with optional reason.

**Fields**:
| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| reason | string | No | Max 500 characters | Explanation for decline (for notifications) |

**JSON Schema**: `decline_job_offer_promotion.schema.json`
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "reason": {
      "type": ["string", "null"],
      "maxLength": 500,
      "description": "Optional reason for declining the job offer promotion"
    }
  },
  "additionalProperties": false
}
```

**Validation**:
- Promotion must exist (FR-007)
- Promotion must have status=pending
- User must have event organization canEdit=true permission (FR-008)

---

### JobOfferPromotionResponse (NEW)

**Purpose**: Complete promotion details returned by API.

**Fields**:
| Field | Type | Always Present | Description |
|-------|------|----------------|-------------|
| id | UUID (string) | Yes | Promotion unique identifier |
| job_offer_id | UUID (string) | Yes | Source job offer ID |
| partnership_id | UUID (string) | Yes | Target partnership ID |
| event_id | UUID (string) | Yes | Target event ID |
| status | string enum | Yes | Current status: "pending", "approved", "declined" |
| promoted_at | ISO datetime | Yes | Submission/re-submission timestamp |
| reviewed_at | ISO datetime | No | Approval/decline timestamp (null when pending) |
| reviewed_by | UUID (string) | No | Reviewer user ID (null when pending) |
| job_offer | JobOfferResponse | Yes | Embedded job offer details (title, url, etc.) |
| created_at | ISO datetime | Yes | Record creation timestamp |
| updated_at | ISO datetime | Yes | Last modification timestamp |

**JSON Schema**: `job_offer_promotion_response.schema.json`
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["id", "job_offer_id", "partnership_id", "event_id", "status", "promoted_at", "job_offer", "created_at", "updated_at"],
  "properties": {
    "id": { "type": "string", "format": "uuid" },
    "job_offer_id": { "type": "string", "format": "uuid" },
    "partnership_id": { "type": "string", "format": "uuid" },
    "event_id": { "type": "string", "format": "uuid" },
    "status": { "type": "string", "enum": ["pending", "approved", "declined"] },
    "promoted_at": { "type": "string", "format": "date-time" },
    "reviewed_at": { "type": ["string", "null"], "format": "date-time" },
    "reviewed_by": { "type": ["string", "null"], "format": "uuid" },
    "job_offer": { "$ref": "job_offer_response.schema.json" },
    "created_at": { "type": "string", "format": "date-time" },
    "updated_at": { "type": "string", "format": "date-time" }
  },
  "additionalProperties": false
}
```

---

## Relationship Diagram

```
┌─────────────────┐
│ CompanyJobOffer │
│  (existing)     │
└────────┬────────┘
         │ 1
         │
         │ * (CASCADE on delete)
         ▼
┌─────────────────────────┐
│ PromotedJobOffer (NEW)  │
│ - id                    │
│ - job_offer_id          │──────┐
│ - partnership_id        │──┐   │
│ - event_id              │──┼───┼──────┐
│ - status                │  │   │      │
│ - promoted_at           │  │   │      │
│ - reviewed_at           │  │   │      │
│ - reviewed_by           │──┼───┼──────┼────┐
└─────────────────────────┘  │   │      │    │
         │                   │   │      │    │
         │ * (NO ACTION)     │   │      │    │
         ▼                   │   │      │    │
┌─────────────────┐          │   │      │    │
│   Partnership   │ ◄────────┘   │      │    │
│   (existing)    │              │      │    │
└─────────────────┘              │      │    │
                                 │      │    │
┌─────────────────┐              │      │    │
│     Event       │ ◄────────────┘      │    │
│   (existing)    │                     │    │
└─────────────────┘                     │    │
                                        │    │
┌─────────────────┐                     │    │
│      User       │ ◄───────────────────┘    │
│   (existing)    │                          │
└─────────────────┘                          │
         ▲                                   │
         └───────────────────────────────────┘
          * (reviewed_by, nullable)
```

**Key Constraints**:
- **Cascade Delete**: job_offer_id → CASCADE (FR-024: delete promotions when job offer deleted)
- **No Cascade**: partnership_id → NO ACTION (FR-032: preserve promotions when partnership terminated)
- **No Cascade**: event_id → NO ACTION (denormalized for performance, events rarely deleted)
- **Nullable FK**: reviewed_by → NULL for pending promotions

---

## State Transitions

### Promotion Lifecycle

```
     promote()
┌──────────────────┐
│   (no record)    │
└────────┬─────────┘
         │
         ▼
    ┌─────────┐    approve()     ┌──────────┐
    │ PENDING │──────────────────>│ APPROVED │
    └─────────┘                   └──────────┘
         │                        (terminal)
         │ decline()
         ▼
    ┌─────────┐    promote()      ┌─────────┐
    │DECLINED │──────────────────>│ PENDING │
    └─────────┘   (re-promotion)  └─────────┘
    (terminal                      (cycle repeats)
     until 
     re-promoted)
```

**Transition Rules**:
1. **Initial**: No record → PENDING (via promote)
2. **Approval**: PENDING → APPROVED (via approve, terminal)
3. **Decline**: PENDING → DECLINED (via decline)
4. **Re-promotion**: DECLINED → PENDING (via promote, FR-031)
5. **Invalid**: APPROVED → * (no transitions from approved)

**Timestamp Updates**:
- `promoted_at`: Set/reset on every promotion (initial or re-promotion)
- `reviewed_at`: Set when transitioning to APPROVED or DECLINED
- `reviewed_by`: Set when transitioning to APPROVED or DECLINED

---

## Query Patterns

### Company Queries (via CompanyJobOfferPromotionRepository)

**List promotions for a specific job offer** (FR-028):
```sql
SELECT * FROM company_job_offer_promotions
WHERE job_offer_id = ?
ORDER BY promoted_at DESC
```

**Check if job offer already promoted to partnership**:
```sql
SELECT id FROM company_job_offer_promotions
WHERE job_offer_id = ? AND partnership_id = ?
```

### Partnership Queries (via PartnershipJobOfferRepository)

**List all promotions for an event** (FR-026):
```sql
SELECT * FROM company_job_offer_promotions
WHERE event_id = ?
ORDER BY promoted_at DESC
```

**Filter promotions by status** (FR-027):
```sql
SELECT * FROM company_job_offer_promotions
WHERE event_id = ? AND status = ?
ORDER BY promoted_at DESC
```

**List pending promotions requiring action** (FR-029):
```sql
SELECT * FROM company_job_offer_promotions
WHERE event_id = ? AND status = 'pending'
ORDER BY promoted_at ASC
```

---

## Validation Matrix

| Requirement | Validation Point | Implementation |
|-------------|------------------|----------------|
| FR-001 | Company ownership | CompanyJobOfferRepository.findById + compare companyId |
| FR-002 | Partnership existence | PartnershipRepository.findById + verify active |
| FR-003 | Duplicate prevention | Upsert pattern (find existing, reset status) |
| FR-004 | Initial status | Set status=PENDING on new record |
| FR-005 | Link to entities | Foreign keys: job_offer_id, partnership_id, event_id |
| FR-006 | Approve permission | AuthorizedOrganisationPlugin validates JWT + canEdit permission |
| FR-007 | Decline permission | AuthorizedOrganisationPlugin validates JWT + canEdit permission |
| FR-008 | Edit permission | Same as FR-006/FR-007 (canEdit=true) |
| FR-037 | Plugin-based auth | install(AuthorizedOrganisationPlugin) on approve/decline routes |
| FR-009 | Approve status | Update: status=APPROVED, reviewed_at=now, reviewed_by=userId |
| FR-010 | Decline status | Update: status=DECLINED, reviewed_at=now, reviewed_by=userId |
| FR-011 | Timestamp | Set reviewed_at on approve/decline |
| FR-012 | Reviewer tracking | Set reviewed_by on approve/decline |
| FR-022 | Job offer FK | FOREIGN KEY job_offer_id REFERENCES company_job_offers(id) |
| FR-023 | Partnership FK | FOREIGN KEY partnership_id REFERENCES partnerships(id) |
| FR-024 | Cascade delete | ON DELETE CASCADE for job_offer_id |
| FR-025 | History | Immutable created_at, mutable updated_at |
| FR-030 | Event end check | Compare event.end_time < now() before INSERT |
| FR-031 | Re-promotion | Upsert: reset DECLINED→PENDING, update promoted_at |
| FR-032 | Partnership preservation | ON DELETE NO ACTION for partnership_id |
| FR-033 | Exposed ORM dual structure | CompanyJobOfferPromotionsTable (UUIDTable) + CompanyJobOfferPromotionEntity (UUIDEntity) |
| FR-034 | Table implementation | object CompanyJobOfferPromotionsTable : UUIDTable with all columns and indexes |
| FR-035 | Entity implementation | class CompanyJobOfferPromotionEntity : UUIDEntity with companion object |
| FR-036 | Datetime columns | Use datetime() for promoted_at, reviewed_at, created_at, updated_at (maps to LocalDateTime) |

---

## Denormalization Decisions

### Event ID in PromotedJobOffer

**Decision**: Store event_id directly in promotions table (denormalized from partnership.event_id).

**Rationale**:
- Query optimization: FR-026, FR-027, FR-029 all filter by event_id
- Avoids JOIN with partnerships table for most common queries
- Event data rarely changes (no consistency risk)
- Storage cost: 16 bytes (UUID) per promotion record

**Tradeoff**: Slight data redundancy for significant query performance gain.

---

## Schema Migration Strategy

### Phase 1: Create New Table
```sql
CREATE TABLE company_job_offer_promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_offer_id UUID NOT NULL REFERENCES company_job_offers(id) ON DELETE CASCADE,
    partnership_id UUID NOT NULL REFERENCES partnerships(id) ON DELETE NO ACTION,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE NO ACTION,
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'approved', 'declined')),
    promoted_at DATETIME NOT NULL,
    reviewed_at DATETIME,
    reviewed_by UUID REFERENCES users(id),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_promotions_job_offer ON company_job_offer_promotions(job_offer_id);
CREATE INDEX idx_promotions_partnership ON company_job_offer_promotions(partnership_id);
CREATE INDEX idx_promotions_event_status ON company_job_offer_promotions(event_id, status);
CREATE UNIQUE INDEX unique_job_offer_partnership ON company_job_offer_promotions(job_offer_id, partnership_id);
```

### Phase 2: No Data Migration
- New feature, no existing data to migrate
- Table starts empty

### Rollback Strategy
- Drop table: `DROP TABLE IF EXISTS company_job_offer_promotions CASCADE;`
- No impact on existing tables (additive change)

---

*Data model complete. All entities, relationships, and validation rules defined. Ready for contract generation.*
