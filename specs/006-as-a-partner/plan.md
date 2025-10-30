
# Implementation Plan: Complete CRUD Operations for Companies

**Branch**: `006-as-a-partner` | **Date**: 30 October 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-as-a-partner/spec.md`

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
Complete the CRUD operations for the /companies resource by adding missing PUT (update) and DELETE (soft delete) endpoints. These public endpoints allow updating company information with partial data support and soft deletion by marking companies as inactive. Includes status filtering for company listings to handle active/inactive visibility.

## Technical Context
**Language/Version**: Kotlin JVM 21  
**Primary Dependencies**: Ktor (HTTP server), Exposed ORM, Koin (DI), kotlinx.serialization  
**Storage**: PostgreSQL with Exposed ORM, H2 in-memory for tests  
**Testing**: kotlin.test, Ktor testing framework, integration tests via HTTP routes  
**Target Platform**: JVM server application  
**Project Type**: web (backend Kotlin/Ktor server)  
**Performance Goals**: <2 seconds response time for standard operations  
**Constraints**: Public endpoints (no authentication), backward compatible schema changes, preserve existing relationships  
**Scale/Scope**: Extend existing company module with 2 new endpoints, 1 new field, status filtering

**Implementation Details**: Update `CompanyRoutes.kt` to add PUT /companies/{id} and DELETE /companies/{id} endpoints. Extend `CompanyRepository.kt` interface with update/delete methods and status filtering. Add `status` field to `CompaniesTable` and `CompanyEntity`. Update `listPaginated` to support status filtering with default showing all companies.

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: ✅ PASS - New endpoints will follow existing ktlint/detekt standards, include KDoc documentation, and maintain test coverage through HTTP route integration tests.

**Testing Strategy**: ✅ PASS - Feature includes HTTP route integration tests for PUT/DELETE endpoints, covers error scenarios, validates schema changes with H2 database. No external service integration required for this feature.

**Clean Architecture**: ✅ PASS - Extends existing companies domain module without circular dependencies. Schema change adds single `status` column with backwards compatibility. Follows repository pattern with interface/implementation separation.

**API Consistency**: ✅ PASS - Follows existing REST patterns: PUT /companies/{id} and DELETE /companies/{id}. Returns consistent JSON error formats via StatusPages plugin. Will update OpenAPI documentation. Meets <2s response requirement.

**Performance & Observability**: ✅ PASS - Database operations use existing Exposed patterns with proper indexing. Status column will be indexed for filtering performance. Follows existing logging patterns with correlation IDs.

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
server/application/src/main/kotlin/fr/devlille/partners/connect/
├── companies/
│   ├── domain/
│   │   ├── CompanyRepository.kt                    # Extend with update/delete/filtering
│   │   ├── UpdateCompany.kt                        # New domain model (to create)
│   │   └── CompanyStatus.kt                        # New enum (to create)
│   ├── application/
│   │   └── CompanyRepositoryExposed.kt             # Implement new methods
│   └── infrastructure/
│       ├── api/
│       │   └── CompanyRoutes.kt                    # Add PUT/DELETE endpoints
│       └── db/
│           ├── CompaniesTable.kt                   # Add status column
│           └── CompanyEntity.kt                    # Add status property
└── server/application/src/test/kotlin/fr/devlille/partners/connect/
    └── companies/
        ├── CompanyUpdateRoutesTest.kt              # New test file
        └── CompanyDeleteRoutesTest.kt              # New test file
```

**Structure Decision**: Web application (Kotlin/Ktor backend) - extending existing companies module within established domain-driven architecture. All changes contained within the companies domain module following existing patterns from job offers CRUD implementation.

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

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (data-model.md, contracts/, quickstart.md)
- Domain model tasks: CompanyStatus enum, UpdateCompany model [P]
- Database tasks: Schema migration, table/entity updates [P]
- Repository tasks: Interface extension, implementation methods
- Route tasks: PUT/DELETE endpoint implementation 
- Test tasks: Contract tests for each endpoint scenario
- Validation tasks: JSON schema, error handling

**Ordering Strategy**:
- TDD order: Contract tests first, then implementation to make tests pass
- Dependency order: Schema → Models → Repository → Routes → Integration
- Database changes before domain logic before API routes
- Mark [P] for parallel execution within same layer
- Schema migration must complete before any code changes

**Estimated Output**: ~18-22 numbered, ordered tasks focusing on:
1. Database schema updates (2-3 tasks)
2. Domain model creation (2-3 tasks) [P]
3. Repository interface/implementation (3-4 tasks)
4. HTTP route implementation (2-3 tasks)
5. Contract test implementation (6-8 tasks) [P]
6. Integration validation (2-3 tasks)

**Key Dependencies**:
- Schema changes must precede all development
- Contract tests written before implementation (TDD)
- Repository methods before route handlers
- JSON schema validation before endpoint implementation

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
- [x] Phase 2: Task planning approach complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented (none required)

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
