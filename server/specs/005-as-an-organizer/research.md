# Research: User Permission Revocation

**Date**: 2025-10-24  
**Feature**: User Permission Revocation for Organisations  
**Status**: Complete

## Executive Summary

This feature adds permission revocation capability to the existing user management system. Research confirms all technical requirements are satisfied by existing infrastructure. The implementation follows established patterns from the `grantUsers()` functionality with inverse logic.

## Research Questions & Findings

### 1. Exposed ORM Permission Deletion Pattern

**Question**: What is the correct Exposed ORM pattern for deleting OrganisationPermissionEntity records?

**Finding**: 
- Exposed provides `Entity.delete()` method for single entity deletion
- For batch operations, use `Table.deleteWhere { condition }` 
- The codebase uses entity-level deletion in similar scenarios
- Transaction boundaries ensure atomicity

**Decision**: Use entity-level deletion (`OrganisationPermissionEntity.find { ... }.forEach { it.delete() }`) to leverage existing helper functions and maintain consistency with codebase patterns.

**Rationale**: Entity-level approach allows reuse of `OrganisationPermissionEntity.singleEventPermission()` helper and maintains audit trail if needed in future.

**Alternatives Considered**:
- Bulk delete with `OrganisationPermissionsTable.deleteWhere { }` - More efficient for large batches but bypasses entity lifecycle hooks and reduces code reusability

### 2. Partial Success Response Model

**Question**: How should the API represent partial success when some users don't exist?

**Finding**:
- Specification requires returning list of non-existent emails in response
- Existing grant endpoint throws NotFoundException for any missing user (all-or-nothing)
- New revoke endpoint needs different behavior per FR-008a
- Kotlin sealed classes or data classes commonly used for complex responses

**Decision**: Create `RevokeUsersResult` data class with two properties:
```kotlin
data class RevokeUsersResult(
    val revokedCount: Int,
    val notFoundEmails: List<String>
)
```

**Rationale**: Simple data class provides clear contract. Serializes cleanly to JSON. Mirrors specification requirement for "partial success response with list of non-existent email addresses."

**Alternatives Considered**:
- Sealed class hierarchy (Success/PartialSuccess) - Over-engineered for this simple case
- Just return list of not-found emails - Loses information about how many were actually revoked

### 3. Self-Revocation Prevention Logic

**Question**: How to detect if user is the last editor before allowing self-revocation?

**Finding**:
- OrganisationPermissionEntity has `canEdit` boolean field
- Need to count users with `canEdit = true` for the organisation
- Exposed provides `count()` aggregate function
- Check must happen before deletion to prevent race conditions

**Decision**: Query count of editors before deletion:
```kotlin
val editorCount = OrganisationPermissionEntity
    .find { 
        (OrganisationPermissionsTable.organisation eq orgId) and 
        (OrganisationPermissionsTable.canEdit eq true) 
    }
    .count()
```
If user is in revocation list and editorCount == 1, throw ConflictException.

**Rationale**: Database-level count ensures accuracy. ConflictException (HTTP 409) semantically correct for business rule violation.

**Alternatives Considered**:
- Load all editors into memory and count - Inefficient, vulnerable to TOCTOU race conditions
- ForbiddenException (HTTP 403) - Less semantically accurate than ConflictException for this business rule

### 4. Idempotency Handling

**Question**: Should revoking already-revoked users be a no-op or return different status?

**Finding**:
- Specification FR-009 requires idempotent operation (no error for already-revoked users)
- Existing grant endpoint updates existing permissions if found
- REST idempotency best practice: same operation produces same result

**Decision**: Filter to only process users who actually have permissions:
```kotlin
userEmails.forEach { email ->
    val permission = OrganisationPermissionEntity
        .singleEventPermission(orgId, userId)
    permission?.delete()  // Null-safe - no-op if already revoked
}
```

**Rationale**: Naturally idempotent. Allows retries without side effects. Aligns with REST principles.

**Alternatives Considered**:
- Throw exception if user already lacks permission - Violates idempotency requirement (FR-009)
- Track and report already-revoked users separately - Adds complexity without clear benefit

### 5. Authorization Pattern Compliance

