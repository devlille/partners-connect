# Tasks: Job Offer Promotion with Approval Workflow

**Input**: Design documents from `/specs/004-i-would-like/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/api-endpoints.md, quickstart.md

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Extract: Kotlin/Ktor stack, Exposed ORM, modular domain architecture
2. Load design documents:
   → data-model.md: PromotedJobOffer entity → model tasks
   → contracts/api-endpoints.md: 6 endpoints → contract test tasks
   → research.md: Notification patterns, permission validation → setup tasks
   → quickstart.md: 8 test scenarios → integration test tasks
3. Generate tasks by category (per constitution):
   → Setup: Database schema, JSON schemas, notification templates
   → Tests: Contract tests (6 endpoints), integration tests (8 scenarios)
   → Core: Exposed entities, repository interfaces, repository implementations
   → Routes: Company domain routes, partnership domain routes
   → Integration: Notification integration, OpenAPI documentation
   → Quality: Code coverage, performance, KDoc documentation
4. Apply task rules:
   → Different files = mark [P] for parallel
   → Same file = sequential (no [P])
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001, T002...)
6. Generate dependency graph
7. Create parallel execution examples
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

---

## Phase 3.1: Setup & Schema

- [x] T001 [P] Create database schema table in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyJobOfferPromotionsTable.kt` with Exposed UUIDTable extending pattern, datetime() columns, enumerationByName for status, proper FK cascades (job_offer CASCADE, partnership/event NO_ACTION), and indexes per data-model.md

- [x] T002 [P] Create PromotionStatus enum in `server/application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/db/PromotionStatusEnum.kt` with PENDING, APPROVED, DECLINED values

- [x] T003 [P] Create Exposed entity class in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyJobOfferPromotionEntity.kt` extending UUIDEntity with companion object, property delegation to table columns, and referencedOn relationships per constitution Database Schema Standards

- [x] T004 [P] Create JSON schema for promotion request in `server/application/src/main/resources/schemas/promote_job_offer.schema.json` with job_offer_id UUID field validation

- [x] T005 [P] Create JSON schema for approval request in `server/application/src/main/resources/schemas/approve_job_offer_promotion.schema.json` (empty object schema)

- [x] T006 [P] Create JSON schema for decline request in `server/application/src/main/resources/schemas/decline_job_offer_promotion.schema.json` with optional reason string field

- [x] T007 [P] Create Mailjet email templates for job_offer_promoted event in `server/application/src/main/resources/notifications/email/job_offer_promoted/` with content.en.html, content.fr.html, header.en.txt, header.fr.txt

- [x] T008 [P] Create Mailjet email templates for job_offer_approved event in `server/application/src/main/resources/notifications/email/job_offer_approved/` with content.en.html, content.fr.html, header.en.txt, header.fr.txt

- [x] T009 [P] Create Mailjet email templates for job_offer_declined event in `server/application/src/main/resources/notifications/email/job_offer_declined/` with content.en.html, content.fr.html, header.en.txt, header.fr.txt

- [x] T010 [P] Create Slack notification templates for job_offer_promoted in `server/application/src/main/resources/notifications/slack/job_offer_promoted/` with en.md, fr.md

- [x] T011 [P] Create Slack notification templates for job_offer_approved in `server/application/src/main/resources/notifications/slack/job_offer_approved/` with en.md, fr.md

- [x] T012 [P] Create Slack notification templates for job_offer_declined in `server/application/src/main/resources/notifications/slack/job_offer_declined/` with en.md, fr.md

---

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3

**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### Contract Tests (Different endpoints = Parallel)

- [x] T013 [P] Contract test POST /companies/{companyId}/partnerships/{partnershipId}/promote in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/PromoteJobOfferRouteTest.kt` covering success (201), validation errors (400), event ended (403), not found (404), duplicate conflict (409), verify Mailjet/Slack notification calls

