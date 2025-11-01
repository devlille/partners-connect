# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → If not found: ERROR "No implementation plan found"
   → Extract: tech stack, libraries, structure
2. Load optional design documents:
   → data-model.md: Extract entities → model tasks
   → contracts/: Each file → contract test task
   → research.md: Extract decisions → setup tasks
3. Generate tasks by category (per constitution):
   → Setup: project init, dependencies, ktlint/detekt config
   → Tests: contract tests, integration tests, database tests (H2)
   → Core: models, services, domain modules
   → Integration: DB optimization, external service calls, logging with correlation IDs
   → Quality: code coverage verification, performance testing, API documentation
4. Apply task rules:
   → Different files = mark [P] for parallel
   → Same file = sequential (no [P])
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001, T002...)
6. Generate dependency graph
7. Create parallel execution examples
8. Validate task completeness:
   → All contracts have tests?
   → All entities have models?
   → All endpoints implemented?
9. Return: SUCCESS (tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
- **Single project**: `src/`, `tests/` at repository root
- **Web app**: `backend/src/`, `frontend/src/`
- **Mobile**: `api/src/`, `ios/src/` or `android/src/`
- Paths shown below assume single project - adjust based on plan.md structure

## Phase 3.1: Setup
- [ ] T001 Create project structure per implementation plan in application/src/main/kotlin/fr/devlille/partners/connect/
- [ ] T002 Initialize Kotlin module with Ktor dependencies and Exposed ORM
- [ ] T003 [P] Configure ktlint and detekt with zero-violation enforcement

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
**Use existing mock factory functions or create them if needed for test data setup**
- [ ] T004 [P] Contract test POST /api/users with mock factory setup in tests/contract/test_users_post.py
- [ ] T005 [P] Contract test GET /api/users/{id} with mock factory setup in tests/contract/test_users_get.py
- [ ] T006 [P] Integration test user registration in tests/integration/test_registration.py
- [ ] T007 [P] Integration test auth flow in tests/integration/test_auth.py

## Phase 3.3: Core Implementation (ONLY after tests are failing)
- [ ] T008 [P] User model in src/models/user.py
- [ ] T009 [P] UserService CRUD in src/services/user_service.py
- [ ] T010 [P] CLI --create-user in src/cli/user_commands.py
- [ ] T011 POST /api/users endpoint
- [ ] T012 GET /api/users/{id} endpoint
- [ ] T013 Input validation
- [ ] T014 Error handling and logging

## Phase 3.4: Schema & API Documentation
- [ ] T015 Generate JSON schemas for request/response types (CreateUserRequest.json, UserResponse.json) in server/application/src/main/resources/schemas/ and update openapi.yaml to reference these schemas

## Phase 3.5: Integration
- [ ] T016 Connect UserService to DB
- [ ] T017 Auth middleware
- [ ] T018 Request/response logging using call.receive<T>(schema) validation
- [ ] T019 CORS and security headers

## Phase 3.6: Quality & Polish
- [ ] T020 [P] Unit tests achieving 80% minimum coverage in application/src/test/kotlin/
- [ ] T021 Performance tests ensuring <2s response times for standard operations
- [ ] T022 [P] OpenAPI documentation update with clear error response formats
- [ ] T023 KDoc documentation for all public APIs and domain classes
- [ ] T024 Database query optimization with proper indexing strategy
- [ ] T025 Structured logging implementation with correlation IDs
- [ ] T026 Code duplication removal and Kotlin idiom compliance verification

## Dependencies
- Tests (T004-T007) before implementation (T008-T014)
- T008 blocks T009, T016
- T015 (JSON schemas) blocks T016-T019 (implementation using call.receive<T>(schema))
- T017 blocks T019
- Implementation before polish (T020-T026)

## Parallel Example
```
# Launch T004-T007 together:
Task: "Contract test POST /api/users in tests/contract/test_users_post.py"
Task: "Contract test GET /api/users/{id} in tests/contract/test_users_get.py"
Task: "Integration test registration in tests/integration/test_registration.py"
Task: "Integration test auth in tests/integration/test_auth.py"
```

## Notes
- [P] tasks = different files, no dependencies
- Verify contract tests fail before implementing
- Use existing mock factory functions or create them for test data setup
- Use @JsonClassDiscriminator("type") for polymorphic serialization
- JSON schemas enable call.receive<T>(schema) validation without manual Kotlin validation code
- OpenAPI specification references JSON schemas to avoid duplication
- Contract tests focus on API schema validation, not business logic
- Commit after each task
- Avoid: vague tasks, same file conflicts

## Task Generation Rules
*Applied during main() execution*

1. **From Contracts**:
   - Each contract file → contract test task [P]
   - Each endpoint → implementation task
   
2. **From Data Model**:
   - Each entity → model creation task [P]
   - Relationships → service layer tasks
   
3. **From User Stories**:
   - Each story → integration test [P]
   - Quickstart scenarios → validation tasks

4. **Ordering**:
   - Setup → Tests → Models → Services → Endpoints → Polish
   - Dependencies block parallel execution

## Validation Checklist
*GATE: Checked by main() before returning*

- [ ] All contracts have corresponding tests
- [ ] All entities have model tasks
- [ ] JSON schema generation task for request/response types
- [ ] Contract tests use mock factory functions for data setup
- [ ] All tests come before implementation
- [ ] Parallel tasks truly independent
- [ ] Each task specifies exact file path
- [ ] No task modifies same file as another [P] task