# Tasks: Multi-Language Sponsoring Pack and Option Management for Organizers

**Input**: Design documents from `/specs/002-for-sponsoring-pack/`
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
- **Web app**: `server/application/src/main/kotlin/fr/devlille/partners/connect/`
- Paths target the sponsoring domain module within clean architecture structure

## Phase 3.1: Setup
- [ ] T001 Verify existing Kotlin/Ktor dependencies support multi-language translation features in server/build.gradle.kts
- [ ] T002 [P] Configure ktlint formatting compliance for new domain entity files
- [ ] T003 [P] Configure detekt static analysis rules for new repository methods

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
- [ ] T004 [P] Contract test GET /orgs/{orgSlug}/events/{eventSlug}/packs with multi-language response validation in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt
- [ ] T005 [P] Contract test GET /orgs/{orgSlug}/events/{eventSlug}/options with multi-language response validation in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringOptionRoutesTest.kt
- [ ] T006 [P] Integration test organizer packs endpoint returns all translations without Accept-Language header in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/OrganizerSponsoringIntegrationTest.kt
- [ ] T007 [P] Integration test organizer options endpoint returns all translations without Accept-Language header in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/OrganizerSponsoringIntegrationTest.kt
- [ ] T008 [P] Integration test backward compatibility - public endpoints still require Accept-Language header in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/PublicEndpointCompatibilityTest.kt

## Phase 3.3: Core Implementation (ONLY after tests are failing)
- [ ] T009 [P] Create SponsoringPackWithTranslations domain entity in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/SponsoringPackWithTranslations.kt
- [ ] T010 [P] Create SponsoringOptionWithTranslations domain entity in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/SponsoringOptionWithTranslations.kt
- [ ] T011 [P] Create OptionTranslation domain entity in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/OptionTranslation.kt
- [ ] T012 Add findPacksByEventWithAllTranslations method to PackRepository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/PackRepository.kt
- [ ] T013 Add getByIdWithAllTranslations method to PackRepository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/PackRepository.kt
- [ ] T014 Add listOptionsByEventWithAllTranslations method to OptionRepository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/OptionRepository.kt
- [ ] T015 [P] Create toDomainWithAllTranslations extension function for SponsoringPackEntity in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/mappers/SponsoringPackEntityWithTranslations.ext.kt
- [ ] T016 [P] Create toDomainWithAllTranslations extension function for SponsoringOptionEntity in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/mappers/SponsoringOptionEntityWithTranslations.ext.kt
- [ ] T017 Implement findPacksByEventWithAllTranslations in PackRepositoryExposed in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/PackRepositoryExposed.kt
- [ ] T018 Implement getByIdWithAllTranslations in PackRepositoryExposed in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/PackRepositoryExposed.kt
- [ ] T019 Implement listOptionsByEventWithAllTranslations in OptionRepositoryExposed in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/OptionRepositoryExposed.kt
- [ ] T020 Update GET /orgs/{orgSlug}/events/{eventSlug}/packs route to remove Accept-Language header requirement and use new repository method in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt
- [ ] T021 Update GET /orgs/{orgSlug}/events/{eventSlug}/options route to remove Accept-Language header requirement and use new repository method in server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt

## Phase 3.4: Integration
- [ ] T022 Optimize translation loading queries to avoid N+1 problems in new repository implementations
- [ ] T023 Add structured logging with correlation IDs for new organizer endpoints
- [ ] T024 Verify H2 in-memory database compatibility with new translation loading logic
- [ ] T025 Test database query performance with multiple translations per option

## Phase 3.5: Quality & Polish
- [ ] T026 [P] Unit tests for new domain entities achieving 80% minimum coverage in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/domain/
- [ ] T027 [P] Unit tests for new repository methods with H2 database testing in server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/application/
- [ ] T028 Performance tests ensuring <2s response times for organizer endpoints with multiple translations
- [ ] T029 [P] Update OpenAPI documentation for modified organizer endpoints in server/application/src/main/resources/openapi/openapi.yaml
- [ ] T030 [P] Add KDoc documentation for all new domain entities and repository methods
- [ ] T031 Verify ktlint formatting compliance for all new Kotlin files
- [ ] T032 Verify detekt static analysis passes for all new code with zero violations
- [ ] T033 [P] Update frontend TypeScript types if needed in front/utils/api.ts and front/types/partner.ts

## Dependencies
- Setup (T001-T003) before everything
- Tests (T004-T008) before implementation (T009-T021)
- Domain entities (T009-T011) before repository interfaces (T012-T014)
- Repository interfaces (T012-T014) before implementations (T017-T019)
- Extension functions (T015-T016) before repository implementations (T017-T019)
- Repository implementations (T017-T019) before route updates (T020-T021)
- Core implementation (T009-T021) before integration (T022-T025)
- Integration (T022-T025) before quality & polish (T026-T033)

## Parallel Example
```bash
# Launch T004-T008 together (different test files):
Task: "Contract test GET /orgs/{orgSlug}/events/{eventSlug}/packs with multi-language response validation in SponsoringPackRoutesTest.kt"
Task: "Contract test GET /orgs/{orgSlug}/events/{eventSlug}/options with multi-language response validation in SponsoringOptionRoutesTest.kt"
Task: "Integration test organizer packs endpoint in OrganizerSponsoringIntegrationTest.kt"
Task: "Integration test organizer options endpoint in OrganizerSponsoringIntegrationTest.kt"
Task: "Integration test backward compatibility in PublicEndpointCompatibilityTest.kt"

# Launch T009-T011 together (different domain entity files):
Task: "Create SponsoringPackWithTranslations domain entity"
Task: "Create SponsoringOptionWithTranslations domain entity"  
Task: "Create OptionTranslation domain entity"

# Launch T015-T016 together (different extension function files):
Task: "Create toDomainWithAllTranslations extension function for SponsoringPackEntity"
Task: "Create toDomainWithAllTranslations extension function for SponsoringOptionEntity"
```

## Testing Strategy
- All new tests use H2 in-memory database for fast execution
- Contract tests validate OpenAPI schema compliance
- Integration tests cover end-to-end organizer workflows from quickstart.md
- Backward compatibility tests ensure public endpoints unchanged
- Performance tests validate constitutional <2s response time requirement

## Notes
- [P] tasks = different files, no dependencies
- Verify tests fail before implementing
- Preserve existing public endpoint behavior (no Accept-Language changes)
- All new code must pass ktlint and detekt with zero violations
- KDoc documentation required for all public APIs per constitution
- Database schema unchanged - leverages existing OptionTranslationsTable