- [x] T014 [P] Contract test GET /companies/{companyId}/job-offers/{jobOfferId}/promotions in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/ListJobOfferPromotionsRouteTest.kt` covering success with pagination (200), partnership_id filter, not found (404), verify embedded job offer data

- [x] T015 [P] Contract test GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers in `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/ListPartnershipJobOffersRouteTest.kt` covering success (200), status filter, pagination, not found (404)

- [x] T016 [P] Contract test GET /orgs/{orgSlug}/events/{eventSlug}/job-offers in `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/ListEventJobOffersRouteTest.kt` covering success with JWT auth (200), status filter, forbidden without canEdit (403), unauthorized without JWT (401), not found (404)

- [x] T017 [P] Contract test POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers/{promotionId}/approve in `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/ApproveJobOfferPromotionRouteTest.kt` covering success (200), AuthorizedOrganisationPlugin validation (401/403), promotion not found (404), invalid status transition (409), verify Mailjet/Slack approval notifications

- [x] T018 [P] Contract test POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers/{promotionId}/decline in `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/DeclineJobOfferPromotionRouteTest.kt` covering success with optional reason (200), AuthorizedOrganisationPlugin validation (401/403), promotion not found (404), invalid status (409), verify Mailjet/Slack decline notifications

### Integration Tests (Different scenarios = Parallel)

- [x] T019 [P] Integration test for successful promotion workflow in `server/application/src/test/kotlin/fr/devlille/partners/connect/integration/JobOfferPromotionSuccessTest.kt` testing Scenario 1 from quickstart.md: promote → verify pending status → verify dual notifications sent

- [x] T020 [P] Integration test for event ended validation in `server/application/src/test/kotlin/fr/devlille/partners/connect/integration/JobOfferPromotionEventEndedTest.kt` testing Scenario 2 from quickstart.md: attempt promotion on expired event → verify 403 Forbidden

- [x] T021 [P] Integration test for approval workflow in `server/application/src/test/kotlin/fr/devlille/partners/connect/integration/JobOfferPromotionApprovalTest.kt` testing Scenario 3 from quickstart.md: create pending promotion → approve with organizer JWT → verify approved status, reviewed_at/reviewed_by set → verify approval notifications sent

- [x] T022 [P] Integration test for decline workflow in `server/application/src/test/kotlin/fr/devlille/partners/connect/integration/JobOfferPromotionDeclineTest.kt` testing Scenario 4 from quickstart.md: create pending promotion → decline with optional reason → verify declined status → verify decline notifications with reason

- [x] T023 [P] Integration test for re-promotion of declined offers in `server/application/src/test/kotlin/fr/devlille/partners/connect/integration/JobOfferPromotionRePromotionTest.kt` testing Scenario 5 from quickstart.md: decline promotion → re-promote same job offer → verify status reset to pending → verify re-promotion notifications

- [x] T024 [P] Integration test for duplicate promotion prevention in `server/application/src/test/kotlin/fr/devlille/partners/connect/integration/JobOfferPromotionDuplicateTest.kt` testing Scenario 6 from quickstart.md: promote job offer → attempt duplicate while pending → verify 409 Conflict → attempt duplicate while approved → verify 409 Conflict

- [x] T025 [P] Integration test for unauthorized approval attempt in `server/application/src/test/kotlin/fr/devlille/partners/connect/integration/JobOfferPromotionUnauthorizedTest.kt` testing Scenario 7 from quickstart.md: attempt approve without JWT → verify 401 → attempt approve without canEdit permission → verify 403 via AuthorizedOrganisationPlugin

- [x] T026 [P] Integration test for cascade delete behavior in `server/application/src/test/kotlin/fr/devlille/partners/connect/integration/JobOfferPromotionCascadeDeleteTest.kt` testing Scenario 8 from quickstart.md: create promotion → delete job offer → verify promotion cascade deleted → create promotion → delete partnership → verify promotion preserved (NO ACTION)

---

## Phase 3.3: Core Implementation (ONLY after tests are failing)

### Domain Layer - Request/Response Models

- [x] T027 [P] Create company promotion request/response models in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/JobOfferPromotionModels.kt` with PromoteJobOfferRequest (job_offer_id), JobOfferPromotionResponse (id, job_offer_id, partnership_id, event_slug, status, promoted_at, reviewed_at, reviewed_by, job_offer embedded, created_at, updated_at), using @Serializable and kotlinx-serialization

