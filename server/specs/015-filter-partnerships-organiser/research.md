# Research Findings: Filter Partnerships by Organiser

**Date**: December 29, 2025  
**Feature**: Filter Partnerships by Assigned Organiser  
**Status**: Complete

## Executive Summary

This research investigates the implementation patterns needed to add an organiser filter to the partnership list endpoint and enhance the PaginatedResponse model with pagination metadata containing available filters and sorts.

**Key Findings**:
1. PartnershipFilters data class needs new `organiser` field for email filtering
2. PaginatedResponse requires metadata field containing filters and sorts arrays
3. Existing partnership query uses `PartnershipEntity.filters()` method which needs organiser parameter
4. Organisation members query pattern exists for fetching edit-permission users
5. Partnership entity already has `organiser` relationship via optional foreign key
6. Email endpoint uses same PartnershipFilters pattern for consistency

---

## 1. Existing PartnershipFilters Structure

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
)
```

**Decision**: Add `organiser: String? = null` field to accept email address for filtering.

**Rationale**: Maintains consistency with existing filter pattern where all filters are optional nullable parameters. Email string matches convention of using domain-specific types (String for email, Boolean for flags, UUID string for IDs).

**Usage Pattern**:
- Filters constructed from query parameters in route handler
- Passed to repository method `listByEvent(eventSlug, filters, direction)`
- Applied via `PartnershipEntity.filters()` companion method

---

## 2. PartnershipEntity.filters() Query Method

**Location**: `partnership/infrastructure/db/PartnershipEntity.kt`

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
        // ... other filters
        return find(op)
    }
}
```

**Decision**: Add `organiserUserId: UUID?` parameter to filters() method, join with users table for case-insensitive email comparison.

**Rationale**: 
- UUID-based filtering more efficient than string comparison
- Requires resolving email to UUID before calling filters()
- Case-insensitive email lookup happens in route/repository layer
- Maintains separation: routes parse emails, repository filters by UUID

**Implementation Pattern**:
```kotlin
// In route layer: resolve email to userId first
val organiserUserId = if (filters.organiser != null) {
    UserEntity.singleUserByEmail(filters.organiser)?.id?.value
} else null

// Then pass UUID to entity filters
val partnerships = PartnershipEntity.filters(
    eventId = eventId,
    packId = filters.packId?.toUUID(),
    validated = filters.validated,
    organiserUserId = organiserUserId, // UUID or null
    // ...
)
```

---

## 3. PaginatedResponse Enhancement

**Location**: `internal/infrastructure/api/PaginatedResponse.kt`

```kotlin
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
    val total: Long,
)
```

**Decision**: Add optional `metadata` field of type `PaginationMetadata?` with filters and sorts.

**Rationale**:
- Optional field maintains backwards compatibility (existing clients can ignore)
- Always included per FR-005 (metadata included in every response)
- Nullable type allows framework serialization but always populated
- Separate data class for metadata improves type safety

**Proposed Structure**:
```kotlin
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
    val total: Long,
    val metadata: PaginationMetadata? = null,
)

@Serializable
data class PaginationMetadata(
    val filters: List<FilterDefinition>,
    val sorts: List<String>,
)

@Serializable
enum class FilterType {
    @SerialName("string")
    STRING,
    
    @SerialName("boolean")
    BOOLEAN,
}

@Serializable
data class FilterDefinition(
    val name: String,
    val type: FilterType,
    val values: List<FilterValue>? = null,
)

@Serializable
data class FilterValue(
    val value: String,
    @SerialName("display_value")
    val displayValue: String,
)
```

---

## 4. Organisation Members Query Pattern

**Location**: `users/infrastructure/db/OrganisationPermissionEntity.kt`

```kotlin
fun UUIDEntityClass<OrganisationPermissionEntity>.listUserGrantedByOrgId(
    orgId: UUID
): SizedIterable<OrganisationPermissionEntity> = this.find {
    OrganisationPermissionsTable.organisationId eq orgId
}
```

**Decision**: Create new query method filtering by `canEdit=true` for available organisers.

**Rationale**:
- FR-009 specifies "users with edit permissions or higher"
- Existing pattern for organisation membership queries
- Returns entities with user relationship for easy mapping

**Proposed Pattern**:
```kotlin
fun UUIDEntityClass<OrganisationPermissionEntity>.listEditorsbyOrgId(
    orgId: UUID
): SizedIterable<OrganisationPermissionEntity> = this.find {
    (OrganisationPermissionsTable.organisationId eq orgId) and
    (OrganisationPermissionsTable.canEdit eq true)
}
```

