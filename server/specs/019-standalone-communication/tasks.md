# Tasks: Schedule Standalone Communication

**Input**: Design documents from `/specs/019-standalone-communication/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Paths are relative to `application/src/main/kotlin/fr/devlille/partners/connect/`

---

## Phase 1: Setup

**Purpose**: JSON schema and OpenAPI spec additions needed before any route can validate requests.

- [X] T001 Create JSON schema `communication_plan_request.schema.json` in `application/src/main/resources/schemas/`
- [X] T002 [P] Add `CommunicationPlanEntry` component schema and 3 new operations (`POST /communication-plan`, `PUT /communication-plan/{id}`, `DELETE /communication-plan/{id}`) to `application/src/main/resources/openapi.yaml`; update `CommunicationItem` schema (`company_name` nullable, add `id`, `title`, `standalone`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database table, entity, domain model, migrations, Koin binding, and factory — everything all user stories build on.

⚠️ **CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Create `partnership/infrastructure/db/CommunicationPlansTable.kt` — `UUIDTable("communication_plans")` with columns: `eventId`, `partnershipId` (nullable FK, `SET_NULL`), `title`, `scheduledDate` (nullable), `description` (nullable), `supportUrl` (nullable), `createdAt`, `updatedAt`; add `init { index(false, eventId) }` for efficient per-event planning view queries (see data-model.md)
- [X] T004 Create `partnership/infrastructure/db/CommunicationPlanEntity.kt` — `UUIDEntity` with companion `UUIDEntityClass`; all property delegations including `optionalReferencedOn` for `partnershipId`
- [X] T005 Create `CommunicationPlanEntry` serializable data class and `toDomain()` / `toCommunicationItem()` extension functions in `partnership/domain/CommunicationPlan.kt` (add alongside existing `CommunicationItem`/`CommunicationPlan`); update `CommunicationItem` to add `id`, `title`, `standalone` fields and make `companyName` nullable
- [X] T006 Create `partnership/domain/CommunicationPlanRepository.kt` — interface with `create` (non-nullable `scheduledDate`), `findById`, `update` (nullable `scheduledDate`), `delete`, and `upsertForPartnership(eventSlug, partnershipId, scheduledDate?, supportUrl?)` methods (see data-model.md)
- [X] T007 Create `partnership/application/CommunicationPlanRepositoryExposed.kt` — `CommunicationPlanRepository` implementation using Exposed; `eventSlug` scoping guard on all methods; implement `upsertForPartnership` as: find existing row by `partnershipId` → update if found, create new row with `title = company.name` if not found; `updatedAt` explicitly set to `Clock.System.now()` on every `update` / `upsertForPartnership` call
- [X] T008 Create `internal/infrastructure/migrations/versions/CreateCommunicationPlansTableMigration.kt` — id `20260316_create_communication_plans_table`; `SchemaUtils.create(CommunicationPlansTable)` in `up()`
- [X] T009 Create `internal/infrastructure/migrations/versions/MigratePartnershipCommunicationsMigration.kt` — id `20260316_migrate_partnership_communications`; reads `PartnershipEntity` rows where `communicationPublicationDate != null`; inserts `CommunicationPlanEntity` rows with `title = company.name`, `scheduledDate`, `supportUrl`; wrapped in `transaction {}`
- [X] T010 Register both migrations in `internal/infrastructure/migrations/MigrationRegistry.kt` after `AddPartnershipPriceOverridesMigration`
- [X] T011 Add `single<CommunicationPlanRepository> { CommunicationPlanRepositoryExposed() }` binding in `partnership/infrastructure/bindings/PartnershipModule.kt`
- [X] T012 Create test factory `insertMockedCommunicationPlan(id, eventId, partnershipId?, title, scheduledDate?, description?, supportUrl?)` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/factories/CommunicationPlan.factory.kt` (no transaction management; UUID-based title default)

**Checkpoint**: Table exists, entity compiles, interface + implementation complete, migrations registered, Koin binding added, test factory ready — US1–US4 can now proceed.

---

## Phase 3: User Story 1 — Schedule a Standalone Communication (Priority: P1) 🎯 MVP

**Goal**: Organiser can `POST` a standalone communication entry for an event; it is saved with correct event scoping and title validation.

**Independent Test**: `POST /orgs/{orgId}/events/{eventId}/communication-plan` with valid body → 201; entry persisted with `standalone: true`.

