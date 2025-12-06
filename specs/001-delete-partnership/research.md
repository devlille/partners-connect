# Research: Delete Unvalidated Partnership

**Feature**: Delete Unvalidated Partnership  
**Date**: December 6, 2025  
**Phase**: 0 - Outline & Research

## Research Questions & Findings

### 1. Partnership State Validation Pattern

**Question**: How to validate that a partnership is in unvalidated state (both `validatedAt` and `declinedAt` are null)?

**Decision**: Use Exposed ORM's conditional query with `isNull()` checks

**Rationale**:
- Existing codebase uses Exposed ORM for all database operations
- `PartnershipEntity.findById()` followed by timestamp null checks provides type-safe validation
- Alternative approaches (custom SQL, multiple queries) add complexity without benefit
- Pattern consistent with existing validation in partnership module (see suggestion approval logic)

**Alternatives Considered**:
- Raw SQL with WHERE clause - rejected due to loss of type safety and ORM benefits
- Separate status enum field - rejected as spec clarified timestamps are the source of truth
- Soft delete with `deletedAt` - rejected per spec requirement for hard delete

**Implementation Pattern**:
```kotlin
// In PartnershipRepositoryExposed.kt
override fun delete(partnershipId: UUID) {
    val partnership = PartnershipEntity.findById(partnershipId)
        ?: throw NotFoundException("Partnership not found")
    
    // Validate unvalidated state (both timestamps must be null)
    if (partnership.validatedAt != null || partnership.declinedAt != null) {
        throw ConflictException("Cannot delete finalized partnership")
    }
    
    partnership.delete()  // Exposed ORM hard delete
}
```

### 2. Authorization Enforcement Approach

**Question**: How to enforce that only users with edit permissions on the organization can delete partnerships?

**Decision**: Use existing `AuthorizedOrganisationPlugin` Ktor plugin

**Rationale**:
- Plugin already installed on all `/orgs/{orgSlug}/...` routes throughout codebase
- Automatically extracts JWT, validates user, checks `canEdit` permission
- Throws `UnauthorizedException` (401) for invalid/missing permissions - handled by StatusPages
- Consistent with constitutional requirement: "NEVER do manual permission checking"
- Same pattern used in all partnership modification endpoints (suggestion approval, agreement signing, etc.)

**Alternatives Considered**:
- Manual permission checking in route handler - rejected per constitution (violates AuthorizedOrganisationPlugin pattern)
- Repository-level permission checks - rejected per constitution (repositories handle data only)
- Custom authorization plugin - rejected as unnecessary (existing plugin sufficient)

**Implementation Pattern**:
```kotlin
// In PartnershipRoutes.kt - orgsPartnershipRoutes() function
private fun Route.orgsPartnershipRoutes() {
    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships") {
        install(AuthorizedOrganisationPlugin)  // Handles all permission checks
        
        delete("/{partnershipId}") {
            // Permission already validated by plugin
            val partnershipId = call.parameters.partnershipId
            partnershipRepository.delete(partnershipId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
```

### 3. Error Response Patterns

**Question**: What HTTP status codes and error formats should be used for different failure scenarios?

**Decision**: Use Ktor StatusPages exception-to-HTTP mapping with domain exceptions

**Rationale**:
- Constitution requires: "Route handlers MUST NOT include try-catch blocks"
- StatusPages plugin already configured to map exceptions to HTTP responses
- `NotFoundException` → 404 Not Found
- `ConflictException` → 409 Conflict (for finalized partnerships)
- `UnauthorizedException` → 401 Unauthorized (from AuthorizedOrganisationPlugin)
- Consistent error response format across all endpoints
- Repository layer throws exceptions, infrastructure layer (StatusPages) handles HTTP mapping

**Alternatives Considered**:
- Try-catch in route handler - rejected per constitution (duplicates StatusPages functionality)
- Return nullable or Result types - rejected per constitution (repositories must throw exceptions)
- Custom error response DTOs - rejected as StatusPages provides consistent format

**Exception Types**:
- `NotFoundException` - partnership doesn't exist
- `ConflictException` - partnership is finalized (validatedAt or declinedAt set)
- `UnauthorizedException` - user lacks edit permission (thrown by AuthorizedOrganisationPlugin)

### 4. OpenAPI Documentation Standards

**Question**: How to properly document the DELETE endpoint in OpenAPI 3.1.0 format?