**Usage in Repository**:
```kotlin
// Fetch organisation
val event = EventEntity.findBySlug(eventSlug)
val organisation = event.organisation

// Get all editors
val editors = OrganisationPermissionEntity.listEditorsbyOrgId(organisation.id.value)

// Map to filter values
val organiserValues = editors.map { permission ->
    val user = permission.user
    FilterValue(
        email = user.email,
        displayName = user.name
    )
}.distinctBy { it.email } // Ensure uniqueness
```

---

## 5. Partnership Organiser Relationship

**Location**: `partnership/infrastructure/db/PartnershipEntity.kt`

Partnership entity already has organiser relationship established in spec 011:

```kotlin
class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)
    
    // ... other properties
    var organiser by UserEntity optionalReferencedOn PartnershipsTable.organiserUserId
}
```

**Key Points**:
- Optional relationship (nullable organiser)
- Foreign key `organiser_user_id` references `users.id`
- Already included in partnership responses
- No schema changes needed

---

## 6. Route Layer Filter Construction

**Location**: `partnership/infrastructure/api/PartnershipRoutes.kt`

```kotlin
get {
    val eventSlug = call.parameters.eventSlug
    
    val filters = PartnershipFilters(
        packId = call.request.queryParameters["filter[pack_id]"],
        validated = call.request.queryParameters["filter[validated]"]?.toBoolean(),
        suggestion = call.request.queryParameters["filter[suggestion]"]?.toBoolean(),
        paid = call.request.queryParameters["filter[paid]"]?.toBoolean(),
        agreementGenerated = call.request.queryParameters["filter[agreement-generated]"]?.toBoolean(),
        agreementSigned = call.request.queryParameters["filter[agreement-signed]"]?.toBoolean(),
    )
    
    val direction = call.request.queryParameters["direction"] ?: "asc"
    val partnerships = repository.listByEvent(eventSlug, filters, direction)
    call.respond(HttpStatusCode.OK, partnerships)
}
```

**Decision**: Add organiser parameter extraction, construct metadata, wrap response.

**Implementation Pattern**:
```kotlin
get {
    val eventSlug = call.parameters.eventSlug
    
    val filters = PartnershipFilters(
        packId = call.request.queryParameters["filter[pack_id]"],
        validated = call.request.queryParameters["filter[validated]"]?.toBoolean(),
        suggestion = call.request.queryParameters["filter[suggestion]"]?.toBoolean(),
        paid = call.request.queryParameters["filter[paid]"]?.toBoolean(),
        agreementGenerated = call.request.queryParameters["filter[agreement-generated]"]?.toBoolean(),
        agreementSigned = call.request.queryParameters["filter[agreement-signed]"]?.toBoolean(),
        organiser = call.request.queryParameters["filter[organiser]"], // NEW
    )
    
    val direction = call.request.queryParameters["direction"] ?: "asc"
    
    // Repository returns paginated response with metadata
    val response = repository.listByEvent(eventSlug, filters, direction)
    call.respond(HttpStatusCode.OK, response)
}
```

---

## 7. Email Endpoint Consistency

**Location**: `partnership/infrastructure/api/PartnershipEmailRoutes.kt`

Email endpoint uses same PartnershipFilters pattern:

```kotlin
post("/email") {
    val filters = PartnershipFilters(
        packId = call.request.queryParameters["filter[pack_id]"],
        validated = call.request.queryParameters["filter[validated]"]?.toBoolean(),
        // ...
    )
    
    val destinations = partnershipEmailRepository.getPartnershipDestination(eventSlug, filters)
    // ... send emails
}
```

**Decision**: Add organiser parameter to email endpoint filter construction.

**Rationale**:
- FR-014 requires same filter in email endpoint
- Maintains consistency with list endpoint
- No repository changes needed (same PartnershipFilters type)

---

## 8. Repository Layer Orchestration Pattern

**Reference**: Constitution Section III (Repository Pattern)

> Repository implementations MUST NOT depend on other repositories.  
> Routes inject multiple repositories, fetch data separately, then orchestrate.

**Decision**: Repository returns paginated data with metadata built from separate queries.

