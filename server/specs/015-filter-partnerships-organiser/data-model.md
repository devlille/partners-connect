# Data Model: Filter Partnerships by Assigned Organiser

**Feature**: 015-filter-partnerships-organiser  
**Date**: December 29, 2025  
**Status**: Draft

---

## Overview

This feature extends the partnership list endpoint with organiser filtering capability and enhances the PaginatedResponse model to include pagination metadata containing available filters (including organisers list) and sorts arrays.

**No Database Schema Changes Required**: Feature leverages existing `partnerships.organiser_user_id` foreign key established in spec 011 (Assign Partnership Organiser).

---

## Modified Domain Models

### PartnershipFilters (Extended)

**Location**: `partnership/domain/PartnershipItem.kt`

```kotlin
@Serializable
data class PartnershipFilters(
    val packId: String? = null,
    val validated: Boolean? = null,
    val suggestion: Boolean? = null,
    val paid: Boolean? = null,
    val agreementGenerated: Boolean? = null,
    val agreementSigned: Boolean? = null,
    val organiser: String? = null,  // NEW: Email address for filtering
)
```

**Changes**:
- **Added**: `organiser: String?` - Optional email address for filtering partnerships by assigned organiser

**Usage**: Constructed from query parameters in route handlers, passed to repository methods.

---

### PaginatedResponse (Enhanced)

**Location**: `internal/infrastructure/api/PaginatedResponse.kt`

```kotlin
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
    val total: Long,
    val metadata: PaginationMetadata? = null,  // NEW: Optional metadata
)
```

**Changes**:
- **Added**: `metadata: PaginationMetadata?` - Optional metadata containing filters and sorts arrays

**Backwards Compatibility**: 
- Nullable field allows existing API consumers to ignore new field
- Always populated in responses (per FR-005) but structurally optional for type safety

---

## New Domain Models

### FilterType (Enum)

**Purpose**: Enum defining available filter data types

```kotlin
@Serializable
enum class FilterType {
    @SerialName("string")
    STRING,
    
    @SerialName("boolean")
    BOOLEAN,
}
```

**Values**:
- `STRING`: Filter accepts string values (e.g., pack_id, organiser)
- `BOOLEAN`: Filter accepts boolean values (e.g., validated, paid)

---

### PaginationMetadata

**Purpose**: Container for pagination metadata including available filters and sorts

```kotlin
@Serializable
data class PaginationMetadata(
    val filters: List<FilterDefinition>,
    val sorts: List<String>,
)
```

**Fields**:
- `filters`: Array of filter definitions describing all available filters for the endpoint
- `sorts`: Array of field names that can be used for sorting (e.g., ["created", "validated"])

**Example JSON**:
```json
{
  "filters": [
    { "name": "pack_id", "type": "string" },
    { "name": "validated", "type": "boolean" },
    { "name": "organiser", "type": "string", "values": [...] }
  ],
  "sorts": ["created", "validated"]
}
```

---

### FilterDefinition

**Purpose**: Describes a single filter parameter with its type and optional values

```kotlin
@Serializable
data class FilterDefinition(
    val name: String,
    val type: FilterType,
    val values: List<FilterValue>? = null,
)
```

**Fields**:
- `name`: Filter parameter name (e.g., "organiser", "validated", "pack_id")
- `type`: Data type of filter (FilterType.STRING or FilterType.BOOLEAN)
- `values`: Optional array of valid options (only populated for organiser filter per FR-010)

**Rules**:
- Only the `organiser` filter includes the `values` array
- Other filters (validated, paid, etc.) have `values: null`

**Example JSON**:
```json
{
  "name": "organiser",
  "type": "string",
  "values": [
    { "value": "john.doe@example.com", "display_value": "John Doe" },
    { "value": "jane.smith@example.com", "display_value": "Jane Smith" }
  ]
}
```

---

### FilterValue

**Purpose**: Represents a valid option for the organiser filter (user with edit permissions)

```kotlin
@Serializable
data class FilterValue(
    val value: String,
    @SerialName("display_value")
    val displayValue: String,
)
```

