<!--
Sync Impact Report:
- Version change: 1.1.0 → 2.0.0
- Modified principles:
  - "Code Quality Standards" → kept, reduced detail (was Principle I)
  - "Comprehensive Testing Strategy" → simplified with skill references (was Principle II)
  - "Clean Modular Architecture" → simplified with skill references (was Principle III)
  - "API Consistency & User Experience" → simplified with skill references (was Principle IV)
  - "Performance & Observability Requirements" → kept intact (was Principle V)
- Removed sections (moved to skills):
  - Test Architecture details → contract-tests, integration-tests, test-factories skills
  - Contract Test Requirements (examples, patterns) → contract-tests skill
  - Integration Test Requirements (examples, patterns) → integration-tests skill
  - Factory Function Pattern (examples) → test-factories skill
  - Repository Layer Separation details → clean-architecture skill
  - Notification Pattern Details → api-routing skill
  - JSON Schema Validation Pattern details → openapi-schemas, api-routing skills
  - Database Schema Standards → exposed-entities skill
  - OpenAPI Documentation Requirements → openapi-schemas skill
  - Schema Integration Requirements → openapi-schemas skill
  - Authorization Pattern details → api-routing skill
  - Exception Handling Pattern details → api-routing skill
  - Parameter Extraction Pattern details → api-routing skill
  - OpenAPI 3.1.0 Compliance details → openapi-schemas skill
  - Validation Requirements details → openapi-schemas skill
  - File Structure Standards → openapi-schemas skill
- Kept sections (no skill coverage):
  - Principle I core (ktlint/detekt compliance)
  - Principle V entirely (Performance & Observability)
  - Code Quality Standards: quality gates, documentation principles
  - API Design Standards: general REST constraints
  - Response Schema Standards: slug vs ID usage
  - Governance section
- Templates requiring updates: ✅ plan-template.md (no changes needed)
  ✅ spec-template.md (no changes needed) ✅ tasks-template.md (no changes needed)
- Follow-up TODOs: None
-->

# Partners Connect Server Constitution

## Core Principles

### I. Code Quality Standards (NON-NEGOTIABLE)

All code MUST adhere to ktlint formatting and detekt static analysis rules
with zero violations. Code reviews MUST verify readability, maintainability,
and adherence to Kotlin idioms. No exceptions will be made for technical debt
without explicit architectural approval and remediation timeline.

**Rationale**: High code quality ensures long-term maintainability, reduces
onboarding time, and prevents accumulation of technical debt in a complex
partnership management system.

### II. Comprehensive Testing Strategy (NON-NEGOTIABLE)

Every new feature MUST include tests achieving minimum 80% code coverage
through HTTP route testing. Database operations MUST be tested via HTTP
endpoints using H2 in-memory database. Test-driven development is STRONGLY
ENCOURAGED for complex business logic.

**CRITICAL**: Focus on HTTP route tests that validate API behavior, NOT
repository tests that directly test implementation details.

> **Skills for detailed patterns**:
> - **contract-tests** — naming, structure, shared DB setup, status code
>   coverage, response assertions
> - **integration-tests** — multi-step workflows, state transitions,
>   cross-domain interactions
> - **test-factories** — `insertMocked*` / `create*` conventions, FK
>   handling, polymorphic factories
> - See also [server/docs/TESTING.md](../server/docs/TESTING.md)

**Rationale**: Contract tests ensure API reliability and prevent breaking
changes. Integration tests validate business requirements. Separation of
concerns prevents brittle tests while maintaining comprehensive coverage.

### III. Clean Modular Architecture

Domain modules MUST remain decoupled with clear boundaries. Each module MUST
expose well-defined interfaces and avoid circular dependencies. Shared
infrastructure MUST be isolated in the `internal/` module. Database schema
changes MUST be backwards-compatible with migration strategy.

> **Skills for detailed patterns**:
> - **clean-architecture** — package organisation, layer responsibilities,
>   module scaffolding, where to place each class
> - **api-routing** — route structure, plugin installation, notification
>   plugins, dependency injection in routes

**Rationale**: Modular architecture enables independent development, testing,
and deployment of features while maintaining system coherence and supporting
team scalability.

