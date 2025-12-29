
# Implementation Plan: User Permission Revocation for Organisations

**Branch**: `005-as-an-organizer` | **Date**: 2025-10-24 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-as-an-organizer/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from file system structure or context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Fill the Constitution Check section based on the content of the constitution document.
4. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, agent-specific template file (e.g., `CLAUDE.md` for Claude Code, `.github/copilot-instructions.md` for GitHub Copilot, `GEMINI.md` for Gemini CLI, `QWEN.md` for Qwen Code or `AGENTS.md` for opencode).
7. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Implement a REST API endpoint (`POST /orgs/{orgSlug}/users/revoke`) that allows organizers with edit permissions to revoke user access from an organisation. The feature mirrors the existing grant permissions functionality but removes edit permissions instead of adding them. The implementation will add a new route handler, repository method, request model, and comprehensive test coverage following the established patterns in the codebase.

## Technical Context
**Language/Version**: Kotlin 1.9 / JVM 21  
**Primary Dependencies**: Ktor 3.0, Exposed ORM, Koin DI  
**Storage**: PostgreSQL (H2 in-memory for tests)  
**Testing**: Kotlin Test with Ktor test application  
**Target Platform**: Linux server (containerized)  
**Project Type**: Backend REST API (Ktor server)  
**Performance Goals**: <2 seconds response time per API standards  
**Constraints**: Must pass ktlint, detekt with zero violations; 80% test coverage minimum  
**Scale/Scope**: Single endpoint with 5-7 test scenarios covering all edge cases

**User-Provided Implementation Details**:
- Update `server/application/src/main/kotlin/fr/devlille/partners/connect/users/infrastructure/api/UserRoutes.kt` to add `/revoke` endpoint next to existing `/grant` endpoint
- Add `revokeUsers()` function to `server/application/src/main/kotlin/fr/devlille/partners/connect/users/domain/UserRepository.kt` interface
- Implement `revokeUsers()` in `server/application/src/main/kotlin/fr/devlille/partners/connect/users/application/UserRepositoryExposed.kt`
- Add necessary domain models in `server/application/src/main/kotlin/fr/devlille/partners/connect/users/domain` package
- Create single test class in `server/application/src/test/kotlin/fr/devlille/partners/connect/users` covering all specification test cases

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: ✅ PASS - All new code will follow ktlint/detekt rules, include KDoc documentation for the public API interface and route handler, and achieve 80%+ test coverage through comprehensive HTTP route integration tests.

**Testing Strategy**: ✅ PASS - Feature will include integration tests via HTTP route testing (following constitution principle of testing through routes, not repositories directly). Test coverage will include: successful revocation, authentication failures, authorization failures, self-revocation edge case, non-existent users (partial success), empty list handling, and idempotent operations. Uses H2 in-memory database for test isolation.

**Clean Architecture**: ✅ PASS - Feature respects existing domain module boundaries (users module). No new database schema changes required (uses existing OrganisationPermissionEntity). Follows established repository pattern with interface in domain/ and implementation in application/. Route handler in infrastructure/api/ orchestrates the operation following separation of concerns.

**API Consistency**: ✅ PASS - New endpoint follows REST conventions matching existing `/grant` endpoint pattern. Returns consistent error formats via Ktor StatusPages exception handling. Uses standard HTTP status codes (200 OK, 401 Unauthorized, 404 Not Found). Response includes success confirmation and list of non-existent emails for partial success scenarios per specification. Meets <2 second response time requirement (simple database delete operations).

**Performance & Observability**: ✅ PASS - Database operations use existing Exposed ORM patterns with proper connection pooling. Queries will use indexed columns (organisation_id, user_id in OrganisationPermissionEntity). Structured logging inherent through Ktor request logging. No complex queries or N+1 problems (batch delete operation in single transaction).

## Project Structure

### Documentation (this feature)
```
specs/005-as-an-organizer/
├── spec.md              # Feature specification (complete)
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
│   └── revoke-users.http
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
server/application/src/
├── main/kotlin/fr/devlille/partners/connect/users/
│   ├── domain/
│   │   ├── UserRepository.kt                    # UPDATE: Add revokeUsers() method
│   │   └── RevokeUsersResult.kt                 # NEW: Result model for partial success
│   ├── application/
│   │   └── UserRepositoryExposed.kt             # UPDATE: Implement revokeUsers()
│   └── infrastructure/
│       └── api/
│           ├── UserRoutes.kt                    # UPDATE: Add POST /revoke endpoint
│           └── RevokePermissionRequest.kt       # NEW: Request model
└── test/kotlin/fr/devlille/partners/connect/users/
    └── RevokePermissionRouteTest.kt             # NEW: Integration test class
```