**Fields**:
- `value`: Filter value (e.g., email address for organiser filter)
- `displayValue`: Human-readable display name (e.g., full name for organiser filter)

**Source**: Derived from organisation permissions where `canEdit=true`

**Example JSON**:
```json
{
  "value": "john.doe@example.com",
  "display_value": "John Doe"
}
```

---

## Existing Entities (Reused)

### Partnership (PartnershipsTable)

**Relevant Fields**:
- `id`: UUID primary key
- `event_id`: Foreign key to events table
- `organiser_user_id`: **Optional** foreign key to users table (established in spec 011)
- `validated_at`: Timestamp for validation filter
- `suggestion_pack_id`: Foreign key for suggestion filter
- `billing_id`: Foreign key for paid filter
- `agreement_url`: String for agreement-generated filter

**Relationships**:
- One Partnership → Optional User (organiser)
- One Partnership → One Event
- One Partnership → One Company

**Organiser Relationship** (Exposed ORM):
```kotlin
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)
    
    var organiser by UserEntity optionalReferencedOn PartnershipsTable.organiserUserId
    // ... other properties
}
```

---

### User (UsersTable)

**Relevant Fields**:
- `id`: UUID primary key
- `email`: String (unique, case-insensitive comparison)
- `name`: String (formatted display name from firstname/lastname)
- `picture_url`: String (profile picture)

**Usage**:
- Source for organiser filter values via organisation permissions
- Referenced by `partnerships.organiser_user_id` FK

---

### OrganisationPermission (OrganisationPermissionsTable)

**Relevant Fields**:
- `id`: UUID primary key
- `organisation_id`: Foreign key to organisations table
- `user_id`: Foreign key to users table
- `can_edit`: Boolean (true for edit permissions, false for view-only)

**Usage**: Query `can_edit=true` permissions to populate available organisers list

**Query Pattern** (New):
```kotlin
fun UUIDEntityClass<OrganisationPermissionEntity>.listEditorsbyOrgId(
    orgId: UUID
): SizedIterable<OrganisationPermissionEntity> = this.find {
    (OrganisationPermissionsTable.organisationId eq orgId) and
    (OrganisationPermissionsTable.canEdit eq true)
}
```

---

## Repository Methods

### PartnershipRepository.listByEvent (Modified)

**Purpose**: List partnerships with filtering and pagination metadata

```kotlin
fun listByEvent(
    eventSlug: String,
    filters: PartnershipFilters,
    direction: String,
): PaginatedResponse<PartnershipItem>
```

**Changes**:
- **Input**: Now accepts `filters.organiser` email address
- **Output**: Returns `PaginatedResponse<PartnershipItem>` with metadata (previously returned `List<PartnershipItem>`)
- **Logic**: Builds metadata with available organisers from organisation permissions

