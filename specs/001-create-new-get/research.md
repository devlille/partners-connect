# Research: Event Sponsoring Packs Public API

**Date**: 2025-10-02  
**Feature**: Public GET endpoint for event sponsoring packages

## Technical Decisions

### Database Query Strategy
**Decision**: Reuse existing Exposed queries and database schema  
**Rationale**: The existing `PackRepositoryExposed.findPacksByEvent()` method already implements the exact functionality needed - it queries sponsoring packages by event slug, includes both required and optional options, and supports language-based translations via `Accept-Language` header. The database schema with `SponsoringPacksTable`, `SponsoringOptionsTable`, `PackOptionsTable`, and `OptionTranslationsTable` provides all necessary data relationships.  
**Alternatives considered**: 
- Creating new optimized queries: Rejected because existing queries are already optimized and well-tested
- Direct SQL queries: Rejected because it would bypass Exposed ORM patterns used throughout the codebase

### API Route Structure  
**Decision**: Add new public route `/events/{eventSlug}/sponsoring/packs` alongside existing `/orgs/{orgSlug}/events/{eventSlug}/packs`  
**Rationale**: This maintains clear separation between authenticated organizational management endpoints and public consumption endpoints. The route structure follows RESTful conventions and makes the public nature of the endpoint immediately apparent.  
**Alternatives considered**:
- Modifying existing route with optional authentication: Rejected due to complexity and security concerns
- Creating separate controller: Rejected because the business logic is identical, only access control differs

### Repository Interface Design
**Decision**: Create new domain interface `EventPackRepository` with single method `findPublicPacksByEvent(eventSlug: String, language: String): List<SponsoringPack>`  
**Rationale**: Follows clean architecture by separating public access concerns from organizational access. Maintains interface segregation principle from the constitution. Allows for different implementations if needed in the future.  
**Alternatives considered**:
- Extending existing PackRepository: Rejected because it would mix public and private access concerns
- Using existing repository directly in route: Rejected because it violates clean architecture principles

### Data Model Reuse
**Decision**: Reuse existing `SponsoringPack` and `SponsoringOption` data classes without modification  
**Rationale**: These data classes already contain all required fields (id, name, basePrice, maxQuantity, requiredOptions, optionalOptions) and support kotlinx-serialization for JSON responses. The `@SerialName` annotations ensure consistent API response format.  
**Alternatives considered**: 
- Creating public-specific DTOs: Rejected because it would duplicate code without adding value
- Modifying existing data classes: Rejected because it could break existing functionality

### Language Support Implementation
**Decision**: Leverage existing `OptionTranslationsTable` and language parameter in `toDomain()` mapper  
**Rationale**: The existing translation infrastructure already supports the `Accept-Language` header and provides localized option names and descriptions. The `toDomain()` extension function in `SponsoringPackEntity.ext.kt` already handles language-based translation lookup.  
**Alternatives considered**:
- Implementing new translation logic: Rejected because existing system is comprehensive and well-tested
- Defaulting to single language: Rejected because internationalization is a stated requirement

### Error Handling Strategy
**Decision**: Follow existing error handling patterns with `NotFoundException` for invalid event slugs and standard HTTP status codes  
**Rationale**: Maintains consistency with existing API endpoints. The `EventEntity.findBySlug()` method already provides proper error handling for missing events. Empty pack lists will return `200 OK` with empty array, following REST conventions.  
**Alternatives considered**:
- Custom error responses: Rejected because existing error handling is sufficient and consistent

### Testing Approach
**Decision**: Follow existing test patterns with H2 in-memory database and route-level integration tests  
**Rationale**: Existing `SponsoringPackRoutesTest.kt` provides proven patterns for testing sponsoring endpoints. H2 database allows testing of complete query flows including joins and translations without external dependencies.  
**Alternatives considered**:
- Unit tests with mocked repositories: Will be included but insufficient alone for database query validation
- End-to-end tests: Rejected as too heavyweight for this simple read-only endpoint

## Implementation Dependencies

### Required Components
- `EventPackRepository` domain interface (new)
- `EventPackRepositoryExposed` implementation (new) 
- Route handler in `SponsoringRoutes.kt` (modification)
- `EventPackRoutesTest.kt` integration tests (new)
- Koin DI binding in `SponsoringModule.kt` (modification)

### Existing Components to Reuse
- `SponsoringPack` and `SponsoringOption` data classes
- `SponsoringPackEntity.toDomain()` mapper with language support
- Database tables: `SponsoringPacksTable`, `SponsoringOptionsTable`, `PackOptionsTable`, `OptionTranslationsTable`
- `EventEntity.findBySlug()` for event resolution
- Existing error handling and serialization mechanisms

## Risk Assessment

### Low Risk
- Database performance: Reusing existing optimized queries with proper indexing on event relationships
- Security: Public endpoint with no authentication required, read-only access
- Maintainability: Following established patterns and reusing existing code

### Mitigation Strategies
- Comprehensive testing with realistic data volumes to ensure response times under 2 seconds
- Input validation for event slug format to prevent injection attacks
- Monitoring endpoint usage for potential abuse detection