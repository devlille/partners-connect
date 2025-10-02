# Tasks: Event Sponsoring Packs Public API

**Input**: Design documents from `/specs/001-create-new-get/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Tech stack: Kotlin 2.1.21, Ktor 3.2.0, Exposed 1.0.0-beta-2, Koin 4.1.0
   → Structure: Clean architecture with domain/application/infrastructure layers
2. Load design documents:
   → data-model.md: EventPackRepository interface, reuse SponsoringPack/SponsoringOption entities
   → contracts/: OpenAPI spec for GET /events/{eventSlug}/sponsoring/packs endpoint
   → research.md: Reuse existing queries, database schema, and translation infrastructure
3. Generate tasks by category (per constitution):
   → Setup: Domain interface, DI configuration
   → Tests: Contract tests, integration tests, database tests (H2)
   → Core: Repository implementation, route handler
   → Integration: Route registration, Accept-Language header handling
   → Quality: Code coverage, performance testing, API documentation
4. Task rules applied:
   → Different files = [P] for parallel execution
   → Tests before implementation (TDD approach)
   → Domain interfaces before implementations
5. Tasks numbered T001-T015
6. Dependencies: Setup → Tests → Implementation → Integration → Quality
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
Based on plan.md structure: Kotlin backend with clean architecture
- Domain: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/`
- Application: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/`
- Infrastructure: `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/`
- Tests: `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/`

## Phase 3.1: Setup
- [ ] T001 Create EventPackRepository domain interface in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/EventPackRepository.kt
- [ ] T002 [P] Configure Koin DI binding for EventPackRepository in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/bindings/SponsoringModule.kt

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
- [ ] T003 [P] Contract test GET /events/{eventSlug}/sponsoring/packs OpenAPI compliance in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/EventPackContractTest.kt
- [ ] T004 [P] Integration test successful pack retrieval with Accept-Language=en in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/EventPackRoutesTest.kt
- [ ] T005 [P] Integration test successful pack retrieval with Accept-Language=fr in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/EventPackRoutesTest.kt
- [ ] T006 [P] Integration test 404 error for non-existent event slug in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/EventPackRoutesTest.kt
- [ ] T007 [P] Integration test empty pack list for event with no packs in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/EventPackRoutesTest.kt
- [ ] T008 [P] Unit test EventPackRepositoryExposed with H2 database in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/EventPackRepositoryExposedTest.kt

## Phase 3.3: Core Implementation (ONLY after tests are failing)
- [ ] T009 EventPackRepositoryExposed implementation in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/EventPackRepositoryExposed.kt
- [ ] T010 Add public route handler GET /events/{eventSlug}/sponsoring/packs in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt
- [ ] T011 Accept-Language header processing and validation in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt
- [ ] T012 Error handling for NotFoundException and proper HTTP status codes in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt

## Phase 3.4: Integration
- [ ] T013 Register EventPackRepository binding in Koin DI container via SponsoringModule.kt
- [ ] T014 Register new public route in main routing configuration to make endpoint accessible

## Phase 3.5: Quality & Polish
- [ ] T015 [P] KDoc documentation for EventPackRepository interface and EventPackRepositoryExposed class with usage examples and error handling details

## Dependencies
- T001 (domain interface) before T002 (DI config), T009 (implementation)
- Tests T003-T008 before implementation T009-T012
- T009 (repository) before T010-T012 (route handlers)
- T002, T013 (DI binding) before T014 (routing)
- All implementation before polish T015

## Parallel Example
```bash
# Launch T003-T008 together (all different test files):
# Terminal 1
Task: "Contract test GET /events/{eventSlug}/sponsoring/packs OpenAPI compliance in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/EventPackContractTest.kt"

# Terminal 2  
Task: "Integration test successful pack retrieval with Accept-Language=en in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/EventPackRoutesTest.kt"

# Terminal 3
Task: "Unit test EventPackRepositoryExposed with H2 database in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/EventPackRepositoryExposedTest.kt"
```

## Task Details

### T001: EventPackRepository Domain Interface
Create domain interface with single method:
```kotlin
interface EventPackRepository {
    /**
     * Retrieves all sponsoring packages for a public event by slug
     * @param eventSlug The unique event identifier
     * @param language Language code from Accept-Language header for translations
     * @return List of sponsoring packages with localized options
     * @throws NotFoundException if event does not exist
     */
    fun findPublicPacksByEvent(eventSlug: String, language: String): List<SponsoringPack>
}
```

### T009: EventPackRepositoryExposed Implementation
Implement using existing database queries and translation logic:
- Reuse EventEntity.findBySlug() for event resolution
- Leverage SponsoringPackEntity.listPacksByEvent() and .toDomain() mapper
- Handle language-based translations via existing OptionTranslationsTable
- Follow transaction patterns from existing PackRepositoryExposed

### T010: Public Route Handler
Add new route in sponsoringRoutes() function:
```kotlin
route("/events/{eventSlug}/sponsoring/packs") {
    get {
        val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException("Missing eventSlug")
        val acceptLanguage = call.request.headers["Accept-Language"]?.lowercase() ?: "en"
        val packs = eventPackRepository.findPublicPacksByEvent(eventSlug, acceptLanguage)
        call.respond(HttpStatusCode.OK, packs)
    }
}
```

## Validation Checklist
- [x] All contracts have tests (T003)
- [x] All entities reused (SponsoringPack, SponsoringOption)
- [x] All endpoints implemented (T010)
- [x] Database integration tested (T008)
- [x] Language support tested (T004, T005)
- [x] Error handling tested (T006)
- [x] Performance considerations (reuse existing optimized queries)
- [x] Constitution compliance (clean architecture, testing, documentation)

## Notes
- [P] tasks = different files, no dependencies
- Verify all tests fail before implementing (TDD approach)
- Reuse existing SponsoringPack and SponsoringOption entities
- Leverage existing database schema and translation infrastructure
- Follow constitutional requirements for code quality and testing
- Commit after each task completion
- Target <2 second response time per constitutional performance requirement