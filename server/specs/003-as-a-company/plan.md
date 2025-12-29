
# Implementation Plan: Job Offers Management for Companies

**Branch**: `003-as-a-company` | **Date**: October 16, 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-as-a-company/spec.md`

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
Enable company owners to create, update, delete and retrieve job offers attached to their companies. Job offers contain URL, title, location, publication date, and optional fields (end date, experience years, salary). These job offers can be promoted to active event partnerships for increased visibility. Implementation requires new REST endpoints in CompanyRoutes, domain repository interface, Exposed database entities, and repository implementation.

## Technical Context
**Language/Version**: Kotlin/JVM 21  
**Primary Dependencies**: Ktor 3.0, Exposed ORM, PostgreSQL, Koin DI  
**Storage**: PostgreSQL with Exposed ORM, H2 in-memory for tests  
**Testing**: Kotlin Test, JUnit 5, H2 database for integration tests  
**Target Platform**: Linux server (containerized deployment)
**Project Type**: Web backend API (Kotlin/Ktor server)  
**Performance Goals**: <2 second response time for standard operations  
**Constraints**: Database schema must be backwards-compatible, modular architecture compliance  
**Scale/Scope**: Company-level job offers management, integration with existing partnership system

**User Implementation Details**: 
- Add job offer routes to `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyRoutes.kt`
- Create `CompanyJobOfferRepository` interface in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain`
- Create Exposed table and entity in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db`
- Implement `CompanyJobOfferRepositoryExposed` in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/application`

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: All new code must pass ktlint and detekt with zero violations, include KDoc documentation for public APIs, and maintain 80% test coverage minimum.

**Testing Strategy**: Feature must include unit tests for all business logic, integration tests for external service calls (Slack, Mailjet, BilletWeb, GCS), and database tests using H2 in-memory DB.

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
└── companies/
    ├── domain/
    │   ├── CompanyJobOfferRepository.kt      # New repository ├── interface
    │   ├── CreateJobOffer.kt                 # New domain model
    │   └── UpdateJobOffer.kt                 # New domain model
    ├── application/
    │   └── CompanyJobOfferRepositoryExposed.kt # New repository implementation
    ├── infrastructure/
    │   ├── api/
    │   │   └── CompanyRoutes.kt              # Extend existing with job offer endpoints
    │   └── db/
    │       ├── CompanyJobOfferTable.kt       # New Exposed table
    │       └── CompanyJobOfferEntity.kt      # New Exposed entity

server/application/src/test/kotlin/fr/devlille/partners/connect/
└── companies/
    └── CompanyJobOfferRoutesTest.kt  # Integration tests

server/application/src/main/resources/
├── openapi/
│   └── openapi.yaml                         # Updated with job offer endpoints
└── schemas/
    ├── create_job_offer.schema.json         # New validation schema
    └── update_job_offer.schema.json         # New validation schema
```

**Structure Decision**: Web application backend extending existing companies domain module. Following established modular architecture with domain interfaces, Exposed-based application layer, and infrastructure for API/database. Maintains separation of concerns and constitutional compliance.

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
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Domain model tasks: Create repository interface, request/response models [P]
- Database tasks: Create Exposed table, entity, migration [P]
- Repository implementation: CompanyJobOfferRepositoryExposed with all CRUD operations
- Route implementation: Extend CompanyRoutes with job offer endpoints
- JSON schema integration: Add validation schemas to resources
- Integration tests: End-to-end API validation per endpoint (create, list, get, update, delete)
- OpenAPI documentation: Update specification with new endpoints

**Ordering Strategy**:
- TDD order: Integration tests before implementation
- Dependency order: Domain models → Database schema → Integration tests → Repository → Routes
- Database and domain models can be created in parallel [P]
- Integration tests can be written in parallel with models [P]
- Route implementation depends on repository completion

**Estimated Output**: 20-25 numbered, ordered tasks in tasks.md focusing on:
1. Domain layer (models, interfaces)
2. Database layer (tables, entities)  
3. Application layer (repository implementation)
4. Infrastructure layer (API routes, validation)
5. Testing (integration)
6. Documentation (OpenAPI, schemas)

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

No constitutional violations identified. The design follows all established patterns:
- Modular architecture maintained within companies domain
- Standard REST API patterns used
- Existing authentication/authorization leveraged  
- Repository pattern consistent with project structure
- Database schema changes are backwards-compatible
- All constitutional requirements satisfied


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
