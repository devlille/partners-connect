# Research: Complete CRUD Operations for Companies

## Technical Context Analysis

All technical context is well-defined from existing codebase patterns and user clarifications. No research required for unknowns.

## Technology Decisions

### 1. Soft Delete Implementation Pattern
**Decision**: Use `status` enum field (ACTIVE/INACTIVE) in companies table
**Rationale**: 
- Preserves all company relationships (partnerships, job offers) as clarified by user
- Follows established patterns in codebase for state management
- Enables simple filtering and reporting
- Backwards compatible with existing queries
**Alternatives considered**: 
- `deleted_at` timestamp field - rejected due to complexity of null handling
- Separate deleted_companies table - rejected due to relationship preservation requirement

### 2. Status Filtering Implementation  
**Decision**: Add optional `status` query parameter to GET /companies endpoint
**Rationale**:
- User clarified default behavior should show all companies (active + inactive)
- Allows explicit filtering when needed: `?status=active`, `?status=inactive`
- Consistent with existing pagination parameters (`page`, `page_size`)
- Simple implementation extending existing `listPaginated` method
**Alternatives considered**:
- Separate endpoints (/companies/active, /companies/inactive) - rejected for API consistency
- Status as header parameter - rejected for REST convention adherence

### 3. Update Endpoint Design
**Decision**: Use partial update with `UpdateCompany` domain model following job offers pattern
**Rationale**:
- Consistent with existing `UpdateJobOffer` implementation in same codebase
- Supports null fields to indicate "no change" for partial updates
- Type-safe with kotlinx.serialization
- Follows established validation patterns with JSON schema
**Alternatives considered**:
- PATCH with JSON Patch operations - rejected for complexity
- Full replacement PUT - rejected due to user requirement for partial updates

### 4. Database Schema Changes
**Decision**: Add `status` column as `VARCHAR(10)` with `ACTIVE` default, indexed
**Rationale**:
- Backwards compatible - existing records automatically become ACTIVE
- Index enables efficient filtering queries
- Enum values stored as strings for readability and OpenAPI compatibility
- Exposed ORM supports `enumerationByName<CompanyStatus>()` pattern
**Alternatives considered**:
- Boolean `is_deleted` field - rejected for unclear semantics and future extensibility
- Integer status codes - rejected for poor readability

## Integration Patterns

### 5. Repository Layer Extensions
**Decision**: Extend `CompanyRepository` interface with new methods, implement in `CompanyRepositoryExposed`
**Rationale**:
- Maintains interface segregation principle from constitution
- Follows established pattern from job offers CRUD operations
- Enables proper dependency injection and testing
- Preserves clean architecture boundaries
**Implementation approach**:
```kotlin
interface CompanyRepository {
    // Existing methods...
    fun update(id: UUID, input: UpdateCompany): Company
    fun softDelete(id: UUID): UUID
    fun listPaginated(query: String?, status: CompanyStatus?, page: Int, pageSize: Int): PaginatedResponse<Company>
}
```

### 6. API Route Design
**Decision**: Add routes to existing `companyRoutes()` function in `CompanyRoutes.kt`
**Rationale**:
- Consistent with REST resource grouping
- Leverages existing parameter extraction and error handling
- Follows constitution's StatusPages exception handling pattern
- Public endpoints consistent with existing company routes
**Route patterns**:
- `PUT /companies/{companyId}` - update company
- `DELETE /companies/{companyId}` - soft delete company
- `GET /companies?status=active|inactive` - filtered listing

## Validation & Error Handling

### 7. Request Validation Strategy
**Decision**: Create JSON schema for `UpdateCompany` validation, reuse business rules from `CreateCompany`
**Rationale**:
- Constitutional requirement for request validation
- Consistency with existing validation patterns
- Schema-driven validation provides clear error messages
- Reuses established SIRET, VAT, country code validation logic
**Schema location**: `server/application/src/main/resources/schemas/update_company.schema.json`

### 8. Concurrent Update Handling
**Decision**: Use Exposed ORM transaction isolation with optimistic locking via `updatedAt` timestamp
**Rationale**:
- Database-level transaction isolation prevents corruption
- Follows existing patterns in job offers implementation
- Simple conflict detection via timestamp comparison
- Graceful error handling via existing exception patterns
**Implementation**: Update `updatedAt` field on every modification, check for concurrent changes

## Testing Strategy

### 9. Test Coverage Approach
**Decision**: HTTP route integration tests covering all endpoints, error cases, and filtering scenarios
**Rationale**:
- Constitutional requirement for integration testing over unit tests
- Tests complete request/response cycle including serialization
- H2 in-memory database validates schema changes
- Follows established test patterns from existing company routes
**Test files**:
- `CompanyUpdateRoutesTest.kt` - PUT endpoint scenarios
- `CompanyDeleteRoutesTest.kt` - DELETE endpoint and filtering scenarios

### 10. OpenAPI Documentation Updates
**Decision**: Update `openapi.yaml` with complete specification for new endpoints
**Rationale**:
- Constitutional requirement for comprehensive API documentation
- Enables frontend code generation
- Documents status filtering parameters and response formats
- Maintains API consistency standards
**Scope**: Add PUT/DELETE operation specs, update GET operation for status parameter, include error response schemas

## Implementation Dependencies

No external dependencies required - all functionality uses existing infrastructure:
- Exposed ORM for database operations
- kotlinx.serialization for JSON handling  
- Ktor routing and StatusPages for HTTP handling
- Koin dependency injection
- H2 database for testing

## Risk Assessment

**Low Risk**: 
- Extends well-established patterns from existing codebase
- No breaking changes to existing functionality
- Schema changes are backwards compatible
- Public endpoint nature eliminates authentication complexity

**Mitigation Strategies**:
- Comprehensive integration tests prevent regression
- Schema migration validates backwards compatibility
- Status enum provides future extensibility for additional states