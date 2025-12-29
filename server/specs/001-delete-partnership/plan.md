# Implementation Plan: Delete Unvalidated Partnership

**Branch**: `001-delete-partnership` | **Date**: December 6, 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-delete-partnership/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Enable event organizers with edit permissions to delete partnerships that haven't been finalized (both `validatedAt` and `declinedAt` are null). The feature adds a DELETE endpoint to the partnership API, enforces permission checks via the existing `AuthorizedOrganisationPlugin`, validates partnership state before deletion, and performs a hard delete with no audit trail. The implementation follows the project's established patterns for route handling, repository interfaces, schema validation, and OpenAPI documentation.

## Technical Context

**Language/Version**: Kotlin 1.9.x with JVM 21 (Amazon Corretto)  
**Primary Dependencies**: Ktor 2.x, Exposed ORM 0.41+, kotlinx.serialization, Koin (DI)  
**Storage**: PostgreSQL database (H2 in-memory for tests)  
**Testing**: Contract tests (schema validation), Integration tests (HTTP routes), 95+ existing tests  
**Target Platform**: JVM server (Linux/Docker containers)
**Project Type**: Web application (Kotlin backend + Nuxt.js frontend)  
**Performance Goals**: <2 seconds for standard operations, <5 seconds for deletion  
**Constraints**: <200ms API response time target, existing authorization patterns (AuthorizedOrganisationPlugin)  
**Scale/Scope**: Partnership management system for developer events, ~200 files codebase, 14 domain modules

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Code Quality Standards
- **ktlint + detekt**: Implementation will follow existing patterns, violations checked in CI
- **Documentation**: Repository interface method will include KDoc, route implementation follows existing patterns
- **Kotlin idioms**: Using standard Exposed ORM patterns, Ktor route DSL, Koin DI

### ✅ Comprehensive Testing Strategy
- **Contract tests**: Will include schema validation tests for DELETE endpoint (TDD approach)
- **Integration tests**: HTTP route test for delete operation with permission checks
- **Coverage target**: Minimum 80% for new delete functionality
- **Mock factories**: Will use existing `insertMockedPartnership()`, `insertMockedEvent()`, `insertMockedCompany()`

### ✅ Clean Modular Architecture  
- **Repository pattern**: Adding `delete(partnershipId: UUID)` to existing `PartnershipRepository` interface
- **No cross-repository dependencies**: Repository implementation uses only Exposed entities
- **Route orchestration**: Permission checks handled by `AuthorizedOrganisationPlugin`, no notification needed for deletion

### ✅ API Consistency & User Experience
- **REST conventions**: DELETE `/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}`
- **HTTP status codes**: 204 No Content (success), 403 Forbidden (no permission), 404 Not Found (not exists), 409 Conflict (already finalized)
- **OpenAPI documentation**: Will update `openapi.yaml` with complete DELETE operation spec
- **JSON Schema validation**: No request body needed for DELETE, response is 204 No Content

### ✅ Performance & Observability
- **Database optimization**: Simple DELETE query with WHERE clause on primary key and timestamp null checks
- **No performance testing**: Feature spec focuses on functional validation only (per constitution)

### ⚠️ Additional Constitution Compliance
- **Authorization Pattern**: Will use `AuthorizedOrganisationPlugin` for org-level permission enforcement (no manual checks)
- **Exception Handling**: Repository will throw `NotFoundException` (404), `ConflictException` (409) - StatusPages handles HTTP mapping
- **Parameter Extraction**: Will use `call.parameters.partnershipId` extension (automatic validation)
- **OpenAPI 3.1.0**: Will include security definition, operationId, summary, and proper response schemas

**GATE RESULT (Initial)**: ✅ PASS - All constitutional requirements can be met with proposed implementation approach

---

**POST-PHASE 1 RE-CHECK**:

### ✅ Code Quality Standards (Re-validated)
- **Documentation**: Repository method signature includes comprehensive KDoc with exceptions
- **No violations**: Design uses existing patterns, no new quality issues introduced

### ✅ Comprehensive Testing Strategy (Re-validated)
- **Contract tests defined**: 6 test scenarios documented in `contracts/delete_partnership.yaml`
- **Integration tests planned**: Quickstart guide includes TDD workflow with test-first approach
- **Mock factories confirmed**: Using existing `insertMockedPartnership()` with state variations

### ✅ Clean Modular Architecture (Re-validated)
- **Single responsibility**: Repository handles delete logic, route handles HTTP, plugin handles auth
- **No new dependencies**: All functionality uses existing infrastructure
- **Clean separation**: Domain → Application → Infrastructure layers maintained

### ✅ API Consistency & User Experience (Re-validated)
- **OpenAPI contract complete**: Full specification with all error responses documented
- **REST compliance**: 204 No Content for successful DELETE (no response body)
- **Error responses**: Consistent use of existing Error schema component

### ✅ Performance & Observability (Re-validated)
- **Simple query**: Single `findById()` + state check + `delete()` - no complex joins
- **No new indexes needed**: Primary key index sufficient for delete operation

**GATE RESULT (Post-Phase 1)**: ✅ PASS - Design complies with all constitutional requirements

## Project Structure

### Documentation (this feature)