### Implementation

- [X] T013 [US1] Create `partnership/infrastructure/api/CommunicationPlanRoutes.kt` — `fun Route.orgsEventCommunicationPlanRoutes()` with route block `route("/orgs/{orgSlug}/events/{eventSlug}/communication-plan") { install(AuthorizedOrganisationPlugin) }`; add `POST` handler: extracts `eventSlug`, receives body against `communication_plan_request.schema.json`, calls `communicationPlanRepository.create(...)`, responds `HttpStatusCode.Created`
- [X] T014 [US1] Register `orgsEventCommunicationPlanRoutes()` inside `fun Route.partnershipRoutes()` in `partnership/infrastructure/api/PartnershipRoutes.kt`

### Contract Tests

- [X] T015 [P] [US1] Contract test `CommunicationPlanRoutePostTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/CommunicationPlanRoutePostTest.kt`: test 201 on valid body; 400 when `title` missing/blank; 401 without auth; 403 for wrong org; 404 for unknown event

**Checkpoint**: `POST /communication-plan` returns 201 with correct payload; unauthorised requests rejected. US1 independently testable.

---

## Phase 4: User Story 2 — View Standalone Communications in Planning (Priority: P1)

**Goal**: `GET /communication` re-sources from `communication_plans` table; standalone and partnership-linked entries both appear, grouped and sorted correctly; `standalone` field distinguishes them.

**Independent Test**: Seed one standalone and one partnership-linked `CommunicationPlanEntity`; call `GET /orgs/{orgId}/events/{eventId}/communication` → both appear in correct `planned`/`done`/`unplanned` group.

### Implementation

- [X] T016 [US2] Update `partnership/application/PartnershipCommunicationRepositoryExposed.kt` — rewrite `listCommunicationPlan()` to query `CommunicationPlanEntity.find { CommunicationPlansTable.eventId eq event.id }` and map via `toCommunicationItem()`; remove dependency on `PartnershipsTable` communication columns
- [X] T016b [US2] Remove stale `communicationPublicationDate` and `communicationSupportUrl` fields from `PartnershipProcessStatus` in `partnership/domain/PartnershipProcessStatus.kt` and from the entity-to-domain mapper in `partnership/application/mappers/PartnershipEntity.ext.kt`; these fields are frozen (FR-012) and must no longer appear in the partnership detail response after the migration
- [X] T017 [US2] Update `PUT .../communication/publication` and `PUT .../communication/support` handlers in `partnership/infrastructure/api/PartnershipCommunicationRoutes.kt` to inject `CommunicationPlanRepository` and call `communicationPlanRepository.upsertForPartnership(eventSlug, partnershipId, scheduledDate, supportUrl)` (writes to `communication_plans`); remove all writes to `PartnershipsTable.communicationPublicationDate` / `communicationSupportUrl`
- [X] T018 [US2] Update `PartnershipCommunicationRepository` interface (`partnership/domain/PartnershipCommunicationRepository.kt`) if method signatures change (e.g., return type updated to use `CommunicationItem` with new fields)

### Contract Tests

- [X] T019 [P] [US2] Update `EventCommunicationPlanRouteGetTest` in `application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/EventCommunicationPlanRouteGetTest.kt`: replace `insertMockedPartnership` communication setup with `insertMockedCommunicationPlan` factory; add assertion for `id`, `title`, `standalone` fields; add standalone entry to verify it appears alongside partnership-linked entries

**Checkpoint**: `GET /communication` returns entries from new table; `standalone: true` correctly set; groups sorted. US2 independently testable.

---

## Phase 5: User Story 3 — Edit Any Communication Plan Entry (Priority: P2)

**Goal**: Organiser can `PUT` on any communication plan entry (standalone or linked); title/date/description updates persist; cross-event edits rejected; blank title rejected.

**Independent Test**: Create entry via POST; PUT update → 200 with updated fields; verify in GET planning view.

### Implementation

- [X] T020 [US3] Add `PUT /{id}` handler inside `orgsEventCommunicationPlanRoutes()` in `partnership/infrastructure/api/CommunicationPlanRoutes.kt`: extract `id`, receive body against schema, call `communicationPlanRepository.update(...)`, respond `HttpStatusCode.OK`; `updatedAt` set in repository

### Contract Tests

