# Data Model: User Permission Revocation

**Date**: 2025-10-24  
**Feature**: User Permission Revocation for Organisations  
**Status**: Complete

## Overview

This feature uses existing database entities without requiring schema changes. The data model leverages the established `OrganisationPermissionEntity` for permission management.

## Existing Entities (No Changes)

### OrganisationEntity
**Location**: `organisations/infrastructure/db/OrganisationEntity.kt`  
**Purpose**: Represents organisations that manage events  
**Key Fields**:
- `id: UUID` - Primary key
- `slug: String` - Unique human-readable identifier (indexed)
- `name: String` - Organisation display name

**Relationships**:
- One-to-many with OrganisationPermissionEntity

### UserEntity
**Location**: `users/infrastructure/db/UserEntity.kt`  
**Purpose**: Represents user accounts  
**Key Fields**:
- `id: UUID` - Primary key
- `email: String` - Unique user identifier (indexed)
- `name: String` - Display name
- `pictureUrl: String?` - Profile picture URL

**Relationships**:
- One-to-many with OrganisationPermissionEntity

### OrganisationPermissionEntity
**Location**: `users/infrastructure/db/OrganisationPermissionEntity.kt`  
**Purpose**: Junction table linking users to organisations with permission levels  
**Key Fields**:
- `id: UUID` - Primary key
- `organisation: OrganisationEntity` - Foreign key reference
- `user: UserEntity` - Foreign key reference
- `canEdit: Boolean` - Permission level flag

**Operations**:
- `singleEventPermission(organisationId, userId)` - Find permission by composite key
- `hasPermission(organisationId, userId)` - Check if user has edit permission
- `listUserGrantedByOrgId(organisationId)` - List all users with permissions

**This Feature**: Deletes records where `canEdit = true` for specified users

## New Domain Models

### RevokePermissionRequest
**Location**: `users/infrastructure/api/RevokePermissionRequest.kt`  
**Purpose**: API request payload for revoke endpoint  
**Type**: Data class (serializable)

```kotlin
@Serializable
data class RevokePermissionRequest(
    @SerialName("user_emails")
    val userEmails: List<String>
)
```

**Validation Rules**:
- `userEmails`: Must be non-null list (can be empty)
- Email format validation via JSON schema
- No maximum list size constraint per FR-002a

**JSON Schema**: `schemas/revoke_permission_request.schema.json`

### RevokeUsersResult
**Location**: `users/domain/RevokeUsersResult.kt`  
**Purpose**: Domain model for revocation operation results  
**Type**: Data class (serializable)

```kotlin
@Serializable
data class RevokeUsersResult(
    @SerialName("revoked_count")
    val revokedCount: Int,
    @SerialName("not_found_emails")
    val notFoundEmails: List<String>
)
```

**Fields**:
- `revokedCount`: Number of users successfully revoked (≥ 0)
- `notFoundEmails`: List of email addresses not found in system (can be empty)

**Semantics**:
- If `notFoundEmails.isEmpty()` → Full success
- If `notFoundEmails.isNotEmpty()` → Partial success (some users revoked)
- If `revokedCount == 0 && notFoundEmails.isNotEmpty()` → No users revoked (all invalid)

## Data Flow

```
HTTP Request
    ↓
POST /orgs/{orgSlug}/users/revoke
    ↓
RevokePermissionRequest (deserialized + validated)
    ↓
AuthorizedOrganisationPlugin (validates auth + permission)
    ↓
UserRepository.revokeUsers(orgSlug, userEmails)
    ↓
[Transaction Scope]
    ├─ Find OrganisationEntity by slug
    ├─ For each email:
    │   ├─ Find UserEntity by email
    │   ├─ If found:
    │   │   ├─ Check self-revocation + last editor condition
    │   │   ├─ Find OrganisationPermissionEntity
    │   │   └─ Delete if exists (idempotent)
    │   └─ If not found: Add to notFoundEmails list
    └─ Return RevokeUsersResult
    ↓
HTTP Response (200 OK with RevokeUsersResult)
```

## Database Operations

### Query 1: Find Organisation
```sql
SELECT * FROM organisations 
WHERE slug = :orgSlug
LIMIT 1
```
**Complexity**: O(1) - Indexed slug column  
**Purpose**: Validate organisation exists and get ID