**Implementation Pattern**:
```kotlin
override fun listByEvent(
    eventSlug: String,
    filters: PartnershipFilters,
    direction: String,
): PaginatedResponse<PartnershipItem> = transaction {
    val event = EventEntity.findBySlug(eventSlug)
        ?: throw NotFoundException("Event with slug $eventSlug not found")
    val eventId = event.id.value
    val sort = if (direction == "asc") SortOrder.ASC else SortOrder.DESC
    
    // Resolve organiser email to userId (case-insensitive)
    val organiserUserId = if (filters.organiser != null) {
        UserEntity.singleUserByEmail(filters.organiser)?.id?.value
    } else null
    
    // Apply filters including organiser
    val partnerships = PartnershipEntity
        .filters(
            eventId = eventId,
            packId = filters.packId?.toUUID(),
            validated = filters.validated,
            suggestion = filters.suggestion,
            agreementGenerated = filters.agreementGenerated,
            agreementSigned = filters.agreementSigned,
            organiserUserId = organiserUserId,  // NEW
        )
        .orderBy(PartnershipsTable.createdAt to sort)
    
    // Apply paid filter (entity-level check)
    val filteredPartnerships = if (filters.paid != null) {
        partnerships.filter {
            val billing = BillingEntity.singleByEventAndPartnership(eventId, it.id.value)
            if (filters.paid) billing?.status == InvoiceStatus.PAID else billing?.status != InvoiceStatus.PAID
        }
    } else {
        partnerships
    }
    
    // Build pagination metadata
    val metadata = buildMetadata(event.organisation.id.value)
    
    // Return paginated response with metadata
    val items = filteredPartnerships
        .paginated(page, pageSize)
        .map { it.toPartnershipItem() }
    
    PaginatedResponse(
        items = items,
        page = page,
        pageSize = pageSize,
        total = filteredPartnerships.count(),
        metadata = metadata,
    )
}

private fun buildMetadata(organisationId: UUID): PaginationMetadata {
    // Query organisation editors for available organisers
    val editors = OrganisationPermissionEntity.listEditorsbyOrgId(organisationId)
    val organiserValues = editors.map { permission ->
        FilterValue(
            value = permission.user.email,
            displayValue = permission.user.name,
        )
    }.distinctBy { it.value }
    
    return PaginationMetadata(
        filters = listOf(
            FilterDefinition("pack_id", FilterType.STRING, null),
            FilterDefinition("validated", FilterType.BOOLEAN, null),
            FilterDefinition("suggestion", FilterType.BOOLEAN, null),
            FilterDefinition("paid", FilterType.BOOLEAN, null),
            FilterDefinition("agreement-generated", FilterType.BOOLEAN, null),
            FilterDefinition("agreement-signed", FilterType.BOOLEAN, null),
            FilterDefinition("organiser", FilterType.STRING, organiserValues),
        ),
        sorts = listOf("created", "validated"),
    )
}
```

**Key Points**:
- Email resolution happens within repository (case-insensitive via `singleUserByEmail`)
- Metadata built in same transaction (single DB round-trip)
- Distinct on value ensures unique organisers list
- Returns all editors regardless of partnership assignments (per FR-010)

---

### PartnershipEntity.filters() (Extended)

**Purpose**: Companion method for building filter query

```kotlin
companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable) {
    @Suppress("LongParameterList")
    fun filters(
        eventId: UUID,
        packId: UUID?,
        validated: Boolean?,
        suggestion: Boolean?,
        agreementGenerated: Boolean?,
        agreementSigned: Boolean?,
        organiserUserId: UUID?,  // NEW
    ): SizedIterable<PartnershipEntity> {
        var op = PartnershipsTable.eventId eq eventId
        
        if (packId != null) {
            op = op and (PartnershipsTable.selectedPackId eq packId)
        }
        
        if (validated != null) {
            op = if (validated) {
                op and (PartnershipsTable.validatedAt.isNotNull())
            } else {
                op and (PartnershipsTable.validatedAt.isNull())
            }
        }
        
        if (suggestion != null) {
            op = if (suggestion) {
                op and (PartnershipsTable.suggestionPackId.isNotNull())
            } else {
                op and (PartnershipsTable.suggestionPackId.isNull())
            }
        }
        
        if (agreementGenerated != null) {
            op = if (agreementGenerated) {
                op and (PartnershipsTable.agreementUrl.isNotNull())
            } else {
                op and (PartnershipsTable.agreementUrl.isNull())
            }
        }
        
        if (agreementSigned != null) {
            op = if (agreementSigned) {
                op and (PartnershipsTable.agreementSignedAt.isNotNull())
            } else {
                op and (PartnershipsTable.agreementSignedAt.isNull())
            }
        }
        
        // NEW: Organiser filter
        if (organiserUserId != null) {
            op = op and (PartnershipsTable.organiserUserId eq organiserUserId)
        }
        
        return find(op)
    }
}
```

**Changes**:
- **Added**: `organiserUserId: UUID?` parameter
- **Logic**: Filters by `organiser_user_id` FK when provided
- **Behavior**: Excludes partnerships with `NULL` organiser when filter applied (per FR-004)

---

### PartnershipEmailRepository.getPartnershipDestination (Modified)

**Purpose**: Fetch partnerships for email sending with filter support

**Changes**: Same `PartnershipFilters` data class, automatically supports organiser filter

**No Code Changes Required**: Email repository already accepts `PartnershipFilters`, which now includes `organiser` field. Repository implementation will automatically filter by organiser when provided.

