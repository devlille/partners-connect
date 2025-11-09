# Tasks: Provider Management Enhancement

**Input**: Design documents from `/specs/009-as-an-organiser/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/, quickstart.md

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Extract: Kotlin/JVM 21, Ktor, Exposed ORM, PostgreSQL
   → Structure: Backend-only enhancement within existing server
2. Load design documents:
   → data-model.md: ProvidersTable, EventProvidersTable, Provider entity
   → contracts/: 6 API endpoints with JSON schemas
   → quickstart.md: 5 test scenarios for validation
3. Generate tasks by category per constitution:
   → Setup: Database migration, schema creation, mock factories
   → Tests: Contract tests (schema validation), integration tests (business logic)
   → Core: Repository enhancements, service layer, domain models
   → Integration: Route implementations, permission validation
   → Quality: Code coverage, ktlint/detekt, API documentation
4. Apply task rules:
   → Different files = [P] for parallel
   → Database/schema changes sequential
   → Tests before implementation (TDD)
5. Constitutional compliance: 80% coverage, ktlint/detekt, JSON schema validation
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Phase 3.1: Database Schema & Foundation
- [x] T001 Database migration: Add organisation_id to ProvidersTable and created_at to EventProvidersTable. BREAKING CHANGE: DELETE existing providers first, then ADD organisation_id column (non-nullable). SQL: DELETE FROM providers; ALTER TABLE providers ADD COLUMN organisation_id UUID NOT NULL REFERENCES organisations(id); ALTER TABLE event_providers ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW(); in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/db/ProvidersTable.kt and server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/db/EventProvidersTable.kt
- [x] T002 [P] Create missing create_provider.schema.json for POST endpoint request validation in specs/009-as-an-organiser/contracts/create_provider.schema.json
- [x] T003 [P] Create mock factory functions for Provider entities in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/factories/ProviderFactory.kt

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
**Use mock factory functions from T003 for consistent test data setup**
- [x] T004 [P] Contract test POST /orgs/{orgSlug}/providers with create_provider.schema.json validation in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/CreateProviderContractTest.kt
- [x] T005 [P] Contract test PUT /orgs/{orgSlug}/providers/{id} with update_provider.schema.json validation in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/UpdateProviderContractTest.kt
- [x] T006 [P] Contract test DELETE /orgs/{orgSlug}/providers/{id} with error response validation in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/DeleteProviderContractTest.kt
- [x] T007 [P] Contract test POST /orgs/{orgSlug}/events/{eventSlug}/providers with create_by_identifiers.schema.json validation in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/AttachProvidersContractTest.kt
- [x] T008 [P] Contract test DELETE /orgs/{orgSlug}/events/{eventSlug}/providers with create_by_identifiers.schema.json validation in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/DetachProvidersContractTest.kt
- [x] T009 [P] Contract test GET /orgs/{orgSlug}/events/{eventSlug}/providers with paginated_provider.schema.json validation in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/ListEventProvidersContractTest.kt
- [x] T010 [P] Contract test GET /providers with paginated_provider.schema.json validation in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/PublicProviderListContractTest.kt
- [ ] T011 [P] Integration test Scenario 1: Organisation-scoped provider creation workflow in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/ProviderCreationIntegrationTest.kt
- [ ] T012 [P] Integration test Scenario 2: Provider event attachment workflow in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/ProviderAttachmentIntegrationTest.kt
- [ ] T013 [P] Integration test Scenario 3: Cascading provider deletion workflow in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/ProviderDeletionIntegrationTest.kt
- [ ] T014 [P] Integration test Scenario 4: Public provider listing with organisation filtering in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/PublicProviderListingIntegrationTest.kt
- [x] T015 [P] Integration test Scenario 5: Permission boundary testing across organisations in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/ProviderPermissionIntegrationTest.kt

## Phase 3.3: Domain Layer Implementation (ONLY after tests are failing)
- [x] T016 [P] Enhanced Provider domain model with organisation relationship in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/domain/Provider.kt

## Phase 3.4: Repository Layer Enhancement
- [x] T017 Enhanced ProvidersRepository with findByOrganisation, create, update, delete methods in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/db/ProviderRepositoryExposed.kt
- [x] T018 Enhanced EventProvidersRepository with attachProviders, detachProviders, findByEvent methods in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/db/EventProviderRepositoryExposed.kt

## Phase 3.5: API Layer Implementation
- [x] T019 POST /orgs/{orgSlug}/providers endpoint with AuthorizedOrganisationPlugin in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/ProviderRoutes.kt
- [x] T020 PUT /orgs/{orgSlug}/providers/{id} endpoint with organisation validation in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/ProviderRoutes.kt
- [x] T021 DELETE /orgs/{orgSlug}/providers/{id} endpoint with event detachment check in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/ProviderRoutes.kt
- [x] T022 POST /orgs/{orgSlug}/events/{eventSlug}/providers bulk attachment endpoint in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/EventProviderRoutes.kt
- [x] T023 DELETE /orgs/{orgSlug}/events/{eventSlug}/providers bulk detachment endpoint in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/EventProviderRoutes.kt
- [x] T024 GET /orgs/{orgSlug}/events/{eventSlug}/providers listing endpoint in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/EventProviderRoutes.kt
- [x] T025 GET /providers public listing with organisation filtering in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/PublicProviderRoutes.kt

## Phase 3.6: Integration & Wiring
- [x] T026 Register provider routes in main application routing in server/application/src/main/kotlin/fr/devlille/partners/connect/App.kt
- [x] T027 Add JSON schema validation using call.receive<T>(schema) pattern for all endpoints: create_provider.schema.json for POST, update_provider.schema.json for PUT, create_by_identifiers.schema.json for bulk attach/detach operations in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/
- [x] T028 Database migration execution on startup in server/application/src/main/kotlin/fr/devlille/partners/connect/App.kt

## Phase 3.7: Quality & Polish
- [~] T029 [P] Repository layer tests with H2 database in server/application/src/test/kotlin/fr/devlille/partners/connect/provider/infrastructure/db/ProviderRepositoryTest.kt
- [x] T030 [P] KDoc documentation for Provider domain classes: ProviderEntity, ProvidersTable, EventProvidersTable, Provider (response model), CreateProviderRequest, UpdateProviderRequest, and all public repository methods
- [x] T031 [P] Database query optimization with proper indexing on organisation_id and created_at columns
- [x] T032 ktlint and detekt validation ensuring zero violations: Run ./gradlew ktlintCheck detekt --no-daemon for all provider module files (server/application/src/main/kotlin/fr/devlille/partners/connect/provider/**/*.kt and server/application/src/test/kotlin/fr/devlille/partners/connect/provider/**/*.kt). Fix violations with ./gradlew ktlintFormat --no-daemon
- [x] T033 [P] OpenAPI documentation update with provider endpoints and schema references in server/application/src/main/resources/openapi/documentation.yaml
- [x] T034 [P] Provider deletion validation: Check event attachments exist before deletion and return 409 Conflict if provider is still attached to events in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/ProviderRoutes.kt
- [x] T035 [P] Public provider listing query parameter preservation: Implement existing query parameters (query, sort, direction, page, page_size) plus new org_slug parameter in server/application/src/main/kotlin/fr/devlille/partners/connect/provider/infrastructure/api/PublicProviderRoutes.kt

## Dependencies
- Database setup (T001) blocks all repository work (T017-T018)
- Schema creation (T002) blocks contract tests (T004-T010)
- Mock factories (T003) block all test tasks (T004-T015)
- All tests (T004-T015) before implementation (T016-T025)
- Domain models (T016) before repository enhancements (T017-T018)
- Repository layer (T017-T018) before API implementation (T019-T025)
- API implementation (T019-T025) before integration (T026-T028)
- Core implementation before quality/polish (T029-T035)

## Parallel Execution Examples
```bash
# Phase 3.1: Foundation setup
Task: "Database migration for ProvidersTable and EventProvidersTable"
Task: "Create create_provider.schema.json"  # [P]
Task: "Create Provider mock factories"      # [P]

