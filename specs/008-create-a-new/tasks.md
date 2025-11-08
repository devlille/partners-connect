# Tasks: Public Partnership Information Endpoint

**Input**: Design documents from `/specs/008-create-a-new/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/partnership-details-api.md, quickstart.md

## Execution Flow (main)
```
1. Load plan.md from feature directory ✅
   → Extract: Kotlin/JVM 21, Ktor 2.x, Exposed ORM, PostgreSQL
   → Project structure: server/application/src/main/kotlin/fr/devlille/partners/connect/
2. Load design documents ✅:
   → data-model.md: Extract PartnershipDetail, DetailedPartnershipResponse models
   → contracts/partnership-details-api.md: GET /events/{eventSlug}/partnerships/{partnershipId}
   → research.md: Multi-repository orchestration, domain model reuse strategy
   → quickstart.md: Test scenarios for validation
3. Generate tasks by category:
   → Tests: contract test for GET endpoint, integration scenarios
   → Core: domain models, repository method, mapper, route implementation  
   → Integration: JSON schema, OpenAPI documentation
   → Quality: validation scenarios, error handling
4. Apply task rules:
   → Different files = mark [P] for parallel
   → Same file = sequential (no [P])
   → Tests before implementation (TDD)
5. Task numbering: T001-T018
6. Dependencies: Tests → Models → Repository → Route → Documentation
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
Web application structure with Kotlin backend:
- **Backend**: `server/application/src/main/kotlin/fr/devlille/partners/connect/`
- **Tests**: `server/application/src/test/kotlin/fr/devlille/partners/connect/`
- **Schemas**: `server/application/src/main/resources/schemas/`

## Phase 3.1: Setup
- [ ] T001 [P] Verify existing Kotlin module dependencies for Ktor Server, Exposed ORM, Kotlinx Serialization in server/build.gradle.kts
- [ ] T002 [P] Validate ktlint and detekt configuration compliance for partnership module

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
**Use existing mock factory functions (mockPartnership, mockCompany, mockEvent) for test data setup**
- [ ] T003 [P] Contract test GET /events/{eventSlug}/partnerships/{partnershipId} with mock factory setup in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipDetailedGetRouteTest.kt
- [ ] T004 [P] Integration test partnership retrieval with full process status in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipDetailIntegrationTest.kt

## Phase 3.3: Core Implementation (ONLY after tests are failing)
- [ ] T005 [P] PartnershipDetail domain model with @SerialName annotations in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipDetail.kt
- [ ] T006 [P] DetailedPartnershipResponse domain model in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/DetailedPartnershipResponse.kt  
- [ ] T007 [P] PartnershipProcessStatus domain model with workflow enums in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipProcessStatus.kt
- [ ] T008 Add getByIdDetailed method to PartnershipRepository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt
- [ ] T009 [P] PartnershipEntityDetailMapper with billing integration in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/mappers/PartnershipEntityDetailMapper.kt
- [ ] T010 Implement getByIdDetailed in PartnershipRepositoryExposed with BillingsTable JOIN in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt
- [ ] T011 GET /events/{eventSlug}/partnerships/{partnershipId} route with three-repository orchestration in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt

## Phase 3.4: Schema & API Documentation  
- [ ] T012 Generate detailed_partnership_response.schema.json referencing existing company.schema.json and event_with_organisation.schema.json in server/application/src/main/resources/schemas/
- [ ] T013 Update OpenAPI specification with new endpoint and component schema references in documentation/openapi/openapi.yaml

## Phase 3.5: Integration & Validation
- [ ] T014 Implement route parameter validation (eventSlug, partnershipId.toUUID) and error handling via existing StatusPages plugin
- [ ] T015 Add database query optimization with partnership-event association validation and proper JOIN indexing
- [ ] T016 Request/response logging with structured correlation IDs for new endpoint

## Phase 3.6: Quality & Polish
- [ ] T017 [P] Quickstart validation scenarios: valid partnership retrieval, error cases (invalid UUID, non-existent entities, mismatched associations) from specs/008-create-a-new/quickstart.md
- [ ] T018 [P] KDoc documentation for new domain models (PartnershipDetail, DetailedPartnershipResponse, PartnershipProcessStatus) and repository methods

## Dependencies
- Tests (T003-T004) before implementation (T005-T011)
- T005-T007 (domain models) before T009 (mapper) and T008 (repository interface)
- T008 (repository interface) before T010 (repository implementation)
- T009 (mapper) and T010 (repository) before T011 (route implementation)
- T011 (route) before T012-T013 (documentation)
- T014-T016 (integration) depend on T011 (route implementation)
- Implementation before polish (T017-T018)

## Parallel Example
```bash
# Launch T005-T007 together (different domain model files):
Task: "PartnershipDetail domain model in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipDetail.kt"
Task: "DetailedPartnershipResponse domain model in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/DetailedPartnershipResponse.kt"  
Task: "PartnershipProcessStatus domain model in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipProcessStatus.kt"

# Launch T001-T002 together (setup tasks):
Task: "Verify existing Kotlin module dependencies in server/build.gradle.kts"
Task: "Validate ktlint and detekt configuration compliance"
```

## Notes
- [P] tasks = different files, no dependencies
- Contract tests must fail before implementing domain models
- Use existing mock factory functions: mockPartnership(), mockCompany(), mockEvent()
- Apply @SerialName annotations for snake_case JSON serialization (e.g., @SerialName("contact_name"))
- Use lowercase enum values with @SerialName (e.g., @SerialName("pending") for PENDING)
- Multi-repository orchestration: fetch from PartnershipRepository, CompanyRepository, EventRepository separately
- Reuse existing Company and EventWithOrganisation domain models
- JSON schema references existing schemas to avoid duplication
- Leverage existing PartnershipEntity.validatedPack extension for business logic
- BillingsTable JOIN for payment status integration
- Commit after each task completion

## Task Generation Rules Applied

1. **From Contracts**:
   - partnership-details-api.md → T003 contract test [P]
   - GET endpoint → T011 route implementation

2. **From Data Model**:
   - PartnershipDetail → T005 model creation [P]
   - DetailedPartnershipResponse → T006 model creation [P]
   - PartnershipProcessStatus → T007 model creation [P]
   - Repository method → T008 interface, T010 implementation
   - Mapper → T009 entity transformation [P]

3. **From Quickstart Scenarios**:
   - Test scenarios → T004 integration test [P], T017 validation [P]
   - Error cases → T014 validation and error handling

4. **Ordering Applied**:
   - Setup (T001-T002) → Tests (T003-T004) → Models (T005-T007) → Repository (T008-T010) → Route (T011) → Documentation (T012-T013) → Integration (T014-T016) → Polish (T017-T018)

## Validation Checklist Verified
- [✅] Contract partnership-details-api.md has corresponding test T003
- [✅] All entities (PartnershipDetail, DetailedPartnershipResponse, PartnershipProcessStatus) have model tasks T005-T007  
- [✅] JSON schema generation task T012 for response types
- [✅] Contract test T003 uses existing mock factory functions
- [✅] All tests (T003-T004) come before implementation (T005-T011)
- [✅] Parallel tasks [P] are truly independent (different files)
- [✅] Each task specifies exact file path
- [✅] No [P] task modifies same file as another [P] task