### Query 2: Find User by Email
```sql
SELECT * FROM users 
WHERE email = :userEmail
LIMIT 1
```
**Complexity**: O(1) per email - Indexed email column  
**Purpose**: Validate user exists and get ID

### Query 3: Count Editors (Self-Revocation Check)
```sql
SELECT COUNT(*) FROM organisation_permissions
WHERE organisation_id = :orgId
AND can_edit = true
```
**Complexity**: O(m) where m = users in org - Indexed foreign key  
**Purpose**: Prevent orphaned organisations (FR-013)  
**Optimization**: Only executed if requesting user is in revocation list

### Query 4: Find Permission
```sql
SELECT * FROM organisation_permissions
WHERE organisation_id = :orgId
AND user_id = :userId
LIMIT 1
```
**Complexity**: O(1) - Composite index on (organisation_id, user_id)  
**Purpose**: Find permission entity to delete

### Operation 5: Delete Permission
```sql
DELETE FROM organisation_permissions
WHERE id = :permissionId
```
**Complexity**: O(1) - Primary key deletion  
**Purpose**: Remove user's edit permission

## State Transitions

### OrganisationPermissionEntity Lifecycle

```
[NOT EXISTS] ──grant──> [EXISTS, canEdit=true]
                              │
                              │ revoke
                              ↓
                        [NOT EXISTS]
```

**Idempotency**: 
- Revoking non-existent permission → No-op (remains NOT EXISTS)
- Revoking already-revoked permission → No-op (remains NOT EXISTS)

## Validation Rules

### Business Rules (Repository Layer)

1. **Organisation Existence** (FR-011)
   - Organisation with slug must exist
   - Violation: Throw `NotFoundException` → HTTP 404

2. **Self-Revocation Guard** (FR-013)
   - IF requesting user in revocation list AND only one editor remaining
   - Violation: Throw `ConflictException` → HTTP 409
   - Message: "Cannot revoke your own access as the last editor"

3. **User Existence** (FR-008a)
   - Users may or may not exist
   - Non-existent users collected in `notFoundEmails`
   - NOT an error condition

4. **Atomicity** (FR-012)
   - All revocations within single transaction
   - Rollback on any database error
   - Exposed transaction block ensures ACID properties

### Input Validation (API Layer)

1. **Request Payload** (FR-010)
   - Must be valid JSON
   - Must match RevokePermissionRequest schema
   - `user_emails` field required (can be empty list)

2. **Email Format**
   - Validated by JSON schema (RFC 5322 format)
   - Invalid format → HTTP 400 Bad Request

3. **Empty List Handling**
   - Empty `user_emails` list is valid
   - Returns `RevokeUsersResult(0, emptyList())` → HTTP 200

## Performance Characteristics

**Time Complexity**: O(n) where n = number of emails in request  
**Space Complexity**: O(n) for notFoundEmails collection

**Expected Performance**:
- 1-10 emails: <100ms
- 50 emails: <500ms
- 100+ emails: <1s

**Scaling**: Linear with number of emails. No N+1 query problems. All operations use indexed columns.

## Error Scenarios & Responses

| Scenario | Exception | HTTP Status | Response Body |
|----------|-----------|-------------|---------------|
| Organisation not found | NotFoundException | 404 | `{"error": "Organisation with slug: X not found"}` |
| No authentication | UnauthorizedException | 401 | `{"error": "Missing Authorization header"}` |
| User not in DB | NotFoundException | 404 | `{"error": "User with email X not found"}` |
| No edit permission | UnauthorizedException | 401 | `{"error": "You do not have permission..."}` |
| Last editor self-revoke | ConflictException | 409 | `{"error": "Cannot revoke your own access..."}` |
| Invalid JSON | BadRequestException | 400 | Schema validation error details |
| Database error | Internal Server Error | 500 | Generic error message |

## Integration with Existing System

**No Breaking Changes**: 
- Existing OrganisationPermissionEntity unchanged
- No schema migrations required
- No impact on grant functionality
- No impact on event access checks

**Compatible with**:
- Existing permission checking throughout application
- Event access control mechanisms
- User listing by organisation
- Organisation member management UI

## Summary

The data model leverages existing entities without modifications. Two new lightweight models (`RevokePermissionRequest`, `RevokeUsersResult`) provide clean API contract. Database operations use existing indexes and patterns. Validation rules ensure data integrity and prevent orphaned organisations. Performance characteristics acceptable for typical usage patterns.

**Ready for Contract Definition (Phase 1 cont.)**
