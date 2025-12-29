# Tasks: Enhanced Sponsoring Options with Four Option Types

**Input**: Design documents from `/specs/007-improve-sponsoring-options/`
**Prerequisites**: plan.md (required), research.md, data-model.md, contracts/

## Execution Flow (main)
```
1. Load plan.md from feature directory
   → Tech stack: Kotlin JVM 21, Ktor 3.0+, Exposed ORM, kotlinx-serialization
   → Structure: Backend-focused extension, polymorphic sealed classes
2. Load optional design documents:
   → data-model.md: SponsoringOption sealed class, PartnershipOption sealed class, enum descriptors
   → contracts/: API endpoints for option creation, partnership creation with selections
   → research.md: Polymorphic sealed class strategy, enum-based type descriptors
3. Generate tasks by category:
   → Setup: Database schema extensions with nullable columns
   → Tests: Contract tests for each endpoint, integration tests for quickstart scenarios
   → Core: Sealed class models, enum descriptors, repository extensions
   → Integration: Route updates with discriminated unions, validation logic
   → Quality: HTTP route integration tests, performance validation, documentation
4. Apply task rules:
   → Different files = mark [P] for parallel
   → Same file = sequential (no [P])
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001, T002...)
6. Generate dependency graph
7. Create parallel execution examples
8. SUCCESS: 32 tasks ready for execution
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

## Path Conventions
- **Backend**: `server/application/src/main/kotlin/fr/devlille/partners/connect/`
- **Tests**: `server/application/src/test/kotlin/`
- Database tables in `infrastructure/db/`
- Domain models in `domain/`
- Routes in `infrastructure/api/`

## Phase 3.1: Setup & Schema
- [x] T001 [P] Extend SponsoringOptionsTable with new columns in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringOptionsTable.kt
- [x] T002 [P] Create SelectableValuesTable for typed_selectable options in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SelectableValuesTable.kt
- [x] T003 [P] Extend PartnershipOptionsTable with selection columns in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipOptionsTable.kt

## Phase 3.2: Tests First (TDD) ✅ COMPLETE - Tests failing as required
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
**Use existing mock factory functions or create them if needed for test data setup**
- [x] T004 [P] Contract test POST /orgs/{orgSlug}/events/{eventSlug}/options with mock factory setup in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/CreateSponsoringOptionTest.kt
- [x] T005 [P] Contract test PUT /orgs/{orgSlug}/events/{eventSlug}/options/{optionId} with mock factory setup in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/UpdateSponsoringOptionTest.kt
- [x] T006 [P] Contract test GET /orgs/{orgSlug}/events/{eventSlug}/options/{optionId} with mock factory setup in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/GetSponsoringOptionTest.kt
- [x] T007 [P] Contract test POST /events/{eventSlug}/partnerships with mock factory setup in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/CreatePartnershipTest.kt
- [x] T008 [P] Contract test GET /events/{eventSlug}/partnerships/{partnershipId} with mock factory setup in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/GetPartnershipTest.kt

## Phase 3.3: Core Implementation ✅ CORE MODELS COMPLETE
- [x] T009 [P] Create enum descriptors (QuantitativeDescriptor, NumberDescriptor, SelectableDescriptor) in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/OptionDescriptors.kt
- [x] T010 [P] Convert SponsoringOption to polymorphic sealed class in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/SponsoringOption.kt
- [x] T011 [P] Convert SponsoringOptionWithTranslations to polymorphic sealed class in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/SponsoringOptionWithTranslations.kt
- [x] T012 [P] Create PartnershipOption polymorphic sealed class in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipOption.kt
- [x] T013 Create database entities for new tables (SelectableValueEntity) in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SelectableValueEntity.kt
- [ ] T014 Extend SponsoringOptionEntity with new nullable columns in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringOptionEntity.kt
- [ ] T015 Extend PartnershipOptionEntity with selection columns in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipOptionEntity.kt

## Phase 3.4: Schema & API Documentation
- [ ] T016 Generate JSON schemas for polymorphic request/response types (CreateSponsoringOptionExtended.json, SponsoringOptionExtended.json, RegisterPartnershipExtended.json, PartnershipExtended.json) in server/application/src/main/resources/schemas/ and update openapi.yaml to reference these schemas

## Phase 3.5: Repository Extensions
- [ ] T017 Update SponsoringOptionRepository for polymorphic creation in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/SponsoringOptionRepository.kt
- [ ] T018 Update SponsoringOptionRepository for polymorphic retrieval in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/SponsoringOptionRepository.kt
- [ ] T019 Update PartnershipRepository for option selections in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepository.kt
- [ ] T020 Add SelectableValueRepository for managing selectable values in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/SelectableValueRepository.kt

## Phase 3.6: API Routes
- [ ] T021 Update SponsoringRoutes for polymorphic option creation using call.receive<T>(schema) in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt
- [ ] T022 Update SponsoringRoutes for polymorphic option retrieval in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt
- [ ] T023 Update PartnershipRoutes for partnership creation with selections using call.receive<T>(schema) in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt
- [ ] T024 Update PartnershipRoutes for partnership retrieval with selections in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt

## Phase 3.7: Business Logic & Validation
- [ ] T025 [P] Implement zero quantity exclusion logic (FR-018) in partnership creation in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipValidation.kt
- [ ] T026 [P] Add positive integer validation for typed_quantitative quantities (FR-020) in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipValidation.kt
- [ ] T027 [P] Implement deletion protection for referenced options (FR-013) in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/SponsoringOptionRepository.kt
- [ ] T028 [P] Implement selectable value deletion protection (FR-019) in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/SelectableValueRepository.kt
- [ ] T029 Implement linear scaling price calculation (FR-017) in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PricingCalculation.kt

## Phase 3.8: Quality & Polish
- [ ] T030 [P] Performance tests ensuring <2s HTTP route response times in server/application/src/test/kotlin/fr/devlille/partners/connect/performance/PolymorphicOptionsRoutePerformanceTest.kt
- [ ] T031 [P] KDoc documentation for all polymorphic sealed classes and enum descriptors
- [ ] T032 Database migration strategy with backward compatibility verification in server/application/src/main/kotlin/fr/devlille/partners/connect/internal/migration/

## Dependencies
- Schema tasks (T001-T003) before entity tasks (T013-T015)
- Tests (T004-T008) before implementation (T009-T024)
- T009 (enums) blocks T010-T012 (sealed classes)
- T010-T012 (domain models) block T016 (JSON schemas)
- T016 (JSON schemas) blocks T017-T020 (repositories) and T021-T024 (routes)
- T017-T020 (repositories) block T021-T024 (routes)
- T021-T024 (routes) block T025-T029 (business logic - routes must exist first)
- Core implementation before quality (T030-T032)

## Parallel Example
```bash
# Launch T004-T008 together (contract tests):
Task: "Contract test POST /orgs/{orgSlug}/events/{eventSlug}/options with mock factory setup in CreateSponsoringOptionTest.kt"
Task: "Contract test PUT /orgs/{orgSlug}/events/{eventSlug}/options/{optionId} with mock factory setup in UpdateSponsoringOptionTest.kt"
Task: "Contract test GET /orgs/{orgSlug}/events/{eventSlug}/options/{optionId} with mock factory setup in GetSponsoringOptionTest.kt"
Task: "Contract test POST /events/{eventSlug}/partnerships with mock factory setup in CreatePartnershipTest.kt"
Task: "Contract test GET /events/{eventSlug}/partnerships/{partnershipId} with mock factory setup in GetPartnershipTest.kt"