- [x] T028 [P] Create partnership approval/decline request/response models in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipJobOfferModels.kt` with ApproveJobOfferRequest (empty), DeclineJobOfferRequest (reason: String?), using @Serializable

### Repository Interfaces

- [x] T029 [P] Create company job offer promotion repository interface in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/CompanyJobOfferPromotionRepository.kt` with methods: promoteJobOffer(companyId, partnershipId, jobOfferId) -> UUID throws NotFoundException/ConflictException/ForbiddenException, listJobOfferPromotions(companyId, jobOfferId, partnershipId?, page, pageSize) -> PaginatedResponse, per constitution exception handling pattern

- [x] T030 [P] Create partnership job offer repository interface in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipJobOfferRepository.kt` with methods: listPartnershipJobOffers(orgSlug, eventSlug, partnershipId, status?, page, pageSize), listEventJobOffers(orgSlug, eventSlug, status?, page, pageSize), approvePromotion(promotionId, reviewerId) -> JobOfferPromotionResponse, declinePromotion(promotionId, reviewerId, reason?) -> JobOfferPromotionResponse, all throwing appropriate exceptions per StatusPages contract

### Repository Implementations (Exposed)

- [x] T031 Create company job offer promotion repository Exposed implementation in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/application/CompanyJobOfferPromotionRepositoryExposed.kt` implementing promoteJobOffer with: validate company owns job offer, validate partnership exists and belongs to company, validate event hasn't ended (FR-030), check existing promotion (upsert if declined per FR-031, conflict if pending/approved), create/update promotion record with status=PENDING and promotedAt=now, return promotion ID, throw exceptions (not null/boolean returns)

- [x] T032 Extend company promotion repository implementation in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/application/CompanyJobOfferPromotionRepositoryExposed.kt` implementing listJobOfferPromotions with: validate company ownership, query promotions with optional partnership filter, apply pagination, join with job offer entity for embedded data, map to response DTOs with event_slug (not event_id per constitution Response Schema Standards), return PaginatedResponse

- [x] T033 Create partnership job offer repository Exposed implementation in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipJobOfferRepositoryExposed.kt` implementing listPartnershipJobOffers and listEventJobOffers with: validate organization/event/partnership existence, query promotions with status filter if provided, apply pagination, join job offers for embedded data, map to DTOs with event_slug, return paginated results

- [x] T034 Extend partnership repository implementation in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipJobOfferRepositoryExposed.kt` implementing approvePromotion with: find promotion by ID (throw NotFoundException if missing), validate status=pending (throw ConflictException if not), update status=APPROVED, set reviewedAt=now and reviewedBy=reviewerId, updatedAt=now, return full promotion response with embedded job offer and event_slug

- [x] T035 Extend partnership repository implementation in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipJobOfferRepositoryExposed.kt` implementing declinePromotion with: find promotion, validate status=pending, update status=DECLINED with reviewedAt, reviewedBy, optional reason stored (if schema supports), return promotion response

### Notification Integration

- [x] T036 Update notification variables in `server/application/src/main/kotlin/fr/devlille/partners/connect/notifications/domain/NotificationVariables.kt` adding job_offer_promoted, job_offer_approved, job_offer_declined events with required variables: job_offer_title, job_offer_url, company_name, event_name, partnership_contact_emails (Mailjet), organization_slack_channel, decline_reason (optional)

