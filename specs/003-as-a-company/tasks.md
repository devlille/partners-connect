# Tasks: Job Offers Management for Companies

**Input**: Design documents from `/specs/003-as-a-company/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

**Feature**: Enable company owners to create, update, delete and retrieve job offers attached to their companies with REST API endpoints, domain repository, Exposed database entities, and comprehensive testing.

**Tech Stack**: Kotlin/JVM 21, Ktor 3.0, Exposed ORM, PostgreSQL, Koin DI, H2 in-memory for tests

## Phase 3.1: Setup & Preparation
- [ ] T001 Create domain model classes (CreateJobOffer, UpdateJobOffer, JobOfferResponse) in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/`
- [ ] T002 Create CompanyJobOfferRepository interface in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/CompanyJobOfferRepository.kt`
- [ ] T003 [P] Create JSON schemas for validation in `server/application/src/main/resources/schemas/create_job_offer.schema.json`
- [ ] T004 [P] Create JSON schemas for validation in `server/application/src/main/resources/schemas/update_job_offer.schema.json`

## Phase 3.2: Database Layer (TDD - Tests First) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
- [ ] T005 [P] Contract test POST /companies/{companyId}/job-offers in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyJobOfferRoutesCreateTest.kt`
- [ ] T006 [P] Contract test GET /companies/{companyId}/job-offers in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyJobOfferRoutesListTest.kt`
- [ ] T007 [P] Contract test GET /companies/{companyId}/job-offers/{jobOfferId} in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyJobOfferRoutesGetTest.kt`
- [ ] T008 [P] Contract test PUT /companies/{companyId}/job-offers/{jobOfferId} in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyJobOfferRoutesUpdateTest.kt`
- [ ] T009 [P] Contract test DELETE /companies/{companyId}/job-offers/{jobOfferId} in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyJobOfferRoutesDeleteTest.kt`
- [ ] T010 [P] Repository integration tests with H2 database in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/application/CompanyJobOfferRepositoryExposedTest.kt`

## Phase 3.3: Database Implementation (ONLY after tests are failing)
- [ ] T011 [P] Create CompanyJobOfferTable Exposed table in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyJobOfferTable.kt`
- [ ] T012 [P] Create CompanyJobOfferEntity Exposed entity in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyJobOfferEntity.kt`
- [ ] T013 CompanyJobOfferRepositoryExposed implementation with create method in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/application/CompanyJobOfferRepositoryExposed.kt`
- [ ] T014 CompanyJobOfferRepositoryExposed findById method implementation (same file as T013)
- [ ] T015 CompanyJobOfferRepositoryExposed findByCompany method with pagination (same file as T013)
- [ ] T016 CompanyJobOfferRepositoryExposed update method implementation (same file as T013)
- [ ] T017 CompanyJobOfferRepositoryExposed delete method implementation (same file as T013)

## Phase 3.4: API Routes Implementation
- [ ] T018 Add job offer routes to CompanyRoutes.kt: POST /companies/{companyId}/job-offers endpoint in `server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyRoutes.kt`
- [ ] T019 Add GET /companies/{companyId}/job-offers endpoint (same file as T018)
- [ ] T020 Add GET /companies/{companyId}/job-offers/{jobOfferId} endpoint (same file as T018)
- [ ] T021 Add PUT /companies/{companyId}/job-offers/{jobOfferId} endpoint (same file as T018)
- [ ] T022 Add DELETE /companies/{companyId}/job-offers/{jobOfferId} endpoint (same file as T018)
- [ ] T023 Add CompanyJobOfferRepository to Koin DI configuration in `server/application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/di/ApplicationModule.kt`

## Phase 3.5: Validation & Error Handling
- [ ] T024 [P] Add business logic validation for CreateJobOffer (URL format, date validation, experience range) in domain models
- [ ] T025 [P] Add business logic validation for UpdateJobOffer (same validations as create, partial update logic) in domain models  
- [ ] T026 Request parameter extraction with proper error handling using existing StringValues extensions in routes
- [ ] T027 JSON schema validation integration in route handlers using existing schema validation patterns

## Phase 3.6: Integration Tests
- [ ] T028 [P] End-to-end integration test for job offer creation scenario in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/integration/JobOfferCreationIntegrationTest.kt`
- [ ] T029 [P] End-to-end integration test for job offer listing with pagination in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/integration/JobOfferListingIntegrationTest.kt`
- [ ] T030 [P] End-to-end integration test for job offer CRUD operations in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/integration/JobOfferCrudIntegrationTest.kt`
- [ ] T031 [P] Authorization integration tests (company ownership validation) in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/integration/JobOfferAuthorizationTest.kt`