# Launch T009-T012 together (sealed classes):
Task: "Create enum descriptors (QuantitativeDescriptor, NumberDescriptor, SelectableDescriptor) in OptionDescriptors.kt"
Task: "Convert SponsoringOption to polymorphic sealed class in SponsoringOption.kt"
Task: "Convert SponsoringOptionWithTranslations to polymorphic sealed class in SponsoringOptionWithTranslations.kt"
Task: "Create PartnershipOption polymorphic sealed class in PartnershipOption.kt"
```

## Notes
- [P] tasks = different files, no dependencies
- Verify contract tests fail before implementing
- Use existing mock factory functions or create them for test data setup
- Use @JsonClassDiscriminator("type") for polymorphic serialization
- Enum descriptors use @SerialName for snake_case JSON
- Database columns are nullable for backward compatibility
- Contract tests focus on API schema validation, not business logic
- JSON schemas enable call.receive<T>(schema) validation without manual Kotlin validation code
- OpenAPI specification references JSON schemas to avoid duplication
- Business logic tasks (T025-T029) implement critical functional requirements from spec.md
- Performance tests (T030) must test HTTP route response times per constitution requirements
- Deletion protection tasks (T027-T028) must throw ConflictException for StatusPages handling

## Task Generation Rules
*Applied during main() execution*

1. **From Contracts**:
   - POST /orgs/{orgSlug}/events/{eventSlug}/options → T004 contract test + T021 implementation
   - PUT /orgs/{orgSlug}/events/{eventSlug}/options/{optionId} → T005 contract test + T022 implementation
   - GET /orgs/{orgSlug}/events/{eventSlug}/options/{optionId} → T006 contract test + T022 implementation
   - POST /events/{eventSlug}/partnerships → T007 contract test + T023 implementation
   - GET /events/{eventSlug}/partnerships/{partnershipId} → T008 contract test + T024 implementation
   
2. **From Data Model**:
   - SponsoringOption sealed class → T010 model creation + T016 JSON schema generation
   - SponsoringOptionWithTranslations sealed class → T011 model creation + T016 JSON schema generation
   - PartnershipOption sealed class → T012 model creation + T016 JSON schema generation
   - Enum descriptors → T009 model creation + T016 JSON schema generation
   - SelectableValuesTable → T002 schema creation + T015 entity creation
   
3. **From Mock Data Setup**:
   - Use existing mock factory functions for test data creation
   - Create mock factories if they don't exist for test entities

4. **Ordering**:
   - Setup (T001-T003) → Tests (T004-T008) → Models (T009-T012) → Entities (T013-T015) → Schemas (T016) → Repositories (T017-T020) → Routes (T021-T024) → Business Logic (T025-T029) → Quality (T030-T032)
   - Dependencies prevent parallel execution within same file

## Validation Checklist
*GATE: Checked by main() before returning*

- [x] All 5 contract endpoints have corresponding tests (T004-T008)
- [x] All 4 sealed class entities have model tasks (T009-T012)
- [x] JSON schema generation task for polymorphic types (T016)
- [x] All critical business logic requirements covered (T025-T029)
- [x] Contract tests use mock factory functions for data setup
- [x] All tests come before implementation (T004-T008 before T009-T029)
- [x] Parallel tasks truly independent (different files)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] Constitutional compliance: HTTP route performance tests, Exposed ORM dual-layer, StatusPages exception handling