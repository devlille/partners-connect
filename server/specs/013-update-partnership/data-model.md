# Data Model: Update Partnership Contact Information

**Feature**: 013-update-partnership  
**Date**: 2025-11-29

## Entity Changes

### Partnership Entity

**Existing Fields** (no changes):
- `id`: UUID (primary key)
- `eventId`: UUID (foreign key to events)
- `companyId`: UUID (foreign key to companies)
- `contactName`: String (partner contact name)
- `contactRole`: String (partner role/title)
- `language`: String (preferred communication language)
- `phone`: String? (phone number, nullable)

**New Fields**:
- `emails`: Array<String> (list of contact email addresses)
  - Type: PostgreSQL text array / Kotlin List<String>
  - Default: empty list
  - Nullable: No (array can be empty)
  - Validation: Each email must be valid RFC 5322 format

**Unchanged Fields** (for context):
- `selectedPackId`: UUID? (selected sponsoring pack)
- `validatedAt`: LocalDateTime? (validation timestamp)
- `createdAt`: LocalDateTime (creation timestamp)
- _(plus other partnership fields not relevant to contact info)_

## Domain Model

### UpdatePartnershipContactInfo (Request DTO)

```kotlin
@Serializable
data class UpdatePartnershipContactInfo(
    @SerialName("contact_name") 
    val contactName: String? = null,
    
    @SerialName("contact_role") 
    val contactRole: String? = null,
    
    val language: String? = null,
    
    val phone: String? = null,
    
    val emails: List<String>? = null
)
```

**Field Constraints**:
- `contactName`: Optional, 1-255 characters if provided
- `contactRole`: Optional, 1-255 characters if provided  
- `language`: Optional, must be valid ISO 639-1 code (en, fr, de, nl, es)
- `phone`: Optional, 1-30 characters if provided (free-form text)
- `emails`: Optional array, each email must match RFC 5322 format

**Partial Update Semantics**:
- All fields are nullable to support partial updates with PUT
- Null values mean "don't update this field"
- Explicit null can clear optional fields (e.g., phone)
- Empty array clears emails list
- If all fields are null, operation is no-op (still returns 200 OK)

### Partnership (Response DTO)

**Existing Response** (no changes to structure, only data):
```kotlin
data class Partnership(
    val id: UUID,
    val eventId: UUID,
    val companyId: UUID,
    val contactName: String,
    val contactRole: String,
    val language: String,
    val phone: String?,
    val emails: List<String>,  // NEW FIELD
    // ... other fields
)
```

**Response After Update**:
- Returns complete partnership object with updated contact information
- HTTP 200 OK status
- Includes all partnership fields, not just updated ones

## Database Schema Changes

### Migration Required

**Add emails column to partnerships table**:

```sql
-- Migration: Add emails array column to partnerships
ALTER TABLE partnerships 
ADD COLUMN emails TEXT[] DEFAULT '{}' NOT NULL;
```

**Exposed Table Definition Update**:

```kotlin
object PartnershipsTable : UUIDTable("partnerships") {
    // ... existing columns ...
    val contactName = text("contact_name")
    val contactRole = text("contact_role")
    val language = text("language")
    val phone = text("phone").nullable()
    val emails = array<String>("emails").default(emptyList())  // NEW
    // ... other columns ...
}
```

**Exposed Entity Update**:

```kotlin
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)
    
    // ... existing properties ...
    var contactName by PartnershipsTable.contactName
    var contactRole by PartnershipsTable.contactRole
    var language by PartnershipsTable.language
    var phone by PartnershipsTable.phone
    var emails by PartnershipsTable.emails  // NEW
    // ... other properties ...
}
```

## Validation Rules

### Request Validation (JSON Schema)

**contact_name**:
- Type: string (optional)
- Min length: 1
- Max length: 255
- Pattern: Any non-empty string

**contact_role**:
- Type: string (optional)
- Min length: 1
- Max length: 255
- Pattern: Any non-empty string

**language**:
- Type: string (optional)
- Enum: ["en", "fr", "de", "nl", "es"]
- Case-sensitive

**phone**:
- Type: string or null (optional)
- Min length: 1
- Max length: 30
- Pattern: Free-form text (no specific format)

**emails**:
- Type: array of strings (optional)
- Items: Each string must match email format (RFC 5322)
- Min items: 0 (empty array allowed)
- Max items: Unlimited (practical limit: 10 for performance)

### Business Rules

1. **Partial Updates**: Only provided fields are updated, others remain unchanged
2. **Event Existence**: Partnership must belong to existing event (404 if not found)
3. **Partnership Existence**: Partnership ID must exist (404 if not found)
4. **Language Validation**: Must be supported ISO 639-1 code (400 if invalid)
5. **Email Validation**: Each email must be valid format (400 if invalid)
6. **Phone Length**: Must not exceed 30 characters (400 if too long)
7. **No Authentication**: Endpoint is completely public (per clarification)

## State Transitions

**No state changes** - this endpoint only updates contact information fields. Partnership status fields (validatedAt, declinedAt, etc.) are not affected by contact information updates.

## Entity Relationships

```text
Partnership (1) ──── (1) Event
    │
    └──── (1) Company

Contact Information Fields (within Partnership):
├── contactName: String
├── contactRole: String  
├── language: String
├── phone: String?
└── emails: List<String>  [NEW]
```

**Relationships Unchanged**:
- Partnership still belongs to one Event (via eventId)
- Partnership still belongs to one Company (via companyId)
- Contact information is partnership-specific, not shared

## Query Patterns

### Update Operation

```kotlin
// Pseudo-code for repository update
fun updateContactInfo(
    eventSlug: String, 
    partnershipId: UUID, 
    update: UpdatePartnershipContactInfo
): Partnership {
    val entity = PartnershipEntity.singleByEventAndPartnership(
        eventRepository.getBySlug(eventSlug).id,
        partnershipId
    ) ?: throw NotFoundException("Partnership not found")
    
    // Apply only provided fields
    update.contactName?.let { entity.contactName = it }
    update.contactRole?.let { entity.contactRole = it }
    update.language?.let { entity.language = it }
    update.phone?.let { entity.phone = it }
    update.emails?.let { entity.emails = it }
    
    return entity.toDomain()
}
```

### Lookup by Event and Partnership

**Existing Pattern** (no changes):
```kotlin
PartnershipEntity.singleByEventAndPartnership(eventId: UUID, partnershipId: UUID)
```

## Migration Strategy

### Backwards Compatibility

**Existing partnerships without emails**:
- Default value: empty array `{}`
- No null values in database
- Existing code reading partnerships must handle empty array

**RegisterPartnership endpoint**:
- Currently accepts `emails: List<String>` parameter
- Need to verify if emails are currently persisted or ignored
- If ignored: start persisting emails during registration
- If persisted elsewhere: migrate existing data

### Rollout Plan

1. **Database Migration**: Add emails column with default empty array
2. **Code Deployment**: Deploy partnership update endpoint
3. **Data Migration** (if needed): Migrate existing email data to partnerships table
4. **Validation**: Verify existing partnerships handle empty emails array
5. **Monitoring**: Track update endpoint usage and errors

## Performance Considerations

**Database Impact**:
- Array column adds minimal storage overhead
- No new indexes required (updates by UUID primary key)
- PostgreSQL array operations are efficient for small arrays (<10 emails)

**Query Performance**:
- Update by UUID is O(1) with primary key index
- No additional joins required
- Transaction overhead minimal (single table update)

**Expected Load**:
- Low frequency updates (contact info changes infrequently)
- <100 updates/day expected
- Response time <100ms for database update
