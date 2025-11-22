# Implementation Plan: Assign Organiser to Partnership

**Branch**: `011-assign-partnership-organiser` | **Date**: November 22, 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/011-assign-partnership-organiser/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Enable organisation administrators to assign team members as dedicated organisers for partnerships, providing partners with direct contact information instead of relying only on generic organisational email addresses. This involves adding an optional foreign key reference from PartnershipsTable to UsersTable, implementing repository methods for assignment/viewing/removal, and extending API endpoints for organiser management.

## Technical Context

**Language/Version**: Kotlin/JVM 21  
**Primary Dependencies**: Ktor 3.2.0, Exposed ORM 1.0.0-beta-2, Koin 4.1.0, PostgreSQL 42.7.1  
**Storage**: PostgreSQL with Exposed ORM, H2 in-memory for tests  
**Testing**: Kotlin Test with H2 database for integration tests, contract tests using JSON schemas  
**Target Platform**: Linux server (containerized deployment, Port 8080)  
**Project Type**: Web backend API (Kotlin/Ktor server) - backend-only feature  
**Performance Goals**: <2 second response time for standard operations (per constitution)  
**Constraints**: Zero ktlint/detekt violations, 80% test coverage minimum, modular architecture compliance  
**Scale/Scope**: Single optional organiser per partnership, organisation-scoped access control

**Implementation Details**: 
- Add `organiserId` nullable foreign key column to `PartnershipsTable` (server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipsTable.kt)
- Create database migration in `server/application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/versions/`
- Extend `PartnershipRepository` interface with organiser assignment methods (server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/)
- Implement organiser methods in `PartnershipRepositoryExposed` (server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/)
- Add routes to `PartnershipRoutes.kt` under `/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/organiser` (POST assign, DELETE remove)
- Create JSON schemas for request/response in `server/application/src/main/resources/schemas/`
- Update OpenAPI spec in `server/application/src/main/resources/openapi/openapi.yaml`
- No frontend changes required for MVP (can be added later)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Code Quality Standards
- ✅ **ktlint/detekt compliance**: Will be verified in CI/CD
- ✅ **Documentation**: All public APIs will have KDoc comments
- ✅ **No TODO markers**: No production TODOs without GitHub issues

### II. Comprehensive Testing Strategy  
- ✅ **Contract tests**: Required for all new endpoints (assign/remove organiser)
- ✅ **80% coverage**: Integration tests for HTTP routes using H2 database
- ✅ **Mock factories**: Existing `insertMockedUser()` factory available for test data setup
- ✅ **TDD approach**: Contract tests written BEFORE implementation

### III. Clean Modular Architecture
- ✅ **No repository dependencies**: PartnershipRepository will NOT inject other repositories
- ✅ **No notifications in repository**: Assignment is silent (FR-013), no notification orchestration needed
- ✅ **Route layer orchestration**: Routes will handle multi-repository operations if needed
- ✅ **Exposed ORM pattern**: Repository uses PartnershipEntity/UserEntity directly

### IV. API Consistency & User Experience
- ✅ **JSON schema validation**: Use `call.receive<T>(schema)` pattern for request validation
- ✅ **Schema files**: Create in `server/application/src/main/resources/schemas/`
- ✅ **OpenAPI documentation**: Update `openapi.yaml` with schema component references
- ✅ **Authorization plugin**: Use `AuthorizedOrganisationPlugin` for org-protected routes
- ✅ **Exception handling**: Throw domain exceptions (NotFoundException, etc.), StatusPages handles HTTP mapping
- ✅ **Parameter extraction**: Use `call.parameters.partnershipId` extension functions

### V. Database Schema Standards
- ✅ **UUIDTable**: PartnershipsTable already extends UUIDTable
- ✅ **datetime() function**: Not needed - FR-015 explicitly states no timestamps for organiser assignment
- ✅ **Foreign key**: `organiserId` as optional reference to UsersTable
- ✅ **Migration**: Create migration with SchemaUtils.createMissingTablesAndColumns pattern
- ✅ **Entity delegation**: Update PartnershipEntity with `var organiser by UserEntity optionalReferencedOn`

### VI. OpenAPI Configuration Standards
- ✅ **Schema components**: Reference external JSON schema files in `components/schemas`
- ✅ **Security definitions**: Use `security: - bearerAuth: []` for protected endpoints
- ✅ **operationId**: Unique camelCase IDs for each operation
- ✅ **npm run validate**: Must pass with zero errors before merge

**GATE RESULT**: ✅ PASSED - All constitutional requirements can be met

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
server/
├── application/src/main/kotlin/fr/devlille/partners/connect/
│   ├── partnership/
│   │   ├── domain/
│   │   │   └── PartnershipRepository.kt          # Add: assignOrganiser, removeOrganiser
│   │   ├── application/
│   │   │   └── PartnershipRepositoryExposed.kt   # Implement new organiser methods
│   │   └── infrastructure/
│   │       ├── api/
│   │       │   └── PartnershipRoutes.kt          # Add: POST/DELETE organiser endpoints
│   │       └── db/
│   │           ├── PartnershipsTable.kt          # Add: organiserId column
│   │           └── PartnershipEntity.kt          # Add: organiser property
│   └── internal/infrastructure/migrations/versions/
│       └── AddPartnershipOrganiserMigration.kt   # Create migration for new column
│
├── application/src/main/resources/
│   ├── schemas/
│   │   ├── assign_organiser_request.schema.json  # Schema for assign request
│   │   └── partnership_organiser_response.schema.json  # Schema for organiser info response
│   └── openapi/
│       └── openapi.yaml                          # Update with new endpoints
│
└── application/src/test/kotlin/fr/devlille/partners/connect/partnership/
    └── infrastructure/api/
        └── PartnershipOrganiserRoutesTest.kt     # Contract tests for organiser endpoints
```

**Structure Decision**: This is a backend-only feature extending the existing partnership module. 
All code follows the established modular architecture pattern (domain → application → infrastructure).
No frontend changes required for MVP - organiser information will be available via API for future UI integration.

## Complexity Tracking

> **No violations - this section not applicable**

All constitution gates pass without requiring justification.
