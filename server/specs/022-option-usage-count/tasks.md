# Tasks: Option Usage Count

**Input**: Design documents from `/specs/022-option-usage-count/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: New files and schemas needed before implementation

- [X] T001 [P] Create `SponsoringOptionWithCount` wrapper data class in `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/SponsoringOptionWithCount.kt`
- [X] T002 [P] Create JSON schema `sponsoring_option_with_count.schema.json` in `application/src/main/resources/schemas/sponsoring_option_with_count.schema.json`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Repository interface and OpenAPI changes that MUST be complete before the route can be updated

**⚠️ CRITICAL**: Route update (Phase 3) depends on these being complete

- [X] T003 Add `listOptionsWithPartnershipCounts(eventSlug: String): List<SponsoringOptionWithCount>` method to `OptionRepository` interface in `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/OptionRepository.kt`
- [X] T004 Implement `listOptionsWithPartnershipCounts` in `OptionRepositoryExposed` using single-pass algorithm in `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/OptionRepositoryExposed.kt`
- [X] T005 Register `SponsoringOptionWithCount` component schema and update list endpoint response in `application/src/main/resources/openapi/openapi.yaml`

**Checkpoint**: Repository layer complete, OpenAPI updated — route can now be wired up

---

## Phase 3: User Story 1 - View Partnership Usage Count per Option (Priority: P1) 🎯 MVP

**Goal**: The options list endpoint returns wrapper objects with `partnership_count` for each option

**Independent Test**: Call `GET /orgs/{orgSlug}/events/{eventSlug}/options` and verify each item has `option` and `partnership_count` fields, with counts matching validated partnership associations

### Implementation for User Story 1

- [X] T006 [US1] Update list route handler in `SponsoringRoutes.kt` to call `listOptionsWithPartnershipCounts` instead of `listOptionsByEventWithAllTranslations` in `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt`
- [X] T007 [US1] Update contract tests in `SponsoringListOptionRouteGetTest.kt` to assert wrapper response shape with `partnership_count` field — cover: (1) options with validated partnerships return correct count, (2) options with no partnerships return count 0, (3) declined partnerships are not counted, (4) empty list returns `[]` — in `application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringListOptionRouteGetTest.kt`

**Checkpoint**: User Story 1 fully functional — list endpoint returns wrapped options with partnership counts

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Validation and quality gates

- [X] T008 Run `./gradlew ktlintFormat --no-daemon` and `./gradlew detekt --no-daemon` to fix formatting and pass static analysis
- [X] T009 Run `npm run validate` to validate OpenAPI spec and `npm run bundle` to regenerate `documentation.yaml`
- [X] T010 Run `./gradlew test --no-daemon` to verify all tests pass (510+ existing + updated contract tests)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: T003 depends on T001 (wrapper type used in interface). T004 depends on T001 + T003. T005 depends on T002 (schema file must exist).
- **User Story 1 (Phase 3)**: T006 depends on T004 (repository method available). T007 depends on T006 (route must return new shape).
- **Polish (Phase 4)**: Depends on all implementation tasks being complete

### Within Phase 1 (Parallel)

- T001 and T002 are independent files — can run in parallel

### Within Phase 2 (Sequential)

- T003 → T004 (interface before implementation)
- T005 can run in parallel with T003/T004 (different file)

### Within Phase 3 (Sequential)

- T006 → T007 (route before contract tests update)

---

## Parallel Example: Phase 1

```
# Launch both setup tasks together:
Task T001: Create SponsoringOptionWithCount.kt (domain class)
Task T002: Create sponsoring_option_with_count.schema.json (JSON schema)
```

## Parallel Example: Phase 2

```
# After T001 completes:
Task T003: Add interface method (depends on T001)
Task T005: Update OpenAPI (depends on T002, independent of T003)

# After T003 completes:
Task T004: Implement repository method (depends on T001 + T003)
```

---

## Implementation Strategy

### MVP First (Single User Story)

1. Complete Phase 1: Setup (T001, T002 in parallel)
2. Complete Phase 2: Foundational (T003 → T004, T005 in parallel)
3. Complete Phase 3: User Story 1 (T006 → T007)
4. **VALIDATE**: Run quickstart.md verification
5. Complete Phase 4: Polish (T008 → T009 → T010)

### Incremental Delivery

This feature has a single user story. The MVP scope IS the full feature. After T010, the feature is complete.

---

## Notes

- [P] tasks = different files, no dependencies
- [US1] maps to the single user story from spec.md (P1 priority)
- `encodeDefaults=false` in Json config — do NOT use default values for `partnershipCount`
- Existing `listOptionsByEventWithAllTranslations` method remains in the interface for other consumers
- Response shape change is breaking — frontend must be updated to read `item.option.*` instead of `item.*`