- [x] T037 ~~Add notification calls in repositories~~ **MOVED TO ROUTES**: Per constitution clean architecture, notification calls will be implemented in route handlers (T040, T045, T046), NOT in repository layer. Repositories handle data access only; routes orchestrate cross-cutting concerns.

- [x] T038 ~~Add notification calls in repositories~~ **MOVED TO ROUTES**: Notification integration will happen in route layer where NotificationRepository is injected alongside domain repositories.

- [x] T039 ~~Add notification calls in repositories~~ **MOVED TO ROUTES**: Routes will fetch domain data, create NotificationVariables, and send notifications per constitution pattern (see PartnershipRoutes.kt reference).

---

## Phase 3.4: Route Implementation

### Company Routes (Public endpoints - NO AuthorizedOrganisationPlugin)

- [x] T040 Add POST /companies/{companyId}/partnerships/{partnershipId}/promote endpoint in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyRoutes.kt` with: extract companyUUID and partnershipUUID from path parameters using StringValues extensions (NO manual null checks), receive<PromoteJobOfferRequest>, call repository.promoteJobOffer(), respond 201 with promotion ID, NO try-catch (StatusPages handles exceptions per constitution), NO AuthorizedOrganisationPlugin (public endpoint)

- [x] T041 Add GET /companies/{companyId}/job-offers/{jobOfferId}/promotions endpoint in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyRoutes.kt` with: extract path parameters, parse query params (partnership_id?, page, page_size), call repository.listJobOfferPromotions(), respond 200 with paginated results, NO authentication required per constitution public endpoint pattern

### Partnership Routes (Protected with AuthorizedOrganisationPlugin)