- [X] T021 [P] [US3] Contract test `CommunicationPlanRoutePutTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/CommunicationPlanRoutePutTest.kt`: 200 on valid update; 400 on blank title; 401 without auth; 403 for wrong org; 404 for unknown id or cross-event id

**Checkpoint**: `PUT /communication-plan/{id}` returns 200 with updated payload; cross-event access returns 404. US3 independently testable.

---

## Phase 6: User Story 4 — Delete Any Communication Plan Entry (Priority: P2)

**Goal**: Organiser can `DELETE` any communication plan entry; entry removed from planning view; partnership unaffected; cross-event deletes rejected.

**Independent Test**: Create entry via POST; send DELETE → 204; confirm absent in GET planning view.

### Implementation

- [X] T022 [US4] Add `DELETE /{id}` handler inside `orgsEventCommunicationPlanRoutes()` in `partnership/infrastructure/api/CommunicationPlanRoutes.kt`: extract `id`, call `communicationPlanRepository.delete(...)`, respond `HttpStatusCode.NoContent`

### Contract Tests

- [X] T023 [P] [US4] Contract test `CommunicationPlanRouteDeleteTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/CommunicationPlanRouteDeleteTest.kt`: 204 on valid delete; 401 without auth; 403 for wrong org; 404 for unknown id or cross-event id

**Checkpoint**: `DELETE /communication-plan/{id}` returns 204; entry absent from subsequent GET. US4 independently testable.

---

## Phase 7: Integration Test & Polish

**Purpose**: End-to-end workflow validation and quality gates.

- [X] T024 Create integration test `CommunicationPlanRoutesTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/CommunicationPlanRoutesTest.kt`: full CRUD workflow — POST standalone → appears in GET planning view → PUT update → GET reflects update → DELETE → GET confirms removal; also verifies migration: seeded partnership communication entry visible in GET response with `standalone: false`
- [X] T025 [P] Run `./gradlew ktlintCheck --no-daemon` and fix any formatting violations across all new/modified files
- [X] T026 [P] Run `./gradlew detekt --no-daemon` and fix any static analysis violations
- [X] T027 Run `npm run validate` from `server/` to confirm OpenAPI spec is valid after additions in T002
- [X] T028 Run `./gradlew check --no-daemon` — all tests pass, coverage ≥ 80%, zero lint/detekt violations

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup) → Phase 2 (Foundational) → Phase 3 (US1) → Phase 4 (US2) → Phase 5 (US3) → Phase 6 (US4) → Phase 7 (Polish)
```

- Phase 1 has no dependencies — start immediately.
- Phase 2 is blocked only by Phase 1 completion.
- Phases 3–6 all depend on Phase 2 completion.
- Phases 3 & 4 can run **in parallel** (US1 creates the POST handler; US2 updates the GET handler — different files).
- Phases 5 & 6 can run **in parallel** with each other after Phase 2; both add handlers to the same routes file but at different paths — coordinate if pairing.
- Phase 7 runs last.

### Parallel Execution Examples

**After Phase 2:**

```
Developer A: T013 → T015 (US1: POST route + contract test)
Developer B: T016 → T018 → T019 (US2: GET re-source + contract test)
```

**After T013–T014 complete (Phase 3 done):**

```
Developer A: T020 → T021 (US3: PUT route + contract test)
Developer B: T022 → T023 (US4: DELETE route + contract test)
```

### User Story Independence

| User Story | Can Start After | Independent Test |
|------------|----------------|-----------------|
| US1 (POST) | Phase 2 complete | `POST` returns 201, `standalone: true` |
| US2 (GET re-source) | Phase 2 complete | Seeded entries appear in GET with `standalone` field |
| US3 (PUT) | Phase 2 complete (POST not required) | Seed via factory, PUT, verify 200 |
| US4 (DELETE) | Phase 2 complete (POST not required) | Seed via factory, DELETE, verify 204 |

---

## Implementation Strategy

**MVP scope**: Phase 1 + Phase 2 + Phase 3 (US1) + Phase 4 (US2) — post and view standalone communications. Delivers the core value stated in the user request.

**Incremental delivery**:
1. Phases 1–2 — infrastructure (no visible API change yet)
2. Phase 3 (US1) — `POST` endpoint live; standalone entries can be created
3. Phase 4 (US2) — `GET /communication` re-sourced; standalone entries visible
4. Phases 5–6 (US3/US4) — edit and delete complete the CRUD surface
5. Phase 7 — quality gates and integration test