**Question**: Should the revoke endpoint use AuthorizedOrganisationPlugin or manual permission checks?

**Finding**:
- Constitution Section V (Authorization Pattern) mandates using AuthorizedOrganisationPlugin for all org-protected routes
- Existing grant endpoint uses manual permission checking (predates constitutional requirement)
- Plugin automatically extracts token, validates user, checks canEdit permission
- Plugin throws UnauthorizedException (HTTP 401) automatically

**Decision**: Install AuthorizedOrganisationPlugin on the revoke route:
```kotlin
route("/orgs/{orgSlug}/users") {
    install(AuthorizedOrganisationPlugin)
    
    post("/revoke") {
        // No manual permission checking needed
        // Plugin guarantees user has canEdit=true
    }
}
```

**Rationale**: Follows constitution. Eliminates duplicate permission-checking code. Ensures consistent authorization behavior across all org-protected endpoints.

**Alternatives Considered**:
- Manual permission checking like grant endpoint - Violates constitutional requirement, creates tech debt
- Refactor grant endpoint simultaneously - Out of scope for this feature; can be done separately

### 6. Request Validation Pattern

**Question**: How should the request payload be validated?

**Finding**:
- Existing grant endpoint uses JSON schema validation via `call.receive<GrantPermissionRequest>(schema = "...")`
- Constitution requires request validation with clear error messages
- JSON Schema files stored in `server/application/src/main/resources/schemas/`
- Ktor validates against schema before deserializing to data class

**Decision**: Create `RevokePermissionRequest` data class and JSON schema:
```kotlin
@Serializable
data class RevokePermissionRequest(
    @SerialName("user_emails")
    val userEmails: List<String>
)
```
With schema file `revoke_permission_request.schema.json`.

**Rationale**: Matches grant endpoint pattern. Leverages framework validation. Clear snake_case field naming for JSON API.

**Alternatives Considered**:
- No schema validation - Violates API consistency requirement, poor developer experience
- Reuse grant schema - Semantically distinct operations should have distinct schemas

## Technology Stack Validation

**Ktor 3.0**: ✅ Confirmed - All routing and plugin features available  
**Exposed ORM**: ✅ Confirmed - Entity deletion patterns well-established  
**Koin DI**: ✅ Confirmed - Repository injection pattern works  
**Kotlin Test**: ✅ Confirmed - HTTP route testing framework available  
**H2 Database**: ✅ Confirmed - In-memory testing setup exists

## Performance Considerations

**Database Operations**:
- Query: Find organisation by slug (indexed column) - O(1)
- Query: Find permissions by org + user (composite index likely) - O(1) per user
- Delete: Remove permission entities - O(1) per user
- Total: O(n) where n = number of emails in request

**Expected Performance**: 
- Small batches (1-10 users): <100ms
- Large batches (100+ users): <1 second
- Well within 2-second API requirement

**No Optimization Required**: Linear performance with small constant factors. No N+1 queries. No complex joins.

## Integration Points

**No External Integrations**: 
- No Slack notifications (specification clarified: no audit logging required)
- No Mailjet emails
- No BilletWeb ticketing
- No Google Cloud Storage

**Internal Dependencies**:
- AuthRepository (token validation) - via AuthorizedOrganisationPlugin
- UserRepository (this interface being extended)
- OrganisationEntity (existing)
- OrganisationPermissionEntity (existing)

## Risk Assessment

**Technical Risks**: 
- ✅ LOW - No new infrastructure required
- ✅ LOW - Pattern mirrors existing grant functionality
- ✅ LOW - No schema migrations needed

**Implementation Risks**:
- ⚠️ MEDIUM - Self-revocation edge case requires careful testing (mitigated by comprehensive test coverage)
- ✅ LOW - Partial success response is straightforward data structure

**Operational Risks**:
- ✅ LOW - Idempotent operation safe to retry
- ✅ LOW - No performance concerns for typical usage patterns

## Conclusion

All technical questions resolved. No blocking unknowns. Implementation can proceed using established patterns with minor adaptations for revocation semantics. Estimated complexity: **Low** (5-7 hours including comprehensive testing).

**Ready for Phase 1: Design & Contracts**