## Phase 3.7: Quality & Polish
- [ ] T032 [P] Add KDoc documentation for all public APIs (CompanyJobOfferRepository interface and domain models)
- [ ] T033 [P] Database query optimization: add indexes for company_id and ensure efficient pagination queries
- [ ] T034 [P] Add structured logging with correlation IDs for all job offer operations in route handlers
- [ ] T035 [P] Update OpenAPI specification with job offer endpoints in `server/application/src/main/resources/openapi/openapi.yaml`
- [ ] T036 Unit test coverage verification achieving minimum 80% for all new classes
- [ ] T037 [P] Performance testing to ensure <2 second response times for all job offer operations
- [ ] T038 [P] Code quality verification: ktlint formatting and detekt static analysis with zero violations

## Dependencies
**Phase Dependencies:**
- T001-T004 (Setup) must complete before all other phases
- T005-T010 (Tests) must complete and FAIL before T011-T023 (Implementation)
- T011-T017 (Database) must complete before T018-T023 (Routes)
- T018-T023 (Routes) must complete before T024-T027 (Validation)
- All implementation must complete before T028-T031 (Integration Tests)
- All core work must complete before T032-T038 (Quality)

**File Dependencies:**
- T013-T017 are sequential (same CompanyJobOfferRepositoryExposed.kt file)
- T018-T022 are sequential (same CompanyRoutes.kt file)
- All other [P] tasks can run in parallel

## Parallel Execution Examples

### Phase 3.1 Setup (can run in parallel):
```bash
Task: "Create domain models in companies/domain/"
Task: "Create repository interface in CompanyJobOfferRepository.kt"  
Task: "Create JSON schema create_job_offer.schema.json"
Task: "Create JSON schema update_job_offer.schema.json"
```

### Phase 3.2 Contract Tests (can run in parallel):
```bash
Task: "Contract test POST endpoint in CompanyJobOfferRoutesCreateTest.kt"
Task: "Contract test GET list endpoint in CompanyJobOfferRoutesListTest.kt"
Task: "Contract test GET single endpoint in CompanyJobOfferRoutesGetTest.kt" 
Task: "Contract test PUT endpoint in CompanyJobOfferRoutesUpdateTest.kt"
Task: "Contract test DELETE endpoint in CompanyJobOfferRoutesDeleteTest.kt"
Task: "Repository integration tests in CompanyJobOfferRepositoryExposedTest.kt"
```

### Phase 3.3 Database Layer (mixed parallel/sequential):
```bash
# Parallel:
Task: "Create CompanyJobOfferTable.kt"
Task: "Create CompanyJobOfferEntity.kt"
# Then Sequential (same file):
Task: "Implement repository create method"
Task: "Implement repository findById method"
Task: "Implement repository findByCompany method"
Task: "Implement repository update method" 
Task: "Implement repository delete method"
```

## Validation Checklist
*GATE: Checked before considering tasks complete*

- [x] All 5 contract endpoints have corresponding test tasks (T005-T009)
- [x] All 3 main entities have model tasks (JobOffer, CreateJobOffer, UpdateJobOffer) 
- [x] All contract tests come before implementation tasks
- [x] All [P] tasks operate on different files (no conflicts)
- [x] Each task specifies exact file path
- [x] Repository methods broken into separate tasks for same file
- [x] Routes implementation broken into separate tasks for same file
- [x] Integration tests cover main user scenarios from quickstart
- [x] Quality tasks address constitutional requirements (80% coverage, <2s response, ktlint/detekt)

## Constitutional Compliance Verification
- **Code Quality**: ktlint/detekt validation (T038), KDoc documentation (T032)
- **Testing Strategy**: 80% coverage (T036), H2 integration tests (T010, T028-T031), contract tests (T005-T009)
- **Clean Architecture**: Repository pattern with domain interfaces, proper module boundaries
- **API Consistency**: OpenAPI documentation (T035), consistent error handling (T026-T027)
- **Performance**: <2 second response time validation (T037), database optimization (T033)

**Total Tasks**: 38 tasks across 7 phases with clear dependencies and parallel execution opportunities