```text
specs/001-delete-partnership/
├── spec.md              # Feature specification (completed)
├── checklists/
│   └── requirements.md  # Specification quality checklist (completed)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (to be generated)
├── data-model.md        # Phase 1 output (to be generated)
├── quickstart.md        # Phase 1 output (to be generated)
├── contracts/           # Phase 1 output (to be generated)
│   └── delete_partnership.yaml  # OpenAPI operation spec
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
server/
├── application/src/
│   ├── main/kotlin/fr/devlille/partners/connect/
│   │   └── partnership/
│   │       ├── domain/
│   │       │   └── PartnershipRepository.kt          # ADD: delete(partnershipId) method
│   │       ├── application/
│   │       │   └── PartnershipRepositoryExposed.kt   # MODIFY: implement delete method
│   │       └── infrastructure/
│   │           └── api/
│   │               └── PartnershipRoutes.kt          # ADD: DELETE endpoint in orgsPartnershipRoutes()
│   ├── main/resources/
│   │   ├── openapi/
│   │   │   └── openapi.yaml                          # MODIFY: add DELETE operation
│   │   └── schemas/
│   │       └── (no new schemas needed for DELETE)
│   └── test/kotlin/fr/devlille/partners/connect/
│       └── partnership/
│           ├── contract/
│           │   └── PartnershipContractTest.kt        # ADD: contract tests for DELETE
│           └── integration/
│               └── PartnershipIntegrationTest.kt     # ADD: integration tests for delete
```

**Structure Decision**: This feature modifies existing partnership domain module following the established Clean Architecture pattern. No new domain modules are created. Changes are localized to:
1. Domain interface (PartnershipRepository.kt) - add delete method signature
2. Application layer (PartnershipRepositoryExposed.kt) - implement delete with Exposed ORM
3. Infrastructure API layer (PartnershipRoutes.kt) - add DELETE route under `/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}`
4. OpenAPI documentation (openapi.yaml) - document DELETE operation
5. Tests (contract + integration) - validate schema and business logic

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No constitutional violations - this feature follows all established patterns and quality standards.

## Phase 0: Research (Completed)

**Output**: `research.md`

All technical unknowns resolved:
- ✅ State validation pattern: Exposed ORM `isNull()` checks on timestamps
- ✅ Authorization approach: `AuthorizedOrganisationPlugin` (existing infrastructure)
- ✅ Error handling: StatusPages + domain exceptions (NotFoundException, ConflictException)
- ✅ OpenAPI documentation: DELETE operation with security definitions
- ✅ Testing strategy: Contract tests (schema) + Integration tests (business logic)

**Key Decisions**:
1. Use Exposed ORM for type-safe state validation
2. Leverage AuthorizedOrganisationPlugin for permission enforcement
3. Follow exception-to-HTTP pattern via StatusPages
4. Return HTTP 204 No Content on success (REST convention)
5. TDD approach with contract tests written first

## Phase 1: Design & Contracts (Completed)

**Outputs**: `data-model.md`, `contracts/delete_partnership.yaml`, `quickstart.md`

### Data Model
- No new entities required
- Operates on existing Partnership entity
- State rule: `validatedAt == null AND declinedAt == null` allows deletion
- Hard delete with no audit trail

### Contracts
- DELETE endpoint: `/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}`
- Success: 204 No Content (empty body)
- Errors: 401 Unauthorized, 404 Not Found, 409 Conflict
- 6 contract test scenarios defined

### Quickstart Guide
- 8-phase implementation workflow
- TDD approach (write tests first)
- Validation checklist included
- Common issues & solutions documented

### Agent Context Update
- Updated `.github/copilot-instructions.md` with technology stack
- Added: Kotlin 1.9.x, JVM 21, Ktor 2.x, Exposed ORM 0.41+
- Added: PostgreSQL (H2 for tests)
- No manual edits needed - automated via script

## Implementation Artifacts Generated

### Documentation
1. **plan.md** - This file (implementation plan)
2. **research.md** - Technical decisions and patterns
3. **data-model.md** - Entity model and validation rules
4. **contracts/delete_partnership.yaml** - OpenAPI contract specification
5. **quickstart.md** - Developer implementation guide

### Ready for Implementation
All planning complete. Next steps:

1. **Follow quickstart.md** for implementation
2. **Run `/speckit.tasks`** to generate task breakdown (Phase 2)
3. **Begin coding** following TDD approach

## Files to Modify (Implementation Phase)

### Domain Layer
- `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt`
  - Add: `delete(partnershipId: UUID)` method signature with KDoc

### Application Layer
- `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt`
  - Implement: `delete()` method with state validation and hard delete

### Infrastructure Layer
- `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt`
  - Add: DELETE endpoint in `orgsPartnershipRoutes()` function

### Documentation
- `server/application/src/main/resources/openapi/openapi.yaml`
  - Add: DELETE operation under partnerships path

### Tests
- `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/contract/PartnershipContractTest.kt`
  - Add: Contract tests for DELETE endpoint (6 scenarios)
- `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/integration/PartnershipIntegrationTest.kt`
  - Add: Integration tests for delete business logic

## Success Criteria

### Functional
- ✅ Organizers with edit permission can delete unvalidated partnerships
- ✅ Deletion blocked for finalized partnerships (validated or declined)
- ✅ Authorization enforced via AuthorizedOrganisationPlugin
- ✅ Returns HTTP 204 No Content on success
- ✅ Returns appropriate errors (401, 404, 409)

### Technical
- ✅ Zero ktlint/detekt violations
- ✅ All contract tests pass
- ✅ All integration tests pass
- ✅ >80% code coverage for new functionality
- ✅ OpenAPI validation passes (`npm run validate`)
- ✅ Constitutional compliance verified

## Timeline Estimate

- **Contract Tests**: 1-2 hours
- **Repository Implementation**: 30 minutes
- **Route Implementation**: 30 minutes
- **OpenAPI Documentation**: 30 minutes
- **Integration Tests**: 1-2 hours
- **Validation & Fixes**: 1 hour

**Total**: 4-6 hours

## Notes

- No new database migrations required
- No new JSON schemas needed (DELETE has no request body)
- No external service integrations (Slack, Mailjet, etc.)
- No frontend changes needed in this phase
- Simple feature with clear boundaries and existing patterns
