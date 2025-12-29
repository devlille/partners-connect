# Research: Public Partnership Information Endpoint

## Technical Decisions

### Decision: Public Endpoint Pattern
**What was chosen**: Implement endpoint in `publicPartnershipRoutes()` function without authentication
**Rationale**: Existing codebase has clear separation between public and authenticated routes in `PartnershipRoutes.kt`. Public endpoints use no security plugins and allow unrestricted access.
**Alternatives considered**: 
- API key authentication (rejected - user specified completely public)
- Rate limiting (rejected - user specified unlimited access)
- Session-based auth (rejected - inconsistent with "public" requirement)

### Decision: Multi-Repository Orchestration Architecture
**What was chosen**: Route handler coordinates three separate repositories (PartnershipRepository, CompanyRepository, EventRepository) to assemble response
**Rationale**: Respects Single Responsibility Principle, reuses existing optimized repository methods, maintains domain boundaries, and avoids code duplication. Existing Company and EventWithOrganisation models already contain all required data.
**Alternatives considered**: 
- Single repository with entity joins (rejected - violates SRP, duplicates existing logic)
- New dedicated models (rejected - unnecessary duplication, maintenance overhead)
- Composite repository pattern (rejected - adds complexity without benefit)

### Decision: Repository Method Strategy  
**What was chosen**: Add new `getByIdDetailed` method to `PartnershipRepository` interface that returns enhanced Partnership model, use existing `getById` methods from CompanyRepository and EventRepository
**Rationale**: Partnership repository focuses only on partnership-specific data. Company and event data fetched from their respective repositories maintains existing optimizations and consistency.
**Alternatives considered**:
- Fetch all data in partnership repository (rejected - violates domain boundaries)
- Modify existing `getById` (rejected - would break existing callers)
- Create cross-repository dependencies (rejected - violates architecture principles)

### Decision: Domain Model Reuse Strategy
**What was chosen**: Reuse existing `Company` and `EventWithOrganisation` domain models, create only new `PartnershipDetail` model
**Rationale**: Existing models contain all required fields with proper serialization. Eliminates duplication, maintains API consistency across endpoints.
**Alternatives considered**:
- New CompanyDetail and EventDetail models (rejected - unnecessary duplication)
- Extend existing models (rejected - breaks existing functionality)
- Flatten response structure (rejected - user specified nested structure)

### Decision: Database Query Optimization
**What was chosen**: Three separate optimized queries via existing repositories instead of complex joins
**Rationale**: Leverages existing query optimizations in CompanyRepository and EventRepository. Partnership query focuses only on partnership-specific data. Avoids complex join logic and maintains repository isolation.
**Alternatives considered**:
- Single complex join query (rejected - duplicates existing optimized queries)
- Eager loading all relationships (rejected - unnecessary data fetching)
- Custom query with DTO (rejected - bypasses existing repository optimizations)

### Decision: Error Handling Pattern
**What was chosen**: Use existing StatusPages exception mapping with `NotFoundException` for missing entities
**Rationale**: Constitution mandates consistent exception handling. Existing pattern automatically maps exceptions to proper HTTP status codes.
**Alternatives considered**:
- Custom error responses (rejected - breaks consistency)
- Nullable return types (rejected - constitution violation)
- Result wrapper types (rejected - not established pattern)

### Decision: Testing Approach
**What was chosen**: Contract tests only, focusing on schema validation using existing mock factories
**Rationale**: User specifically requested contract tests with injected data. Constitution emphasizes API schema validation over business logic testing.
**Alternatives considered**:
- Full integration tests (rejected - user specified contract tests only)
- Unit tests for mappers (rejected - contract tests provide coverage)
- End-to-end tests (rejected - out of scope)

### Decision: OpenAPI Schema Integration
**What was chosen**: Create external JSON schema file and reference from `openapi.yaml` using `$ref`
**Rationale**: Constitution mandates external schema references for consistency. Enables automatic validation via `call.receive<T>(schema)` pattern.
**Alternatives considered**:
- Inline schema in OpenAPI (rejected - constitution violation)
- No schema validation (rejected - reduces reliability)
- Runtime validation only (rejected - missing documentation)

## Implementation Dependencies

### Existing Patterns to Follow
- **Route Declaration**: Follow `publicPartnershipRoutes()` pattern in `PartnershipRoutes.kt`
- **Multi-Repository Orchestration**: Inject multiple repositories in route handler
- **Domain Model Reuse**: Use existing `Company` and `EventWithOrganisation` models
- **Mock Factories**: Extend existing `mockPartnership()`, `mockCompany()`, `mockEvent()` functions
- **Exception Handling**: Leverage existing StatusPages configuration for consistent error responses

### Required Repository Dependencies
- **PartnershipRepository**: New `getByIdDetailed()` method
- **CompanyRepository**: Existing `getById(UUID)` method
- **EventRepository**: Existing `getBySlug(String)` method
- **Single Mapper**: Only `PartnershipEntityDetailMapper` needed

### Performance Considerations
- **Database Access**: Three separate optimized queries leveraging existing repository patterns
- **Caching Benefits**: Inherits existing caching strategies from Company and Event repositories
- **Memory Usage**: Reuses existing domain models, no additional object creation
- **Query Optimization**: Leverages existing indexes and query patterns per repository

## Risks and Mitigations

### Risk: Data Privacy Exposure
**Impact**: Public endpoint exposes all partnership data including private contact details
**Mitigation**: User explicitly approved exposing all data (Option A in clarification)
**Monitoring**: Consider logging access patterns for future privacy assessments

### Risk: Performance with Unlimited Access
**Impact**: Public endpoint with no rate limiting could impact server performance
**Mitigation**: User explicitly approved unlimited access; monitor resource usage
**Fallback**: Can add rate limiting later if needed without breaking API contract

### Risk: Schema Evolution
**Impact**: Changes to nested response structure could break API consumers
**Mitigation**: Use external JSON schema for validation; follow semantic versioning
**Prevention**: Comprehensive contract tests will catch breaking changes

## Next Phase Requirements

All technical decisions are resolved. No additional research required for Phase 1 design.