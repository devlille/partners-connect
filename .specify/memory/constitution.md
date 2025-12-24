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

> **📚 COMPLETE TESTING DOCUMENTATION**: See [server/docs/TESTING.md](../server/docs/TESTING.md) for comprehensive testing patterns, architecture, and examples.

#### Test Architecture (MANDATORY - Updated 2025-12-28)

**Shared Database Pattern** (ALL tests MUST follow):
- Use `moduleSharedDb(userId)` for ALL new tests (NOT `moduleMocked()`)
- Pre-create ALL UUIDs before data initialization: `val companyId = UUID.randomUUID()`
- Initialize ALL test data in a SINGLE `transaction {}` block
- Factory functions MUST NOT manage transactions themselves
- Use UUID-based defaults in factories to ensure uniqueness: `name = id.toString()`

**Test Organization**:
- **Contract Tests**: `<feature>.infrastructure.api` package - validates HTTP contract
- **Integration Tests**: `<feature>` package (root) - validates end-to-end workflows

#### Contract Test Requirements (CRITICAL)
All new API endpoints MUST include contract tests that focus on API schema validation:

**Naming Convention**: `<Feature><EndpointResource>Route<Verb>Test`
- Examples: `PartnershipRegisterRoutePostTest`, `CompanyJobOfferRouteDeleteTest`, `EventBySlugRouteGetTest`

**Location**: `<feature>.infrastructure.api` package

**Contract Test Scope**:
- Request/response schema validation ONLY - not business logic
- Test ALL HTTP status codes the endpoint can return (200/201/204, 400, 401, 403, 404, 409)
- JSON serialization/deserialization correctness
- Parameter validation (path, query, body)
- MUST use `call.receive<T>(schema)` pattern with JSON schemas
- MUST be written BEFORE implementation (TDD approach)

**Factory Function Pattern** (MANDATORY):
- **Naming**: `insertMocked<Entity>()` for database entities, `create<Domain>()` for domain objects
- **File naming**: `<Name>.factory.kt` (e.g., `Company.factory.kt`, `Partnership.factory.kt`)
- **Location**: `<feature>/factories/` package
- All parameters MUST have defaults
- Unique fields MUST use UUID-based defaults: `name = id.toString()`
- NO transaction management in factories
- Follow existing factory patterns exactly

**Example Factory**:
```kotlin
@Suppress("LongParameterList")
fun insertMockedCompany(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(), // UUID-based unique default
    siret: String = "SIRET-${id}",
    // ... all parameters with defaults
): CompanyEntity = CompanyEntity.new(id) {
    this.name = name
    this.siret = siret
    // ... set all fields
}
```

**Example Test Structure**:
```kotlin
package fr.devlille.partners.connect.partnership.infrastructure.api

class PartnershipRegisterRoutePostTest {
    @Test
    fun `POST registers valid partnership - returns 201`() = testApplication {
        val userId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
            }
        }
        
        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody("""{"packId":"...", "companyId":"$companyId"}""")
        }
        
        assertEquals(HttpStatusCode.Created, response.status)
    }
    
    @Test
    fun `POST with invalid data - returns 400`() = testApplication { /* ... */ }
    
    @Test
    fun `POST without auth - returns 401`() = testApplication { /* ... */ }
    
    // Test ALL status codes!
}
```

#### Integration Test Requirements (CRITICAL)

**Naming Convention**: `<Feature>(<EndpointResource>)RoutesTest` (plural "Routes")
- Examples: `PartnershipSpeakersRoutesTest`, `CompanyJobOfferRoutesTest`, `ProvidersAttachmentRoutesTest`

**Location**: `<feature>` package (root of domain)

**Integration Test Scope** (separate from contract tests):
- End-to-end business logic validation across multiple endpoints
- Cross-domain operations and notifications
- Complex workflows and state transitions (create → verify → update → delete)
- Database persistence and consistency
- Minimal setup - only create data necessary for the workflow

**Example Test Structure**:
```kotlin
package fr.devlille.partners.connect.partnership

/**
 * Integration test for complete speaker-partnership workflow.
 * Tests end-to-end business logic from attachment to detachment.
 */
class PartnershipSpeakersRoutesTest {
    @Test
    fun `complete workflow from attachment to detachment`() = testApplication {
        val userId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        
        application {
            moduleSharedDb(userId)
            transaction {
                // Minimal setup for workflow
                insertMockedUser(userId)
                insertMockedEvent(eventId)
                insertMockedPartnership(partnershipId, eventId)
                insertMockedSpeaker(speakerId, eventId)
            }
        }
        
        // Step 1: Attach speaker
        val attachResponse = client.post("/events/$eventId/partnerships/$partnershipId/speakers/$speakerId")
        assertEquals(HttpStatusCode.Created, attachResponse.status)
        
        // Step 2: Verify attachment
        val getResponse = client.get("/events/$eventId/partnerships/$partnershipId")
        // Assert speaker is in response
        
        // Step 3: Detach speaker
        val detachResponse = client.delete("/events/$eventId/partnerships/$partnershipId/speakers/$speakerId")
        assertEquals(HttpStatusCode.NoContent, detachResponse.status)
        
        // Step 4: Verify detachment
        val finalResponse = client.get("/events/$eventId/partnerships/$partnershipId")
        // Assert speaker is NOT in response
    }
}
```

**Rationale**: Contract tests ensure API reliability and prevent breaking changes. Integration 
tests validate business requirements. Separation of concerns prevents brittle tests that fail 
due to implementation details while maintaining comprehensive coverage of API contracts. The 
shared database pattern improves performance while UUID-based uniqueness prevents data conflicts.

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

