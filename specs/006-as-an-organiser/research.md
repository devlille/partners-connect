# Phase 0: Research & Technical Decisions

## Feature Context
Partnership validation enhancement to support customizable package details (ticket count, job offer count, booth size) at validation time, replacing boolean booth flag with size-based approach for future floor plan integration.

## Technical Unknowns & Resolutions

### 1. Database Schema Migration Strategy

**Question**: How to migrate existing `withBooth` boolean to `boothSize` string without breaking existing data?

**Research Findings**:
- Exposed ORM handles schema changes via `SchemaUtils.createMissingTablesAndColumns()`
- Existing partnerships already validated with old schema must remain compatible
- H2 in-memory DB resets for each test run (no migration needed in tests)
- Production: Add nullable `boothSize` column first, then drop `withBooth` in separate migration

**Decision**: 
- Add `boothSize = text("booth_size").nullable()` to `SponsoringPacksTable`
- Remove `withBooth = bool("with_booth").default(false)` 
- No data migration script needed (old partnerships don't reference pack's withBooth value)
- New partnerships will use boothSize from pack or validation override

**Rationale**: Nullable column allows backward compatibility. Old packs without booth size can exist. Partnerships validated with new feature will always have explicit booth size value.

**Alternatives Considered**:
- Two-phase migration (populate boothSize based on withBooth) - Rejected: unnecessary complexity, withBooth doesn't map to specific sizes
- Keep both fields temporarily - Rejected: violates single source of truth principle

---

### 2. Booth Size Cross-Pack Validation Pattern

**Question**: How to efficiently validate that a booth size override exists in at least one pack for the event?

**Research Findings**:
- Exposed provides query builders: `SponsoringPackEntity.find { (packTable.eventId eq eventId) and (packTable.boothSize eq size) }`
- Single indexed query returns all packs with that size for the event
- Foreign key index on `eventId` already exists, compound index not needed (booth size is low cardinality)
- Empty result set = invalid booth size for event

**Decision**:
```kotlin
fun validateBoothSizeForEvent(eventId: UUID, boothSize: String): Boolean = transaction {
    SponsoringPackEntity
        .find { (SponsoringPacksTable.eventId eq eventId) and (SponsoringPacksTable.boothSize eq boothSize) }
        .any()
}
```

**Rationale**: Simple, performant query using existing indexes. Returns boolean for validation flow. Executes in single transaction.

**Alternatives Considered**:
- Preload all pack booth sizes for event - Rejected: over-fetching, unnecessary memory
- Separate booth size catalog table - Rejected: over-engineering, packs already define available sizes

---

### 3. Validation Request Body Structure

**Question**: Should validation endpoint accept optional fields in request body or separate endpoints for default vs. custom validation?

**Research Findings**:
- Existing partnership endpoints follow single-responsibility principle (register, validate, decline are separate)
- Ktor supports optional request bodies via nullable deserialization
- OpenAPI spec can document optional request body with schema

**Decision**: Single POST `/validate` endpoint with optional JSON body:
```kotlin
@Serializable
data class ValidatePartnershipRequest(
    @SerialName("nb_tickets")
    val nbTickets: Int? = null,  // Optional: defaults to pack value
    
    @SerialName("nb_job_offers")
    val nbJobOffers: Int,  // Required field
    
    @SerialName("booth_size")
    val boothSize: String? = null  // Optional: defaults to pack value
)
```

**Rationale**: Matches REST principles (single validation action with optional parameters). Backward compatible (no body = use all defaults). Clear semantics (required vs. optional).

**Alternatives Considered**:
- Separate `/validate-custom` endpoint - Rejected: unnecessary complexity, violates RESTful design
- All fields required - Rejected: doesn't match spec requirement for ticket/booth defaults

---

### 4. Partnership Re-Validation Conflict Handling

**Question**: How to prevent concurrent validations while allowing re-validation before agreement signature?

**Research Findings**:
- Exposed transactions provide ACID guarantees
- Agreement signature check: `partnership.agreementSignedUrl != null`
- Validation timestamp check: `partnership.validatedAt != null`
- First validation wins naturally via transaction isolation

**Decision**: Check agreement signature and validation state in single transaction:
```kotlin
override fun validateWithDetails(eventSlug: String, partnershipId: UUID, request: ValidatePartnershipRequest): UUID = transaction {
    val partnership = findPartnership(eventId, partnershipId)
    
    // Check if already signed (immutable)
    if (partnership.agreementSignedUrl != null) {
        throw ForbiddenException("Cannot re-validate signed partnership")
    }
    
    // Proceed with validation (re-validation allowed before signature)
    partnership.validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    partnership.validatedNbTickets = request.nbTickets ?: partnership.selectedPack.nbTickets
    partnership.validatedNbJobOffers = request.nbJobOffers
    partnership.validatedBoothSize = request.boothSize ?: partnership.selectedPack.boothSize
    
    partnership.id.value
}
```

