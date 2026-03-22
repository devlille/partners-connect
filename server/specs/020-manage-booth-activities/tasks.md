# Tasks: Manage Booth Activities

**Input**: Design documents from `/specs/020-manage-booth-activities/`
**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/ ✅ | quickstart.md ✅

**Tech stack**: Kotlin 1.9.x / Ktor 2.x / Exposed 0.41+ / Koin / PostgreSQL  
**Module**: `fr.devlille.partners.connect.partnership`  
**Route prefix**: `/events/{eventSlug}/partnerships/{partnershipId}/activities`

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Parallelizable — operates on different files with no pending dependencies
- **[Story]**: Maps to user story from spec.md (US1 = Create, US2 = List, US3 = Edit, US4 = Delete)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the new table, entity, domain model, repository interface, and Koin binding that ALL user stories depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T001 Create `BoothActivitiesTable` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/BoothActivitiesTable.kt` extending `UUIDTable("booth_activities")` with columns: `partnershipId` (FK → PartnershipsTable CASCADE), `title` (varchar 255), `description` (text), `startTime` (datetime nullable), `endTime` (datetime nullable), `createdAt` (datetime clientDefault Clock.System.now().toLocalDateTime(TimeZone.UTC))

- [X] T002 [P] Create `BoothActivityEntity` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/BoothActivityEntity.kt` extending `UUIDEntity` with companion object `UUIDEntityClass<BoothActivityEntity>(BoothActivitiesTable)` and delegated properties for all columns

- [X] T003 [P] Create domain model files in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/`:
  - `BoothActivity.kt` — `@Serializable` data class (response DTO): `id: UUID`, `partnershipId: UUID`, `title: String`, `description: String`, `startTime: LocalDateTime?`, `endTime: LocalDateTime?`, `createdAt: LocalDateTime`
  - `BoothActivityRequest.kt` — `@Serializable` data class (request DTO): `title: String`, `description: String`, `startTime: LocalDateTime?`, `endTime: LocalDateTime?`

- [X] T004 [P] Create `BoothActivityRepository` interface in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/BoothActivityRepository.kt` with methods: `list(partnershipId: UUID): List<BoothActivity>`, `create(partnershipId: UUID, request: BoothActivityRequest): BoothActivity`, `update(partnershipId: UUID, activityId: UUID, request: BoothActivityRequest): BoothActivity`, `delete(partnershipId: UUID, activityId: UUID)`

- [X] T005 Create `BoothActivityRepositoryExposed` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/BoothActivityRepositoryExposed.kt` implementing `BoothActivityRepository` using Exposed ORM; implement booth eligibility check via `PartnershipOptionsTable` ⋈ `SponsoringOptionsTable` where `selectableDescriptor == SelectableDescriptor.BOOTH`; sorting: `startTime ASC_NULLS_LAST, createdAt ASC`; throw `NotFoundException` if partnership/activity not found; throw `ForbiddenException` if no booth option

- [X] T006 [P] Create `BoothActivityModule` Koin binding in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/bindings/BoothActivityModule.kt` — bind `BoothActivityRepositoryExposed` as `BoothActivityRepository` singleton

- [X] T007 Register `BoothActivitiesTable` in the existing `MigrationRegistry` so SchemaUtils creates the table on startup; add `BoothActivityModule` to the module list in `App.kt`

- [X] T008 [P] Create JSON schema `booth_activity_request.schema.json` in `application/src/main/resources/schemas/` — `title` and `description` required non-empty strings; `startTime` and `endTime` optional nullable ISO date-time strings

- [X] T009 [P] Create JSON schema `booth_activity_response.schema.json` in `application/src/main/resources/schemas/` — all BoothActivity fields, `startTime`/`endTime` as nullable union types (`["string","null"]`)

- [X] T010 [P] Create JSON schema `booth_activity_list_response.schema.json` in `application/src/main/resources/schemas/` — array of `booth_activity_response` items

**Checkpoint**: Foundation ready — all user story route implementations can now begin.

---

## Phase 2: User Story 1 — Create a Booth Activity (Priority: P1) 🎯 MVP

**Goal**: `POST /events/{eventSlug}/partnerships/{partnershipId}/activities` — save a new activity and return 201 with the created activity.

**Independent Test**: Create an activity on a booth partnership; verify 201 response with all fields; verify 400 on missing title/description; verify 400 on startTime after endTime; verify 403 on non-booth partnership.

