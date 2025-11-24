# Implementation Plan: Synchronize Pack Options

**Branch**: `012-sync-pack-options` | **Date**: November 24, 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/012-sync-pack-options/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Modify the existing `POST /orgs/{orgSlug}/events/{eventSlug}/packs/{packId}/options` endpoint to perform complete synchronization of pack options instead of only adding new ones. The endpoint will accept a complete list of required and optional option IDs and ensure the pack's final state exactly matches the submitted configuration by:
- Removing options not in the submitted lists
- Adding new options from the submitted lists
- Updating requirement status (required ↔ optional) for existing options
- Performing all operations atomically within a database transaction

## Technical Context

**Language/Version**: Kotlin (JVM 21) with Ktor framework  
**Primary Dependencies**: Ktor 2.x, Exposed ORM, Koin (DI), kotlinx.serialization  
**Storage**: PostgreSQL database with Exposed ORM, H2 in-memory for tests  
**Testing**: Kotlin Test with contract tests (JSON schema validation) and integration tests (HTTP route testing)  
**Target Platform**: JVM server application (Linux/Docker deployment)  
**Project Type**: Web application - Kotlin/Ktor backend with REST API  
**Performance Goals**: 500ms response time for up to 50 options (per spec SC-003)  
**Constraints**: Atomic database transactions required, last-write-wins concurrency  
**Scale/Scope**: Single endpoint modification, impacts existing OptionRepository implementation

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Code Quality Standards
- ✅ **ktlint & detekt**: Will enforce zero violations via existing CI/CD pipeline
- ✅ **Documentation**: Repository interface and implementation changes will include KDoc
- ✅ **Test Coverage**: Will achieve 80%+ coverage via contract + integration tests

### Testing Strategy
- ✅ **Contract Tests Required**: New/modified contract tests for endpoint behavior (TDD approach)
- ✅ **Integration Tests Required**: HTTP route tests validating synchronization logic end-to-end
- ✅ **Repository Testing**: Tested implicitly through route tests (constitution requirement)
- ✅ **Mock Factories**: Will use existing `insertMockedSponsoringPack`, `insertMockedSponsoringOption` test utilities

### Clean Modular Architecture  
- ✅ **Repository Separation**: OptionRepository will NOT depend on other repositories (constitution compliance)
- ✅ **No Notification Logic**: Endpoint modification only - no cross-domain operations needed
- ✅ **Database Transactions**: Exposed transaction blocks ensure atomicity (existing pattern)

### API Consistency
- ✅ **JSON Schema Validation**: Existing `attach_options_to_pack.schema.json` already used, no changes needed
- ✅ **HTTP Status Codes**: Spec clarifications define 409/403/404 error codes matching existing patterns
- ✅ **Authorization Plugin**: Route already uses `AuthorizedOrganisationPlugin` (no manual permission checks)
- ✅ **Exception Handling**: Will use existing exception classes (ConflictException, ForbiddenException, NotFoundException)
- ✅ **Parameter Extraction**: Already uses `call.parameters.eventSlug`, `call.parameters.packId` extensions

### Database Schema
- ✅ **No Schema Changes**: Uses existing PackOptionsTable with pack/option/required columns
- ✅ **Exposed ORM Pattern**: Existing table/entity structure unchanged (UUIDTable, datetime, enumerationByName)

### OpenAPI Documentation
- ✅ **Schema Components**: Existing schema component already defined, no updates needed
- ✅ **Operation Documentation**: POST operation documentation exists, summary may need update
- ✅ **Response Schemas**: HTTP 409/403/404 error responses already documented in components

**GATE STATUS**: ✅ **PASSED** - All constitutional requirements satisfied. This is a behavior modification of an existing endpoint following established patterns.

### Post-Design Re-evaluation

**Re-checked**: November 24, 2025 after Phase 1 design completion

#### Design Artifacts Review

