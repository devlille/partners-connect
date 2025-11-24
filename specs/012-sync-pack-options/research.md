# Research: Synchronize Pack Options

**Feature**: 012-sync-pack-options  
**Date**: November 24, 2025

## Research Summary

This feature modifies an existing endpoint's behavior from "add-only" to "synchronize". All required patterns, technologies, and architectural decisions are already established in the codebase. This research documents the existing patterns that will be followed.

---

## Database Transaction Patterns

**Decision**: Use Exposed `transaction {}` blocks for atomic operations  
**Rationale**: All repository methods in the codebase already use this pattern. The existing `attachOptionsToPack()` method wraps all operations in a transaction block, ensuring ACID properties.  
**Alternatives Considered**:
- Manual transaction management: Rejected - Exposed DSL provides idiomatic Kotlin transaction management
- No explicit transactions: Rejected - Spec requires atomicity (FR-009)

**Reference Implementation**: `OptionRepositoryExposed.attachOptionsToPack()` lines 185-250

---

## Synchronization Algorithm

**Decision**: Three-phase sync within single transaction:
1. Delete phase: Remove all existing pack-option attachments not in submitted lists
2. Update phase: Update `required` flag for options that exist but changed status
3. Insert phase: Add new pack-option attachments from submitted lists

**Rationale**: 
- Ensures final state exactly matches submitted configuration
- Single transaction ensures atomicity (all-or-nothing)
- Prevents duplicate key violations by deleting before inserting
- Minimizes database operations by updating in-place where possible

**Alternatives Considered**:
- Delete-all-then-insert: Rejected - Inefficient for large option lists, loses idempotency benefits
- Upsert pattern: Rejected - PostgreSQL UPSERT requires composite keys, Exposed doesn't support cleanly for this use case
- Read-modify-write with diff calculation: Selected approach - most efficient and maintainable

---

## Test Strategy

**Decision**: Update existing contract tests + add new integration scenarios

**Contract Test Approach** (TDD - write tests first):
1. Modify existing tests that expect 409 "already attached" errors - new behavior is idempotent
2. Add scenarios for option removal (submit subset of existing options)
3. Add scenarios for requirement status changes (move option between required/optional)
4. Add scenarios for empty configuration (remove all options)

**Integration Test Approach**:
- Test complete synchronization flow via HTTP endpoint
- Verify database state after sync operations
- Use existing mock factories: `insertMockedSponsoringPack`, `insertMockedSponsoringOption`
- Follow existing test pattern in `SponsoringPackRoutesTest.kt`

**Rationale**: Constitution requires contract tests (schema validation) separate from integration tests (business logic). Existing test file already has excellent coverage - we extend it.

---

## Error Handling

**Decision**: Maintain existing exception-based error handling

**Pattern**:
- Repository throws domain exceptions: `NotFoundException`, `ConflictException`, `ForbiddenException`
- Ktor StatusPages plugin converts to HTTP responses
- No try-catch in route handlers (constitution requirement)

**No Changes Needed**: Error handling for this feature already defined in clarifications:
- 409 Conflict: Duplicate option in required AND optional lists
- 403 Forbidden: Options don't belong to event
- 404 Not Found: Pack or options don't exist
- 500 Internal Server Error: Database transaction failure (automatic rollback)

**Reference**: `SponsoringRoutes.kt` lines 82-87 (route handler delegates to repository, no exception handling)

---

## Concurrency Handling

**Decision**: Last-write-wins strategy (per clarification)

**Implementation**: No special concurrency control needed
- Database transaction isolation handles concurrent requests
- Most recent request overwrites previous state
- No optimistic locking (version/timestamp) required

**Rationale**: Clarification session confirmed last-write-wins is acceptable for this use case. Event organizers rarely edit same pack simultaneously, and if they do, most recent intent should prevail.

---

## Performance Considerations

**Decision**: Bulk operations within single transaction

**Optimization Strategy**:
- Use Exposed `batchInsert` for adding multiple options (if >10 options)
- Use single `deleteWhere` with `inList` for bulk deletes
- Minimize database round-trips by fetching all related entities upfront

**Performance Target**: SC-003 requires 500ms for up to 50 options
- Current implementation fetches options in 2 queries (required + optional)
- Synchronization adds 1-3 additional queries (delete, update, insert)
- Well within 500ms target for 50 options

**Alternatives Considered**:
- Streaming/pagination: Rejected - No limit on options per spec clarification, but 50-option target is small enough for single transaction
- Async processing: Rejected - Synchronous operation provides immediate feedback, simpler error handling

---

## OpenAPI Documentation Updates

**Decision**: Update operation summary and description only

**Changes Needed**:
- Summary: Change from "Create sponsoring option" to "Synchronize pack options"
- Description: Document synchronization behavior (adds, removes, updates)
- Responses: Already documented (409, 403, 404, 500 exist in components)
- Schema: No changes - `AttachOptionsToPack` DTO structure unchanged

**Rationale**: API contract (request/response format) is identical to current implementation. Only behavior changes, so documentation updates are minimal.

---

## Exposed ORM Query Patterns

**Decision**: Use existing Exposed DSL patterns from codebase

**Key Patterns to Follow**:
```kotlin
// Bulk delete with conditions
PackOptionsTable.deleteWhere {
    (pack eq packId) and (option notInList submittedOptions)
}

// Bulk update with conditions  
PackOptionsTable.update({ (pack eq packId) and (option inList changedOptions) }) {
    it[required] = newRequiredValue
}

// Bulk insert
submittedOptions.forEach { optionId ->
    PackOptionsTable.insert {
        it[pack] = packId
        it[option] = optionId
        it[required] = isRequired
    }
}
```

**Reference**: Similar patterns in `OptionRepositoryExposed` lines 240-258, `CompanyRepositoryExposed`, `PartnershipRepositoryExposed`

---

## Summary of Decisions

| Aspect | Decision | Source |
|--------|----------|--------|
| Transaction Management | Exposed transaction blocks | Existing pattern |
| Sync Algorithm | Delete-update-insert in single transaction | Research |
| Error Handling | Domain exceptions + StatusPages | Constitution |
| Concurrency | Last-write-wins | Clarification |
| Testing | Contract + integration tests | Constitution |
| Performance | Bulk operations, 500ms target | Spec + Research |
| Documentation | Update OpenAPI summary only | Research |

**No Unresolved Questions** - All implementation details are established through existing patterns and specification clarifications.
