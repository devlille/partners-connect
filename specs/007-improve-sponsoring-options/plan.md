
# Implementation Plan: Enhanced Sponsoring Options with Four Option Types

**Branch**: `007-improve-sponsoring-options` | **Date**: 31 October 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/007-improve-sponsoring-options/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from file system structure or context (backend=server-only, web=frontend+backend, mobile=app+api)
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
Extend the existing sponsoring system to support four distinct option types through polymorphic sealed classes: text (current), typed_quantitative (partners select quantities with enum-based type descriptors), typed_number (fixed quantities), and typed_selectable (predefined value selection). Implementation uses sealed class architecture with @JsonClassDiscriminator for clean polymorphic serialization, enum-based type descriptors (QuantitativeDescriptor.JOB_OFFER, NumberDescriptor.NB_TICKET, SelectableDescriptor.BOOTH), and maintains backward compatibility through nullable database columns. Server-side implementation focuses on API endpoints, domain models, and database extensions while maintaining existing frontend compatibility.

## Technical Context
**Language/Version**: Kotlin JVM 21 with Ktor 3.0+  
**Primary Dependencies**: Ktor (REST API), Exposed ORM, Koin (DI), kotlinx-serialization, kotlinx-datetime  
**Storage**: PostgreSQL (production), H2 in-memory (testing) with Exposed ORM dual-layer structure  
**Testing**: Kotlin Test with Ktor test engine, HTTP route integration tests, 80% coverage minimum  
**Target Platform**: JVM server application (Docker containerized, Google Cloud Run)
**Project Type**: backend - Kotlin/Ktor server API extension  
**Performance Goals**: <2 seconds response time for standard operations per constitution  
**Constraints**: Zero ktlint/detekt violations, no repository unit tests (HTTP route integration only), StatusPages exception handling, backward compatibility with existing sponsoring system  
**Scale/Scope**: Extend existing sponsoring module (13 domain modules), support unlimited option configurations per event, server-side API extensions only

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
server/
├── application/src/main/kotlin/fr/devlille/partners/connect/
│   ├── sponsoring/
│   │   ├── domain/                    # Polymorphic SponsoringOption sealed classes, enum descriptors
│   │   ├── application/               # Update repositories with polymorphic option types
│   │   └── infrastructure/
│   │       ├── api/                   # Update SponsoringRoutes.kt with discriminated unions
│   │       └── db/                    # Extend tables with nullable columns for compatibility
│   ├── partnership/
│   │   ├── domain/                    # Polymorphic PartnershipOption sealed classes with selections
│   │   ├── application/               # Update repositories for polymorphic partner selections
│   │   └── infrastructure/
│   │       ├── api/                   # Update PartnershipRoutes.kt with discriminated unions
│   │       └── db/                    # Extend partnership tables with nullable columns
│   └── internal/                      # Shared infrastructure, validation
└── application/src/test/kotlin/       # HTTP route integration tests
```

**Structure Decision**: Backend-focused extension of existing sponsoring and partnership modules following established domain-driven architecture patterns. Frontend remains unchanged and will consume new API endpoints.

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
- Database schema tasks: Extend SponsoringOptionsTable, create SelectableValuesTable, extend PartnershipOptionsTable [P]
- Domain model tasks: Extend SponsoringOption, create OptionType enum, extend PartnershipOption [P]
- Repository tasks: Update SponsoringOptionRepository, PartnershipRepository with new methods
- API contract tests: Each endpoint in api-contracts.yaml → failing HTTP test [P]
- Integration test tasks: Each quickstart scenario → HTTP route test covering end-to-end flow

**Ordering Strategy**:
- TDD order: Database schema → Domain models → Contract tests → Repository implementations → Route updates
- Dependency order: Core domain models before API routes to ensure type safety
- Mark [P] for parallel execution within same layer (database tables, domain models, tests)
- Constitutional compliance: HTTP route integration tests, not repository unit tests

**Task Categories**:
1. **Schema Extension** (3-4 tasks): Database table modifications with backward compatibility
2. **Domain Models** (4-5 tasks): Entity extensions, enums, validation logic
3. **Contract Tests** (8-10 tasks): Failing HTTP tests for each API endpoint scenario
4. **Repository Implementation** (4-5 tasks): Database operations for new option types
5. **API Routes** (3-4 tasks): Extend existing routes to handle new request/response schemas
6. **Integration Validation** (3-4 tasks): End-to-end tests matching quickstart scenarios

**Estimated Output**: 25-30 numbered, ordered tasks in tasks.md

**Constitutional Alignment**:
- All database tasks use Exposed ORM dual-layer pattern
- Repository tasks focus on data access only (no cross-domain dependencies)  
- Route tasks handle orchestration and use AuthorizedOrganisationPlugin
- Test tasks target HTTP routes for 80% coverage requirement
- No try-catch blocks in routes (StatusPages handles exceptions)

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