**Pattern**:
```kotlin
override fun listByEvent(
    eventSlug: String,
    filters: PartnershipFilters,
    direction: String,
): PaginatedResponse<PartnershipItem> = transaction {
    val event = EventEntity.findBySlug(eventSlug)
        ?: throw NotFoundException("Event with slug $eventSlug not found")
    
    // Resolve organiser email to UUID if provided
    val organiserUserId = if (filters.organiser != null) {
        UserEntity.singleUserByEmail(filters.organiser)?.id?.value
    } else null
    
    // Apply filters including organiser
    val partnerships = PartnershipEntity.filters(
        eventId = event.id.value,
        packId = filters.packId?.toUUID(),
        validated = filters.validated,
        suggestion = filters.suggestion,
        agreementGenerated = filters.agreementGenerated,
        agreementSigned = filters.agreementSigned,
        organiserUserId = organiserUserId, // NEW
    ).orderBy(PartnershipsTable.createdAt to sort)
    
    // Build pagination metadata
    val metadata = buildMetadata(event.organisation.id.value)
    
    // Return paginated response
    val items = partnerships
        .paginated(page, pageSize)
        .map { it.toPartnershipItem() }
    
    PaginatedResponse(
        items = items,
        page = page,
        pageSize = pageSize,
        total = partnerships.count(),
        metadata = metadata,
    )
}

private fun buildMetadata(organisationId: UUID): PaginationMetadata {
    // Query organisation editors for available organisers
    val editors = OrganisationPermissionEntity.listEditorsbyOrgId(organisationId)
    val organiserValues = editors.map { permission ->
        FilterValue(
            value = permission.user.email,
            displayValue = permission.user.name
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
- Single transaction for consistency
- Direct entity access (no repository dependencies)
- Metadata built within repository (no additional API calls)
- Email resolution happens in repository (case-insensitive)

---

## 9. Case-Insensitive Email Comparison

**Location**: `users/infrastructure/db/UserEntity.kt`

Existing pattern for email lookup:

```kotlin
fun UUIDEntityClass<UserEntity>.singleUserByEmail(email: String): UserEntity? = this.find {
    UsersTable.email eq email
}.singleOrNull()
```

**Decision**: PostgreSQL email column should use case-insensitive comparison.

**Investigation Needed**: Check if UsersTable.email uses citext or requires LOWER() comparison.

**Pattern for Case-Insensitive**:
```kotlin
// Option 1: Database-level citext type (preferred)
val email = varchar("email", 255).index()  // If column is citext in PostgreSQL

// Option 2: Application-level lowercase (fallback)
fun UUIDEntityClass<UserEntity>.singleUserByEmail(email: String): UserEntity? = this.find {
    UsersTable.email.lowerCase() eq email.lowercase()
}.singleOrNull()
```

**Assumption**: Based on FR-006 requirement, implement case-insensitive search. PostgreSQL typically handles emails case-insensitively if column is citext or application uses lowercase().

---

## 10. JSON Schema Requirements

**Location**: `application/src/main/resources/schemas/`

**Decision**: Create new schemas for pagination metadata structures.

**Required Schemas**:

1. **pagination_metadata.schema.json**:
```json
{
  "type": "object",
  "required": ["filters", "sorts"],
  "properties": {
    "filters": {
      "type": "array",
      "items": { "$ref": "#/components/schemas/filter_definition.schema" }
    },
    "sorts": {
      "type": "array",
      "items": { "type": "string" }
    }
  }
}
```

2. **filter_definition.schema.json**:
```json
{
  "type": "object",
  "required": ["name", "type"],
  "properties": {
    "name": { "type": "string" },
    "type": { "type": "string", "enum": ["string", "boolean"] },
    "values": {
      "type": ["array", "null"],
      "items": { "$ref": "#/components/schemas/filter_value.schema" }
    }
  }
}
```

3. **filter_value.schema.json**:
```json
{
  "type": "object",
  "required": ["value", "display_value"],
  "properties": {
    "value": { "type": "string" },
    "display_value": { "type": "string" }
  }
}
```

4. **Update paginated_partnership_list.schema.json** (if exists) or **partnership_list_response.schema.json**:
```json
{
  "type": "object",
  "required": ["items", "page", "page_size", "total"],
  "properties": {
    "items": {
      "type": "array",
      "items": { "$ref": "#/components/schemas/partnership_item.schema" }
    },
    "page": { "type": "integer" },
    "page_size": { "type": "integer" },
    "total": { "type": "integer" },
    "metadata": { "$ref": "#/components/schemas/pagination_metadata.schema" }
  }
}
```

---

## 11. Performance Considerations

**Target**: Sub-2-second response time for list with metadata (FR-TC-001)

**Assumptions**:
- Typical partnership list: up to 1000 partnerships
- Typical organisation: 10-100 members with edit permissions
- Metadata query adds ~50-100ms overhead

**Optimization Strategies**:

1. **Single Transaction**: All queries in one transaction reduces round trips
2. **Index on organiser_user_id**: Foreign key already indexed
3. **Distinct on Email**: Ensures unique organisers list
4. **Lazy Loading Avoided**: Metadata built with direct queries

**Query Analysis**:
```sql
-- Main partnership query (with organiser filter)
SELECT p.* FROM partnerships p
LEFT JOIN users u ON p.organiser_user_id = u.id
WHERE p.event_id = :eventId
  AND (u.email ILIKE :organiserEmail OR :organiserEmail IS NULL)
  -- ... other filters