- ✅ **research.md**: Documents existing patterns, no new technical debt
- ✅ **data-model.md**: No schema changes, uses existing tables
- ✅ **contracts/**: API contract maintains backward compatibility
- ✅ **quickstart.md**: TDD approach with contract tests first

#### Constitutional Compliance Verification

**Code Quality Standards**:
- ✅ Implementation follows Exposed ORM patterns (transaction blocks, bulk operations)
- ✅ KDoc documentation required on modified `attachOptionsToPack()` method
- ✅ ktlint/detekt will be enforced via existing CI/CD

**Testing Strategy**:
- ✅ Contract tests defined (9 scenarios minimum)
- ✅ Integration tests planned for database state verification
- ✅ TDD approach documented in quickstart (tests before implementation)
- ✅ Uses existing mock factories (no new test infrastructure needed)

**Clean Architecture**:
- ✅ Repository layer only - no cross-domain dependencies
- ✅ No notification logic (endpoint is data operation only)
- ✅ Single transaction ensures atomicity
- ✅ Repository doesn't depend on other repositories

**API Consistency**:
- ✅ JSON schema validation via existing `attach_options_to_pack.schema.json`
- ✅ Exception handling via StatusPages (no try-catch in routes)
- ✅ Parameter extraction via existing extensions (`call.parameters.eventSlug`)
- ✅ Authorization via `AuthorizedOrganisationPlugin` (no manual checks)

**Database Standards**:
- ✅ No schema changes - uses existing UUIDTable structure
- ✅ Exposed ORM patterns maintained (transaction {}, bulk operations)
- ✅ datetime() columns (not timestamp()) per constitution

**OpenAPI Standards**:
- ✅ Operation documentation updated (summary, description)
- ✅ Existing schema component referenced (no duplication)
- ✅ npm run validate will verify compliance
- ✅ Error responses already defined in components

**FINAL GATE STATUS**: ✅ **PASSED** - Design artifacts demonstrate full constitutional compliance. Ready for Phase 2 (task decomposition).

## Project Structure

### Documentation (this feature)

```text
specs/012-sync-pack-options/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (minimal - pattern already established)
├── data-model.md        # Phase 1 output (documents existing schema)
├── quickstart.md        # Phase 1 output (test validation guide)
├── contracts/           # Phase 1 output (test scenarios)
│   └── sync_pack_options_contract.md
├── checklists/
│   └── requirements.md  # Already created during specification
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
server/application/src/
├── main/kotlin/fr/devlille/partners/connect/
│   └── sponsoring/
│       ├── domain/
│       │   ├── AttachOptionsToPack.kt              # Existing DTO (unchanged)
│       │   └── OptionRepository.kt                 # Interface (method signature unchanged)
│       ├── application/
│       │   └── OptionRepositoryExposed.kt          # MODIFIED: attachOptionsToPack() implementation
│       └── infrastructure/api/
│           └── SponsoringRoutes.kt                 # Existing route (unchanged - calls repository)
├── resources/
│   ├── schemas/
│   │   └── attach_options_to_pack.schema.json     # Existing schema (unchanged)
│   └── openapi/
│       └── openapi.yaml                            # MODIFIED: Update operation summary/description
└── test/kotlin/fr/devlille/partners/connect/
    └── sponsoring/
        └── SponsoringPackRoutesTest.kt             # MODIFIED: Update existing tests + add new scenarios
```

**Structure Decision**: This is an endpoint behavior modification within the existing `sponsoring` domain module. No new files needed - only modifications to:
1. `OptionRepositoryExposed.attachOptionsToPack()` - Replace add-only logic with sync logic
2. `SponsoringPackRoutesTest.kt` - Update contract tests for new behavior
3. `openapi.yaml` - Update operation documentation

The route handler, DTO, and schema remain unchanged as the API contract (request/response format) is identical.

## Complexity Tracking

**N/A** - All Constitution Check gates passed. No violations to justify.

---

## Planning Phase Complete

**Status**: ✅ All planning phases completed successfully  
**Date**: November 24, 2025

### Phase 0: Outline & Research ✅

**Deliverable**: [research.md](./research.md)

**Key Decisions**:
- Database transaction patterns using Exposed `transaction {}`
- Three-phase sync algorithm: delete → update → insert
- Last-write-wins concurrency strategy (no optimistic locking)
- Contract-first testing approach (TDD)
- Bulk operations for performance optimization

**Unknowns Resolved**: All implementation patterns established through existing codebase analysis.

---

### Phase 1: Design & Contracts ✅

**Deliverables**:
- [data-model.md](./data-model.md) - Existing database schema documented
- [contracts/sync_pack_options_contract.md](./contracts/sync_pack_options_contract.md) - API contract and test scenarios
- [quickstart.md](./quickstart.md) - Step-by-step implementation guide
- Agent context updated (GitHub Copilot instructions)

**Design Highlights**:
- No schema changes required - uses existing PackOptionsTable
- 9 contract test scenarios defined (3 success, 6 error cases)
- TDD workflow documented with detailed test examples
- OpenAPI documentation update requirements specified
- Backward compatible API - no breaking changes

**Constitution Re-check**: ✅ PASSED - All design artifacts comply with project standards

---

### Generated Artifacts Summary

| Artifact | Purpose | Lines | Status |
|----------|---------|-------|--------|
| research.md | Implementation patterns and decisions | ~250 | ✅ Complete |
| data-model.md | Database schema and relationships | ~300 | ✅ Complete |
| contracts/sync_pack_options_contract.md | API contract and test cases | ~650 | ✅ Complete |
| quickstart.md | Implementation step-by-step guide | ~550 | ✅ Complete |
| plan.md | This planning document | ~350 | ✅ Complete |

**Total Documentation**: ~2,100 lines of comprehensive planning and design documentation

---

### Next Steps

**Recommended Command**: `/speckit.tasks`

**What it does**:
- Generate task decomposition in `tasks.md`
- Break down implementation into atomic, testable tasks
- Create task dependency graph
- Provide task completion checklist

**Manual Alternative**: Follow [quickstart.md](./quickstart.md) step-by-step

**Implementation Readiness**:
- ✅ All technical unknowns resolved
- ✅ Database schema documented  
- ✅ API contract fully specified
- ✅ Test scenarios defined (TDD ready)
- ✅ Constitutional compliance verified
- ✅ Performance targets established
- ✅ Error handling patterns documented

**Estimated Implementation Time**: 4-6 hours (2 hours tests, 2 hours implementation, 1-2 hours documentation/validation)

---

### Key Reference Documents

**Before Starting Implementation**:
1. Review [spec.md](./spec.md) - Understand user requirements
2. Review [contracts/sync_pack_options_contract.md](./contracts/sync_pack_options_contract.md) - API contract
3. Follow [quickstart.md](./quickstart.md) - Step-by-step guide

**During Implementation**:
1. Reference [research.md](./research.md) - Implementation patterns
2. Reference [data-model.md](./data-model.md) - Database operations
3. Use constitution for standards compliance

**After Implementation**:
1. Run validation checklist from quickstart.md
2. Verify all success criteria from spec.md
3. Complete code quality checks (ktlint, detekt, tests)
