
# Implementation Plan: Public Partnership Information Endpoint

**Branch**: `008-create-a-new` | **Date**: 8 November 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/008-create-a-new/spec.md`

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
Create a new public GET endpoint at `/events/{eventSlug}/partnerships/{partnershipId}` that returns comprehensive partnership information including detailed company data, event information, and partnership process status with timestamps. The endpoint will be completely public (no authentication), return all data including private contact details, and use a nested JSON response structure with separate company, event, and partnership objects.

## Technical Context
**Language/Version**: Kotlin/JVM 21 with Ktor 2.x framework  
**Primary Dependencies**: Ktor Server, Exposed ORM, PostgreSQL driver, Kotlinx Serialization  
**Storage**: PostgreSQL database with existing partnership/company/event entities  
**Testing**: Kotlin Test with H2 in-memory database for integration tests  
**Target Platform**: JVM server application (Linux/Docker deployment)
**Project Type**: Web application (Kotlin backend + Nuxt.js frontend)  
**Performance Goals**: <2 seconds response time per constitution API standards  
**Constraints**: Public endpoint with no authentication, unlimited access allowed  
**Scale/Scope**: Single new GET endpoint, new repository method, response mappers, contract tests

**User-Provided Implementation Details**: 
- Endpoint declaration in `PartnershipRoutes.kt` within `publicPartnershipRoutes` function
- New `getByIdDetailed` method in `PartnershipRepository.kt` interface (keeping existing `getById`)
- Implementation in `PartnershipRepositoryExposed.kt` using existing Exposed entities
- Mappers in `partnership/application/mappers/` to reduce repository complexity
- Contract tests only (no integration tests) with injected test data during app configuration

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: ✅ PASS - All new Kotlin code will follow ktlint formatting, detekt static analysis, and include comprehensive KDoc documentation for public APIs. Contract tests will provide required coverage.

**Testing Strategy**: ✅ PASS - Feature will include contract tests focusing on API schema validation using existing mock factories (mockPartnership, mockCompany, mockEvent). Tests will validate request/response schemas and HTTP status codes but not business logic details.

**Clean Architecture**: ✅ PASS - New repository method respects domain module boundaries, implementation uses existing Exposed entities without circular dependencies. Mappers will be added to maintain separation of concerns between repository and presentation layers.

**API Consistency**: ✅ PASS - REST endpoint follows existing `/events/{eventSlug}/partnerships/{partnershipId}` pattern, will return consistent error formats via existing StatusPages plugin, and requires OpenAPI documentation updates to maintain consistency.

**Performance & Observability**: ✅ PASS - Database queries will leverage existing indexes on partnerships table, structured logging handled by existing Ktor configuration. No new performance-critical operations introduced beyond single-record lookup.

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
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->
```
# Option 2: Web application (selected - frontend + backend detected)
server/
├── application/src/main/kotlin/fr/devlille/partners/connect/
│   ├── partnership/
│   │   ├── domain/
│   │   │   ├── PartnershipRepository.kt           # Add getByIdDetailed method
│   │   │   ├── PartnershipDetail.kt               # New partnership detail model
│   │   │   └── DetailedPartnershipResponse.kt     # New response model (reuses Company, EventWithOrganisation)
│   │   ├── application/
│   │   │   ├── PartnershipRepositoryExposed.kt    # Implement getByIdDetailed  
│   │   │   └── mappers/
│   │   │       └── PartnershipEntityDetailMapper.kt  # Single mapper for partnership only
│   │   └── infrastructure/
│   │       └── api/
│   │           └── PartnershipRoutes.kt           # Add GET endpoint with 3-repository orchestration
│   ├── companies/domain/Company.kt                # REUSE existing model
│   ├── events/domain/EventWithOrganisation.kt     # REUSE existing model
│   └── tests/
│       └── partnership/
│           └── PartnershipDetailedGetRouteTest.kt         # Contract tests

documentation/
├── openapi/
│   └── openapi.yaml                                   # Update API docs  
└── schemas/
    └── DetailedPartnershipResponse.json               # New schema referencing existing Company/Event schemas
```

**Structure Decision**: Web application structure selected based on existing Kotlin/Ktor backend and Nuxt.js frontend. New endpoint implementation will extend existing partnership module while leveraging existing Company and EventWithOrganisation domain models from their respective repositories, improving SOLID compliance by avoiding duplication and maintaining single responsibility per repository.

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
- Generate tasks from Phase 1 design documents (data-model.md, contracts/, quickstart.md)
- Contract tests first: API endpoint gets dedicated contract test [P] using existing mock factories
- Domain models: Only `PartnershipDetail` and `DetailedPartnershipResponse` models need creation [P]
- Repository interface: New `getByIdDetailed` method in `PartnershipRepository` [P]
- Repository implementation: `PartnershipRepositoryExposed.getByIdDetailed` with partnership-only queries
- Single mapper: `PartnershipEntityDetailMapper` only (reuse existing Company/Event models) [P]
- Route implementation: GET endpoint with 3-repository orchestration in `publicPartnershipRoutes()`
- JSON schema: External schema file creation leveraging existing Company/Event schemas [P]
- OpenAPI documentation: Update openapi.yaml with new endpoint using existing model references

**Ordering Strategy**:
- Phase 1: Contract tests (TDD - tests before implementation)
- Phase 2: Domain models (PartnershipDetail, DetailedPartnershipResponse) [P]
- Phase 3: Repository interface update [P]
- Phase 4: Single mapper implementation [P]
- Phase 5: Repository implementation (partnership-only data)
- Phase 6: Route implementation (orchestrates 3 repositories)
- Phase 7: Documentation and schema (references existing schemas) [P]

**Mock Factory Strategy**:
- Reuse existing `mockCompany()`, `mockEvent()`, `mockPartnership()` functions without modification
- Mock factories only exist for database entities (PartnershipEntity, CompanyEntity, EventEntity)
- Response models (`PartnershipDetail`, `DetailedPartnershipResponse`) are created by mappers from mocked entities
- Test data created by calling existing mock factories and applying mappers in contract tests
- Support different process status scenarios by parameterizing existing `mockPartnership()` with various timestamp fields

**Estimated Output**: 15-18 numbered tasks in dependency order with [P] parallel markers (reduced from original estimate due to model reuse)

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
- [ ] Complexity deviations documented

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