**Decision**: Follow existing OpenAPI patterns with security, operationId, and response schemas

**Rationale**:
- Constitution requires: "All operations MUST include security definitions"
- DELETE operations typically return 204 No Content (no response body)
- Must include error responses (401, 403, 404, 409) for completeness
- operationId format: `deletePartnership` (camelCase)
- Path parameters consistent with existing partnership endpoints

**Alternatives Considered**:
- Inline schema definitions - rejected per constitution (must use components/schemas)
- Omitting error responses - rejected for incomplete documentation
- 200 OK with response body - rejected per REST conventions for DELETE (use 204)

**OpenAPI Structure**:
```yaml
/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}:
  delete:
    operationId: deletePartnership
    summary: Delete an unvalidated partnership
    security:
      - bearerAuth: []
    parameters:
      - $ref: '#/components/parameters/orgSlug'
      - $ref: '#/components/parameters/eventSlug'
      - $ref: '#/components/parameters/partnershipId'
    responses:
      '204':
        description: Partnership successfully deleted (no content)
      '401':
        $ref: '#/components/responses/Unauthorized'
      '404':
        $ref: '#/components/responses/NotFound'
      '409':
        $ref: '#/components/responses/Conflict'
```

### 5. Testing Strategy

**Question**: What contract and integration tests are needed to validate the delete functionality?

**Decision**: Contract tests for API schema validation, integration tests for business logic

**Rationale**:
- Constitution requires: "Focus on HTTP route integration tests, NOT repository tests"
- Contract tests validate DELETE endpoint exists, accepts correct parameters, returns 204
- Integration tests validate permission checks, state validation, and error scenarios
- Use existing mock factories: `insertMockedPartnership()`, `insertMockedEvent()`, `insertMockedCompany()`
- TDD approach: write tests before implementation

**Test Scenarios**:

**Contract Tests** (schema validation only):
1. DELETE request to valid endpoint returns 204 No Content
2. DELETE without authentication returns 401
3. DELETE with missing partnershipId returns 400

**Integration Tests** (business logic):
1. Authorized user can delete unvalidated partnership → 204 No Content
2. Unauthorized user cannot delete partnership → 401 Unauthorized
3. Cannot delete finalized partnership (validatedAt set) → 409 Conflict
4. Cannot delete declined partnership (declinedAt set) → 409 Conflict
5. Cannot delete non-existent partnership → 404 Not Found
6. Concurrent deletion returns 404 for second request

**Alternatives Considered**:
- Repository unit tests - rejected per constitution (test via HTTP routes)
- Manual API testing only - rejected (automated tests required)
- Performance tests - rejected per constitution (not part of implementation phase)

## Technology Decisions Summary

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| State Validation | Exposed ORM `isNull()` checks | Type-safe, consistent with existing patterns |
| Authorization | `AuthorizedOrganisationPlugin` | Constitutional requirement, existing pattern |
| Error Handling | StatusPages + domain exceptions | Constitutional requirement, consistent responses |
| HTTP Response | 204 No Content | REST convention for successful DELETE |
| OpenAPI Docs | Component schemas, security definitions | OpenAPI 3.1.0 compliance |
| Testing | Contract + Integration via HTTP routes | Constitutional requirement, >80% coverage |

## Dependencies & Integration Points

### Existing Components Used
- **AuthorizedOrganisationPlugin** - Handles authentication and permission validation
- **StatusPages** - Maps exceptions to HTTP responses
- **Exposed ORM** - Database operations via `PartnershipEntity`
- **PartnershipsTable** - Database schema with `validatedAt`, `declinedAt` columns
- **PartnershipRepository** - Domain interface to extend with delete method
- **OpenAPI specification** - Document DELETE operation

### No New Dependencies Required
- All functionality achievable with existing infrastructure
- No external service integrations needed (Slack, Mailjet, etc.)
- No new database tables or schema changes required

## Research Completion

All technical unknowns have been resolved:
- ✅ State validation pattern identified (Exposed ORM null checks)
- ✅ Authorization approach confirmed (AuthorizedOrganisationPlugin)
- ✅ Error handling pattern established (StatusPages exceptions)
- ✅ OpenAPI documentation format determined
- ✅ Testing strategy defined (contract + integration)

**Ready for Phase 1**: Data model documentation and contract generation.
