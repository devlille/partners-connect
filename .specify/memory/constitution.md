<!-- 
Sync Impact Report:
- Version change: Initial → 1.0.0
- New constitution created with 5 core principles
- Added sections: Code Quality Standards, API Design Standards
- Templates requiring updates: ⚠ All templates need initial review
- Follow-up TODOs: None
-->

# Partners Connect Server Constitution

## Core Principles

### I. Code Quality Standards (NON-NEGOTIABLE)
All code MUST adhere to ktlint formatting and detekt static analysis rules with zero violations. 
Every class, function, and property MUST have meaningful documentation. Code reviews MUST verify 
readability, maintainability, and adherence to Kotlin idioms. No exceptions will be made for 
technical debt without explicit architectural approval and remediation timeline.

**Rationale**: High code quality ensures long-term maintainability, reduces onboarding time, 
and prevents accumulation of technical debt in a complex partnership management system.

### II. Comprehensive Testing Strategy (NON-NEGOTIABLE)
Every new feature MUST include integration tests achieving minimum 80% code coverage through 
HTTP route testing. Integration tests MUST cover all external service interactions (Slack, 
Mailjet, BilletWeb, Google Cloud Storage). Database operations MUST be tested via HTTP endpoints 
using H2 in-memory database. Test-driven development is STRONGLY ENCOURAGED for complex business logic.

**CRITICAL**: Focus on HTTP route integration tests that validate API behavior, NOT repository 
tests that directly test implementation details. Repository logic is validated implicitly through 
route tests which provide better coverage of the complete request/response cycle including 
serialization, validation, and error handling.

**Rationale**: Partnership and billing operations require absolute reliability. Integration-focused 
testing prevents revenue-impacting bugs, ensures proper API contracts, and provides confidence 
in deployments while avoiding brittle implementation-specific tests.

### III. Clean Modular Architecture
Domain modules MUST remain decoupled with clear boundaries. Each module (auth, billing, companies, 
events, etc.) MUST expose well-defined interfaces and avoid circular dependencies. Shared 
infrastructure MUST be isolated in the internal/ module. Database schema changes MUST be 
backwards-compatible with migration strategy.

**Rationale**: Modular architecture enables independent development, testing, and deployment 
of features while maintaining system coherence and supporting team scalability.

### IV. API Consistency & User Experience
REST API endpoints MUST follow consistent naming conventions, HTTP status codes, and error 
response formats. All endpoints MUST include comprehensive OpenAPI documentation. Response 
times MUST not exceed 2 seconds for standard operations. User-facing error messages MUST 
be clear and actionable.

**Rationale**: Consistent APIs reduce integration complexity for frontend teams and external 
partners, while performance standards ensure responsive user experience.

### V. Performance & Observability Requirements
All database queries MUST be optimized with proper indexing and connection pooling. Application 
MUST emit structured logs with correlation IDs for request tracing. Performance metrics MUST 
be collected for all critical business operations. Resource usage MUST be monitored in production 
with alerting on thresholds.

**Rationale**: Partnership operations involve financial transactions and time-sensitive event 
management requiring high performance and comprehensive monitoring for operational excellence.

## Code Quality Standards

All code contributions MUST pass automated quality gates:
- ktlint formatting compliance (enforced in CI/CD)
- detekt static analysis with zero violations
- Minimum 80% test coverage for new code
- All public APIs documented with KDoc
- No TODO comments in production branches without GitHub issues

### Documentation Principles (CRITICAL)
Interface documentation MUST be comprehensive and serve as the single source of truth. Implementation 
classes MUST NOT duplicate interface documentation - they should only document implementation-specific 
details, design choices, or technical constraints not covered by the interface contract. This prevents 
documentation drift and reduces maintenance burden while keeping the interface as the authoritative 
contract documentation.

## API Design Standards

REST API design MUST follow these constraints:
- RESTful resource naming (plural nouns, clear hierarchies)
- Consistent error response format with error codes and messages
- Proper HTTP status codes (2xx success, 4xx client errors, 5xx server errors)
- Request/response validation with clear validation error messages
- Rate limiting implemented for all public endpoints
- Comprehensive OpenAPI/Swagger documentation maintained

