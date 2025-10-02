
# Implementation Plan: Event Sponsoring Packs Public API

**Branch**: `001-create-new-get` | **Date**: 2025-10-02 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-create-new-get/spec.md`

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
Create a public GET endpoint `/events/{eventSlug}/sponsoring/packs` that returns all sponsoring packages for an event without authentication. The service should include embedded options and optional add-ons, use Accept-Language header for translations, and follow clean architecture with routing in infrastructure/api, domain logic in domain package, and repository implementation in application package. Reuse existing SponsoringPack and SponsoringOption data classes.

## Technical Context
**Language/Version**: Kotlin 2.1.21 with JVM 21  
**Primary Dependencies**: Ktor 3.2.0 (REST API), Exposed 1.0.0-beta-2 (ORM), Koin 4.1.0 (DI), kotlinx-serialization 1.7.3  
**Storage**: PostgreSQL 42.7.1 (production), H2 2.2.224 (testing)  
**Testing**: Kotlin Test with MockK 1.14.5, H2 in-memory database for integration tests  
**Target Platform**: JVM server application (Docker containerized)
**Project Type**: web - backend API with frontend separation  
**Performance Goals**: <2 seconds response time for standard operations (per constitution)  
**Constraints**: Accept-Language header for i18n, clean architecture with domain/infrastructure separation, reuse existing SponsoringPack/SponsoringOption entities  
**Scale/Scope**: Public endpoint, no pagination needed (low volume per event), must handle special characters in event slugs

**User-provided Implementation Details**: Service should be created in fr.devlille.partners.connect.sponsoring.infrastructure.api.sponsoringRoutes route function but with a new route starting with `/events` instead of `/orgs`. Must use `Accept-Language` header for options translations. Must respect clean architecture with routing in `infrastructure/api` package, interface repository and business logic entities in `domain` package, and repository exposed implementation in `application` package. Must reuse `SponsoringPack` and `SponsoringOption` data classes and create a new repository with exposed implementation using existing exposed tables.

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: ✅ PASS - All new code will follow existing ktlint/detekt standards, include KDoc documentation for public repository interface, and maintain 80% test coverage with unit and integration tests.

**Testing Strategy**: ✅ PASS - Feature will include unit tests for repository implementation, integration tests using H2 in-memory database, and route-level tests. No external service integration needed for this read-only endpoint.

**Clean Architecture**: ✅ PASS - Feature extends existing sponsoring domain module, reuses existing entities (SponsoringPack/SponsoringOption), creates new domain interface (EventPackRepository), implements in application layer (EventPackRepositoryExposed), and adds route in infrastructure layer. No circular dependencies introduced.

**API Consistency**: ✅ PASS - New endpoint follows RESTful patterns (`GET /events/{eventSlug}/sponsoring/packs`), will return consistent JSON format matching existing SponsoringPack structure, include proper HTTP status codes (200, 404), and leverage existing optimized database queries.

**Performance & Observability**: ✅ PASS - Will reuse existing optimized Exposed queries and database tables, structured logging via Ktor's built-in mechanisms, and leverage existing database indexing on event relationships. Response time will be well under 2-second requirement for simple SELECT operations.

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
├── application/
│   └── src/
│       ├── main/kotlin/fr/devlille/partners/connect/
│       │   └── sponsoring/
│       │       ├── domain/              # Business logic entities & interfaces
│       │       │   ├── SponsoringPack.kt (existing)
│       │       │   ├── SponsoringOption.kt (existing)
│       │       │   ├── PackRepository.kt (existing)
│       │       │   └── EventPackRepository.kt (NEW)
│       │       ├── application/         # Repository implementations
│       │       │   └── EventPackRepositoryExposed.kt (NEW)
│       │       └── infrastructure/
│       │           └── api/
│       │               └── SponsoringRoutes.kt (MODIFY - add /events route)
│       └── test/kotlin/fr/devlille/partners/connect/
│           └── sponsoring/
│               └── EventPackRoutesTest.kt (NEW)
└── gradle/
    └── libs.versions.toml

front/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/
```

**Structure Decision**: Web application with Kotlin/Ktor backend and Nuxt.js frontend. This feature extends the existing sponsoring domain module following clean architecture patterns with domain interfaces, application implementations, and infrastructure API routes. The new public endpoint will be added to the existing SponsoringRoutes.kt file alongside the authenticated organizational routes.

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
- Generate tasks from Phase 1 design docs (contracts, data-model.md, quickstart.md)
- Domain interface creation → EventPackRepository interface task [P]
- Repository implementation → EventPackRepositoryExposed class task [P]
- API endpoint → New route in SponsoringRoutes.kt task 
- Dependency injection → Koin binding in SponsoringModule.kt task
- Integration tests → EventPackRoutesTest.kt test task
- Contract validation → OpenAPI compliance test task [P]
- Performance validation → Response time verification task

**Ordering Strategy**:
- TDD order: Domain interface → Tests → Repository implementation → Route implementation → DI binding
- Dependency order: Domain interfaces before implementations, repository before routes
- Mark [P] for parallel execution where no dependencies exist
- Integration tests run after all components are implemented

**Estimated Output**: 12-15 numbered, ordered tasks in tasks.md

**Key Task Categories**:
1. **Domain Layer** (2 tasks): EventPackRepository interface, KDoc documentation
2. **Application Layer** (3 tasks): EventPackRepositoryExposed implementation, error handling, transaction management
3. **Infrastructure Layer** (3 tasks): Route handler, request/response mapping, Accept-Language header processing  
4. **Integration** (2 tasks): Koin DI binding, route registration
5. **Testing** (4 tasks): Repository unit tests, route integration tests, contract validation, quickstart verification
6. **Quality Assurance** (2 tasks): Code formatting/linting, performance validation

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
- [x] Complexity deviations documented (None required)

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
