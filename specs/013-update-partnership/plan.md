# Implementation Plan: Update Partnership Contact Information

**Branch**: `013-update-partnership` | **Date**: 2025-11-29 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/013-update-partnership/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Add a public PUT endpoint at `/events/{eventSlug}/partnerships/{partnershipId}` to allow partners to update their contact information (contact_name, contact_role, language, phone, emails) without authentication. The endpoint supports partial updates using JSON schema validation, persists changes to the PostgreSQL database via Exposed ORM, and returns the updated partnership data. Implementation follows existing partnership route patterns with schema-based request validation and proper HTTP status code handling.

## Technical Context

**Language/Version**: Kotlin 1.9.x with JVM 21 (Amazon Corretto)
**Primary Dependencies**: Ktor 2.x, Exposed ORM 0.41+, kotlinx.serialization, Koin (DI)
**Storage**: PostgreSQL database (H2 in-memory for tests)
**Testing**: Kotlin Test with contract tests (schema validation) + integration tests (HTTP routes)
**Target Platform**: JVM server application (Port 8080)
**Project Type**: Backend REST API service
**Performance Goals**: <2 seconds response time for update operations (per SC-003)
**Constraints**: Public endpoint with no authentication, schema-based validation required
**Scale/Scope**: Single endpoint with partial update support for 5 contact fields

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Code Quality Standards ✅
- ktlint and detekt compliance: PASS - will run `./gradlew ktlintCheck detekt` before commit
- Documentation: PASS - all new interfaces and functions will have KDoc
- Code review: PASS - standard PR process

### Testing Strategy ✅
- Contract tests: REQUIRED - create schema validation tests for update request/response
- Integration tests: REQUIRED - HTTP route tests for update operations with H2 database
- 80% coverage: TARGET - focus on update logic, validation, and error cases
- Mock factories: WILL USE existing `insertMockedPartnership()` pattern for test setup

### Clean Architecture ✅
- Repository separation: PASS - update function added to PartnershipRepository interface only
- No repository dependencies: PASS - repository returns updated Partnership, no cross-domain calls
- Route orchestration: PASS - route handler in publicPartnershipRoutes handles request/response

### API Consistency ✅
- JSON Schema validation: REQUIRED - create `update_partnership_request.schema.json`
- OpenAPI documentation: REQUIRED - add PATCH operation to openapi.yaml
- HTTP status codes: PASS - 200 OK, 400 Bad Request, 404 Not Found
- Public endpoint security: PASS - `security: - {}` in OpenAPI spec

### Database Standards ✅
- Exposed ORM: PASS - existing PartnershipsTable and PartnershipEntity will be used
- datetime() usage: N/A - no new datetime columns added
- Entity pattern: PASS - update existing PartnershipEntity properties via delegation

### Response Schema Standards ✅
- event_slug usage: PASS - partnership responses already use event_slug pattern
- Consistent identifiers: PASS - partnership_id (UUID) in response

### Authorization Pattern ✅
- Public endpoint: PASS - NO AuthorizedOrganisationPlugin, completely public access
- No manual auth: PASS - no authentication checks required

### Exception Handling ✅
- StatusPages pattern: PASS - throw NotFoundException for missing partnership/event
- Repository exceptions: PASS - PartnershipRepository.updateContactInfo throws NotFoundException
- No try-catch in routes: PASS - let StatusPages handle exception mapping

### Parameter Extraction ✅
- Extension functions: PASS - use `call.parameters.eventSlug` and `call.parameters.partnershipId`
- No manual validation: PASS - StringValues extensions handle missing parameters

### OpenAPI 3.1.0 Compliance ✅
- Security definition: REQUIRED - `security: - {}` for public endpoint
- operationId: REQUIRED - `updatePartnershipContactInfo`
- Schema compatibility: REQUIRED - no `nullable: true`, use union types
- Validation: REQUIRED - `npm run validate` must pass

**GATE STATUS**: ✅ PASS - All constitutional requirements satisfied

### Post-Phase 1 Re-evaluation ✅

**Code Quality Standards**: ✅ MAINTAINED
- JSON schema created with proper validation rules
- OpenAPI documentation complete with examples
- KDoc comments planned for all new interfaces/classes

**Testing Strategy**: ✅ MAINTAINED  
- Contract test structure defined in quickstart.md
- Integration test scenarios documented
- Schema validation tests cover all edge cases

**Clean Architecture**: ✅ MAINTAINED
- PartnershipRepository interface method signature designed
- No cross-repository dependencies introduced
- Route handler orchestrates request/response only

**API Consistency**: ✅ MAINTAINED
- JSON schema validation via `call.receive<T>(schema)` pattern
- OpenAPI 3.1.0 compliant specification
- Proper HTTP status codes (200, 400, 404)

**Database Standards**: ✅ MAINTAINED
- Emails column uses PostgreSQL array type
- Entity delegation pattern preserved
- No timestamp() usage (not needed for this feature)

**All Gates**: ✅ PASS - Ready for implementation (Phase 2)

## Project Structure

### Documentation (this feature)

