
# Implementation Plan: Partnership Validation with Customizable Package Details

**Branch**: `006-as-an-organiser` | **Date**: 2025-10-26 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-as-an-organiser/spec.md`

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
Allow event organizers to customize package details (ticket count, job offer count, booth size) when validating partnerships. The system replaces the boolean `withBooth` property in sponsoring packs with a `boothSize` string field, and adds three new validated fields to partnerships that default to pack values but can be overridden at validation time. Booth size validation ensures cross-pack consistency by requiring any override value to exist in at least one pack for the event. This prepares for future floor plan booth location selection based on validated booth sizes.

## Technical Context
**Language/Version**: Kotlin 1.9.x with JVM 21 (Amazon Corretto)  
**Primary Dependencies**: Ktor 3.x, Exposed ORM, Gradle 8.13+  
**Storage**: PostgreSQL (production), H2 in-memory (tests)  
**Testing**: Kotlin Test with HTTP route integration tests (95+ existing tests)  
**Target Platform**: JVM server application (containerized with Docker)  
**Project Type**: Backend API (Ktor server)  
**Performance Goals**: <2 seconds response time for validation endpoints, database optimized with indexing  
**Constraints**: Zero ktlint/detekt violations, 80% test coverage minimum, backward compatible migrations  
**Scale/Scope**: 13 domain modules, server-only changes, existing partnerships must remain functional

**Implementation Details** (from user):
- Replace `withBooth` boolean in `SponsoringPacksTable` with `boothSize` nullable string
- Update `SponsoringPackEntity`, `PackRepositoryExposed`, and `CreateSponsoringPack` model
- Add `validatedNbTickets`, `validatedNbJobOffers`, `validatedBoothSize` columns to `PartnershipsTable`
- Modify validation endpoint in `PartnershipRoutes` to accept new optional request body
- Implement validation logic in `PartnershipRepositoryExposed`:
  - Default values from pack if not provided
  - Job offers required
  - Booth size cross-pack validation
  - Agreement signature check for re-validation

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: ✅ PASS - All new Kotlin code will follow ktlint formatting, pass detekt static analysis, include KDoc documentation for public APIs and domain interfaces. Test coverage minimum 80% achieved via HTTP route integration tests.

**Testing Strategy**: ✅ PASS - Feature includes HTTP route integration tests for validation endpoint covering:
- Default value scenarios (tickets, booth size from pack)
- Override scenarios (custom values with validation)
- Error cases (invalid booth size, negative values, missing job offers)
- Re-validation scenarios (before/after agreement signature)
- Legacy partnership handling (null values)
- Uses H2 in-memory database for test isolation

**Clean Architecture**: ✅ PASS - Changes respect existing domain boundaries:
- `sponsoring/` module: Pack table, entity, repository modifications (booth size)
- `partnership/` module: Partnership table, entity, repository, routes (validated fields)
- No circular dependencies introduced
- Database migration maintains backward compatibility (nullable columns)
- Repository pattern preserved (no cross-repository dependencies)

**API Consistency**: ✅ PASS - Validation endpoint follows established patterns:
- POST `/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}/validate`
- Uses `AuthorizedOrganisationPlugin` for permissions (no manual checks)
- Request body: Optional JSON with `nbTickets`, `nbJobOffers` (required), `boothSize`
- Response: Standard `IdentifierSchema` with partnership ID
- Error responses via StatusPages (ForbiddenException, ConflictException)
- OpenAPI documentation updates required in `openapi.yaml`
- Response time <2 seconds (simple DB operations with indexed queries)

**Performance & Observability**: ✅ PASS - Database optimizations:
- Indexed foreign keys on partnerships table (`eventId`, `selectedPackId`)
- Booth size validation uses single query: `SponsoringPackEntity.find { packTable.eventId eq eventId and packTable.boothSize eq size }`
- Structured logging preserved via Ktor's existing logger
- Transaction scoping follows Exposed patterns
- No N+1 query issues (single partnership lookup, single pack validation query)

## Project Structure

### Documentation (this feature)
```
specs/[###-feature]/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
server/
├── application/src/main/kotlin/fr/devlille/partners/connect/
│   ├── sponsoring/
│   │   ├── domain/CreateSponsoringPack.kt [MODIFY - add boothSize, remove withBooth]
│   │   ├── application/PackRepositoryExposed.kt [MODIFY - handle boothSize in create/update]
│   │   └── infrastructure/db/
│   │       ├── SponsoringPacksTable.kt [MODIFY - replace withBooth column with boothSize]
│   │       └── SponsoringPackEntity.kt [MODIFY - update entity property]
│   ├── partnership/
│   │   ├── domain/
│   │   │   ├── PartnershipRepository.kt [MODIFY - add validateWithDetails method]
│   │   │   └── ValidatePartnershipRequest.kt [NEW - request model]
│   │   ├── application/PartnershipRepositoryExposed.kt [MODIFY - implement validation logic]
│   │   └── infrastructure/
│   │       ├── db/
│   │       │   ├── PartnershipsTable.kt [MODIFY - add validated_* columns]
│   │       │   └── PartnershipEntity.kt [MODIFY - add entity properties]
│   │       └── api/PartnershipRoutes.kt [MODIFY - accept request body in validate endpoint]
│   └── tickets/
│       └── application/TicketRepositoryExposed.kt [MODIFY - use validatedNbTickets]
├── application/src/main/resources/
│   ├── openapi/openapi.yaml [MODIFY - update validate endpoint schema]
│   └── schemas/validate-partnership.json [NEW - request body schema]
└── application/src/test/kotlin/fr/devlille/partners/connect/
    ├── sponsoring/SponsoringPackCrudTest.kt [MODIFY - update tests for boothSize]
    └── partnership/PartnershipValidationRoutesTest.kt [NEW - HTTP integration tests]
```

**Structure Decision**: Server-only changes to the Kotlin/Ktor backend. All modifications follow existing domain module structure: `sponsoring/` for pack changes, `partnership/` for validation logic, maintaining clean separation of concerns. No frontend changes required.

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - For each NEEDS CLARIFICATION → research task
   - For each dependency → best practices task
   - For each integration → patterns task

2. **Generate and dispatch research agents**:
   ```
   For each unknown in Technical Context:
     Task: "Research {unknown} for {feature context}"
   For each technology choice:
     Task: "Find best practices for {tech} in {domain}"
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]

