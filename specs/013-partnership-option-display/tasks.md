# Tasks: Display Partnership-Specific Options

**Input**: Design documents from `/specs/013-partnership-option-display/`  
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/, quickstart.md  
**Tests**: Not explicitly requested - focus on updating existing tests

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `- [ ] [ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Validate prerequisites and environment setup

- [X] T001 Verify Java 21 (Amazon Corretto) installation and Gradle 8.13+ availability
- [X] T002 [P] Run `./gradlew build --no-daemon` from server/ to verify clean build
- [X] T003 [P] Run `npm install` and `npm run validate` from server/ to verify OpenAPI tooling

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain model changes that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Create PartnershipOption sealed class in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipOption.kt with 4 subtypes (TextPartnershipOption, QuantitativePartnershipOption, NumberPartnershipOption, SelectablePartnershipOption) and SelectedValue data class
- [X] T005 Update PartnershipDetail domain model in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipDetail.kt to add currency: String = "EUR" field
- [X] T006 Update PartnershipPack domain model in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/Partnership.kt to replace options field with requiredOptions: List<PartnershipOption>, optionalOptions: List<PartnershipOption>, and add totalPrice: Int field
- [X] T007 Create PartnershipOptionEntity.toDomain() mapper extension in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/mappers/PartnershipOptionEntity.ext.kt to map entities to PartnershipOption with complete descriptions (parentheses format) and pricing
- [X] T008 Run `./gradlew compileKotlin --no-daemon` from server/ to verify domain model changes compile successfully

**Checkpoint**: Foundation ready - domain models complete, user story implementation can now begin

---

## Phase 3: User Story 1 - View Partnership with Complete Option Details (Priority: P1) 🎯 MVP

**Goal**: Display only partnership-specific options (required + selected optional) with complete descriptions and structured data in partnership's language

**Independent Test**: View any partnership detail and verify only selected/required options appear with complete descriptions in correct language

### Implementation for User Story 1

- [X] T009 [US1] Update PartnershipRepository.getByIdDetailed() in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt to document enhanced return type with new fields
- [X] T010 [US1] Implement enhanced getByIdDetailed() in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt to fetch partnership-specific options, map to PartnershipOption domain models with complete descriptions, separate into requiredOptions/optionalOptions lists based on PackOptionsTable, validate translations exist for partnership language (throw ForbiddenException if missing)
- [X] T011 [US1] Update existing PartnershipDetailedGetRouteTest in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipDetailedGetRouteTest.kt to add assertions for currency field, requiredOptions/optionalOptions lists, complete description format (parentheses), and option properties (id, name, description, completeDescription, price, quantity, totalPrice)
- [X] T012 [US1] Update existing mock factories in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/ to populate partnership options data with required and optional options for test partnerships
- [X] T013 [US1] Run `./gradlew test --tests "*PartnershipDetailedGetRouteTest" --no-daemon` from server/ to verify US1 implementation passes

**Checkpoint**: User Story 1 complete - partnership details display partnership-specific options with complete descriptions

---

## Phase 4: User Story 2 - View Complete Partnership Pricing Breakdown (Priority: P2)

**Goal**: Display complete pricing breakdown including pack base price, per-option costs with quantities, and total partnership amount

**Independent Test**: View partnership with mixed option types and verify all pricing is accurate (base price, option totals, grand total)

### Implementation for User Story 2

- [X] T014 [US2] Update PartnershipRepositoryExposed.getByIdDetailed() in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt to calculate pack totalPrice as basePrice + sum(optionalOptions.totalPrice) for each pack (selected, suggestion, validated)
- [X] T015 [P] [US2] Update BillingRoutes.kt in server/application/src/main/kotlin/fr/devlille/partners/connect/billing/infrastructure/api/BillingRoutes.kt to replace partnershipBillingRepository.computePricing() calls with partnershipRepository.getByIdDetailed() and extract pricing from validatedPack
- [X] T016 [P] [US2] Update PartnershipAgreementRepositoryExposed.kt in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipAgreementRepositoryExposed.kt to use partnershipRepository.getByIdDetailed() instead of separate option queries
- [X] T017 [US2] Update existing BillingRoutesTest in server/application/src/test/kotlin/fr/devlille/partners/connect/billing/infrastructure/api/BillingRoutesTest.kt to verify partnershipRepository.getByIdDetailed() is called instead of billing repository, and verify pricing data matches validatedPack values
- [X] T018 [US2] Update existing AgreementRoutesTest in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/AgreementRoutesTest.kt to verify partnershipRepository.getByIdDetailed() is called and complete descriptions are passed to agreement generation
- [X] T019 [US2] Run `./gradlew test --tests "*BillingRoutesTest" --tests "*AgreementRoutesTest" --no-daemon` from server/ to verify US2 implementation passes

**Checkpoint**: User Story 2 complete - pricing breakdown available in partnership details, billing/agreement endpoints use enhanced detail

---

## Phase 5: User Story 3 - Edit Partnership with Correct Options (Priority: P3)

**Goal**: Edit forms pre-populate with partnership-specific options (not all pack options)

**Independent Test**: Open edit form for any partnership and verify options match actual selections

### Implementation for User Story 3

- [X] T020 [US3] Verify frontend auto-regeneration via `pnpm orval` in front/ directory will consume new partnership detail structure from OpenAPI spec
- [X] T021 [US3] Document in quickstart.md that frontend changes are minimal (Orval auto-generates TypeScript types from OpenAPI, components consuming partnership detail will automatically receive new structure)

**Checkpoint**: User Story 3 complete - frontend will consume enhanced partnership detail via auto-generated client

---

## Phase 6: JSON Schemas & OpenAPI Updates

**Purpose**: Schema validation and API documentation updates

- [X] T022 [P] Copy partnership_option.schema.json from specs/013-partnership-option-display/contracts/ to server/application/src/main/resources/schemas/
- [X] T023 [P] Copy partnership_pack.schema.json from specs/013-partnership-option-display/contracts/ to server/application/src/main/resources/schemas/
- [X] T024 [P] Update partnership_detail.schema.json in server/application/src/main/resources/schemas/ to add currency field and reference updated partnership_pack schema
- [X] T025 [P] Copy detailed_partnership_response.schema.json from specs/013-partnership-option-display/contracts/ to server/application/src/main/resources/schemas/
- [X] T026 Update openapi.yaml in server/application/src/main/resources/openapi/ to add partnership_option schema component, update partnership_pack component, update partnership_detail component, and update GET /events/{eventSlug}/partnerships/{partnershipId} response reference
- [X] T027 Run `npm run validate` from server/ to verify OpenAPI spec has zero errors
- [X] T028 Run `npm run bundle` from server/ to generate bundled OpenAPI spec for frontend consumption

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [X] T029 [P] Run `./gradlew ktlintCheck --no-daemon` from server/ to verify code formatting
- [X] T030 [P] Run `./gradlew detekt --no-daemon` from server/ to verify static analysis passes
- [X] T031 Run `./gradlew test --no-daemon` from server/ to verify all tests pass (95+ tests)
- [X] T032 Run `./gradlew build --no-daemon` from server/ to verify full build succeeds
- [X] T033 Manual validation: Start backend with `./gradlew run --no-daemon` and test GET /events/{eventSlug}/partnerships/{partnershipId} endpoint returns enhanced response with currency, totalPrice, requiredOptions, optionalOptions
- [X] T034 Run quickstart.md validation checklist to verify all quality gates pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T001-T003) - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (T004-T008) completion
- **User Story 2 (Phase 4)**: Depends on User Story 1 (T009-T013) completion for pricing data
- **User Story 3 (Phase 5)**: Depends on User Story 1 (T009-T013) completion for API structure
- **JSON Schemas (Phase 6)**: Depends on Foundational (T004-T008) completion, can run in parallel with user stories
- **Polish (Phase 7)**: Depends on all user stories and schemas being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Depends on User Story 1 repository implementation - uses enhanced getByIdDetailed()
- **User Story 3 (P3)**: Depends on User Story 1 API structure - frontend consumes new response format

### Within Each Phase

- **Foundational**: T004-T006 can run in parallel (different domain models), T007 depends on T004-T006, T008 validates all
- **User Story 1**: T009 before T010, T011-T012 in parallel after T010, T013 validates all
- **User Story 2**: T015-T016 in parallel (different files), T014 before T015-T016, T017-T018 in parallel after T015-T016, T019 validates all
- **JSON Schemas**: T022-T025 in parallel (different schema files), T026 after schemas copied, T027-T028 validate
- **Polish**: T029-T030 in parallel, T031-T032 sequential validation, T033-T034 manual validation

### Parallel Opportunities

- **Setup Phase**: T002-T003 can run in parallel
- **Foundational Phase**: T004-T006 can run in parallel (different domain model files)
- **User Story 1**: T011-T012 can run in parallel (test updates in different concerns)
- **User Story 2**: T015-T016 can run in parallel (different route files), T017-T018 can run in parallel (different test files)
- **JSON Schemas Phase**: T022-T025 can all run in parallel (different schema files)
- **Polish Phase**: T029-T030 can run in parallel (independent validation tools)

---

## Parallel Example: User Story 1

```bash
# After T010 completes, launch test updates together:
Task T011: "Update PartnershipDetailedGetRouteTest assertions"
Task T012: "Update mock factories for partnership options"