### OpenAPI Documentation Requirements (CRITICAL)
All new or modified REST API endpoints MUST be documented in `server/application/src/main/resources/openapi/openapi.yaml`. 
The `documentation.yaml` file is auto-generated by Redocly CLI in CI/CD and MUST NOT be edited manually. 
Endpoint specifications MUST include complete parameter definitions, request/response schemas, error codes, 
and descriptive documentation. Header requirements (especially Accept-Language) MUST be accurately reflected 
with correct `required` flags.

### Exception Handling Pattern (CRITICAL)
Route handlers MUST NOT include try-catch blocks for exception-to-HTTP conversion. The Ktor 
StatusPages plugin handles all exception-to-HTTP response mapping automatically with consistent 
error formatting. Domain/repository layers MUST throw appropriate exceptions (NotFoundException, 
ConflictException, ForbiddenException, etc.) defined in `internal/infrastructure/api` which 
StatusPages converts to proper HTTP responses. Repository implementations MUST throw exceptions 
instead of returning nullable types or Boolean success flags - this ensures consistent error 
handling and proper HTTP status code mapping. Manual exception handling in routes creates 
inconsistent error responses and duplicates functionality.

**Repository Exception Requirements**:
- NEVER return `null` or `Boolean` for operations that can fail - throw exceptions instead
- Use `NotFoundException` for missing entities (mapped to HTTP 404)
- Use `ConflictException` for business rule violations (mapped to HTTP 409)  
- Use `ForbiddenException` for authorization failures (mapped to HTTP 403)
- All exceptions MUST be defined in `internal/infrastructure/api` package for StatusPages mapping

### Parameter Extraction Pattern (CRITICAL)
Route handlers MUST use the predefined StringValues extension functions for parameter extraction 
instead of manual null checks with custom exceptions. Use `call.parameters.eventSlug`, 
`call.parameters.orgSlug`, etc. These extensions automatically throw MissingRequestParameterException 
(handled by StatusPages as BadRequestException) when parameters are missing. NEVER throw 
IllegalArgumentException or other custom exceptions for missing parameters - this breaks the 
StatusPages exception handling contract and creates inconsistent error responses.

## OpenAPI Configuration Standards (CRITICAL)

### Validation Requirements
The OpenAPI specification MUST pass complete validation with zero errors using `npm run validate` 
from the project root. This runs Redocly CLI validation against the comprehensive OpenAPI 3.1.0 
specification. Warnings are acceptable but all errors MUST be resolved before merging.

**Validation Command**: `npm run validate` (executes `redocly lint server/application/src/main/resources/openapi/openapi.yaml`)

### OpenAPI 3.1.0 Compliance (NON-NEGOTIABLE)
All API operations MUST conform to OpenAPI 3.1.0 standards:

#### Security Configuration
- **All operations** MUST include security definitions: `security: - bearerAuth: []`
- **Public endpoints**  MUST use empty security: `security: - {}`
- **bearerAuth scheme** is defined in `components/securitySchemes` for JWT token authentication
- Backend implementation MUST handle optional authentication appropriately for public endpoints

#### Operation Standards
- **operationId**: Every operation MUST have a unique operationId in camelCase (e.g., `getEvents`, `postCompanies`)
- **summary**: Every operation MUST have a summary field with brief description
- **responses**: Operations SHOULD include both 2xx success and 4xx error responses for completeness
- **parameters**: Path parameters MUST be consistent across related endpoints (use `{orgSlug}` consistently, not mixed with `{slug}`)

#### Schema Compatibility
- **JSON Schema files** (`server/application/src/main/resources/schemas/*.json`) MUST be OpenAPI 3.1 compatible
- **NO `nullable: true`** - Use union types instead: `"type": ["string", "null"]`
- **Schema references** MUST resolve correctly from openapi.yaml to external schema files

### File Structure Standards
- **Main specification**: `server/application/src/main/resources/openapi/openapi.yaml` (2500+ lines)
- **External schemas**: `server/application/src/main/resources/schemas/*.json` (50+ schema files)
- **Generated files**: `documentation.yaml` is auto-generated - NEVER edit manually

## Governance

This constitution supersedes all other development practices and coding guidelines. All pull 
requests MUST be reviewed for constitutional compliance before merge approval. Technical 
decisions that deviate from these principles MUST be documented with architectural decision 
records (ADRs) and approved by senior engineering staff.

Amendment procedure: Constitutional changes require consensus from engineering team leads, 
documentation of impact analysis, and migration plan for existing code where applicable.

**Version**: 1.0.0 | **Ratified**: 2025-10-02 | **Last Amended**: 2025-10-02