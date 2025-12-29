
# Implementation Plan: Provider Management Enhancement

**Branch**: `009-as-an-organiser` | **Date**: 8 November 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/009-as-an-organiser/spec.md`

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
Backend-only enhancement to provider management: link providers to organisations, enable organisers to create/update/delete providers within their organisation scope via REST API, attach/detach providers to events, and provide public listing with organisation filtering. Includes database migration to add organisation_id to providers table. No frontend changes required.

## Technical Context
**Language/Version**: Kotlin/JVM 21  
**Primary Dependencies**: Ktor, Exposed ORM, PostgreSQL  
**Storage**: PostgreSQL database with Exposed ORM migrations  
**Testing**: Kotlin JUnit 4 for contract tests, H2 in-memory database for integration tests  
**Target Platform**: JVM backend server (Port 8080)  
**Project Type**: backend-only (Kotlin/Ktor REST API enhancement)  
**Performance Goals**: <2 seconds response time for standard operations  
**Constraints**: Zero ktlint/detekt violations, 80% test coverage minimum  
**Scale/Scope**: Organisation-scoped provider management with event attachment workflows

**Implementation Details**: 
- Update existing `ProviderRoutes.kt` and `EventProviderRoutes.kt` for routing enhancements
- Modify `ProvidersTable.kt` to add organisation_id foreign key (non-nullable)
- Database migration: DELETE existing providers, ADD organisation_id column (breaking change acceptable)
- Preserve existing repository pattern and improve with organisation scoping

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: All new code must pass ktlint and detekt with zero violations, include KDoc documentation for public APIs, and maintain 80% test coverage minimum.

**Testing Strategy**: Feature must include Kotlin JUnit contract tests for API schema validation, integration tests for complete HTTP route workflows using H2 in-memory database, and mock factory functions for consistent test data setup.

**Clean Architecture**: Feature must respect domain module boundaries, avoid circular dependencies, maintain interface segregation, and include database migration strategy if schema changes required.

**API Consistency**: REST endpoints must follow established naming conventions, return consistent error formats, include OpenAPI documentation, and meet 2-second response time requirement.

**Performance & Observability**: Database queries must be optimized with proper indexing, include structured logging with correlation IDs, and provide metrics for critical operations.

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
├── provider/
│   ├── domain/
│   │   └── ProviderRepository.kt
│   └── infrastructure/
│       ├── api/
│       │   └── ProviderRoutes.kt          # MODIFY: Add org scoping
│       └── db/
│           ├── ProvidersTable.kt          # MODIFY: Add organisation_id
│           └── ProviderRepositoryExposed.kt
├── events/
│   └── infrastructure/
│       └── api/
│           └── EventProviderRoutes.kt     # MODIFY: Org-scoped attachment
├── organisations/
│   ├── domain/
│   └── infrastructure/
└── internal/
    └── infrastructure/
        └── api/
            └── AuthorizedOrganisationPlugin.kt  # REUSE: For permissions

server/application/src/test/kotlin/
├── provider/
│   ├── ProviderRoutesTest.kt              # ADD: Contract tests
│   └── EventProviderRoutesTest.kt         # ADD: Attachment tests
└── factories/
    └── ProviderFactory.kt                 # ADD: Mock factory
```

**Structure Decision**: Backend-only enhancement within existing Kotlin/Ktor server structure. This feature enhances existing provider module by adding organisation relationships and event attachment workflows. No frontend changes required - all functionality exposed via REST API endpoints. Following established domain-driven module pattern.

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
- Load `.specify/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Database migration task: Update ProvidersTable with organisation_id column [P]
- JSON schema creation tasks for validation schemas [P]
- Mock factory creation task: `mockProvider()`, `mockCreateProviderRequest()` [P] 
- Contract test tasks for each endpoint (schema validation only) [P]
- Repository enhancement tasks: Add organisation-scoped methods [P]
- Route implementation tasks: Update ProviderRoutes and EventProviderRoutes
- Integration test tasks: End-to-end business logic validation
- OpenAPI documentation update task: Add new endpoints and schema references

**Ordering Strategy**:
- TDD order: Database migration → Mock factories → Contract tests → Repository → Routes → Integration tests
- Dependency order: Schema files before contract tests, repositories before routes
- Mark [P] for parallel execution where files are independent
- Sequential for items that depend on previous database/repository changes

**Constitutional Compliance Tasks**:
- ktlint/detekt validation after each code change
- Test coverage validation (target 80%+)
- AuthorizedOrganisationPlugin integration for permission checks
- JSON schema validation using `call.receive<T>(schema)` pattern
- Repository layer isolation (no cross-repository dependencies)

**Estimated Output**: 18-22 numbered, ordered tasks in tasks.md covering database, testing, implementation, and documentation

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
- [x] Complexity deviations documented

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