- [X] T011 [US1] Create `BoothActivityRoutes.kt` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/BoothActivityRoutes.kt`:
  - Top-level route `route("/events/{eventSlug}/partnerships/{partnershipId}/activities")`: contains `GET` handler only — no `WebhookPartnershipPlugin` here
  - Nested **mutation** route block (separate `route {}`) wrapping `POST`, `PUT /{activityId}`, and `DELETE /{activityId}`: install `WebhookPartnershipPlugin` **on this block only** so the plugin fires exclusively after write operations
  - `POST` handler: use `call.receive<BoothActivityRequest>(boothActivityRequestSchema)`; validate booth eligibility (throws `ForbiddenException` → 403); validate `startTime < endTime` (throws `BadRequestException` → 400); call `boothActivityRepository.create(partnershipId, request)`; respond `201 Created`

- [X] T012 [P] [US1] Create contract test `BoothActivityRoutePostTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/BoothActivityRoutePostTest.kt` covering: 201 with full fields, 201 with null times, 400 missing title, 400 missing description, 400 startTime after endTime, 403 no booth option, 404 unknown partnership, **201 when webhook integration configured but webhook call fails** (verifies FR-011: activity is still created successfully; webhook failure must not roll back)

**Checkpoint**: POST endpoint fully functional and contract-tested. US1 independently verifiable.

---

## Phase 3: User Story 2 — List Booth Activities (Priority: P1)

**Goal**: `GET /events/{eventSlug}/partnerships/{partnershipId}/activities` — return sorted list of all activities for the partnership.

**Independent Test**: Seed activities on a booth partnership; verify GET returns them sorted by `startTime ASC NULLS LAST, createdAt ASC`; verify empty list when no activities; verify 403 on non-booth partnership; verify 404 on unknown partnership.

- [X] T013 [US2] Add `GET` handler in `BoothActivityRoutes.kt` (outside the mutation route block — no booth-eligibility gate on GET per FR-001): resolve partnership by `partnershipId` (throw `NotFoundException` → 404 if not found); call `boothActivityRepository.list(partnershipId)`; respond `200 OK` with list

- [X] T014 [P] [US2] Create contract test `BoothActivityRouteGetTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/BoothActivityRouteGetTest.kt` covering: 200 with activities (sorted by startTime ASC NULLS LAST), 200 empty list, **200 on partnership with no booth option** (GET is exempt from booth gate per FR-001), 404 unknown partnership

**Checkpoint**: GET endpoint fully functional and contract-tested. US2 independently verifiable.

---

## Phase 4: User Story 3 — Edit a Booth Activity (Priority: P2)

**Goal**: `PUT /events/{eventSlug}/partnerships/{partnershipId}/activities/{activityId}` — update an existing activity and return 200 with updated fields.

**Independent Test**: Create an activity, update title/description/times; verify 200 with new values; set times to null and verify; verify 400 on blank title/description; verify 404 on unknown activity or wrong partnership; verify 403 on non-booth partnership.

- [X] T015 [US3] Add `PUT` handler in `BoothActivityRoutes.kt` under `route("/{activityId}")`: validate body via schema; validate `startTime < endTime`; call `boothActivityRepository.update(partnershipId, activityId, request)` (repository enforces ownership check — returns 404 if activityId not in this partnership); respond `200 OK` with updated `BoothActivity`

- [X] T016 [P] [US3] Create contract test `BoothActivityRoutePutTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/BoothActivityRoutePutTest.kt` covering: 200 full update, 200 clear times to null, 400 blank title, 400 blank description, 400 startTime after endTime, 403 no booth option, 404 unknown activity, 404 activity from different partnership

**Checkpoint**: PUT endpoint fully functional and contract-tested. US3 independently verifiable.

---

## Phase 5: User Story 4 — Delete a Booth Activity (Priority: P2)

**Goal**: `DELETE /events/{eventSlug}/partnerships/{partnershipId}/activities/{activityId}` — remove an activity and return 204.

**Independent Test**: Create an activity, delete it; verify 204; verify it no longer appears in the list; verify 404 on unknown activity or wrong partnership; verify 403 on non-booth partnership.

- [X] T017 [US4] Add `DELETE` handler in `BoothActivityRoutes.kt` under `route("/{activityId}")`: call `boothActivityRepository.delete(partnershipId, activityId)` (repository enforces ownership — throws `NotFoundException` if not found); respond `204 No Content`

- [X] T018 [P] [US4] Create contract test `BoothActivityRouteDeleteTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/BoothActivityRouteDeleteTest.kt` covering: 204 on valid delete, 403 no booth option, 404 unknown activity, 404 activity from different partnership

**Checkpoint**: DELETE endpoint fully functional and contract-tested. US4 independently verifiable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Wire routes into App.kt, update OpenAPI spec, add integration test, run quality gates.

- [X] T019 Register `boothActivityRoutes()` in `App.kt` (or the relevant routing entry point) inside the `/events/{eventSlug}/partnerships/{partnershipId}` route block

- [X] T020 [P] Update `application/src/main/resources/openapi.yaml` — add activity paths and schema `$ref` entries from `contracts/openapi.yaml`

- [X] T021 Create integration test `BoothActivityRoutesTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/BoothActivityRoutesTest.kt` covering the full CRUD lifecycle: POST → GET (verify sorted) → PUT → GET (verify updated) → DELETE → GET (verify empty); single `transaction {}` setup with `insertMockedPartnership`, `insertMockedBoothOption`, etc.

- [X] T022 [P] Create test factories in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/factories/BoothActivity.factory.kt`:
  - `insertMockedBoothActivity(id = UUID.randomUUID(), partnershipId: UUID, title: String = id.toString(), description: String = id.toString(), startTime: LocalDateTime? = null, endTime: LocalDateTime? = null)` — all params defaulted, no transaction management
  - `insertMockedBoothSponsoringOption(eventId: UUID): SponsoringOptionEntity` — inserts a `SponsoringOptionsTable` row with `selectableDescriptor = SelectableDescriptor.BOOTH`; used to make a partnership booth-eligible in tests

