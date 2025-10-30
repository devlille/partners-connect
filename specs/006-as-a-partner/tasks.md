# Tasks: Complete CRUD operations on /companies resource

**Input**: Design documents from `/specs/006-as-a-partner/`
**Prerequisites**: plan.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓, quickstart.md ✓

## Execution Flow Summary
1. **Setup**: CompanyStatus enum and UpdateCompany model
2. **Tests First (TDD)**: Contract and integration tests MUST FAIL before implementation
3. **Core Implementation**: Database schema, repository methods, route handlers
4. **Quality & Polish**: Performance, documentation, validation

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Phase 3.1: Setup
- [ ] T001 [P] Create CompanyStatus enum in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/CompanyStatus.kt
- [ ] T002 [P] Create UpdateCompany data class in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/UpdateCompany.kt
- [ ] T003 [P] Configure ktlint and detekt validation for new domain classes

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
- [ ] T004 [P] Contract test PUT /companies/{id} in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/http/CompanyRoutesUpdateTest.kt
- [ ] T005 [P] Contract test DELETE /companies/{id} in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/http/CompanyRoutesDeleteTest.kt
- [ ] T006 [P] Contract test GET /companies with status filter in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/http/CompanyRoutesListWithStatusTest.kt
- [ ] T007 [P] Integration test company update flow in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/CompanyUpdateIntegrationTest.kt
- [ ] T008 [P] Integration test company soft delete flow in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/CompanySoftDeleteIntegrationTest.kt
- [ ] T009 [P] Integration test status filtering in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/CompanyStatusFilterIntegrationTest.kt
- [ ] T010 [P] Repository tests for update method in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyRepositoryExposedUpdateTest.kt
- [ ] T011 [P] Repository tests for softDelete method in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyRepositoryExposedSoftDeleteTest.kt
- [ ] T012 [P] Repository tests for listPaginated with status in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyRepositoryExposedStatusFilterTest.kt

## Phase 3.3: Core Implementation (ONLY after tests are failing)
- [ ] T013 Add status column to CompaniesTable in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompaniesTable.kt
- [ ] T014 Update CompanyEntity with status field in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyEntity.kt
- [ ] T015 Add update method to CompanyRepository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/CompanyRepository.kt
- [ ] T016 Add softDelete method to CompanyRepository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/CompanyRepository.kt
- [ ] T017 Enhance listPaginated with status parameter in CompanyRepository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/domain/CompanyRepository.kt
- [ ] T018 Implement update method in CompanyRepositoryExposed in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyRepositoryExposed.kt
- [ ] T019 Implement softDelete method in CompanyRepositoryExposed in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyRepositoryExposed.kt
- [ ] T020 Implement enhanced listPaginated in CompanyRepositoryExposed in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyRepositoryExposed.kt
- [ ] T021 Add PUT /companies/{id} endpoint in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/http/CompanyRoutes.kt
- [ ] T022 Add DELETE /companies/{id} endpoint in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/http/CompanyRoutes.kt
- [ ] T023 Enhance GET /companies with status parameter in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/http/CompanyRoutes.kt
- [ ] T024 [P] Add input validation for UpdateCompany in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/http/validation/UpdateCompanyValidation.kt
- [ ] T025 [P] Add error handling for company not found scenarios in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/http/CompanyErrorHandling.kt

## Phase 3.4: Integration
- [ ] T026 Connect new endpoints to dependency injection in server/application/src/main/kotlin/fr/devlille/partners/connect/App.kt
- [ ] T027 Add database migration for status column with ACTIVE default in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyMigration.kt
- [ ] T028 [P] Update Company response serialization with status field in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/http/CompanyResponse.kt
- [ ] T029 [P] Add structured logging with correlation IDs for CRUD operations in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/logging/CompanyOperationLogger.kt

## Phase 3.5: Quality & Polish
- [ ] T030 [P] Unit tests achieving 95% coverage for new domain classes in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/domain/
- [ ] T031 Performance tests ensuring <2s response times for CRUD operations in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/performance/CompanyCrudPerformanceTest.kt
- [ ] T032 [P] OpenAPI documentation update for new endpoints in server/application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/http/CompanyApiDocumentation.kt
- [ ] T033 [P] KDoc documentation for CompanyStatus enum and UpdateCompany model
- [ ] T034 [P] Database query optimization with proper indexing on status column
- [ ] T035 [P] Code duplication removal and Kotlin idiom compliance verification
- [ ] T036 [P] Backwards compatibility verification for existing GET /companies endpoint

## Dependencies
```
Setup Phase:
T001, T002, T003 (parallel - different files)

Tests Phase (all must fail before implementation):
T004-T012 (parallel - different test files)
↓
Core Implementation Phase:
T013 → T014 (same file sequence)
T015 → T016 → T017 (same file sequence)
T018 → T019 → T020 (same file sequence)
T021 → T022 → T023 (same file sequence)
T024, T025 (parallel - different files)

Integration Phase:
T026 (depends on T015-T025)
T027, T028, T029 (parallel - different files)

Quality Phase:
T030-T036 (parallel - different areas)
```

## Parallel Execution Examples

**Setup Phase (T001-T003)**:
```bash
# All can run simultaneously - different files
Task T001: "Create CompanyStatus enum"
Task T002: "Create UpdateCompany data class" 
Task T003: "Configure linting"
```

**Tests Phase (T004-T012)**:
```bash
# All test files can be created in parallel
Task T004: "Contract test PUT /companies/{id}"
Task T005: "Contract test DELETE /companies/{id}"
Task T006: "Contract test GET /companies with status"
Task T007: "Integration test update flow"
Task T008: "Integration test soft delete"
Task T009: "Integration test status filtering"
Task T010: "Repository update tests"
Task T011: "Repository soft delete tests"
Task T012: "Repository status filter tests"
```

**Quality Phase (T030-T036)**:
```bash
# Independent quality improvements
Task T030: "Unit test coverage"
Task T032: "OpenAPI documentation"
Task T033: "KDoc documentation"
Task T034: "Database optimization"
Task T035: "Code compliance"
Task T036: "Backwards compatibility"
```

## Validation Checklist
*GATE: Checked before execution*

- [x] All contracts have corresponding tests (T004-T006)
- [x] All entities have model tasks (T001-T002)
- [x] All tests come before implementation (T004-T012 → T013-T025)
- [x] Parallel tasks truly independent (different files/modules)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] TDD enforced: tests must fail before implementation
- [x] Constitutional requirements met: <2s response, public endpoints, soft delete
- [x] Backwards compatibility preserved: enhanced existing methods

## Notes
- **Public endpoints**: No authentication required as specified
- **Soft delete**: Preserves relationships, filters by status
- **Default behavior**: All companies shown unless status filter applied
- **Database**: H2 in-memory for tests, PostgreSQL for production
- **Response time**: <2s constitutional requirement for all operations
- **Test isolation**: Each test uses transaction rollback pattern