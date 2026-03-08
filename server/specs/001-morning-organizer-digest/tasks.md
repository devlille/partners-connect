# Tasks: Morning Organiser Daily Digest

**Feature**: `001-morning-organizer-digest`
**Input**: Design documents from `/specs/001-morning-organizer-digest/`
**Tech stack**: Kotlin 2.1.21 · Ktor 3.2.0 · Exposed 1.0.0-beta-2 · Koin 4.1.0
**Base package**: `fr.devlille.partners.connect`
**Base path**: `application/src/main/kotlin/fr/devlille/partners/connect/`

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no incomplete dependencies)
- **[US#]**: User story this task belongs to
- Exact file paths included in every task description

---

## Phase 1: Setup

**Purpose**: Create the new module directory skeleton so all subsequent tasks have a known target path.

- [X] T001 Create directory structure: `digest/domain/`, `digest/application/`, `digest/infrastructure/api/`, `digest/infrastructure/bindings/` under the base package path, and `application/src/main/resources/notifications/slack/digest/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain objects, templates, route wiring, and Koin registration that ALL user stories depend on. No user story implementation can begin until this phase is done.

- [X] T002 [P] Create `DigestEntry.kt` — `data class DigestEntry(val companyName: String, val partnershipLink: String)` in `digest/domain/DigestEntry.kt`
- [X] T003 [P] Create `EventDigest.kt` — `data class EventDigest(val event: EventWithOrganisation, val agreementItems: List<DigestEntry>, val quoteItems: List<DigestEntry>, val socialMediaItems: List<DigestEntry>)` with computed `hasItems` boolean, in `digest/domain/EventDigest.kt`
- [X] T004 [P] Create `DigestRepository.kt` — interface with `suspend fun queryDigest(eventId: UUID, today: LocalDate): EventDigest` in `digest/domain/DigestRepository.kt`
- [X] T005 [P] Create Slack template files `application/src/main/resources/notifications/slack/digest/fr.md` and `application/src/main/resources/notifications/slack/digest/en.md` with `{{event_name}}`, `{{agreement_section}}`, `{{quote_section}}`, `{{social_media_section}}` placeholders
- [X] T006 Add `NotificationVariables.MorningDigest` subclass to `notifications/domain/NotificationVariables.kt` — `usageName = "digest"`, `company` implemented as `error("Not applicable")`, `populate()` substitutes all four placeholders; add top-level `buildSection(items, language, type)` helper returning mrkdwn block or `""` (depends on T002)
- [X] T007 Create `DigestRepositoryExposed.kt` skeleton in `digest/application/DigestRepositoryExposed.kt` — implements `DigestRepository`, returns `EventDigest` with all three lists empty (`emptyList()`); throws `NotFoundException` if event not found (depends on T002, T003, T004)
- [X] T008 Create `DigestRoutes.kt` in `digest/infrastructure/api/DigestRoutes.kt` — `POST /orgs/{orgSlug}/events/{eventId}/jobs/digest`; reads `Accept-Language` header (default `fr`); calls `digestRepository.queryDigest()`; if `hasItems` builds `NotificationVariables.MorningDigest` and calls `notificationRepository.sendMessageFromMessaging(variables)`; responds `204` (depends on T003, T004, T006)
- [X] T009 Create `DigestModule.kt` in `digest/infrastructure/bindings/DigestModule.kt` — Koin module binding `DigestRepository` to `DigestRepositoryExposed` (depends on T004, T007)
- [X] T010 Register `DigestModule` and mount `digestRoutes()` in `App.kt` (depends on T008, T009)
- [X] T011 [P] Create `DigestJobRoutePostTest.kt` in `digest/infrastructure/api/DigestJobRoutePostTest.kt` — contract test: `POST` returns `204` when event exists with no actionable items; returns `404` when event ID is unknown (depends on T008, T009, T010)
- [X] T012 Create `DigestJobRoutesTest.kt` shell in `digest/DigestJobRoutesTest.kt` — `testApplication` + `moduleSharedDb` setup; import factory helpers; empty test class body ready for per-US scenarios (depends on T010)

**Checkpoint**: Foundation complete — route compiles and returns 204, no Slack message sent (all lists empty). Contract test passes. User story implementation can now begin.

---

## Phase 3: User Story 1 — Agreement-Readiness Reminders (Priority: P1) 🎯 MVP

**Goal**: Organiser receives a Slack message listing partner companies that are ready for agreement generation but have not yet had their agreement produced.

**Independent Test**: Create an event with a Slack integration and several partnerships — some with all required agreement fields set and `agreement_url` null, others missing fields or with `agreement_url` non-null or with `declined_at` non-null. Trigger `POST /orgs/{orgSlug}/events/{eventId}/jobs/digest`. Verify only fully-ready, non-generated, non-declined partnerships appear in the agreement section.

- [X] T013 [US1] Implement agreement-readiness query in `DigestRepositoryExposed.kt`: join `partnerships`, `companies`; filter `declined_at IS NULL`, `validated_at IS NOT NULL`, `agreement_url IS NULL`, company fields non-null/non-blank (`name`, `siret`, `address`, `zip_code`, `city`, `country`), `contact_name`/`contact_role` non-blank; build `DigestEntry` list for `agreementItems` (depends on T007)
- [X] T014 [US1] Add US1 acceptance scenarios to `DigestJobRoutesTest.kt`: partnership with all agreement data → appears in section; missing `siret` → excluded; `agreement_url` non-null → excluded; `declined_at` non-null → excluded; no qualifying items → no Slack call (depends on T012, T013)

**Checkpoint**: US1 fully functional — agreement section appears in Slack message with correct items only. Quote and social-media sections are empty/omitted. Deliverable standalone value.

---

## Phase 4: User Story 2 — Quote-Readiness Reminders (Priority: P2)

**Goal**: Organiser receives a Slack message listing partner companies that are ready for quote generation but have no quote yet produced.

**Independent Test**: Create partnerships with varying billing completeness. Trigger the endpoint. Verify only partnerships with all required billing fields, `validated_at` non-null, `base_price > 0`, and no existing `quote_pdf_url` appear in the quote section. Verify US1 agreement section still works in the same message when applicable.

- [X] T015 [US2] Add quote-readiness query to `DigestRepositoryExposed.kt`: join `billings` LEFT OUTER, `sponsoring_packs`; filter `declined_at IS NULL`, `validated_at IS NOT NULL`, no billing row with non-null `quote_pdf_url`, all company address fields non-null/non-blank, `selected_pack_id IS NOT NULL`, `base_price > 0`; build `DigestEntry` list for `quoteItems` (depends on T013)
- [X] T016 [US2] Add US2 acceptance scenarios to `DigestJobRoutesTest.kt`: partnership with complete billing data and no quote → appears in section; missing billing address → excluded; `quote_pdf_url` non-null → excluded; `base_price = 0` → excluded; both agreement and quote sections present in single message when applicable (depends on T014, T015)

**Checkpoint**: US2 fully functional — quote section appears correctly alongside agreement section in one consolidated Slack message.

---

## Phase 5: User Story 3 — Social Media Announcement Reminders (Priority: P3)

**Goal**: Organiser receives a Slack message listing partner companies whose planned social media announcement date is today, preventing missed scheduled communications.

**Independent Test**: Set `communication_publication_date` to today on one partnership, yesterday on another, tomorrow on a third, null on a fourth. Trigger the endpoint. Only today's partnership should appear in the social media section. Verify declined partnerships with today's date are also excluded.

- [X] T017 [US3] Add social-media-due query to `DigestRepositoryExposed.kt`: filter `declined_at IS NULL`, `DATE(communication_publication_date) = today` (UTC date comparison using Exposed date function); build `DigestEntry` list for `socialMediaItems` (depends on T015)
- [X] T018 [US3] Add US3 acceptance scenarios to `DigestJobRoutesTest.kt`: `communication_publication_date` = today → appears in section; yesterday → excluded; tomorrow → excluded; null → excluded; `declined_at` non-null with today's date → excluded; all three sections present together in a single message (depends on T016, T017)

**Checkpoint**: US3 fully functional — all three sections appear correctly in one consolidated Slack message. Feature is complete end-to-end.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Quality gates required before merge. Must pass CI.

- [X] T019 Run `./gradlew ktlintFormat --no-daemon` from `server/` and fix any remaining formatting violations across all new and modified files
- [X] T020 Run `./gradlew detekt --no-daemon` from `server/` and manually address any reported violations in all new and modified files
- [X] T021 Add `POST /orgs/{orgSlug}/events/{eventId}/jobs/digest` to `application/src/main/resources/openapi.yaml` — document as a public job-action endpoint with `security: - {}` (no bearer auth), `Accept-Language` header parameter, `204 No Content` and `404 Not Found` responses, and `operationId: postDigestJob` (depends on T008)
- [X] T022 [P] Add FR-011 Slack failure-isolation scenario to `digest/DigestJobRoutesTest.kt` — verify that when the Slack gateway throws an error the endpoint still returns `204 No Content` and the exception does not propagate as a 500 response (depends on T012)

---

## Dependencies

```
T002 ──┐
T003 ──┼──► T007 ──► T013 ──► T015 ──► T017
T004 ──┘         └──► T014     └──► T016     └──► T018
T002 ──► T006 ──► T008 ──┐
T004 ──► T009 ──────────┼──► T010 ──► T011
                         └──────────► T012 ──► T014
T005 (independent — no blockers)
T019, T020, T021 (after all implementation tasks; T021 depends on T008)
T022 (after T012; can run in parallel with T013–T018)
```

**User story completion order**: US1 (T013-T014) → US2 (T015-T016) → US3 (T017-T018)
**MVP scope**: Complete Phase 1 + Phase 2 + Phase 3 (US1 only) for the first deliverable increment.

---

## Parallel Execution Opportunities

### Within Phase 2 (independent new files)
Run simultaneously: **T002, T003, T004, T005**

### After T002-T005 complete
Run simultaneously: **T006, T007**; then T008 (needs T006), T009 (needs T007)

### After T010
Run simultaneously: **T011, T012**

### Per user story
Each US phase is sequential internally (implementation → test). US1 must complete before US2 can start (same `DigestRepositoryExposed.kt` file being extended).

---

## Implementation Strategy

1. **MVP**: Phases 1 + 2 + 3 deliver US1 (agreement reminders) as a standalone, testable, shippable increment.
2. **Increment 2**: Phase 4 (US2 — quote reminders) extends the query and adds a second message section; no structural changes needed.
3. **Increment 3**: Phase 5 (US3 — social media reminders) completes the feature by adding the third section.
4. **Each increment** leaves the system in a fully working, quality-gated state.
