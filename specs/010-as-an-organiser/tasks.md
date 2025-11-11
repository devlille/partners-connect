# Tasks: OpenPlanner Speaker-Partnership Integration

**Input**: Design documents from `/specs/010-as-an-organiser/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/, quickstart.md

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Implementation plan loaded successfully
   → Extract: Kotlin 1.9/JVM 21, Ktor 2.x, Exposed ORM, PostgreSQL
2. Load design documents:
   → data-model.md: Extract entities → SpeakerPartnership, Session, Speaker, AgendaResponse
   → contracts/: JSON schemas and API contracts → contract test tasks
   → research.md: Extract decisions → OpenPlanner integration, error handling
   → quickstart.md: User validation scenarios → integration test tasks
3. Generate tasks by category (per constitution):
   → Setup: Database schema, JSON schemas, ktlint/detekt config
   → Tests: Contract tests, integration tests, database tests (H2)
   → Core: Repository interface/implementation, domain models, route handlers
   → Integration: Enhanced partnership details, agenda retrieval endpoints
   → Quality: Documentation, validation, final testing
4. Apply task rules:
   → Different files = mark [P] for parallel
   → Same file = sequential (no [P])
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001, T002...)
6. Dependencies: Tests before implementation, schemas before validation
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Phase 3.1: Setup & Database Schema
- [ ] T001 [P] Create SpeakerPartnershipTable in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/SpeakerPartnershipTable.kt
- [ ] T002 [P] Create SpeakerPartnershipEntity in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/SpeakerPartnershipEntity.kt
- [ ] T003 [P] Generate agenda_response.schema.json in server/application/src/main/resources/schemas/agenda_response.schema.json
- [ ] T004 [P] Generate session.schema.json in server/application/src/main/resources/schemas/session.schema.json
- [ ] T005 [P] Generate speaker.schema.json in server/application/src/main/resources/schemas/speaker.schema.json
- [ ] T006 [P] Generate speaker_partnership_response.schema.json in server/application/src/main/resources/schemas/speaker_partnership_response.schema.json
- [ ] T007 [P] Update partnership_detail.schema.json with speakers array in server/application/src/main/resources/schemas/partnership_detail.schema.json

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
**Use existing mock factory functions or create them if needed for test data setup**
- [ ] T008 [P] Contract test GET /orgs/{orgSlug}/events/{eventSlug}/agenda in server/application/src/test/kotlin/fr/devlille/partners/connect/events/api/AgendaRoutesContractTest.kt
- [ ] T009 [P] Contract test POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/speakers/{speakerId} in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/api/SpeakerPartnershipRoutesContractTest.kt
- [ ] T010 [P] Contract test DELETE /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/speakers/{speakerId} in same file as T009
- [ ] T011 [P] Contract test enhanced GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId} with speakers in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/api/PartnershipRoutesContractTest.kt
- [ ] T012 [P] Create mockSpeaker factory in server/application/src/test/kotlin/fr/devlille/partners/connect/testing/MockFactories.kt
- [ ] T013 [P] Create mockSession factory in same file as T012
- [ ] T014 [P] Create mockSpeakerPartnership factory in same file as T012
- [ ] T015 [P] Integration test import and attachment workflow in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/SpeakerPartnershipIntegrationTest.kt
- [ ] T016 [P] Integration test error handling scenarios in server/application/src/test/kotlin/fr/devlille/partners/connect/agenda/AgendaErrorHandlingTest.kt
- [ ] T017 [P] Integration test authorization boundaries in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/SpeakerPartnershipAuthorizationTest.kt

## Phase 3.3: Core Implementation (ONLY after tests are failing)
- [ ] T018 [P] SpeakerPartnershipRepository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/SpeakerPartnershipRepository.kt
- [ ] T019 [P] SpeakerPartnership domain model in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/SpeakerPartnership.kt
- [ ] T020 [P] Session domain model in server/application/src/main/kotlin/fr/devlille/partners/connect/events/domain/Session.kt
- [ ] T021 [P] Speaker domain model in server/application/src/main/kotlin/fr/devlille/partners/connect/events/domain/Speaker.kt
- [ ] T022 [P] AgendaResponse domain model in server/application/src/main/kotlin/fr/devlille/partners/connect/events/domain/AgendaResponse.kt
- [ ] T023 SpeakerPartnershipRepositoryExposed implementation in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/SpeakerPartnershipRepositoryExposed.kt
- [ ] T024 Configure SpeakerPartnershipTable in SchemaUtils.createMissingTablesAndColumns in server/application/src/main/kotlin/fr/devlille/partners/connect/App.kt
- [ ] T025 Configure SpeakerPartnershipRepository in Koin dependency injection in server/application/src/main/kotlin/fr/devlille/partners/connect/App.kt

## Phase 3.4: API Routes Implementation
- [ ] T026 POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/speakers/{speakerId} endpoint in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/api/SpeakerPartnershipRoutes.kt
- [ ] T027 DELETE /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/speakers/{speakerId} endpoint in same file as T026
- [ ] T028 Configure SpeakerPartnershipRoutes in Application.configure() in server/application/src/main/kotlin/fr/devlille/partners/connect/App.kt
- [ ] T029 Enhance GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId} to include speakers in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/api/PartnershipRoutes.kt
- [ ] T030 Enhance GET /orgs/{orgSlug}/events/{eventSlug}/agenda endpoint in server/application/src/main/kotlin/fr/devlille/partners/connect/events/api/EventAgendaRoutes.kt

## Phase 3.5: Schema & API Documentation
- [ ] T031 [P] Add schema components for Session, Speaker, AgendaResponse, SpeakerPartnershipResponse in server/application/src/main/resources/openapi/openapi.yaml
- [ ] T032 [P] Add POST attach speaker operation documentation in same file as T031
- [ ] T033 [P] Add DELETE detach speaker operation documentation in same file as T031
- [ ] T034 [P] Update GET partnership details operation to include speakers in same file as T031
- [ ] T035 [P] Update GET agenda operation with enhanced response schema in same file as T031

## Phase 3.6: Quality & Polish
- [ ] T036 [P] Add KDoc documentation for all new public APIs and domain classes in SpeakerPartnership, Session, Speaker, AgendaResponse classes
- [ ] T037 [P] Verify 80% minimum test coverage for speaker-partnership functionality using Jacoco reports
- [ ] T038 [P] Run ktlint formatting and detekt static analysis with zero violations across all modified files
- [ ] T039 [P] Validate JSON schema correctness using npm run validate from project root
- [ ] T040 [P] Integration test complete organizer workflow from quickstart scenarios in server/application/src/test/kotlin/fr/devlille/partners/connect/integration/OrganizerWorkflowTest.kt

## Dependencies
- Database setup (T001-T002) before any repository implementation (T023)
- JSON schemas (T003-T007) before contract tests (T008-T011)
- Mock factories (T012-T014) before contract tests (T008-T011)
- Contract tests (T008-T011) before implementation (T018-T022)
- Domain models (T018-T022) before repository implementation (T023)
- Repository implementation (T023) before API routes (T026-T030)
- Schema components (T031) before API operations documentation (T032-T035)
- Core implementation (T018-T030) before quality verification (T036-T040)

## Parallel Example
```
# Launch T003-T007 together (JSON schema generation):
Task: "Generate agenda_response.schema.json"
Task: "Generate session.schema.json"
Task: "Generate speaker.schema.json"
Task: "Generate speaker_partnership_response.schema.json"
Task: "Update partnership_detail.schema.json with speakers array"