#### JSON Schema Validation Pattern (CRITICAL)
All API endpoints that accept request bodies MUST use JSON schema validation through 
`call.receive<T>(schema)` pattern instead of manual Kotlin validation:

**Required Implementation**:
- Generate JSON schemas for all request/response types in `server/application/src/main/resources/schemas/`
- Schema files MUST follow naming convention: `{snake_case_name}.schema.json` (e.g., `attach_speaker_request.schema.json`, `speaker_partnership_response.schema.json`)
- Update `openapi.yaml` to reference schema files using `$ref: "schemas/{schema_name}.schema.json"`
- Use `call.receive<T>(schema)` in route handlers for automatic validation
- JSON schemas enable automatic 400 Bad Request responses for validation failures

**Schema Standards**:
- Use OpenAPI 3.1.0 compatible JSON schemas (no `nullable: true`, use union types instead)
- Polymorphic types MUST use `@JsonClassDiscriminator("type")` with `@SerialName` annotations
- Enum descriptors MUST use snake_case JSON serialization: `@SerialName("typed_quantitative")`
- Schema validation errors automatically handled by Ktor without custom exception handling

**Example Pattern**:
```kotlin
// ✅ CORRECT - Schema-based validation
post("/options") {
    val request = call.receive<CreateSponsoringOptionRequest>(CreateSponsoringOptionSchema)
    // Validation already completed, proceed with business logic
}

// ❌ WRONG - Manual validation
post("/options") {
    val request = call.receive<CreateSponsoringOptionRequest>()
    if (request.name.isBlank()) throw BadRequestException("Name required")
    // ... manual validation logic
}
```

**Rationale**: JSON schema validation ensures consistent request validation, reduces boilerplate 
code, provides clear error messages, and maintains OpenAPI documentation accuracy. Schema-based 
validation is more maintainable and prevents validation logic duplication across endpoints.

### V. Performance & Observability Requirements
All database queries MUST be optimized with proper indexing and connection pooling. Application 
MUST emit structured logs with correlation IDs for request tracing. Performance metrics MUST 
be collected for all critical business operations. Resource usage MUST be monitored in production 
with alerting on thresholds.

**CRITICAL**: Performance testing and load testing are NOT part of the implementation phase. 
Feature specifications and quickstart guides MUST focus on functional validation only. 
Performance requirements are production concerns handled by infrastructure and operations teams.

**Rationale**: Partnership operations involve financial transactions and time-sensitive event 
management requiring high performance and comprehensive monitoring for operational excellence. 
However, performance testing is a separate concern from feature implementation and should 
not be included in development specifications.

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

#### Schema Integration Requirements (CRITICAL)
OpenAPI documentation MUST reference dedicated schema components to prevent duplication and ensure consistency:

**Schema Component Pattern**:
- Create external JSON schema files in `server/application/src/main/resources/schemas/{schema_name}.schema.json` first
- Define schema components in `openapi.yaml` under `components/schemas` section by referencing external files
- Reference schemas using: `$ref: "#/components/schemas/SchemaName"` for internal component references
- Use `npm run bundle` to resolve and bundle external schema references during build process
- Schema files MUST be OpenAPI 3.1.0 compatible (use union types, not `nullable: true`)
- All schema components MUST reference external JSON schema files, never define schemas inline

**Required Schema Components for New Features**:
- Create JSON schema files first: `create_{entity}_request.schema.json`, `update_{entity}_request.schema.json`
- Create response schema files: `{entity}_response.schema.json`, `{entity}_list_response.schema.json` 
- Reference external schema files in `components/schemas`: each component MUST use `$ref: "schemas/{filename}.schema.json"`
- Update `openapi.yaml` requestBody and responses to reference schema components (not external files directly)
- Use `npm run bundle` to resolve external schema references and update documentation.yaml

**Validation Integration**:
- JSON schemas enable `call.receive<T>(schema)` validation in route handlers
- Schema validation errors automatically return HTTP 400 with descriptive messages
- Reduces manual validation code and ensures OpenAPI documentation accuracy
- Use `npm run validate` to verify schema correctness and reference resolution

**Example**:
```yaml
# In openapi.yaml - Reference external schema files in components
components:
  schemas:
    CreateSponsoringOptionRequest:
      $ref: "schemas/create_sponsoring_option_request.schema.json"

# Then reference components in operations
requestBody:
  required: true
  content:
    application/json:
      schema:
        $ref: "#/components/schemas/CreateSponsoringOptionRequest"
```

**NPM Script Integration**:
- Use `npm run validate` to check schema correctness and OpenAPI specification validity
- Use `npm run bundle` to resolve and generate final documentation.yaml
- External schema files can be bundled using the npm run bundle command

**Rationale**: Schema components in OpenAPI MUST always reference external JSON schema files to maintain 
single source of truth for validation logic. External schema files enable `call.receive<T>(schema)` 
validation in route handlers and are resolved via `npm run bundle` during build process. This 
approach eliminates duplication between validation logic and documentation, ensures consistency 
between API implementation and documentation, and prevents inline schema definitions that can 
drift from actual validation logic while maintaining clean separation of concerns.

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

## Governance

This constitution supersedes all other development practices and coding guidelines. All pull 
requests MUST be reviewed for constitutional compliance before merge approval. Technical 
decisions that deviate from these principles MUST be documented with architectural decision 
records (ADRs) and approved by senior engineering staff.

Amendment procedure: Constitutional changes require consensus from engineering team leads, 
documentation of impact analysis, and migration plan for existing code where applicable.

**Version**: 1.1.0 | **Ratified**: 2025-10-02 | **Last Amended**: 2025-11-01