# Phase 3.2: Contract tests (after T001-T003)
Task: "Contract test POST /orgs/{orgSlug}/providers"           # [P]
Task: "Contract test PUT /orgs/{orgSlug}/providers/{id}"       # [P] 
Task: "Contract test DELETE /orgs/{orgSlug}/providers/{id}"    # [P]
Task: "Contract test POST /orgs/{orgSlug}/events/{eventSlug}/providers"  # [P]
Task: "Contract test DELETE /orgs/{orgSlug}/events/{eventSlug}/providers" # [P]

# Phase 3.3: Domain models (after all tests fail)
Task: "Enhanced Provider domain model"         # [P]

# Phase 3.4: Repository layer (after domain model)
Task: "Enhanced ProvidersRepository methods"   # [P]
Task: "Enhanced EventProvidersRepository methods" # [P]
```

## Test Scenario Mapping
**From quickstart.md scenarios → integration tests:**
- Scenario 1 (Organisation-scoped provider management) → T011
- Scenario 2 (Provider event attachment) → T012  
- Scenario 3 (Cascading provider deletion) → T013
- Scenario 4 (Organisation-filtered public listing) → T014
- Scenario 5 (Permission boundary testing) → T015

## API Endpoint Mapping
**From provider-api.md contracts → implementation tasks:**
- POST /orgs/{orgSlug}/providers → T019
- PUT /orgs/{orgSlug}/providers/{id} → T020
- DELETE /orgs/{orgSlug}/providers/{id} → T021
- POST /orgs/{orgSlug}/events/{eventSlug}/providers → T022
- DELETE /orgs/{orgSlug}/events/{eventSlug}/providers → T023
- GET /orgs/{orgSlug}/events/{eventSlug}/providers → T024
- GET /providers → T025

## Schema Files Coverage
**From contracts/ directory → validation implementation:**
- provider.schema.json → GET endpoint responses
- create_provider.schema.json → POST endpoint requests (T002 creates this)
- update_provider.schema.json → PUT endpoint requests
- paginated_provider.schema.json → GET list endpoint responses
- create_by_identifiers.schema.json → Bulk attachment/detachment requests
- error_response.schema.json → Error response validation

## Constitutional Compliance Checklist
- [ ] Database migration with proper foreign key constraints (T001)
- [ ] JSON schema validation using call.receive<T>(schema) pattern (T027)
- [ ] AuthorizedOrganisationPlugin for permission checks (T019-T024)
- [ ] Repository pattern isolation (T017-T018)
- [ ] 80% minimum test coverage (T029)
- [ ] ktlint/detekt zero violations (T032)
- [ ] OpenAPI documentation updates (T033)
- [ ] Provider deletion validation with event detachment checks (T034)
- [ ] Query parameter preservation in public endpoints (T035)

## Notes
- [P] tasks = different files, no dependencies
- Contract tests MUST fail before implementing endpoints
- Use existing AuthorizedOrganisationPlugin for organisation scoping
- Database migration includes DELETE of existing providers (acceptable breaking change)
- All provider operations are organisation-scoped except public GET /providers
- Event attachment validates provider belongs to same organisation as event
- Provider deletion requires detachment from all events first
- Commit after each task completion