**Output**: research.md with all NEEDS CLARIFICATION resolved

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Entity name, fields, relationships
   - Validation rules from requirements
   - State transitions if applicable

2. **Generate API contracts** from functional requirements:
   - For each user action → endpoint
   - Use standard REST/GraphQL patterns
   - Output OpenAPI/GraphQL schema to `/contracts/`

3. **Generate contract tests** from contracts:
   - One test file per endpoint
   - Assert request/response schemas
   - Tests must fail (no implementation yet)

4. **Extract test scenarios** from user stories:
   - Each story → integration test scenario
   - Quickstart test = story validation steps

5. **Update agent file incrementally** (O(1) operation):
   - Run `.specify/scripts/bash/update-agent-context.sh copilot`
     **IMPORTANT**: Execute it exactly as specified above. Do not add or remove any arguments.
   - If exists: Add only NEW tech from current plan
   - Preserve manual additions between markers
   - Update recent changes (keep last 3)
   - Keep under 150 lines for token efficiency
   - Output to repository root

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, agent-specific file

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

### Task Generation Strategy

**Input Documents**:
1. `contracts/validate-partnership-request.json` - Request body schema with 3 fields
2. `contracts/validate-partnership-response.json` - Response schema with validated_* fields
3. `contracts/openapi-endpoint.yaml` - Full endpoint specification with error cases
4. `data-model.md` - Database schema changes, entity models, validation rules
5. `quickstart.md` - 15 test scenarios with expected results

**Task Generation Rules**:

**Database Layer** (5-7 tasks):
1. Migration task: Add `boothSize` column to `sponsoring_packs` table, remove `with_booth`
2. Migration task: Add `validated_nb_tickets`, `validated_nb_job_offers`, `validated_booth_size` to `partnerships` table
3. Entity update task: Modify `SponsoringPackEntity` [P]
4. Entity update task: Modify `PartnershipEntity` [P]
5. Entity test task: Update `SponsoringPackCrudTest` for booth size

**Domain Model Layer** (3-4 tasks):
6. Create `ValidatePartnershipRequest` data class with validation [P]
7. Update `CreateSponsoringPack` model (remove withBooth, add boothSize) [P]
8. Contract test: Validate request schema against JSON schema [P]

**Repository Layer** (4-5 tasks):
9. Implement booth size cross-pack validation query in `PackRepositoryExposed` [P]
10. Update `PackRepositoryExposed.create()` and `update()` for boothSize
11. Implement `PartnershipRepositoryExposed.validateWithDetails()`:
    - Accept optional request body
    - Default to pack values when not provided
    - Query pack for defaults
    - Cross-pack booth size validation
    - Agreement signature check
12. Update `TicketRepositoryExposed` to use `validatedNbTickets` instead of pack defaults

**API Layer** (2-3 tasks):
13. Update `PartnershipRoutes.validate()` endpoint:
    - Accept optional `ValidatePartnershipRequest` body
    - Deserialize request with validation
    - Call repository with request
    - Handle exceptions (ForbiddenException, ConflictException)
14. Update OpenAPI spec in `openapi.yaml` with new endpoint contract