**Existing Implementation**:
```kotlin
override suspend fun getPartnershipDestination(
    eventSlug: String,
    filters: PartnershipFilters,
): List<Destination> = transaction {
    // ... existing logic uses filters
    val partnerships = PartnershipEntity.filters(
        eventId = eventId,
        packId = filters.packId?.toUUID(),
        validated = filters.validated,
        suggestion = filters.suggestion,
        agreementGenerated = filters.agreementGenerated,
        agreementSigned = filters.agreementSigned,
        organiserUserId = organiserUserId,  // Will be passed when filters.organiser present
    )
    // ...
}
```

---

## Query Patterns

### Primary Query (Partnership List with Organiser Filter)

```sql
SELECT 
    p.id,
    p.event_id,
    p.organiser_user_id,
    p.validated_at,
    p.created_at,
    u.email AS organiser_email,
    u.name AS organiser_name
FROM partnerships p
LEFT JOIN users u ON p.organiser_user_id = u.id
WHERE p.event_id = :eventId
  AND (p.organiser_user_id = :organiserUserId OR :organiserUserId IS NULL)
  AND (p.validated_at IS NOT NULL OR :validated = FALSE OR :validated IS NULL)
  AND (p.suggestion_pack_id IS NOT NULL OR :suggestion = FALSE OR :suggestion IS NULL)
  -- ... other filters
ORDER BY p.created_at ASC/DESC
LIMIT :pageSize OFFSET :offset;
```

**Performance**: 
- Indexed on `organiser_user_id` FK
- LEFT JOIN preserves partnerships without organisers (unless filtered)
- Estimated 200-500ms for typical dataset (1000 partnerships)

---

### Metadata Query (Available Organisers)

```sql
SELECT DISTINCT 
    u.id,
    u.email,
    u.name AS display_name
FROM organisation_permissions op
JOIN users u ON op.user_id = u.id
WHERE op.organisation_id = :orgId
  AND op.can_edit = TRUE
ORDER BY u.name;
```

**Performance**:
- Indexed on `organisation_id` FK and `can_edit` column
- Typical result set: 10-100 users
- Estimated 50-100ms

**Total Query Time**: ~250-600ms (well under 2-second target)

---

### Email Resolution (Case-Insensitive)

```sql
SELECT id, email, name, picture_url
FROM users
WHERE LOWER(email) = LOWER(:email)
LIMIT 1;
```

**Note**: Assumes PostgreSQL handles case-insensitive comparison via citext type or application-level lowercase().

---

## Data Flow

### 1. Partnership List Request with Organiser Filter

```
Client Request:
GET /orgs/devlille/events/2025/partnerships?filter[organiser]=john.doe@example.com

↓

Route Handler:
- Extract query parameters
- Construct PartnershipFilters(organiser = "john.doe@example.com")
- Call repository.listByEvent(eventSlug, filters, direction)

↓

Repository (transaction):
- Find event by slug
- Resolve email to userId (UserEntity.singleUserByEmail)
- Query partnerships (PartnershipEntity.filters with organiserUserId)
- Apply paid filter (entity-level check)
- Build metadata (query organisation editors)
- Return PaginatedResponse with metadata

↓

Response:
{
  "items": [...],
  "page": 1,
  "page_size": 20,
  "total": 5,
  "metadata": {
    "filters": [
      { "name": "pack_id", "type": "string" },
      { "name": "organiser", "type": "string", "values": [...] }
    ],
    "sorts": ["created", "validated"]
  }
}
```

---

### 2. Partnership Email Request with Organiser Filter

```
Client Request:
POST /orgs/devlille/events/2025/partnerships/email
Body: { "subject": "...", "body": "..." }
Query: ?filter[organiser]=jane.smith@example.com

↓

Route Handler:
- Extract query parameters
- Construct PartnershipFilters(organiser = "jane.smith@example.com")
- Call partnershipEmailRepository.getPartnershipDestination(eventSlug, filters)
- Send emails via notificationRepository (route-layer orchestration)

↓

Repository (transaction):
- Same filtering logic as list endpoint
- Returns Destination objects for email sending

↓

Response:
204 No Content (or 404 if no partnerships match)
```

