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

#### Repository Layer Separation of Concerns (CRITICAL)

Repository implementations MUST adhere to strict architectural boundaries:

1. **Repository implementations MUST NOT depend on other repositories**
   - ❌ WRONG: `class MyRepositoryExposed(private val notificationRepository: NotificationRepository, private val otherRepository: OtherRepository)`
   - ✅ CORRECT: `class MyRepositoryExposed : MyRepository` (no constructor parameters for repositories)
   - Repository layer is pure data access - cross-domain operations belong in route handlers

2. **Notification sending MUST happen in the route layer, NOT in repositories**
   - ❌ WRONG: Repository methods that call `notificationRepository.sendMessage()`
   - ✅ CORRECT: Route handlers inject `NotificationRepository`, fetch domain data, create `NotificationVariables`, send notifications
   - Repositories return data; routes orchestrate cross-cutting concerns

3. **Clean Architecture Layer Responsibilities**:
   - **Domain Layer** (`domain/`): Interfaces, domain models, business rules (no implementations)
   - **Application Layer** (`application/`): Repository implementations using Exposed entities directly, database operations only
   - **Infrastructure Layer** (`infrastructure/api/`): HTTP routes, request/response handling, orchestration of multiple repositories
   - Cross-cutting concerns (notifications, logging, metrics) belong in infrastructure layer

4. **Exposed ORM Pattern**:
   - Repository implementations MUST use Exposed entities directly: `Entity.findById()`, `Entity.find { query }`, etc.
   - Repository implementations MUST ONLY interact with database via Exposed - no other repository dependencies
   - Cross-domain data fetching happens in route handlers after repository calls return

**Reference Implementation**: See `PartnershipRoutes.kt` lines 35-53, 91-105, 118-126 for correct notification pattern:
```kotlin
// ✅ CORRECT - Route handler orchestration
fun Route.partnershipRoutes() {
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()
    val eventRepository by inject<EventRepository>()
    
    post("/{id}/approve") {
        val partnership = partnershipRepository.approve(id)
        
        // Fetch related domain objects for notification
        val event = eventRepository.findById(partnership.eventId)
        val company = partnership.company  // From partnership entity
        
        // Create notification variables
        val variables = NotificationVariables.PartnershipApproved(
            language = partnership.language,
            event = event,
            company = company,
            partnership = partnership
        )
        
        // Send notification directly
        notificationRepository.sendMessage(event.orgSlug, variables)
        
        call.respond(HttpStatusCode.OK, partnership)
    }
}
```

**Why This Matters**:
- Prevents circular dependencies between repositories
- Makes testing easier (mock repository interfaces, not internal dependencies)
- Follows Single Responsibility Principle (repositories handle data, routes handle orchestration)
- Matches existing codebase patterns (auth, billing, partnership all follow this)
- Enables independent evolution of domain modules

**Notification Pattern Details**:
- Template files: `server/application/src/main/resources/notifications/email/{event_name}/content.{lang}.html` and `notifications/slack/{event_name}/{lang}.md`
- Route layer: Inject `NotificationRepository`, fetch domain data after repository operations, create `NotificationVariables` subclass, call `sendMessage()`
- Error handling: Notification failures are handled internally by the notification repository implementation
- Language: Use partnership/company language preference for template selection

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

### Database Schema Standards (CRITICAL)
All database implementations MUST use Exposed ORM with complete dual-layer structure:

**Table Objects (Required)**:
- MUST extend `UUIDTable` for all entity tables
- MUST use `datetime()` function for all date/time columns (maps to PostgreSQL DATETIME, Kotlin LocalDateTime)
- MUST NEVER use `timestamp()` function - this is not the project standard
- MUST define all foreign key relationships with appropriate cascade rules (CASCADE, NO ACTION, etc.)
- MUST use `enumerationByName<EnumType>()` for enum columns
- MUST use `clientDefault {}` for created_at/updated_at with Clock.System.now().toLocalDateTime(TimeZone.UTC)
- MUST define appropriate indexes for query optimization

**Entity Classes (Required)**:
- MUST extend `UUIDEntity` with constructor parameter `id: EntityID<UUID>`
- MUST include companion object: `companion object : UUIDEntityClass<EntityName>(TableName)`
- MUST define properties using delegation: `var propertyName by TableName.columnName`
- MUST define foreign key relationships using `referencedOn` or `optionalReferencedOn`
- MUST include KDoc documentation for non-obvious business logic

**Rationale**: The dual Table/Entity structure is the consistent pattern across the entire codebase 
(see CompanyEntity/CompaniesTable, EventEntity/EventsTable, etc.). Tables define schema structure 
while Entities provide type-safe object-relational mapping. Using datetime() instead of timestamp() 
ensures LocalDateTime mapping which is the project standard for date/time handling.

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

### Authorization Pattern (CRITICAL)
Routes requiring organization-level permissions MUST use the `AuthorizedOrganisationPlugin` Ktor plugin 
instead of manual permission checking. This plugin automatically:
- Extracts JWT token from Authorization header via `call.token`
- Validates token and retrieves user info via `AuthRepository.getUserInfo()`
- Checks user has canEdit permission for organization via `UserRepository.hasEditPermissionByEmail()`
- Throws `UnauthorizedException` if user lacks permission (handled by StatusPages as HTTP 401)

**Usage Pattern** (applies to all routes under `/orgs/{orgSlug}/...`):
```kotlin
route("/orgs/{orgSlug}/events/{eventSlug}/resource") {
    install(AuthorizedOrganisationPlugin)  // REQUIRED for org-protected routes
    
    post {
        // No manual permission checking needed
        // Plugin guarantees user has canEdit=true before reaching this code
        val eventSlug = call.parameters.eventSlug
        // ... business logic
    }
}
```