### IV. API Consistency & User Experience

REST API endpoints MUST follow consistent naming conventions, HTTP status
codes, and error response formats. All endpoints MUST include comprehensive
OpenAPI documentation. Response times MUST not exceed 2 seconds for standard
operations. User-facing error messages MUST be clear and actionable.

> **Skills for detailed patterns**:
> - **api-routing** — authorization plugin, exception handling, parameter
>   extraction, schema validation, response patterns
> - **openapi-schemas** — JSON schema authoring, OpenAPI path patterns,
>   schema registration, validation/bundle workflow
> - **exposed-entities** — table/entity definitions, companion queries,
>   DSL operators

**Rationale**: Consistent APIs reduce integration friction and improve
developer experience for API consumers. Schema-based validation ensures
consistent request validation and maintains OpenAPI documentation accuracy.

### V. Performance & Observability Requirements

All database queries MUST be optimized with proper indexing and connection
pooling. Application MUST emit structured logs with correlation IDs for
request tracing. Performance metrics MUST be collected for all critical
business operations. Resource usage MUST be monitored in production with
alerting on thresholds.

**CRITICAL**: Performance testing and load testing are NOT part of the
implementation phase. Feature specifications and quickstart guides MUST focus
on functional validation only. Performance requirements are production
concerns handled by infrastructure and operations teams.

**Rationale**: Partnership operations involve financial transactions and
time-sensitive event management requiring high performance and comprehensive
monitoring for operational excellence.

## Code Quality Standards

All code contributions MUST pass automated quality gates:
- ktlint formatting compliance (enforced in CI/CD)
- detekt static analysis with zero violations
- Minimum 80% test coverage for new code
- All public APIs documented with KDoc
- No TODO comments in production branches without GitHub issues

### Documentation Principles (CRITICAL)

Interface documentation MUST be comprehensive and serve as the single source
of truth. Implementation classes MUST NOT duplicate interface documentation —
they should only document implementation-specific details, design choices, or
technical constraints not covered by the interface contract. This prevents
documentation drift and reduces maintenance burden while keeping the interface
as the authoritative contract documentation.

## API Design Standards

REST API design MUST follow these constraints:
- RESTful resource naming (plural nouns, clear hierarchies)
- Consistent error response format with error codes and messages
- Proper HTTP status codes (2xx success, 4xx client errors, 5xx server errors)
- Request/response validation with clear validation error messages
- Rate limiting implemented for all public endpoints
- Comprehensive OpenAPI/Swagger documentation maintained

> **Skills for implementation details**: see **api-routing** and
> **openapi-schemas** skills for authorization, exception handling,
> parameter extraction, schema integration, and OpenAPI compliance.

### Response Schema Standards (CRITICAL)

API responses MUST follow consistent identifier patterns for resources:

#### Slug vs ID Usage
- **Events**: MUST use `event_slug` (string) in response bodies, NEVER
  `event_id` (UUID)
- **Organizations**: MUST use `org_slug` (string) in response bodies, NEVER
  `org_id` (UUID)
- **Other entities**: Use UUID identifiers (`company_id`, `partnership_id`,
  `user_id`, etc.)

**Rationale**: Events and organizations are accessed via slug-based URLs
throughout the application (`/orgs/{orgSlug}/events/{eventSlug}/...`).
Response bodies must provide the slug to enable client-side URL construction
without additional lookups.

**Example** (Job Offer Promotion response):
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440000",
  "job_offer_id": "550e8400-e29b-41d4-a716-446655440000",
  "partnership_id": "750e8400-e29b-41d4-a716-446655440000",
  "event_slug": "devlille-2025",
  "status": "pending"
}
```

## Governance

This constitution supersedes all other development practices and coding
guidelines. All pull requests MUST be reviewed for constitutional compliance
before merge approval. Technical decisions that deviate from these principles
MUST be documented with architectural decision records (ADRs) and approved by
senior engineering staff.

Amendment procedure: Constitutional changes require consensus from engineering
team leads, documentation of impact analysis, and migration plan for existing
code where applicable.

**Version**: 2.0.0 | **Ratified**: 2025-10-02 | **Last Amended**: 2026-03-24