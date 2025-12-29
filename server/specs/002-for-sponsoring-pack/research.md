# Research: Multi-Language Sponsoring Pack and Option Management for Organizers

**Feature**: Remove Accept-Language header dependency from organizer-facing sponsoring endpoints  
**Research Date**: October 4, 2025  
**Context**: Enable organizers to see all available translations to identify missing language support

## Technical Decisions

### Language Parameter Removal Strategy
**Decision**: Remove language parameter from PackRepository and OptionRepository methods used by organizer services, while preserving existing public API methods  
**Rationale**: Clean separation of concerns - organizer management needs differ from public consumption needs. Allows us to evolve organizer APIs independently without breaking public endpoints.  
**Alternatives considered**:
- Adding optional parameter with default behavior: Rejected because it creates ambiguous API contracts and potential bugs
- Creating separate organizer-specific repositories: Rejected due to code duplication and maintenance overhead
- Single repository with boolean flag: Rejected because it violates single responsibility principle

### Data Model Transformation Strategy  
**Decision**: Modify SponsoringOption domain model to include translation map (Map<String, OptionTranslation>) for organizer endpoints, while maintaining existing single-language model for public endpoints  
**Rationale**: Enables organizers to see all translations while preserving existing public API contracts. Follows existing patterns in the codebase for domain model variations.  
**Alternatives considered**:
- Single model with nullable translation map: Rejected because it creates confusing API semantics
- Separate organizer-specific domain models: Rejected due to significant refactoring overhead
- Always return all translations: Rejected because it breaks existing public API contracts

### Repository Interface Evolution Strategy
**Decision**: Add new methods to existing repository interfaces (e.g., `findPacksByEventWithAllTranslations`) while maintaining existing methods for backward compatibility  
**Rationale**: Minimizes breaking changes while clearly separating organizer vs public API concerns. Follows existing patterns in the codebase.  
**Alternatives considered**:
- Modifying existing method signatures: Rejected because it breaks existing public endpoints
- Method overloading with optional parameters: Rejected due to Kotlin's handling of default parameters and potential ambiguity
- Separate repository interfaces: Rejected due to interface segregation violations and dependency injection complexity

### Translation Handling Strategy
**Decision**: Leverage existing OptionTranslationsTable infrastructure and modify existing `toDomain()` extension functions to support both single-language and multi-language scenarios  
**Rationale**: Reuses proven translation infrastructure, minimizes new code, maintains consistency with existing patterns.  
**Alternatives considered**:
- New translation loading mechanism: Rejected because existing system is comprehensive and well-tested
- Eager loading all translations always: Rejected due to performance implications for public endpoints
- Lazy loading with caching: Rejected due to complexity and current system's sufficiency

### API Route Modification Strategy
**Decision**: Modify existing organizer routes in SponsoringRoutes.kt to remove Accept-Language header requirement and call new repository methods  
**Rationale**: Minimal code changes, preserves existing route structure, maintains authentication/authorization patterns.  
**Alternatives considered**:
- Creating new organizer-specific route handlers: Rejected due to code duplication
- Route versioning (v1, v2): Rejected as overkill for internal organizer API
- Conditional behavior based on user type: Rejected due to complexity and violation of single responsibility

### Testing Strategy
**Decision**: Update existing integration tests to verify new behavior while adding new tests for multi-language scenarios  
**Rationale**: Ensures backward compatibility testing while validating new functionality. Follows existing test patterns.  
**Alternatives considered**:
- Complete test rewrite: Rejected due to unnecessary risk and effort
- Only testing new functionality: Rejected because it doesn't ensure non-regression
- Mock-heavy unit testing: Rejected because integration tests provide better confidence for API changes

### OpenAPI Documentation Strategy
**Decision**: Update existing endpoint documentation to reflect removed Accept-Language requirement and new response schema for organizer endpoints  
**Rationale**: Maintains accurate API documentation as required by constitution. Uses existing OpenAPI infrastructure.  
**Alternatives considered**:
- Separate OpenAPI specs for organizer vs public: Rejected due to maintenance overhead
- Generic documentation without specifics: Rejected because it violates constitution requirements for comprehensive API documentation
- Auto-generation only: Rejected because manual schema definitions provide better control and documentation quality

## Implementation Risks & Mitigations

### Risk: Breaking Existing Public Endpoints
**Mitigation**: Preserve all existing public endpoint methods and test coverage. New functionality targets only organizer-specific paths under `/orgs/`.

### Risk: Database Performance Impact  
**Mitigation**: Leverage existing optimized queries in OptionTranslationsTable. No new N+1 query issues since we're modifying existing queries, not adding new ones.

### Risk: Frontend Type Compatibility
**Mitigation**: Update generated TypeScript types and test frontend integration. Changes are additive (more data) so existing frontend code should be compatible.

### Risk: Test Suite Complexity
**Mitigation**: Follow existing test patterns and use H2 in-memory database for fast, isolated testing. Maintain clear separation between public and organizer API tests.

## Dependencies & Prerequisites

### Internal Dependencies
- Existing `OptionTranslationsTable` infrastructure
- Current `SponsoringPackEntity` and `SponsoringOptionEntity` models  
- Established Exposed ORM patterns
- Existing test infrastructure with H2 database

### External Dependencies  
- No new external dependencies required
- PostgreSQL schema remains unchanged
- Existing Ktor server and kotlinx.serialization versions sufficient

### Performance Considerations
- No significant performance impact expected - we're modifying existing queries rather than adding new ones
- Translation data is already loaded for single-language responses, we're just loading all instead of filtering
- Response size increase is minimal and acceptable for organizer management use cases

## Next Steps

1. **Phase 1**: Create data model definitions and API contracts based on these research findings
2. **Validate assumptions**: Confirm existing translation infrastructure can support multi-language loading efficiently  
3. **Design review**: Ensure proposed changes align with clean architecture principles and constitutional requirements