**Integration Tests** (8-10 tasks based on quickstart scenarios):
15. Test: Validate with pack defaults (Scenario 1) [P]
16. Test: Validate with custom ticket count (Scenario 2) [P]
17. Test: Validate with custom booth size (Scenario 3) [P]
18. Test: Reject invalid booth size (Scenario 4) [P]
19. Test: Validate with zero tickets (Scenario 5) [P]
20. Test: Re-validate before signature (Scenario 6) [P]
21. Test: Block re-validate after signature (Scenario 7) [P]
22. Test: Reject negative values (Scenario 8) [P]
23. Test: Reject missing job offers (Scenario 9) [P]
24. Test: Legacy partnership handling (Scenario 12) [P]

### Ordering Strategy

**TDD Ordering** (Tests Before Implementation):
- Database migration tasks FIRST (foundation)
- Entity test tasks BEFORE entity implementation tasks
- Contract test tasks in parallel with domain model creation [P]
- Integration test tasks BEFORE repository implementation
- All 10 integration tests created together [P], then implementation

**Dependency Ordering** (Bottom-Up):
```
Phase A: Database Schema (tasks 1-2)
  ↓
Phase B: Entity Models (tasks 3-5) [Parallel]
  ↓
Phase C: Domain Models + Contract Tests (tasks 6-8) [Parallel]
  ↓
Phase D: Repository Logic (tasks 9-12) [Sequential - shared logic]
  ↓
Phase E: API Layer (tasks 13-14) [Sequential]
  ↓
Phase F: Integration Tests (tasks 15-24) [Parallel - independent scenarios]
```

**Parallelization** (marked [P]):
- Entity updates: `SponsoringPackEntity` and `PartnershipEntity` independent
- Domain models: `ValidatePartnershipRequest` and `CreateSponsoringPack` independent
- Contract tests: Schema validation tests independent of entity tests
- Integration tests: All 10 HTTP route tests can run in parallel (isolated H2 databases)

**Task Template Format**:
```markdown
### Task [##]: [Action] [Component] [for Feature Context]

**Type**: [Migration|Model|Repository|API|Test]
**Estimated Effort**: [S|M|L] (S=<30min, M=30-60min, L=>60min)
**Parallel**: [Yes|No] - Can execute independently of adjacent tasks
**Dependencies**: Task [##], Task [##] (must complete first)

**Objective**: [One-sentence goal]

**Files to Modify**:
- `path/to/file.kt` [CREATE|MODIFY]

**Implementation Steps**:
1. [Step with code snippet/reference]
2. [Step with expected behavior]

**Validation**:
- [ ] Tests pass: `./gradlew test --tests ClassName`
- [ ] Linting passes: `./gradlew ktlintCheck`
- [ ] No regression: Run related tests
```

**Estimated Total Output**: 24 numbered, ordered tasks in `tasks.md`

**Task Complexity Breakdown**:
- Small (S): 12 tasks (entity updates, contract tests, simple tests)
- Medium (M): 9 tasks (repository logic, API updates, complex tests)
- Large (L): 3 tasks (database migrations, cross-pack validation)

**Estimated Implementation Time**: 14-18 hours (assuming all S=0.5h, M=0.75h, L=1.5h)

**IMPORTANT**: This phase is executed by the `/tasks` command, NOT by `/plan`. The above is a PLAN for task generation, not the actual tasks.md file.

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |


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
- [x] Complexity deviations documented (none required)

**Generated Artifacts**:
- [x] `research.md` - 6 technical decisions documented
- [x] `data-model.md` - Database schema, entities, domain models, validation rules
- [x] `contracts/validate-partnership-request.json` - Request body JSON schema
- [x] `contracts/validate-partnership-response.json` - Response JSON schema
- [x] `contracts/openapi-endpoint.yaml` - Complete OpenAPI 3.1.0 endpoint specification
- [x] `quickstart.md` - 15 test scenarios with curl/Bruno/Kotlin examples
- [x] `.github/copilot-instructions.md` - Updated agent context

**Next Command**: `/tasks` to generate tasks.md from Phase 1 design artifacts

**Summary**:
The `/plan` command has successfully completed all phases within its scope. The feature is fully designed with:
- **Database changes**: 2 table modifications (SponsoringPacksTable, PartnershipsTable) documented in data-model.md
- **Domain models**: ValidatePartnershipRequest, updated CreateSponsoringPack documented with validation rules
- **API contract**: OpenAPI 3.1.0 specification with request/response schemas, error cases, and examples
- **Test scenarios**: 15 executable quickstart scenarios covering happy paths, edge cases, and error conditions
- **Implementation guidance**: 24 estimated tasks with TDD ordering, dependency graph, and parallelization strategy

All constitutional checks passed (zero violations). Ready for task generation via `/tasks` command.

**Branch Name**: `006-as-an-organiser`  
**Estimated Implementation**: 14-18 hours across 24 tasks

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
