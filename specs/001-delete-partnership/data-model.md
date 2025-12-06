# Data Model: Delete Unvalidated Partnership

**Feature**: Delete Unvalidated Partnership  
**Date**: December 6, 2025  
**Phase**: 1 - Design & Contracts

## Entity Overview

This feature does not introduce new entities. It operates on the existing `Partnership` entity with specific state validation rules.

## Partnership Entity (Existing)

**Table**: `partnerships` (defined in `PartnershipsTable.kt`)  
**Entity Class**: `PartnershipEntity` (Exposed ORM)  
**Primary Key**: `id` (UUID)

### Relevant Fields for Delete Operation

| Field | Type | Nullable | Description | Validation Rule |
|-------|------|----------|-------------|-----------------|
| `id` | UUID | No | Primary key identifier | Must exist for delete operation |
| `validatedAt` | LocalDateTime | Yes | Timestamp when partnership was validated | MUST be null for deletion |
| `declinedAt` | LocalDateTime | Yes | Timestamp when partnership was declined | MUST be null for deletion |
| `eventId` | UUID (FK) | No | Reference to Event entity | Used for route validation |
| `companyId` | UUID (FK) | No | Reference to Company entity | Not validated for delete |

### State Definitions

**Unvalidated/Draft State** (deletion allowed):
- `validatedAt == null` AND `declinedAt == null`
- Partnership has been created but not yet finalized
- Business rule: Can be safely deleted without data integrity concerns

**Finalized State** (deletion forbidden):
- `validatedAt != null` OR `declinedAt != null`
- Partnership has been either approved or declined
- Business rule: Represents a commitment that should not be deleted

### State Transitions

```
[Created]
   ↓
[Unvalidated] ← DELETE allowed here
   ↓
   ├─→ [Validated] (validatedAt set) ← DELETE forbidden
   └─→ [Declined] (declinedAt set) ← DELETE forbidden
```

**Delete Operation Constraints**:
1. Can only transition from `[Unvalidated]` to `[Deleted]` (removed from database)
2. Cannot delete from `[Validated]` or `[Declined]` states
3. Attempting to delete finalized partnership → `ConflictException` (HTTP 409)

## Relationships (Existing)

**No relationship changes required** - delete operation works within existing schema:

- **Partnership → Event** (many-to-one): Partnership belongs to an event
  - Used in route path: `/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}`
  - Validates partnership belongs to specified event
  
- **Partnership → Company** (many-to-one): Partnership associated with a company
  - Not validated during delete operation
  
- **Partnership → SponsoringPack** (optional): Selected sponsorship package
  - Not validated during delete operation (may be null for draft partnerships)

- **Partnership → User** (organiser, optional): Assigned organizer
  - Not validated during delete operation

## Validation Rules

### Pre-Delete Validation Checks

1. **Existence Check**:
   ```kotlin
   val partnership = PartnershipEntity.findById(partnershipId)
       ?: throw NotFoundException("Partnership not found")
   ```
   - Result: HTTP 404 if partnership doesn't exist

2. **State Validation Check**:
   ```kotlin
   if (partnership.validatedAt != null || partnership.declinedAt != null) {
       throw ConflictException("Cannot delete finalized partnership")
   }
   ```
   - Result: HTTP 409 if partnership is finalized

3. **Permission Check** (handled by `AuthorizedOrganisationPlugin`):
   - User must have `canEdit=true` for the organization
   - Result: HTTP 401 if user lacks permission
   - Implementation: Automatic via plugin, not in repository

### Post-Delete Effects

**Hard Delete**: Partnership record is completely removed from database
- No soft delete flag
- No audit trail in database
- No cascade to related entities (foreign key constraints handle this)

**No Related Data Cleanup Required**:
- Per spec clarification: "Unvalidated partnerships have no related data"
- Focus only on deleting the partnership entity itself

## Database Schema

**No schema changes required** - using existing `PartnershipsTable`:

```kotlin
object PartnershipsTable : UUIDTable("partnerships") {
    val eventId = reference("event_id", EventsTable)
    val companyId = reference("company_id", CompaniesTable)
    // ... other fields ...
    val validatedAt = datetime("validated_at").nullable()
    val declinedAt = datetime("declined_at").nullable()
    // ... other fields ...
}
```

**Indexes** (existing):
- Primary key index on `id` (automatic)
- Foreign key indexes on `event_id`, `company_id` (automatic)
- No additional indexes needed for delete operation

## Error Scenarios

| Scenario | Exception | HTTP Status | Response Message |
|----------|-----------|-------------|------------------|
| Partnership not found | `NotFoundException` | 404 | "Partnership not found" |
| Partnership already validated | `ConflictException` | 409 | "Cannot delete finalized partnership" |
| Partnership already declined | `ConflictException` | 409 | "Cannot delete finalized partnership" |
| User lacks edit permission | `UnauthorizedException` | 401 | "Unauthorized" |
| Partnership already deleted (concurrent) | `NotFoundException` | 404 | "Partnership not found" |

**Exception Handling**: All exceptions thrown by repository, mapped to HTTP by `StatusPages` plugin (no try-catch in routes per constitution)

## Data Flow

### Delete Operation Flow

```
HTTP DELETE /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}
   ↓
[AuthorizedOrganisationPlugin] - validates user has canEdit permission
   ↓
[Route Handler] - extracts partnershipId parameter
   ↓
[PartnershipRepository.delete(partnershipId)]
   ↓
[PartnershipRepositoryExposed]
   ├─→ Find partnership by ID (throw NotFoundException if not found)
   ├─→ Check validatedAt == null AND declinedAt == null (throw ConflictException if not)
   └─→ Execute hard delete via Exposed ORM
   ↓
[Route Handler] - respond with HTTP 204 No Content
   ↓
[Client receives 204 response]
```

### Concurrent Delete Handling

```
Request 1: DELETE /partnerships/uuid-123
Request 2: DELETE /partnerships/uuid-123 (simultaneous)
   ↓
Request 1: finds partnership, validates state, deletes record → 204
Request 2: partnership.findById() returns null → NotFoundException → 404
```

**Result**: First request succeeds (204), second request fails (404)

## Testing Data Requirements

### Mock Factories (Existing)

Use existing test helper functions:
- `insertMockedEvent()` - creates event with organization
- `insertMockedCompany()` - creates company entity
- `insertMockedPartnership()` - creates partnership entity

### Test Data States

**Valid for Deletion**:
```kotlin
val partnership = insertMockedPartnership(
    eventId = event.id,
    companyId = company.id,
    validatedAt = null,  // Unvalidated
    declinedAt = null    // Not declined
)
```

**Invalid for Deletion** (finalized):
```kotlin
// Validated partnership
val validatedPartnership = insertMockedPartnership(
    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
    declinedAt = null
)

// Declined partnership
val declinedPartnership = insertMockedPartnership(
    validatedAt = null,
    declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
)
```

## Summary

- **No new entities or schema changes required**
- **Operates on existing Partnership entity** with timestamp-based state validation
- **State rule**: `validatedAt == null AND declinedAt == null` allows deletion
- **Hard delete**: Complete removal from database, no audit trail
- **Error handling**: Domain exceptions mapped to HTTP by StatusPages
- **Authorization**: Enforced by `AuthorizedOrganisationPlugin` (no manual checks)
