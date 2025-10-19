
# Implementation Plan: Job Offer Promotion with Approval Workflow

**Branch**: `004-i-would-like` | **Date**: 2025-10-18 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-i-would-like/spec.md`

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
Enable company owners to promote job offers to event partnerships and allow event organizers to approve or decline those promotions with automated notifications. Company owners submit job offers for promotion through public REST endpoints in CompanyRoutes (companies domain - no authentication required). Event organizers with edit permissions review promotions through organization-protected partnership endpoints using AuthorizedOrganisationPlugin for automatic JWT validation and permission checking. The system sends dual notifications via Mailjet (email to partnership contacts) and Slack (organization channel) for all status changes (promoted, approved, declined). Database cascade deletes handle job offer deletion, while partnership termination preserves pending promotions. Implementation uses Exposed ORM for database operations, follows existing notification patterns, and includes comprehensive integration tests covering all workflows.

## Technical Context
**Language/Version**: Kotlin JVM 21 with Ktor 3.0.2  
**Primary Dependencies**: Ktor (web framework), Exposed 0.58.0 (ORM), Koin (DI), kotlinx-serialization, kotlinx-datetime  
**Storage**: PostgreSQL 15+ (production), H2 in-memory (testing)  
**Testing**: Kotlin Test with Ktor test engine for integration tests (HTTP route testing), minimum 80% coverage  
**Target Platform**: JVM server (Linux/Docker), deployed on Google Cloud Run
**Project Type**: Backend API (Ktor REST server with modular domain architecture)  
**Performance Goals**: <2 second response time for all endpoints, handle concurrent promotion/approval operations  
**Constraints**: Zero ktlint/detekt violations, no repository unit tests (integration tests only via HTTP routes), StatusPages exception handling (no try-catch in routes), use existing notification infrastructure  
**Scale/Scope**: Support unlimited job offer promotions per partnership, multi-language notifications, event edit permission validation

**Implementation Details from User**:
- **Company promotion endpoint**: Add route in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyRoutes.kt`
- **Company domain layer**: New repository interface in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain` package
- **Company application layer**: Exposed-based implementation in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/application` package
- **Partnership approval/decline endpoints**: New routes in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api` package with token-based security for event edit permission validation
- **Partnership domain layer**: New repository interface in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain` package
- **Partnership application layer**: Exposed-based implementation in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application` package
- **Testing strategy**: Integration tests with multiple input scenarios for all service cases, no unit tests on repositories
- **OpenAPI documentation**: Update `server/application/src/main/resources/openapi/openapi.yaml` with new endpoints using JSON schemas from `server/application/src/main/resources/schemas`

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: All new code must pass ktlint and detekt with zero violations, include KDoc documentation for public APIs, and maintain 80% test coverage minimum.

**Testing Strategy**: Feature requires integration tests via HTTP routes (NOT repository unit tests per constitution). Tests must cover all external service interactions (Slack, Mailjet), database operations via HTTP endpoints using H2 in-memory DB, and achieve 80% coverage minimum.

**Database Schema Standards**: Feature must implement Exposed ORM dual-layer structure: Table object (UUIDTable) + Entity class (UUIDEntity). MUST use datetime() for all date/time columns (maps to LocalDateTime). MUST define foreign key cascades appropriately. MUST include indexes for query optimization.

**Clean Architecture**: Feature must respect domain module boundaries, avoid circular dependencies, maintain interface segregation, and include database migration strategy if schema changes required.

**API Consistency**: REST endpoints must follow established naming conventions, return consistent error formats, include OpenAPI documentation, and meet 2-second response time requirement.

**Authorization Pattern**: Routes requiring organization permissions MUST use AuthorizedOrganisationPlugin instead of manual permission checking. Plugin automatically validates JWT token, checks canEdit permission, and throws UnauthorizedException if unauthorized. Applied to all routes under `/orgs/{orgSlug}/...`.

**Exception Handling**: Routes MUST NOT include try-catch for exception-to-HTTP conversion. Repository implementations MUST throw exceptions (NotFoundException, ConflictException, ForbiddenException) instead of returning null/Boolean. StatusPages plugin handles all exception mapping automatically.

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
server/application/src/
├── main/
│   ├── kotlin/fr/devlille/partners/connect/
│   │   ├── companies/
│   │   │   ├── domain/
│   │   │   │   ├── CompanyJobOfferPromotionRepository.kt (NEW - interface)
│   │   │   │   └── JobOfferPromotionModels.kt (NEW - request/response DTOs)
│   │   │   ├── application/
│   │   │   │   └── CompanyJobOfferPromotionRepositoryExposed.kt (NEW - Exposed impl)
│   │   │   └── infrastructure/
│   │   │       ├── api/
│   │   │       │   └── CompanyRoutes.kt (MODIFIED - add promotion endpoint)
│   │   │       └── db/
│   │   │           └── CompanyJobOfferPromotionsTable.kt (NEW - DB schema)
│   │   ├── partnership/
│   │   │   ├── domain/
│   │   │   │   ├── PartnershipJobOfferRepository.kt (NEW - interface)
│   │   │   │   └── PartnershipJobOfferModels.kt (NEW - approval/decline DTOs)
│   │   │   ├── application/
│   │   │   │   └── PartnershipJobOfferRepositoryExposed.kt (NEW - Exposed impl)
│   │   │   └── infrastructure/
│   │   │       └── api/
│   │   │           └── PartnershipJobOfferRoutes.kt (NEW - approval/decline endpoints)
│   │   ├── notifications/
│   │   │   └── domain/
│   │   │       └── NotificationVariables.kt (MODIFIED - add job offer promotion notifications)
│   │   └── internal/
│   │       └── infrastructure/
│   │           └── db/
│   │               └── PromotionStatusEnum.kt (NEW - status enum)
│   └── resources/
│       ├── schemas/
│       │   ├── promote_job_offer.schema.json (NEW)
│       │   ├── approve_job_offer_promotion.schema.json (NEW)
│       │   └── decline_job_offer_promotion.schema.json (NEW)
│       ├── openapi/
│       │   └── openapi.yaml (MODIFIED - add new endpoints)
│       └── notifications/
│           ├── email/
│           │   ├── job_offer_promoted/ (NEW - content.en.html, content.fr.html, header.en.txt, header.fr.txt)
│           │   ├── job_offer_approved/ (NEW - same structure)
│           │   └── job_offer_declined/ (NEW - same structure)
│           └── slack/
│               ├── job_offer_promoted/ (NEW - en.md, fr.md)
│               ├── job_offer_approved/ (NEW - en.md, fr.md)
│               └── job_offer_declined/ (NEW - en.md, fr.md)
└── test/
    └── kotlin/fr/devlille/partners/connect/
        ├── companies/
        │   └── infrastructure/
        │       └── api/
        │           └── CompanyJobOfferPromotionRoutesTest.kt (NEW - integration tests)
        └── partnership/
            └── infrastructure/
                └── api/
                    └── PartnershipJobOfferRoutesTest.kt (NEW - integration tests)
```