# Both update different test concerns and can execute simultaneously
```

---

## Parallel Example: User Story 2

```bash
# After T014 completes, launch route updates together:
Task T015: "Update BillingRoutes to use getByIdDetailed"
Task T016: "Update AgreementRepository to use getByIdDetailed"

# Both update different route files and can execute simultaneously

# After T015-T016 complete, launch test updates together:
Task T017: "Update BillingRoutesTest assertions"
Task T018: "Update AgreementRoutesTest assertions"

# Both update different test files and can execute simultaneously
```

---

## Parallel Example: JSON Schemas

```bash
# Launch all schema file copies together:
Task T022: "Copy partnership_option.schema.json"
Task T023: "Copy partnership_pack.schema.json"
Task T024: "Update partnership_detail.schema.json"
Task T025: "Copy detailed_partnership_response.schema.json"

# All operate on different files and can execute simultaneously
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational (T004-T008) - CRITICAL foundation
3. Complete Phase 3: User Story 1 (T009-T013)
4. Complete Phase 6: JSON Schemas (T022-T028) - Required for US1 validation
5. Complete Phase 7: Polish (T029-T034) - Validate US1 works
6. **STOP and VALIDATE**: Test User Story 1 independently - partnership details show correct options with complete descriptions
7. Deploy/demo if ready - MVP delivers core value

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (T001-T008)
2. Add User Story 1 + Schemas → Test independently → Deploy/Demo (T009-T013, T022-T028) - **MVP: Partnership-specific options with complete descriptions**
3. Add User Story 2 → Test independently → Deploy/Demo (T014-T019) - **Increment: Pricing breakdown and billing integration**
4. Add User Story 3 → Test independently → Deploy/Demo (T020-T021) - **Increment: Frontend auto-consumes new structure**
5. Polish → Final validation → Production ready (T029-T034)