**Rationale**: Transaction isolation prevents concurrent modifications. Explicit agreement check prevents re-validation after signature. Re-validation naturally supported (overwrites previous validated values).

**Alternatives Considered**:
- Optimistic locking with version field - Rejected: unnecessary complexity for rare concurrency case
- Distributed lock - Rejected: over-engineering, database transaction isolation sufficient

---

### 5. Ticket Allocation Migration

**Question**: How to migrate existing ticket generation logic to use validated ticket count?

**Research Findings**:
- Ticket creation in `TicketRepositoryExposed.create()` currently uses `validatedPack()` helper
- `validatedPack()` resolves which pack to use (selected vs. suggestion)
- New logic needs validated field instead of pack's default

**Decision**: Modify ticket allocation to check validated field first:
```kotlin
val ticketCount = transaction {
    val billing = BillingEntity.singleByEventAndPartnership(eventId, partnershipId)
    val partnership = billing.partnership
    partnership.validatedNbTickets ?: partnership.validatedPack()?.nbTickets
        ?: throw NotFoundException("No validated pack found")
}
```

**Rationale**: Graceful fallback for legacy partnerships (null validated count → pack default). New partnerships always have explicit validated count.

**Alternatives Considered**:
- Require re-validation of all partnerships - Rejected: disruptive to existing users
- Keep using pack values - Rejected: doesn't implement spec requirement

---

### 6. Validation Response & Notification

**Question**: Should validation response include validated details, and should notification templates change?

**Research Findings**:
- Existing validate endpoint returns `IdentifierSchema` (just partnership ID)
- Notification sent to company via `NotificationVariables.PartnershipValidated`
- Frontend fetches full partnership details separately via GET endpoint

**Decision**: 
- Keep existing response format: `{ "id": "partnership-uuid" }`
- Update GET partnership endpoint to include validated fields in response
- No notification template changes (notifications don't include specific counts)

**Rationale**: Follows existing pattern (POST returns ID, GET returns full details). Notifications remain generic. Frontend can refresh partnership view after validation.

**Alternatives Considered**:
- Return full partnership in validation response - Rejected: breaks existing frontend expectations
- Add counts to notification - Rejected: out of scope, notifications are informational only

---

## Best Practices Applied

### Exposed ORM Patterns
- Use `datetime()` for all timestamp columns (project standard)
- Entity properties via delegation: `var prop by Table.column`
- Transaction scoping: All DB operations wrapped in `transaction {}`
- Foreign keys with cascade rules: `reference("pack_id", SponsoringPacksTable).nullable()`

### Ktor Route Patterns
- Use `AuthorizedOrganisationPlugin` for permission checks (no manual auth)
- Extract parameters via extensions: `call.parameters.eventSlug`
- Throw domain exceptions (`ForbiddenException`, `NotFoundException`) handled by StatusPages
- Inject repositories via Koin: `val repository by inject<PartnershipRepository>()`

### Testing Patterns
- HTTP route integration tests only (no direct repository tests)
- H2 in-memory database for test isolation
- Test all acceptance scenarios from spec
- Use `testApplication {}` block with mock modules

---

## Dependencies & Constraints

### Existing Code Dependencies
- `SponsoringPackEntity.toDomain()` method needs updated to include boothSize
- Partnership serialization models need validated fields
- OpenAPI schema validation must pass (`npm run validate`)

### Constitutional Constraints Met
- ✅ Zero ktlint/detekt violations (follow existing code style)
- ✅ 80% test coverage via HTTP route tests
- ✅ Backward compatible (nullable columns, graceful fallbacks)
- ✅ No circular dependencies (partnership module calls sponsoring, not reverse)
- ✅ Performance <2s (simple indexed queries)

---

## Implementation Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Breaking existing partnerships | HIGH | Nullable columns, fallback to pack defaults |
| Invalid booth size data | MEDIUM | Cross-pack validation at input time |
| Ticket generation errors | MEDIUM | Null-safe checks with clear error messages |
| OpenAPI spec validation fails | LOW | Incremental validation during development |
| Re-validation race conditions | LOW | Transaction isolation handles naturally |

---

## Next Steps (Phase 1)

1. Design complete data model with column definitions
2. Create OpenAPI contract for validate endpoint
3. Extract test scenarios from spec acceptance criteria
4. Generate failing integration tests for TDD approach