```text
specs/013-update-partnership/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output - validation and patterns research
├── data-model.md        # Phase 1 output - partnership entity updates
├── quickstart.md        # Phase 1 output - testing and validation guide
├── contracts/           # Phase 1 output - OpenAPI specs and JSON schemas
│   ├── openapi-patch.yaml
│   └── update_partnership_request.schema.json
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/
├── domain/
│   ├── PartnershipRepository.kt         # ADD: updateContactInfo() interface method
│   └── UpdatePartnershipContactInfo.kt  # NEW: request DTO with validation rules
├── application/
│   └── PartnershipRepositoryExposed.kt  # ADD: updateContactInfo() implementation
└── infrastructure/
    └── api/
        └── PartnershipRoutes.kt         # ADD: PATCH endpoint in publicPartnershipRoutes()

server/application/src/main/resources/
├── openapi/
│   └── openapi.yaml                     # ADD: PATCH /events/{eventSlug}/partnerships/{partnershipId}
└── schemas/
    └── update_partnership_request.schema.json  # NEW: JSON schema for request validation

server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/
├── infrastructure/
│   └── api/
│       └── PartnershipContactInfoUpdateContractTest.kt  # NEW: contract tests
│       └── PartnershipContactInfoUpdateIntegrationTest.kt  # NEW: integration tests
```

**Structure Decision**: Server-only feature following existing partnership module structure. Uses Kotlin domain/application/infrastructure layers with Exposed ORM for database operations. Tests follow contract + integration test pattern per constitution.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations - all constitutional requirements are satisfied for this feature.

---

## Phase 0: Research (COMPLETED ✅)

**Status**: All unknowns resolved  
**Output**: [research.md](./research.md)

### Key Decisions

1. **Email Validation**: JSON schema with `format: email` (RFC 5322)
2. **Language Codes**: ISO 639-1 enum validation in schema
3. **Partial Updates**: Nullable DTO fields with repository-level filtering
4. **Email Storage**: New PostgreSQL array column in partnerships table
5. **Phone Validation**: Free-form text with maxLength: 30
6. **Security**: Completely public endpoint (no authentication)

### Research Artifacts
- ✅ Validation patterns documented
- ✅ Exposed ORM update pattern defined
- ✅ Email storage strategy decided
- ✅ Security considerations documented
- ✅ Implementation risks identified

---

## Phase 1: Design & Contracts (COMPLETED ✅)

**Status**: All design artifacts generated  
**Outputs**: 
- [data-model.md](./data-model.md) - Entity and DTO structures
- [contracts/](./contracts/) - JSON schema and OpenAPI spec
- [quickstart.md](./quickstart.md) - Testing guide

### Artifacts Created

**Data Model**:
- ✅ UpdatePartnershipContactInfo DTO defined
- ✅ Partnership entity changes documented
- ✅ Database migration strategy defined
- ✅ Validation rules specified

**API Contracts**:
- ✅ JSON schema: `update_partnership_request.schema.json`
- ✅ OpenAPI PUT operation: `openapi-put.yaml`
- ✅ Request/response examples documented
- ✅ Error response scenarios defined

**Testing Guide**:
- ✅ Quickstart curl commands for all scenarios
- ✅ Contract test requirements listed
- ✅ Integration test scenarios documented
- ✅ Validation checklist provided

**Agent Context**:
- ✅ GitHub Copilot instructions updated with Kotlin/Ktor context

### Design Validation

**Constitution Re-check**: ✅ PASS
- All patterns follow existing codebase conventions
- Schema-based validation per constitution
- No architectural violations introduced

---

## Phase 2: Implementation Tasks (NEXT STEP)

**Status**: Ready for `/speckit.tasks` command  
**Command**: Run `/speckit.tasks` to generate task breakdown

**Expected Tasks**:
1. Database migration for emails column
2. Create UpdatePartnershipContactInfo DTO
3. Add updateContactInfo() to PartnershipRepository interface
4. Implement updateContactInfo() in PartnershipRepositoryExposed
5. Add PUT endpoint to publicPartnershipRoutes
6. Copy JSON schema to server resources
7. Update OpenAPI specification
8. Write contract tests
9. Write integration tests
10. Validate with ktlint/detekt

---

## Summary & Next Steps

### Completed Work (Phases 0-1)

✅ **Research Phase**: All technical unknowns resolved, decisions documented  
✅ **Design Phase**: Complete data model, API contracts, and testing guide  
✅ **Constitution Check**: All requirements satisfied, no violations  
✅ **Agent Context**: Updated with feature-specific technology stack

### Ready for Implementation

- **Branch**: `013-update-partnership` ✅ Created and checked out
- **Specification**: Complete with all clarifications resolved
- **Plan**: Technical approach documented and validated
- **Contracts**: JSON schema and OpenAPI spec ready to integrate
- **Tests**: Contract and integration test scenarios defined

### Next Command

```bash
# Generate implementation task breakdown
/speckit.tasks
```

This will create `tasks.md` with specific implementation steps, test cases, and acceptance criteria for the development phase.
