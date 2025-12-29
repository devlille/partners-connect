# Research Findings: Provider Management Enhancement (Backend-Only)

## Backend-Only Implementation Scope

**Decision**: Implement as backend-only REST API enhancement  
**Rationale**:
- Provider management is primarily an organisational administrative function
- Frontend can consume new API endpoints when needed in future iterations
- Reduces complexity and implementation time
- All functionality exposed via REST API for future frontend integration

**Implementation Scope**:
- Backend: Enhanced REST API endpoints for provider CRUD and event attachment
- Frontend: No changes required - existing frontend remains functional
- API: New endpoints follow existing patterns for future frontend consumption

## Database Migration Strategy

**Decision**: DELETE existing providers + ADD organisation_id as non-nullable column  
**Rationale**: 
- Existing providers have no organisation association and cannot be retroactively assigned
- Business requirement states providers MUST belong to an organisation 
- Clean slate approach prevents orphaned data and ensures data integrity
- Acceptable for current stage since existing provider data is minimal

**Alternatives considered**:
- Nullable organisation_id with gradual migration → Rejected: Violates business requirement that all providers must belong to organisation
- Default organisation assignment → Rejected: No logical default organisation exists
- Keep existing providers unassigned → Rejected: Creates inconsistent data model

## Organisation Permission Model

**Decision**: Reuse existing `AuthorizedOrganisationPlugin` for permission checking  
**Rationale**:
- Established pattern across events, sponsoring, partnerships modules
- Automatically validates JWT token and organisation membership
- Throws consistent UnauthorizedException for unauthorized access
- Eliminates duplicate permission checking code

**Implementation Pattern**:
```kotlin
route("/orgs/{orgSlug}/providers") {
    install(AuthorizedOrganisationPlugin)  // Handles all permission validation
    
    post { /* Create provider - no manual auth needed */ }
    put("/{id}") { /* Update provider - no manual auth needed */ }
    delete("/{id}") { /* Delete provider - no manual auth needed */ }
}
```

## Provider-Event Attachment Strategy

**Decision**: Create join table for many-to-many relationship between providers and events  
**Rationale**:
- One provider can service multiple events
- One event can have multiple providers
- Cascade deletion: Remove attachments when provider is deleted
- Events cannot be deleted (per clarification), so no reverse cascade needed

**Table Structure**:
```kotlin
object ProviderEventTable : Table("provider_events") {
    val providerId = reference("provider_id", ProvidersTable)
    val eventId = reference("event_id", EventsTable)
    override val primaryKey = PrimaryKey(providerId, eventId)
}
```

## Query Parameter Preservation Strategy

**Decision**: Extend existing provider list endpoint with optional org_slug parameter  
**Rationale**:
- Maintains backward compatibility with existing query parameters (query, sort, direction, page, page_size)
- Additive change - no breaking modifications to API contract
- Enables both public listing (all providers) and org-scoped listing (filtered)

**API Enhancement**:
- Existing: `GET /providers?query=catering&sort=name&page=1`
- Enhanced: `GET /providers?org_slug=devlille&query=catering&sort=name&page=1`

## Repository Pattern Enhancement

**Decision**: Enhance existing ProviderRepository with organisation-scoped methods  
**Rationale**:
- Follows established repository pattern in codebase
- Clean separation between data access and business logic
- Enables easy testing with repository mocking
- Constitutional requirement for domain module isolation

**New Repository Methods**:
```kotlin
interface ProviderRepository {
    // Enhanced existing methods
    fun findByOrganisation(orgSlug: String, query: String?, sort: String?, direction: String?, page: Int, pageSize: Int): Page<Provider>
    fun createForOrganisation(orgSlug: String, request: CreateProviderRequest): Provider
    fun updateForOrganisation(orgSlug: String, providerId: UUID, request: UpdateProviderRequest): Provider
    fun deleteForOrganisation(orgSlug: String, providerId: UUID)
    
    // New attachment methods
    fun attachToEvent(providerId: UUID, eventId: UUID)
    fun detachFromEvent(providerId: UUID, eventId: UUID)
    fun detachFromAllEvents(providerId: UUID)
    fun findByEvent(eventId: UUID): List<Provider>
}
```

## JSON Schema Enhancement Strategy

**Decision**: Enhance existing schemas rather than create entirely new ones  
**Rationale**:
- `provider.schema.json` already exists but lacks `org_slug` field 
- `create_provider.schema.json` already exists and is suitable
- `paginated_provider.schema.json` already exists but uses `items` instead of `providers`
- `create_by_identifiers.schema.json` already exists and perfect for attachment operations

**Enhancements Required**:
- Add `org_slug` field to `provider.schema.json` for organisation association
- Create `update_provider.schema.json` for partial updates (doesn't exist yet)
- Enhance `paginated_provider.schema.json` to use `providers` + `pagination` structure
- Reuse existing `create_by_identifiers.schema.json` for attachment/detachment
- Add `org_slug` query parameter to existing public `/providers` endpoint

## Testing Strategy

**Decision**: Contract tests + Integration tests following constitutional requirements  
**Rationale**:
- Contract tests validate API schema compliance without business logic
- Integration tests validate end-to-end workflows with H2 database
- Mock factory pattern for reusable test data setup
- TDD approach with failing tests before implementation

**Test Coverage Requirements**:
- Contract tests for all new endpoints (schema validation only)
- Integration tests for CRUD operations with organisation scoping
- Integration tests for provider-event attachment workflows
- Mock factory for consistent test data: `mockProvider()`, `mockProviderRequest()`

## Performance Considerations

**Decision**: Add database indexes for organisation-scoped queries  
**Rationale**:
- Provider queries will be filtered by organisation most of the time
- Public listing may include thousands of providers across all organisations
- Constitutional requirement for optimized database queries

**Required Indexes**:
```sql
CREATE INDEX idx_providers_organisation_id ON providers(organisation_id);
CREATE INDEX idx_provider_events_provider_id ON provider_events(provider_id);
CREATE INDEX idx_provider_events_event_id ON provider_events(event_id);
```

## OpenAPI Documentation Requirements

**Decision**: Update openapi.yaml with external schema references  
**Rationale**:
- Constitutional requirement for comprehensive API documentation
- Schema references prevent duplication between validation and docs
- Enables automatic validation through schema integration

**Documentation Updates**:
- Add provider management endpoints under `/orgs/{orgSlug}/providers`
- Add event provider attachment endpoints under `/orgs/{orgSlug}/events/{eventSlug}/providers`
- Reference external schema files for consistent validation
- Include org_slug parameter in public provider listing endpoint