# Launch T008-T017 together (contract tests and integration tests):
Task: "Contract test GET agenda endpoint"
Task: "Contract test POST attach speaker"
Task: "Contract test DELETE detach speaker"
Task: "Contract test enhanced partnership details"
Task: "Create mockSpeaker factory"
Task: "Create mockSession factory"
Task: "Create mockSpeakerPartnership factory"
Task: "Integration test import and attachment workflow"
Task: "Integration test error handling scenarios"
Task: "Integration test authorization boundaries"
```

## Notes
- [P] tasks = different files, no dependencies
- Verify contract tests fail before implementing
- Use existing mock factory pattern (mockCompany, mockEvent, mockPartnership)
- Use JSON schema validation: call.receive<T>(schema) instead of manual validation
- Follow existing repository pattern: no cross-repository dependencies, notifications in route layer
- Use AuthorizedOrganisationPlugin for all org-protected routes
- Database operations use Exposed ORM with UUIDTable/UUIDEntity pattern
- OpenAPI documentation references external JSON schema files
- Implement OpenPlanner error handling with transaction rollback per research decisions
- Commit after each task
- Focus on minimal API surface: attach/detach only, speakers integrated into partnership details

## Task Generation Rules
*Applied during main() execution*

1. **From Contracts**:
   - agenda_response.schema.json → GET agenda contract test
   - speaker_partnership_response.schema.json → POST/DELETE speaker attachment tests
   - partnership_detail.schema.json → enhanced partnership details test
   
2. **From Data Model**:
   - SpeakerPartnership → domain model and repository tasks
   - Session, Speaker, AgendaResponse → domain model tasks
   - SpeakerPartnershipTable → database schema tasks
   
3. **From Quickstart Scenarios**:
   - Import and attachment workflow → integration test task
   - Error handling scenarios → error handling test task  
   - Authorization boundaries → authorization test task

4. **Ordering**:
   - Setup → Tests → Models → Services → Endpoints → Documentation → Quality
   - Dependencies block parallel execution

## Validation Checklist
*GATE: Checked by main() before returning*

- [x] All contracts have corresponding tests (T008-T011)
- [x] All entities have model creation tasks (T018-T022)
- [x] JSON schema generation for all request/response types (T003-T007)
- [x] Contract tests use mock factory functions (T012-T014)
- [x] All tests come before implementation (Phase 3.2 before 3.3)
- [x] Repository layer follows clean architecture (T018, T023)
- [x] API routes use AuthorizedOrganisationPlugin for org protection (T026-T030)
- [x] OpenAPI documentation includes schema components (T031-T035)
- [x] Parallel tasks are truly independent and modify different files
- [x] Each task specifies exact file path for implementation
- [x] Integration tests validate complete user workflows (T015-T017, T040)