- [X] T023 [P] Run `./gradlew ktlintFormat --no-daemon` from `/server` and fix any formatting violations

- [X] T024 [P] Run `./gradlew detekt --no-daemon` from `/server` and fix any static analysis violations

- [X] T025 Run `./gradlew test --no-daemon` from `/server` — all tests must pass with ≥80% coverage on new code

- [X] T026 [P] Run quickstart.md manual validation against local dev stack to confirm end-to-end behaviour

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately; T001–T010 can all begin in parallel after T001 unblocks T002–T004
- **Phase 2 (US1)**: Requires Phase 1 complete (T001–T010)
- **Phase 3 (US2)**: Requires Phase 1 complete; can run in parallel with Phase 2 if staffed
- **Phase 4 (US3)**: Requires Phase 1 complete; can run in parallel with Phases 2–3 if staffed
- **Phase 5 (US4)**: Requires Phase 1 complete; can run in parallel with Phases 2–4 if staffed
- **Phase 6 (Polish)**: Requires all story phases (2–5) complete

### User Story Dependencies

- **US1 (Create)**: No dependency on other stories
- **US2 (List)**: No dependency on other stories; `list()` repo method is independent
- **US3 (Edit)**: No dependency on US1/US2; `update()` repo method is independent
- **US4 (Delete)**: No dependency on other stories; `delete()` repo method is independent

### Within Each Story

- Route handler (T011/T013/T015/T017) before contract test (T012/T014/T016/T018) is NOT required — write tests first (TDD) if preferred; the schema-based contract tests can be written as soon as Phase 1 is complete
- Route handler must exist before integration test (T021)

### Parallel Opportunities

Within Phase 1 (after T001 table is defined):
- T002, T003, T004 can all start at the same time
- T005 can start once T001–T004 are done
- T006 can start once T004 is done
- T008, T009, T010 can start any time (no code dependencies)

Across stories (once Phase 1 is done):
- T011 (US1 route) + T013 (US2 route) + T015 (US3 route) + T017 (US4 route) can all run in parallel

---

## Parallel Execution Example: Phase 1 (Setup)

```bash
# Step 1 — Must go first
T001: BoothActivitiesTable.kt

# Step 2 — All in parallel once T001 is done
T002: BoothActivityEntity.kt        [P]
T003: BoothActivity.kt (domain)     [P]
T004: BoothActivityRepository.kt    [P]
T008: booth_activity_request.schema.json   [P]
T009: booth_activity_response.schema.json  [P]
T010: booth_activity_list_response.schema.json [P]

# Step 3 — Sequential, depends on T001–T004
T005: BoothActivityRepositoryExposed.kt
T006: BoothActivityModule.kt        [P once T004 done]
T007: MigrationRegistry + App.kt wiring
```

## Parallel Execution Example: Phases 2–5 (Stories)

```bash
# All four story route handlers in parallel, once Phase 1 is done:
T011 [US1] POST route handler        [P]
T013 [US2] GET route handler         [P]
T015 [US3] PUT route handler         [P]
T017 [US4] DELETE route handler      [P]

# Contract tests in parallel (can be written alongside or after handlers):
T012 [US1] BoothActivityRoutePostTest   [P]
T014 [US2] BoothActivityRouteGetTest    [P]
T016 [US3] BoothActivityRoutePutTest    [P]
T018 [US4] BoothActivityRouteDeleteTest [P]
```

---

## Implementation Strategy

**MVP Scope** (Phase 1 + Phase 2 + Phase 3): Delivers the foundational create and list capabilities — enough to verify the booth check, webhook plugin installation, and sorting logic end to end.

**Full Scope**: Add Phase 4 (edit) and Phase 5 (delete) to complete all four user stories. Phase 6 (polish + integration test) completes the feature for production.

**Summary**

| Phase | Tasks | Stories |
|-------|-------|---------|
| Setup | T001–T010 | — |
| US1 Create | T011–T012 | P1 |
| US2 List | T013–T014 | P1 |
| US3 Edit | T015–T016 | P2 |
| US4 Delete | T017–T018 | P2 |
| Polish | T019–T026 | — |
| **Total** | **26 tasks** | **4 user stories** |

**Format validation**: All 26 tasks use `- [ ] T###` format. Story-phase tasks carry `[US#]` labels. Parallelizable tasks carry `[P]`. All tasks include file paths.