**NEVER do manual permission checking**:
```kotlin
// ❌ WRONG - duplicates plugin functionality
post {
    val userInfo = authRepository.getUserInfo(call.token)
    if (!userRepository.hasEditPermissionByEmail(userInfo.email, orgSlug)) {
        throw UnauthorizedException()
    }
    // ...
}
```

**Rationale**: Consistent authorization across all organization-protected routes (events, sponsoring, 
partnerships, billing, etc.). Eliminates duplicate permission-checking code, ensures uniform error 
responses, and centralizes authentication logic for easier maintenance and security audits.

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

### Response Schema Standards (CRITICAL)
API responses MUST follow consistent identifier patterns for resources:

#### Slug vs ID Usage
- **Events**: MUST use `event_slug` (string) in response bodies, NEVER `event_id` (UUID)
- **Organizations**: MUST use `org_slug` (string) in response bodies, NEVER `org_id` (UUID)
- **Other entities**: Use UUID identifiers (`company_id`, `partnership_id`, `user_id`, etc.)

**Rationale**: Events and organizations are accessed via slug-based URLs throughout the application 
(`/orgs/{orgSlug}/events/{eventSlug}/...`). Response bodies must provide the slug to enable client-side 
URL construction without additional lookups. UUIDs are internal database identifiers not exposed 
in URLs for events/organizations. This pattern applies consistently across ALL endpoints that return 
event or organization data, whether in top-level responses or nested objects.

**Example** (Job Offer Promotion response):
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440000",
  "job_offer_id": "550e8400-e29b-41d4-a716-446655440000",
  "partnership_id": "750e8400-e29b-41d4-a716-446655440000",
  "event_slug": "devlille-2025",  // ✅ CORRECT - use slug
  "status": "pending"
  // ❌ NEVER include "event_id": "uuid-here"
}
```

### File Structure Standards
- **Main specification**: `server/application/src/main/resources/openapi/openapi.yaml` (2500+ lines)
- **External schemas**: `server/application/src/main/resources/schemas/*.json` (50+ schema files)
- **Generated files**: `documentation.yaml` is auto-generated - NEVER edit manually

### OpenAPI Validation Process (CRITICAL)

All OpenAPI changes MUST be validated before committing to ensure specification accuracy and completeness.

#### Validation Command
From the `server/` directory, run:
```bash
cd server && npm run validate
```

This executes Redocly CLI linting: `redocly lint application/src/main/resources/openapi/openapi.yaml`

#### Success Criteria
- **Zero errors required** - validation must pass with 0 errors (warnings are acceptable)
- All schema `$ref` references must resolve correctly
- All examples must validate against their schemas
- JSON Schema files must use OpenAPI 3.1-compatible syntax

#### Common Issues & Resolution

**1. Unresolved Schema References**
- **Error**: `Can't resolve $ref to <SchemaName>`
- **Cause**: Schema referenced in `components/schemas` but `.schema.json` file doesn't exist
- **Solution**:
  1. Create schema file: `server/application/src/main/resources/schemas/<schema_name>.schema.json`
  2. Follow JSON Schema Draft 7 format with proper validation rules
  3. Add to `openapi.yaml` components section:
     ```yaml
     components:
       schemas:
         SchemaName:
           $ref: "../schemas/schema_name.schema.json"
     ```

**2. Invalid `nullable` Property**
- **Error**: `Property 'nullable' is not expected here`
- **Cause**: JSON Schema Draft 7 doesn't support OpenAPI 3.0's `nullable` keyword
- **Solution**: Use union types:
  ```json
  {
    "type": ["string", "null"]  // ✅ CORRECT
  }
  ```
  Instead of:
  ```json
  {
    "type": "string",
    "nullable": true  // ❌ WRONG - OpenAPI 3.0 only
  }
  ```

**3. Example Validation Errors**
- **Error**: `Example must have required property 'field_name'`
- **Cause**: Request/response examples don't match schema constraints
- **Solution**: Update example to include all required fields or adjust schema requirements

**4. Structural Validation Errors**
- Missing required OpenAPI fields (info, paths, components)
- Invalid JSON Schema syntax in external `.schema.json` files
- Incorrect `$ref` paths (must be relative: `"../schemas/file.schema.json"`)

#### Integration Points
- **Before Commit**: Run `npm run validate` to catch errors early
- **CI/CD**: GitHub Actions should include validation step (if configured)
- **Documentation Build**: Run `npm run build-docs` (validates + bundles spec)
- **PR Reviews**: Reviewers must verify validation passes for OpenAPI changes

#### Related Commands
```bash
# Bundle specification into single file
npm run bundle

# Preview API documentation locally
npm run preview

# Full documentation build (validate + bundle)
npm run build-docs
```

**Rationale**: Valid OpenAPI specifications ensure accurate API documentation, enable automated 
client generation, prevent deployment issues, and maintain API contract integrity. The validation 
process catches schema mismatches, reference errors, and format violations before they reach 
production, reducing integration bugs and improving developer experience.

## Governance

This constitution supersedes all other development practices and coding guidelines. All pull 
requests MUST be reviewed for constitutional compliance before merge approval. Technical 
decisions that deviate from these principles MUST be documented with architectural decision 
records (ADRs) and approved by senior engineering staff.

Amendment procedure: Constitutional changes require consensus from engineering team leads, 
documentation of impact analysis, and migration plan for existing code where applicable.

**Version**: 1.0.0 | **Ratified**: 2025-10-02 | **Last Amended**: 2025-10-02