ORDER BY p.created_at DESC
LIMIT :pageSize OFFSET :offset;

-- Metadata query (organisation editors)
SELECT DISTINCT u.email, u.name
FROM organisation_permissions op
JOIN users u ON op.user_id = u.id
WHERE op.organisation_id = :orgId
  AND op.can_edit = true;
```

**Expected Performance**:
- Partnership query: ~200-500ms (indexed FK, typical size)
- Metadata query: ~50-100ms (indexed permissions, small result set)
- Total: ~250-600ms (well under 2-second target)

---

## 12. Testing Strategy

**Contract Tests** (Required):
- Test partnership list with `filter[organiser]` parameter
- Test response includes metadata with filters and sorts
- Test organiser values array populated correctly
- Test filter application (only matching partnerships returned)

**Integration Tests** (Required):
- Test organiser filter with existing filters (AND logic)
- Test case-insensitive email matching
- Test empty organiser list when no editors exist
- Test partnerships without organisers excluded when filter applied
- Test metadata includes all filter definitions
- Test email endpoint accepts organiser filter

**Test Data Factories**:
```kotlin
// No new factories needed - reuse existing:
// - insertMockedPartnership()
// - insertMockedUser()
// - insertMockedOrgaPermission()
```

---

## Summary Table

| Component | Current State | Required Change | Complexity |
|-----------|---------------|-----------------|------------|
| PartnershipFilters | 6 filter fields | Add `organiser: String?` | Low |
| PaginatedResponse | Basic pagination | Add `metadata` field | Medium |
| PartnershipEntity.filters() | 5 filter parameters | Add `organiserUserId: UUID?` | Low |
| Route handler | Constructs filters | Add organiser param, build metadata | Medium |
| Repository | Returns List | Return PaginatedResponse with metadata | Medium |
| OrganisationPermissionEntity | Generic queries | Add listEditorsbyOrgId() | Low |
| JSON Schemas | Basic schemas | Add 3 new metadata schemas | Low |
| OpenAPI spec | Current filters | Add organiser parameter, metadata docs | Low |

---

## Implementation Checklist

- [ ] Add `organiser: String?` to PartnershipFilters data class
- [ ] Add `metadata: PaginationMetadata?` to PaginatedResponse
- [ ] Create PaginationMetadata, FilterDefinition, FilterValue data classes
- [ ] Add `organiserUserId: UUID?` parameter to PartnershipEntity.filters()
- [ ] Create OrganisationPermissionEntity.listEditorsbyOrgId() query method
- [ ] Update PartnershipRepository.listByEvent() to build metadata
- [ ] Update partnership list route to extract organiser parameter
- [ ] Update email endpoint route to extract organiser parameter
- [ ] Create pagination_metadata.schema.json
- [ ] Create filter_definition.schema.json  
- [ ] Create filter_value.schema.json
- [ ] Update partnership list response schema
- [ ] Update OpenAPI spec with organiser filter parameter
- [ ] Verify case-insensitive email comparison in UserEntity query
- [ ] Write contract tests for organiser filter
- [ ] Write integration tests for filter combinations
- [ ] Write integration tests for metadata population

---

## References

- **Constitution**: Section III (Repository Pattern), Section IV (API Consistency)
- **Spec 011**: Assign Partnership Organiser (organiser relationship established)
- **Spec 014**: Email Partnerships (filter pattern for email endpoint)
- **Existing Code**: PartnershipRoutes.kt, PartnershipRepositoryExposed.kt, OrganisationPermissionEntity.kt