---

## Validation Rules

### Filter Parameter Validation

- **organiser**: String, optional, email format (no explicit validation, exact match used)
- **Case Insensitivity**: Email comparison case-insensitive per FR-006
- **Empty String**: Treated as null (no filter applied)

### Metadata Population Rules

- **Always Included**: Metadata present in every response per FR-005
- **Available Organisers**: All organisation members with `canEdit=true` per FR-009
- **Distinct Emails**: Deduplicated on email field per FR-010
- **Include Unassigned**: Users with no partnerships still included per FR-010

---

## Error Handling

### Email Not Found (Organiser Filter)

**Scenario**: `filter[organiser]=nonexistent@example.com`

**Behavior**: 
- Email resolution returns `null`
- `organiserUserId = null` passed to filters
- No filter applied (returns all partnerships)

**Alternative Considered**: Return HTTP 404 or empty list
**Decision**: Return all partnerships (treat as filter opt-out) for better UX

**Rationale**: User may mistype email; showing all partnerships better than empty result

### No Edit Permissions (Metadata)

**Scenario**: Organisation has no users with `canEdit=true`

**Behavior**:
- Metadata still included
- `organiser` filter has empty `values` array
- Other filters still listed

**Response Example**:
```json
{
  "items": [...],
  "metadata": {
    "filters": [
      { "name": "organiser", "type": "string", "values": [] }
    ],
    "sorts": ["created"]
  }
}
```

### No Partnerships Match Filter

**Scenario**: All partnerships assigned to different organiser

**Behavior**:
- HTTP 200 with empty items array
- Metadata still included
- Total count = 0

**Response Example**:
```json
{
  "items": [],
  "page": 1,
  "page_size": 20,
  "total": 0,
  "metadata": {
    "filters": [...]
  }
}
```

---

## JSON Schema Files

### Required New Schemas

1. **pagination_metadata.schema.json**: Defines metadata structure
2. **filter_definition.schema.json**: Defines individual filter structure
3. **filter_value.schema.json**: Defines organiser value structure

### Updated Schemas

1. **paginated_response.schema.json** (or create specific partnership_list_response.schema.json): Add metadata field

**See**: [contracts/schemas.md](contracts/schemas.md) for complete schema definitions

---

## Testing Considerations

### Factory Functions (Reused)

**No new factories needed** - use existing:
- `insertMockedPartnership(id, eventId, organiserUserId = null)`
- `insertMockedUser(id, email, name)`
- `insertMockedOrgaPermission(orgId, userId, canEdit = true)`

**Test Data Pattern**:
```kotlin
transaction {
    insertMockedOrganisationEntity(orgId)
    insertMockedFutureEvent(eventId, orgId = orgId)
    
    // Create users with edit permissions
    val user1 = insertMockedUser(userId1, email = "john@example.com", name = "John Doe")
    val user2 = insertMockedUser(userId2, email = "jane@example.com", name = "Jane Smith")
    insertMockedOrgaPermission(orgId, userId1, canEdit = true)
    insertMockedOrgaPermission(orgId, userId2, canEdit = true)
    
    // Create partnerships with assigned organisers
    insertMockedPartnership(p1Id, eventId, organiserUserId = userId1)
    insertMockedPartnership(p2Id, eventId, organiserUserId = userId2)
    insertMockedPartnership(p3Id, eventId, organiserUserId = null)  // Unassigned
}
```

---

## Summary

**Modified Components**:
- PartnershipFilters: Added `organiser` field
- PaginatedResponse: Added `metadata` field
- PartnershipEntity.filters(): Added `organiserUserId` parameter
- PartnershipRepository.listByEvent(): Returns PaginatedResponse with metadata

**New Components**:
- PaginationMetadata: Metadata wrapper
- FilterDefinition: Filter descriptor
- FilterValue: Organiser option
- OrganisationPermissionEntity.listEditorsbyOrgId(): Query for available organisers

**No Database Changes**: Leverages existing schema from spec 011

**Performance Target**: Sub-2-second response time (estimated 250-600ms actual)

**Backwards Compatibility**: Metadata field nullable, existing clients can ignore
