
# Implementation Plan: Multi-Language Sponsoring Pack and Option Management for Organizers

**Branch**: `002-for-sponsoring-pack` | **Date**: October 4, 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-for-sponsoring-pack/spec.md`

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
Modify organizer-facing sponsoring pack and option endpoints under `/orgs/{orgSlug}` paths to return all available translations instead of filtering by Accept-Language header. This enables organizers to see complete translation data and identify which languages need additional translation work. The solution involves updating PackRepository and OptionRepository interfaces, modifying the data model to include translation maps, updating API routes, tests, and OpenAPI documentation.

## Technical Context
**Language/Version**: Kotlin/JVM 21, Ktor 2.x  
**Primary Dependencies**: Ktor server, Exposed ORM, PostgreSQL, kotlinx.serialization  
**Storage**: PostgreSQL with OptionTranslationsTable for multi-language support  
**Testing**: Kotlin Test, H2 in-memory database for integration tests, existing test infrastructure  
**Target Platform**: JVM server (containerized deployment)  
**Project Type**: web - backend API modification  
**Performance Goals**: <2 seconds response time per constitution, maintain current throughput  
**Constraints**: Must preserve existing public API behavior, backward compatibility required  
**Scale/Scope**: Modification affects organizer endpoints only, ~13 domain modules, existing translation infrastructure

**User-provided Implementation Details**: Remove Accept-Language header in SponsoringRoutes.kt organizer routes, update PackRepository and OptionRepository to remove language parameter for organizer services, modify return model to include translation maps with language as key and option model as value, update tests and OpenAPI documentation.

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: ✅ PASS - All new repository methods and domain entities will include comprehensive KDoc documentation. Existing ktlint/detekt rules apply. Unit tests for new repository methods and integration tests for route changes planned.

**Testing Strategy**: ✅ PASS - Integration tests using H2 in-memory database for new repository methods. Route-level tests for organizer endpoints. No external service integration changes required (no Slack, Mailjet, etc. impact).

**Clean Architecture**: ✅ PASS - Changes respect existing domain module boundaries. New methods added to existing repository interfaces without circular dependencies. No database schema changes required - leverages existing OptionTranslationsTable infrastructure.

**API Consistency**: ✅ PASS - Follows existing organizer endpoint patterns under `/orgs/{orgSlug}` paths. OpenAPI documentation updates planned. Error response formats consistent with existing endpoints. Performance requirement <2s maintained through optimized existing queries.

**Performance & Observability**: ✅ PASS - Leverages existing optimized queries in OptionTranslationsTable. No new database queries required - modifying existing translation loading logic. Existing structured logging and correlation IDs preserved.

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
server/
├── application/
│   ├── src/main/kotlin/fr/devlille/partners/connect/
│   │   ├── sponsoring/
│   │   │   ├── domain/                    # Repository interfaces
│   │   │   ├── application/               # Repository implementations  
│   │   │   └── infrastructure/api/        # SponsoringRoutes.kt
│   │   └── internal/                      # Shared infrastructure
│   ├── src/test/kotlin/                   # Integration tests
│   └── src/main/resources/
│       └── openapi/openapi.yaml          # API documentation
└── gradle/                                # Build configuration

front/
├── utils/api.ts                           # Generated API client (may need updates)
└── types/partner.ts                       # TypeScript types
```

**Structure Decision**: Web application with existing Kotlin/Ktor backend. Modifications target the sponsoring domain module within the clean architecture structure: domain interfaces, application implementations, and infrastructure API routes. Frontend may need type updates but core changes are backend-focused.

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
- Domain model entities → Kotlin data class creation tasks [P]
- Repository interface methods → interface modification tasks [P]
- Repository implementations → Exposed ORM implementation tasks
- Route modifications → API endpoint update tasks
- Contract endpoints → integration test tasks following quickstart scenarios
- OpenAPI documentation → schema update tasks [P]

**Ordering Strategy**:
- TDD order: Integration tests for new endpoints before implementation
- Dependency order: Domain models → Repository interfaces → Repository implementations → Routes → Tests
- Mark [P] for parallel execution: Domain models, interface updates, documentation, test files
- Sequential execution: Repository implementations depend on interfaces, routes depend on repositories

**Specific Task Categories**:
1. **Domain Model Tasks**: Create new data classes (SponsoringPackWithTranslations, SponsoringOptionWithTranslations, OptionTranslation)
2. **Repository Interface Tasks**: Add new methods to PackRepository and OptionRepository interfaces  
3. **Repository Implementation Tasks**: Implement new methods in PackRepositoryExposed and OptionRepositoryExposed
4. **Extension Function Tasks**: Create new toDomainWithAllTranslations() extension functions
5. **Route Modification Tasks**: Update SponsoringRoutes.kt organizer endpoints to remove Accept-Language dependency
6. **Test Tasks**: Update existing integration tests and add new multi-language test scenarios
7. **Documentation Tasks**: Update OpenAPI specification for modified organizer endpoints

**Estimated Output**: 15-20 numbered, ordered tasks in tasks.md (focused scope, leveraging existing infrastructure)

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
- [x] Complexity deviations documented (None - no violations found)

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