- [x] T042 Create new PartnershipJobOfferRoutes.kt in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipJobOfferRoutes.kt` with route structure: route("/orgs/{orgSlug}/events/{eventSlug}") { install(AuthorizedOrganisationPlugin) /* routes here */ } per constitution Authorization Pattern, Koin DI for repository injection

- [x] T043 Add GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers endpoint in PartnershipJobOfferRoutes.kt with: extract orgSlug and eventSlug using StringValues extensions, parse query params (status?, page, page_size), call repository.listPartnershipJobOffers(), respond 200, AuthorizedOrganisationPlugin validates permission automatically

- [x] T044 Add GET /orgs/{orgSlug}/events/{eventSlug}/job-offers endpoint in PartnershipJobOfferRoutes.kt with: extract path parameters, parse query params (status?, page, page_size), call repository.listEventJobOffers(), respond 200 with paginated results, plugin ensures canEdit=true before handler execution

- [x] T045 ADD POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers/{promotionId}/approve endpoint in PartnershipJobOfferRoutes.kt with: extract promotionUUID, receive empty request body, get reviewerId from authenticated user context (call.token → getUserInfo), call repository.approvePromotion(promotionId, reviewerId), respond 200 with full promotion response, NO manual permission checks (plugin handles)

- [x] T046 Add POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers/{promotionId}/decline endpoint in PartnershipJobOfferRoutes.kt with: extract promotionUUID, receive<DeclineJobOfferRequest>, get reviewerId from user context, call repository.declinePromotion(promotionId, reviewerId, request.reason), respond 200 with promotion response

- [x] T047 Register PartnershipJobOfferRoutes in main application routing configuration in `server/application/src/main/kotlin/fr/devlille/partners/connect/App.kt` adding route registration to existing routing block

---

## Phase 3.5: Integration & Documentation

- [x] T048 Update OpenAPI specification in `server/application/src/main/resources/openapi/openapi.yaml` adding all 6 endpoints with: complete parameter definitions (path, query), request/response schema references to JSON schema files, security configuration (company endpoints use `security: - {}` for public, partnership endpoints use `security: - bearerAuth: []`), operation IDs, tags, error responses (400, 403, 404, 409) per contracts/api-endpoints.md and constitution OpenAPI Configuration Standards

- [x] T049 Add Koin DI module registration in `server/application/src/main/kotlin/fr/devlille/partners/connect/App.kt` or relevant DI config file with: single<CompanyJobOfferPromotionRepository> { CompanyJobOfferPromotionRepositoryExposed() }, single<PartnershipJobOfferRepository> { PartnershipJobOfferRepositoryExposed() }

- [x] T050 Run database migration or schema update to create company_job_offer_promotions table with all columns, indexes, and foreign key constraints as defined in CompanyJobOfferPromotionsTable (if using manual migrations, generate SQL; if using Exposed SchemaUtils, ensure table creation in startup)

---

## Phase 3.6: Quality & Polish

- [x] T051 [P] Add KDoc documentation to all public APIs in repository interfaces (`CompanyJobOfferPromotionRepository.kt`, `PartnershipJobOfferRepository.kt`) documenting method contracts, parameter requirements, exception conditions, and business rules per constitution Documentation Principles

- [x] T052 [P] Add KDoc documentation to Exposed entity and table classes (`CompanyJobOfferPromotionEntity.kt`, `CompanyJobOfferPromotionsTable.kt`) documenting schema design, relationship semantics, cascade behavior, and index rationale

- [x] T053 [P] Verify integration test coverage meets 80% minimum for all new files using Kotlin test coverage tools, ensuring all endpoints, repository methods, and notification integrations are tested via HTTP routes per constitution Testing Strategy

- [x] T054 [P] Run ktlint formatting check and auto-format all new Kotlin files: `./gradlew ktlintFormat --no-daemon` from server directory

- [x] T055 [P] Run detekt static analysis and resolve all violations: `./gradlew detekt --no-daemon` from server directory, achieving zero violations per constitution Code Quality Standards

- [x] T056 [P] Performance test all endpoints using quickstart.md load test examples ensuring p95 latency < 2000ms for promotion, approval, and list operations, verify database query optimization with EXPLAIN for index usage

- [x] T057 [P] Validate OpenAPI specification with Redocly CLI: `npm run validate` from project root, ensuring zero errors and resolving any warnings for completeness per constitution OpenAPI Validation Requirements

- [x] T058 Review code for Kotlin idiom compliance: verify use of extension functions, data classes, sealed classes where appropriate, null safety, and avoid Java-style patterns (e.g., no Boolean return flags, use exceptions instead per constitution Exception Handling Pattern)

---

## Dependencies

**Critical Path**:
1. Setup (T001-T012) → Tests (T013-T026) → Core (T027-T039) → Routes (T040-T047) → Integration (T048-T050) → Quality (T051-T058)

**Specific Blockers**:
- T001 (Table) blocks T003 (Entity)
- T002 (Enum) blocks T001 (Table uses enum)
- T013-T026 (All tests) MUST COMPLETE and FAIL before T027-T047 (Implementation)
- T027-T028 (Models) block T029-T030 (Repository interfaces)
- T029-T030 (Interfaces) block T031-T035 (Repository implementations)
- T031-T035 (Repository implementations) block T036-T039 (Notification integration)
- T031-T035 (Repositories) block T040-T046 (Route implementations)
- T042 (PartnershipJobOfferRoutes.kt creation) blocks T043-T046 (endpoints in same file)
- T040-T046 (All routes) block T047 (Route registration)
- T048 (OpenAPI) can run after contracts known (parallel with implementation)
- T050 (Migration) can run after T001 (Table defined)

**Parallel Opportunities**:
- T001, T002, T004-T012 can all run in parallel (different files, no dependencies except T002→T001)
- T013-T026 can ALL run in parallel (different test files, independent scenarios)
- T027, T028, T029, T030 can run in parallel (different domain files)
- T051, T052, T053, T054, T055, T056, T057, T058 can run in parallel (polish tasks on different aspects)

---

## Parallel Execution Examples

### Setup Phase (Run after T002 completes):
```bash
# Launch T001, T004-T012 together (11 tasks):
Task: "Create database schema table in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyJobOfferPromotionsTable.kt"
Task: "Create JSON schema for promotion request in server/application/src/main/resources/schemas/promote_job_offer.schema.json"
Task: "Create JSON schema for approval request in server/application/src/main/resources/schemas/approve_job_offer_promotion.schema.json"
Task: "Create JSON schema for decline request in server/application/src/main/resources/schemas/decline_job_offer_promotion.schema.json"
Task: "Create Mailjet email templates for job_offer_promoted in server/application/src/main/resources/notifications/email/job_offer_promoted/"
# ... (all T004-T012)
```

### Test Phase (Run ALL together):
```bash
# Launch T013-T026 together (14 tests):
Task: "Contract test POST /companies/{companyId}/partnerships/{partnershipId}/promote in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/PromoteJobOfferRouteTest.kt"
Task: "Contract test GET /companies/{companyId}/job-offers/{jobOfferId}/promotions in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/ListJobOfferPromotionsRouteTest.kt"
Task: "Integration test for successful promotion workflow in server/application/src/test/kotlin/fr/devlille/partners/connect/integration/JobOfferPromotionSuccessTest.kt"
# ... (all T013-T026)
```

### Domain Models Phase:
```bash
# Launch T027-T030 together (4 tasks):
Task: "Create company promotion request/response models in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/JobOfferPromotionModels.kt"
Task: "Create partnership approval/decline request/response models in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipJobOfferModels.kt"
Task: "Create company job offer promotion repository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/CompanyJobOfferPromotionRepository.kt"
Task: "Create partnership job offer repository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipJobOfferRepository.kt"
```

### Quality Phase:
```bash
# Launch T051-T058 together (8 tasks):
Task: "Add KDoc documentation to repository interfaces"
Task: "Add KDoc documentation to Exposed entity and table classes"
Task: "Verify integration test coverage meets 80% minimum"
Task: "Run ktlint formatting check and auto-format"
Task: "Run detekt static analysis"
Task: "Performance test all endpoints"
Task: "Validate OpenAPI specification with Redocly CLI"
Task: "Review code for Kotlin idiom compliance"
```

---

## Notes

- **[P] tasks**: Can run in parallel (different files, no dependencies)
- **TDD enforcement**: All tests (T013-T026) MUST fail before implementation begins
- **Commit strategy**: Commit after each task completion for rollback safety
- **Same file conflicts**: T031-T032 sequential (same file), T033-T035 sequential (same file), T040-T041 sequential (same file), T043-T046 sequential (same file)
- **Constitution compliance**: AuthorizedOrganisationPlugin on org routes, StatusPages exception handling, datetime() columns, Exposed dual structure, 80% test coverage, zero ktlint/detekt violations
- **Response schema standard**: Use event_slug (not event_id) per constitution Response Schema Standards

---

## Validation Checklist
*GATE: Checked before task execution completion*

- [x] All contracts have corresponding tests (6 endpoints → T013-T018)
- [x] All entities have model tasks (PromotedJobOffer → T001, T003)
- [x] All tests come before implementation (T013-T026 before T027-T047)
- [x] Parallel tasks truly independent (verified file paths)
- [x] Each task specifies exact file path (all tasks include full paths)
- [x] No task modifies same file as another [P] task (T031-T032, T033-T035, T040-T041, T043-T046 are sequential within same files)
- [x] All quickstart scenarios have integration tests (8 scenarios → T019-T026)
- [x] Notification integration included (T036-T039, T007-T012 templates)
- [x] OpenAPI documentation task included (T048)
- [x] Quality tasks enforce constitution standards (T051-T058)

**Total Tasks**: 58 (Setup: 12, Tests: 14, Core: 13, Routes: 8, Integration: 3, Quality: 8)
**Estimated Parallel Groups**: 5 major parallel execution opportunities
**Critical Path Length**: ~15 sequential dependencies