**Structure Decision**: Backend-only Ktor modular architecture with domain-driven design. Feature spans two existing domain modules (companies and partnership) with clear separation: companies handle promotion submission, partnership handles approval/decline. Uses existing notification and internal infrastructure modules for cross-cutting concerns. No frontend changes required for this backend-focused feature.

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
- Follow test-driven development order: schemas → database → repositories → routes → integration tests
- Each JSON schema → schema creation task [P]
- Database schema → table creation task
- Each repository interface → repository implementation task
- Each API endpoint → route implementation task
- Each acceptance scenario → integration test task
- Notification templates → create email/Slack templates [P]
- OpenAPI documentation → update openapi.yaml task

**Ordering Strategy**:
1. **Foundation Phase** (can be parallel):
   - JSON schema files (4 schemas: promote, approve, decline, response)
   - Database table and enum (CompanyJobOfferPromotionsTable, PromotionStatus)
   - Notification template files (email + Slack for 3 events × 2 languages = 18 files)

2. **Domain Layer** (sequential within each domain):
   - Companies domain: 
     - CompanyJobOfferPromotionRepository interface
     - JobOfferPromotionModels DTOs
     - CompanyJobOfferPromotionRepositoryExposed implementation
   - Partnership domain:
     - PartnershipJobOfferRepository interface
     - PartnershipJobOfferModels DTOs
     - PartnershipJobOfferRepositoryExposed implementation
   - Notifications domain:
     - Extend NotificationVariables with 3 new classes

3. **API Layer** (after domain layer):
   - Update CompanyRoutes with promotion endpoint
   - Create PartnershipJobOfferRoutes with approve/decline endpoints
   - Update Koin DI bindings

4. **Integration Tests** (after API layer):
   - CompanyJobOfferPromotionRoutesTest covering FR-001 to FR-005, FR-030, FR-031
   - PartnershipJobOfferRoutesTest covering FR-006 to FR-012, FR-026 to FR-029
   - Notification integration tests for FR-013 to FR-021
   - Cascade delete tests for FR-024, FR-032

5. **Documentation** (can be parallel with tests):
   - Update OpenAPI specification (openapi.yaml)
   - Validate OpenAPI with `npm run validate`

**Parallel Execution Markers**:
- [P] for JSON schemas (independent files)
- [P] for notification templates (independent files)
- [P] for OpenAPI documentation (can update while implementing)

**Sequential Dependencies**:
- Database schema must exist before repository implementations
- Repository interfaces must exist before implementations
- Repository implementations must exist before route handlers
- Routes must exist before integration tests

**Estimated Output**: 35-40 numbered, ordered tasks in tasks.md covering:
- 4 JSON schema tasks
- 2 database schema tasks (table + enum)
- 18 notification template tasks
- 6 domain model tasks (interfaces + DTOs)
- 3 repository implementation tasks
- 3 notification variable tasks
- 2 route implementation tasks
- 1 DI binding task
- 4 integration test tasks
- 2 OpenAPI documentation tasks

**Test Coverage Target**: 80% minimum via integration tests only (no repository unit tests per user requirement)

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
- [x] Complexity deviations documented (none required)

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
