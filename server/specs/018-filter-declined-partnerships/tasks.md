# Tasks: Filter Partnerships by Declined Status

**Input**: Design documents from `specs/018-filter-declined-partnerships/`
**Feature**: Add `filter[declined]` boolean query parameter to the partnership list endpoint (GET) and the email endpoint (POST)
**Branch**: `018-filter-declined-partnerships`
**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/api-changes.md ✅ | quickstart.md ✅

---

## Format: `[ID] [P?] [Story?] Description — file path`

- **[P]**: Can run in parallel (independent files, no blocking dependencies)
- **[US1]**: User Story 1 — View Declined Partnerships in Partnership List
- **[US2]**: User Story 2 — Control Declined Partnerships in Bulk Email Sending

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Shared helpers and domain model changes required by **both** user stories. Must complete before any story implementation begins.

- [x] T001 Add `toBooleanStrict(paramName: String, default: Boolean): Boolean` extension to `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/StringValues.ext.kt` — returns `default` when null, `true`/`false` for valid values, throws `BadRequestException` for invalid strings (satisfies FR-005)
- [x] T002 [P] Add `val declined: Boolean = false` field to `PartnershipFilters` data class in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipItem.kt` — non-nullable with explicit default (encodes default-exclusion at domain level, unlike other `Boolean?` filter fields)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database query layer — the `PartnershipEntity.filters()` method is the single shared entry point used by both the list repository and the email repository. Must complete before either user story can wire up its application layer.

**⚠️ CRITICAL**: This phase must complete before Phase 3 (US1) and Phase 4 (US2) can begin.

- [x] T003 Add `declined: Boolean = false` parameter to `PartnershipEntity.filters()` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEntity.kt` — when `!declined`, append `op = op and (PartnershipsTable.declinedAt.isNull())`; use `PartnershipsTable.declinedAt` only (NOT `suggestionDeclinedAt`)

**Checkpoint**: DB filter layer ready — both user story application-layer tasks (T004 [P] and T008 [P]) can now start in parallel.

---

## Phase 3: User Story 1 — View Declined Partnerships in Partnership List (Priority: P1) 🎯 MVP

**Goal**: Organisers can list partnerships with or without declined ones using `filter[declined]` on the GET endpoint. Default excludes declined (intentional breaking change).

**Independent Test**: Authenticate as organiser, create active + declined partnerships for an event, call `GET /orgs/{slug}/events/{slug}/partnerships` without filter → declined absent; call with `?filter[declined]=true` → declined present; call with `?filter[declined]=maybe` → HTTP 400; verify `metadata.filters` includes `{ "name": "declined", "type": "boolean" }`.

- [x] T004 [P] [US1] Create contract test `PartnershipListDeclinedFilterRouteGetTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipListDeclinedFilterRouteGetTest.kt` — cover: HTTP 200 with no filter param (declined excluded), HTTP 200 with `filter[declined]=false` (excluded), HTTP 200 with `filter[declined]=true` (included), HTTP 400 with invalid value, HTTP 401 without auth token (follow contract test pattern from `PartnershipOrganiserFilterRoutesTest.kt`)
- [x] T005 [P] [US1] Update `listByEvent()` to pass `declined = filters.declined` to `PartnershipEntity.filters()` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt`, and add `FilterDefinition("declined", FilterType.BOOLEAN)` at the end of the filters list in `buildMetadata()` (satisfies FR-011, SC-005)
- [x] T006 [US1] Parse `filter[declined]` in the GET partnerships handler in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt` — add `declined = call.request.queryParameters["filter[declined]"].toBooleanStrict("filter[declined]", default = false)` to the `PartnershipFilters(...)` constructor call (depends on T001, T002, T005)
- [x] T007 [US1] Create integration test `PartnershipDeclinedFilterRoutesTest` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipDeclinedFilterRoutesTest.kt` — US1 scenarios: default excludes declined, `filter[declined]=false` excludes declined, `filter[declined]=true` includes declined, AND logic with another filter (e.g., `filter[validated]=true`), metadata response includes `declined` entry with `type: "boolean"`, all-declined event with no filter returns empty 200 (use `moduleSharedDb` + `Partnership.factory.kt` `insertMockedPartnership(declinedAt = ...)` pattern; depends on T006)

**Checkpoint**: User Story 1 is fully functional and independently testable.

---

## Phase 4: User Story 2 — Control Declined Partnerships in Bulk Email Sending (Priority: P2)

**Goal**: Organisers sending bulk emails can control whether declined partnerships are included using the same `filter[declined]` parameter on the POST email endpoint. Default excludes declined.

**Independent Test**: Call `POST /orgs/{slug}/events/{slug}/partnerships/email` without `filter[declined]` → declined partnerships absent from recipients; call with `?filter[declined]=true` → declined partnerships appear in the recipient list.

- [x] T008 [P] [US2] Update `getPartnershipDestination()` to pass `declined = filters.declined` to `PartnershipEntity.filters()` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipEmailRepositoryExposed.kt` (depends on T003; T002 must be complete)
- [x] T009 [US2] Parse `filter[declined]` in the POST email handler in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipEmailRoutes.kt` — add `declined = call.request.queryParameters["filter[declined]"].toBooleanStrict("filter[declined]", default = false)` to the `PartnershipFilters(...)` constructor call (depends on T001, T002, T008)
- [x] T010 [US2] Extend `PartnershipDeclinedFilterRoutesTest` with US2 scenarios in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipDeclinedFilterRoutesTest.kt` — email endpoint: no filter excludes declined from recipients, `filter[declined]=true` includes declined, AND logic with another email filter, all-declined event with no filter returns correct empty response (depends on T009)

