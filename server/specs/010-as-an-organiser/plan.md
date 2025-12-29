
# Implementation Plan: OpenPlanner Integration for Agenda and Speaker Management

**Branch**: `010-as-an-organiser` | **Date**: November 11, 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/010-as-an-organiser/spec.md`

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
Enhance existing OpenPlanner agenda integration to include speaker-partnership linking functionality. The feature extends the current agenda import workflow (already implemented in `agenda` package) with a new public endpoint allowing users with active partnerships to attach speakers to their partnerships for organizer visibility. Implementation leverages existing OpenPlanner gateway and database tables while adding new partnership repository and API endpoints.

## Technical Context
**Language/Version**: Kotlin 1.9, JVM 21  
**Primary Dependencies**: Ktor 2.x, Exposed ORM, Koin DI  
**Storage**: PostgreSQL with existing SessionsTable and SpeakersTable  
**Testing**: JUnit 5, HTTP route integration tests with H2 in-memory DB  
**Target Platform**: Linux server, Docker containers  
**Project Type**: Backend web service - server module only  
**Performance Goals**: <2s response time for agenda import, <500ms for speaker attachment  
**Constraints**: Leverage existing agenda package, no OpenPlanner calls outside agenda workflow  
**Scale/Scope**: Extend existing 13 domain modules with new partnership repository, ~5 new API endpoints

**Implementation Context**: 
- OpenPlanner integration already exists in `server/application/src/main/kotlin/fr/devlille/partners/connect/agenda`
- Existing endpoint: `POST /orgs/{orgSlug}/events/{eventSlug}/agenda` for agenda import
- New endpoint needed: `GET /orgs/{orgSlug}/events/{eventSlug}/agenda` for agenda retrieval
- New public endpoint: `POST /partnerships/{partnershipId}/speakers/{speakerId}` for speaker attachment
- New partnership repository required for speaker-partnership associations
- Error handling: OpenPlanner failures should not impact database state

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: ✅ PASS - Feature will follow ktlint/detekt standards with comprehensive KDoc documentation for new APIs and maintain 80% test coverage through HTTP route integration tests.

**Testing Strategy**: ✅ PASS - Implementation includes contract tests for all new API endpoints using mock factories, integration tests for partnership-speaker linking workflows, and leverages existing H2 database test infrastructure.

**Clean Architecture**: ✅ PASS - Feature respects domain module boundaries by creating new partnership repository without circular dependencies, extends existing agenda module capabilities, and maintains interface segregation. No schema changes required (existing tables sufficient).

**API Consistency**: ✅ PASS - New REST endpoints follow established partnership API patterns, include OpenAPI documentation with JSON schema validation, and meet <2s response time requirements by leveraging existing database indexes.

**Performance & Observability**: ✅ PASS - Agenda import leverages existing optimized database queries, new speaker attachment operations use indexed foreign key lookups, includes structured logging for audit trails, and provides metrics for speaker attachment success rates.

**POST-DESIGN CONSTITUTION CHECK**: ✅ PASS  
- Database design uses existing Exposed patterns with proper foreign keys and constraints
- API endpoints follow established REST patterns with consistent error handling
- New domain repository maintains clean architecture boundaries without circular dependencies  
- JSON schema validation pattern matches constitutional requirements for all endpoints
- Error handling follows StatusPages plugin pattern without manual try-catch blocks

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
server/application/src/main/kotlin/fr/devlille/partners/connect/
├── agenda/                                    # [EXISTING] OpenPlanner integration
│   ├── domain/AgendaRepository.kt            # [EXISTING] Repository interface  
│   ├── application/AgendaRepositoryExposed.kt # [EXISTING] Implementation
│   └── infrastructure/
│       ├── gateways/OpenPlannerAgendaGateway.kt # [EXISTING] OpenPlanner API client
│       └── db/                               # [EXISTING] Sessions & Speakers tables
├── events/infrastructure/api/
│   └── EventAgendaRoutes.kt                  # [ENHANCE] Add GET endpoint for agenda retrieval
├── partnership/
│   ├── domain/                               # [NEW] Domain interfaces
│   │   └── PartnershipSpeakerRepository.kt  # [NEW] Speaker attachment repository (attach/detach only)
│   ├── application/                          # [NEW] Repository implementations  
│   │   └── PartnershipSpeakerRepositoryExposed.kt # [NEW] Exposed implementation
│   └── infrastructure/api/                   # [NEW] Public API endpoints
│       └── PartnershipSpeakerRoutes.kt       # [NEW] Speaker attachment endpoints (POST attach, DELETE detach)
└── internal/infrastructure/api/              # [EXISTING] Exception handling
    └── [existing exception types]            # [LEVERAGE] For error responses

server/application/src/test/kotlin/fr/devlille/partners/connect/
├── agenda/infrastructure/api/
│   └── EventAgendaRoutesTest.kt              # [ENHANCE] Add error scenarios
├── partnership/infrastructure/api/
│   └── PartnershipSpeakerRoutesTest.kt       # [NEW] Contract tests
└── [test support]/
    └── [mock factories]                      # [LEVERAGE] Existing + new speaker mocks

server/application/src/main/resources/
├── schemas/                                  # [NEW] JSON schemas for validation
│   ├── speaker_partnership_response.schema.json  # [NEW] Speaker attachment response
│   ├── partnership_detail.schema.json       # [ENHANCE] Enhanced partnership details with speakers
│   ├── agenda_response.schema.json          # [NEW] Complete agenda response with sessions and speakers
│   ├── session.schema.json                  # [NEW] Individual session schema
│   └── speaker.schema.json                  # [NEW] Individual speaker schema
└── openapi/openapi.yaml                     # [ENHANCE] Document new endpoints with component references

specs/010-as-an-organiser/contracts/          # [NEW] Contract specifications for implementation
├── api-contracts.md                          # [NEW] API documentation
├── speaker_partnership_response.schema.json  # [NEW] Speaker attachment response schema  
├── partnership_detail.schema.json           # [NEW] Enhanced partnership details schema
├── agenda_response.schema.json              # [NEW] Complete agenda response schema
├── session.schema.json                      # [NEW] Session schema for agenda response
└── speaker.schema.json                      # [NEW] Speaker schema for agenda response
```