**Structure Decision**: Single backend project (Option 1). This is a backend-only REST API feature within the existing Ktor server application. All changes are contained within the `users` domain module following the established modular architecture. No frontend, mobile, or additional projects required.

## Phase 0: Outline & Research
✅ **COMPLETE**

**Research Questions Addressed**:
1. Exposed ORM permission deletion pattern → Entity-level deletion chosen
2. Partial success response model → RevokeUsersResult data class
3. Self-revocation prevention logic → Database count query before deletion
4. Idempotency handling → Null-safe delete (no-op if already revoked)
5. Authorization pattern compliance → Use AuthorizedOrganisationPlugin per constitution
6. Request validation pattern → JSON schema validation matching grant endpoint

**Output**: research.md with all decisions documented and rationale provided

## Phase 1: Design & Contracts
✅ **COMPLETE**

**Deliverables Created**:
1. **data-model.md**: Complete entity relationships, validation rules, state transitions
2. **contracts/revoke-users.http**: Full HTTP contract with 10 test scenarios
3. **quickstart.md**: Step-by-step implementation guide with code examples
4. **New Models Designed**:
   - `RevokePermissionRequest`: API request payload
   - `RevokeUsersResult`: Domain result model with partial success support

**Design Decisions**:
- Reuse existing OrganisationPermissionEntity (no schema changes)
- Follow constitutional requirements (AuthorizedOrganisationPlugin, JSON schema validation)
- Mirror grant endpoint pattern with revocation semantics
- Handle non-existent users gracefully per FR-008a

**Agent Context Update**: Not applicable - this is a Copilot-based workflow

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
The /tasks command will load `.specify/templates/tasks-template.md` and generate ordered, concrete tasks based on Phase 1 artifacts. Task structure will follow this pattern:

**Contract Test Tasks** (Parallel - marked [P]):
- Task 1: Create RevokePermissionRouteTest test class skeleton [P]
- Task 2-10: Implement individual test scenarios (auth failures, permission checks, edge cases) [P]

**Model Creation Tasks** (Parallel - marked [P]):
- Task 11: Create RevokeUsersResult domain model [P]
- Task 12: Create RevokePermissionRequest API model [P]
- Task 13: Create JSON schema file [P]

**Repository Layer Tasks** (Sequential):
- Task 14: Add revokeUsers() method to UserRepository interface
- Task 15: Implement revokeUsers() in UserRepositoryExposed

**API Layer Tasks** (Sequential - depends on Task 14-15):
- Task 16: Register JSON schema in ApplicationCall.ext.kt
- Task 17: Add POST /revoke route handler in UserRoutes.kt

**Integration Tasks** (Sequential - depends on all above):
- Task 18: Run full build and fix any compilation errors
- Task 19: Run ktlint and detekt, fix violations
- Task 20: Execute all tests and verify 100% pass rate
- Task 21: Measure test coverage (target: 80%+ for new code)
- Task 22: Update OpenAPI documentation in openapi.yaml
- Task 23: Run OpenAPI validation (npm run validate)

**Ordering Strategy**:
1. **Tests first (TDD)**: Tasks 1-10 create failing tests that define expected behavior
2. **Models next**: Tasks 11-13 create data structures (parallel execution possible)
3. **Repository then API**: Tasks 14-17 implement business logic (API depends on repository)
4. **Integration last**: Tasks 18-23 validate complete feature

**Parallelization Markers**:
- [P] = Can execute in parallel with other [P] tasks
- No marker = Must execute sequentially after dependencies

**Estimated Task Count**: 23 tasks total
- 10 test implementation tasks (parallel)
- 3 model creation tasks (parallel)
- 2 repository tasks (sequential)
- 2 API tasks (sequential)
- 6 integration/validation tasks (sequential)

**Estimated Effort**: 
- Test tasks: 15-20 min each = 150-200 min
- Model tasks: 10 min each = 30 min
- Repository/API: 45-60 min combined
- Integration: 30-45 min
- **Total: 5.5-7 hours**

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*No constitutional violations identified - all requirements satisfied by existing infrastructure*

**Justification**: Not applicable - feature follows established patterns with zero deviations from constitutional requirements.


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented (N/A - no deviations)

---
*Based on Constitution v1.0.0 - See `.specify/memory/constitution.md`*