### Parallel Team Strategy

With multiple developers:

1. **Team completes Setup + Foundational together** (T001-T008)
2. Once Foundational is done:
   - **Developer A**: User Story 1 implementation (T009-T010)
   - **Developer B**: JSON Schemas (T022-T028) - can start in parallel
   - **Developer C**: Test updates for US1 (T011-T012) - waits for T010
3. After US1 complete:
   - **Developer A**: User Story 2 repository updates (T014)
   - **Developer B**: User Story 2 route updates (T015-T016) - parallel after T014
   - **Developer C**: User Story 2 test updates (T017-T018) - parallel after T015-T016
4. After US2 complete:
   - **Developer A**: User Story 3 (T020-T021)
   - **Developer B**: Polish validation (T029-T034)

### Estimated Timeline

- **Phase 1 (Setup)**: 15 minutes
- **Phase 2 (Foundational)**: 1-2 hours (domain models, mapper)
- **Phase 3 (User Story 1)**: 2-3 hours (repository implementation, test updates)
- **Phase 4 (User Story 2)**: 1-2 hours (billing/agreement integration)
- **Phase 5 (User Story 3)**: 30 minutes (documentation)
- **Phase 6 (JSON Schemas)**: 1 hour (schema files, OpenAPI updates)
- **Phase 7 (Polish)**: 30 minutes (validation, quality gates)

**Total**: 5-8 hours (matches quickstart.md estimate)

---

## Notes

- **[P] tasks**: Different files, no dependencies - can execute in parallel
- **[Story] labels**: Map tasks to user stories for traceability
- **Breaking change**: PartnershipPack.options type changes from SponsoringOption to PartnershipOption (user explicitly waived backward compatibility concerns)
- **No database migrations**: Feature uses existing schema and relationships
- **Tests**: Update existing tests only - no new test files requested
- **Complete descriptions**: Parentheses format `"Description (value)"` per research decision
- **Pricing calculations**: Repository layer computes totalPrice = basePrice + sum(optionalOptions.totalPrice)
- **Translation validation**: Throw ForbiddenException if option translations missing for partnership language
- **Frontend**: Auto-regenerates TypeScript client via Orval from OpenAPI spec
- Each user story should be independently testable at its checkpoint
- Stop at any checkpoint to validate and potentially deploy that increment
- Commit after each task or logical group for rollback safety