**Structure Decision**: Extend existing server-only architecture with new partnership domain repository for speaker attachments (attach/detach operations only) while leveraging existing agenda package for OpenPlanner integration. Enhanced partnership details will automatically include speakers. No frontend changes needed as this is primarily backend API enhancement.

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
   - Assert request/response schemas (NOT business logic)
   - Use existing mock factory functions or plan new ones for test data
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

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base structure
- Generate tasks from Phase 1 design docs (data-model.md, contracts/, quickstart.md)
- Create database schema tasks for SpeakerPartnershipTable and SpeakerPartnershipEntity
- Generate JSON schema files: agenda response, session, speaker, speaker attachment response, enhanced partnership details
- Create contract test tasks using existing mock factories (mockSpeaker, mockPartnership, mockSession)
- Generate domain repository interface and Exposed implementation tasks (attach/detach operations only)
- Add agenda retrieval endpoint to existing EventAgendaRoutes (GET /orgs/{orgSlug}/events/{eventSlug}/agenda)
- Create new partnership API routes with minimal endpoint surface (POST attach, DELETE detach only)
- Enhance existing partnership details to automatically include speakers (no separate list endpoint)
- Enhance existing agenda routes with improved error handling for OpenPlanner failures

**Ordering Strategy**:
1. **Schema/Database Tasks** [P]: Create table and entity classes (can run in parallel)
2. **Contract Definition Tasks** [P]: JSON schemas and mock factory updates (parallel)  
3. **Domain Layer Tasks**: Repository interface and implementation (sequential)
4. **Contract Test Tasks** [P]: API endpoint contract tests using mock factories (parallel)
5. **API Implementation Tasks**: Route handlers with schema validation (sequential after domain)
6. **Integration Test Tasks**: End-to-end workflow tests (after API implementation)
7. **Enhancement Tasks**: Improve existing agenda error handling (parallel with new features)
8. **Documentation Tasks**: Update OpenAPI specification with new endpoints

**Mock Factory Extensions**:
- Extend existing `mockSpeaker()` to support OpenPlanner external IDs
- Create `mockSpeakerPartnership()` factory for association testing
- Leverage existing `mockPartnership()` with APPROVED status for eligibility testing
- Add `mockPartnershipSpeakersResponse()` for contract test assertions

**Constitutional Compliance Tasks**:
- KDoc documentation for all public APIs (embedded in implementation tasks)
- ktlint/detekt compliance verification (embedded in quality gates)
- Route-based integration tests achieving 80% coverage (dedicated test tasks)
- OpenAPI documentation updates with schema component references following `$ref: "#/components/schemas/ComponentName"` pattern
- JSON schema files follow snake_case naming: `speaker_partnership_response.schema.json`, `partnership_detail.schema.json`

**Error Handling Integration**:
- Update agenda routes to use transaction rollback on OpenPlanner failures
- Implement proper StatusPages exception mapping for new partnership endpoints
- Add structured logging for speaker attachment audit trail
- Create metrics collection for partnership-speaker operation success rates

**Estimated Output**: 20-25 numbered, ordered tasks in tasks.md with clear dependencies and parallel execution markers [P]

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

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
- [x] Phase 0: Research complete (/plan command) - research.md created
- [x] Phase 1: Design complete (/plan command) - data-model.md, contracts/, quickstart.md, agent context updated
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