**Checkpoint**: Both user stories are independently functional. All acceptance scenarios from spec.md covered.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: OpenAPI documentation and quality gates.

- [x] T011 [P] Add `filter[declined]` parameter (boolean, not required) to both endpoint blocks in `application/src/main/resources/openapi.yaml` — after the existing `filter[organiser]` parameter in the GET `/orgs/{orgSlug}/events/{eventSlug}/partnerships` block (~line 2937) and in the POST `/orgs/{orgSlug}/events/{eventSlug}/partnerships/email` block (~line 4184); use exact YAML from `contracts/api-changes.md`
- [x] T012 Run `./gradlew ktlintFormat --no-daemon` from `/server` directory to auto-fix formatting violations, then run `./gradlew detekt --no-daemon` to verify zero static analysis violations
- [x] T013 Run `./gradlew test --no-daemon` from `/server` directory to verify all tests pass (including newly created contract and integration tests), then run `npm run validate` to verify OpenAPI schema is valid

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)         — no dependencies; start immediately
    │
    ├── T001 (toBooleanStrict)
    └── T002 [P] (PartnershipFilters.declined field)
         │
Phase 2 (Foundational)
    └── T003 (PartnershipEntity.filters())  ← depends on T002
         │
         ├── Phase 3 (US1)                  ← T004 [P] and T005 [P] start in parallel
         │
         └── Phase 4 (US2)                  ← T008 [P] starts in parallel with Phase 3 tasks
              │
Phase 5 (Polish) ← starts after Phase 3 and Phase 4 complete
```

### User Story Dependencies

- **User Story 1 (P1)**: Depends on T001 (validation helper) + T003 (entity filter). No dependency on US2.
- **User Story 2 (P2)**: Depends on T001 (validation helper) + T003 (entity filter). Can start in parallel with US1 after Phase 2 completes.

### Within US1 (Phase 3)

```
T003 ──► T004 [P]                (contract test file — independent, can run alongside T005)
     └── T005 [P]                (repository changes — independent)
              │
         T001 + T005 ──► T006   (route parsing — needs both helper and repo)
                               │
                          T006 ──► T007  (integration test — needs working route)
```

### Within US2 (Phase 4)

```
T003 ──► T008 [P]               (email repository — can run in parallel with US1)
              │
         T001 + T008 ──► T009  (email route — needs helper and email repo)
                               │
                          T009 ──► T010  (integration test append — needs email route)
```

---

## Parallel Execution Examples

### Phase 3: User Story 1

After T003 completes, start these two tasks simultaneously:

```bash
# Terminal 1 — write contract test
# PartnershipListDeclinedFilterRouteGetTest.kt (T004)

# Terminal 2 — update repository
# PartnershipRepositoryExposed.kt (T005)
```

Then T006 (once T001 + T005 are done) → T007 (once T006 is done).

### Phase 3 + Phase 4 in parallel

After T003 completes, all three can start at once:

```bash
# Terminal 1 — T004: contract test
# Terminal 2 — T005: list repository
# Terminal 3 — T008: email repository
```

### Phase 5: Polish

T011 (OpenAPI) can run immediately after the implementation tasks complete. T012 and T013 must run sequentially in order (format → lint → test → validate).

---

## Implementation Strategy

**MVP Scope (Phase 1 + Phase 2 + Phase 3 only)**: Delivers User Story 1 — the list endpoint filter. This is the primary stated requirement and delivers immediate value. US2 (email) is a consistent but lower-priority extension.

**Incremental Delivery**:
1. Foundation (T001–T003): shared helpers, ~30 min
2. US1 implementation (T004–T007): list endpoint + tests, ~1 hour
3. US2 implementation (T008–T010): email endpoint + tests, ~30 min
4. Polish (T011–T013): OpenAPI + quality gates, ~15 min

**Risk area**: `openapi.yaml` is large (~4000+ lines) — use the exact YAML snippets in `contracts/api-changes.md` and confirm line numbers before editing.
