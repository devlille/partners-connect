# Feature Specification: Per-user favorite events

**Feature Branch**: `027-favorite-events`
**Created**: 2026-05-24
**Status**: Draft
**Input**: User description: "I would like to work on 3 new endpoints: put an event in favorite, delete a favorite and get list of favorite events. This should be protected for organisers only. Event should be part in one organisers where they have access."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add an event to my favorites (Priority: P1)

As an authenticated organiser, I can `PUT /users/me/favorite-events/{eventSlug}` to add an event I have access to (i.e. an event belonging to one of my organisations) to my personal favorites list.

**Why this priority**: This is the gateway action — without it, no favorites can exist and the list/remove endpoints are meaningless. It is also the endpoint that exercises every cross-cutting concern (auth, cross-org check, uniqueness).

**Independent Test**: With an organiser-permissioned user on org A and an event `event-A` belonging to org A, a `PUT /users/me/favorite-events/event-A` returns 201 Created and a follow-up `GET /users/me/favorite-events` returns a list containing `event-A`.

**Acceptance Scenarios**:

1. **Given** an authenticated organiser of org A and an event `event-A` belonging to org A, **When** they `PUT /users/me/favorite-events/event-A` for the first time, **Then** the server returns 201 Created with an empty body.
2. **Given** the same setup with `event-A` already favorited by the caller, **When** they `PUT /users/me/favorite-events/event-A` again, **Then** the server returns 409 Conflict with a message naming the event.
3. **Given** an authenticated organiser of org A and an event `event-B` belonging to org B (caller has no permission on org B), **When** they `PUT /users/me/favorite-events/event-B`, **Then** the server returns 403 Forbidden.
4. **Given** an authenticated organiser and an event slug that does not exist, **When** they `PUT /users/me/favorite-events/unknown`, **Then** the server returns 404 Not Found.
5. **Given** an unauthenticated request, **When** they `PUT /users/me/favorite-events/event-A`, **Then** the server returns 401 Unauthorized.

---

### User Story 2 - List my favorite events (Priority: P1)

As an authenticated organiser, I can `GET /users/me/favorite-events` to retrieve all the events I have favorited across every organisation I belong to, ordered by event `start_time` ascending.

**Why this priority**: Listing is the second indispensable half of the feature — the favorite action is only useful if the caller can later see what they favorited. Together with User Story 1, this story constitutes the minimum viable feature.

**Independent Test**: With an organiser who has favorited `event-A` (start 2026-06-15) and `event-C` (start 2026-04-10) across two organisations they belong to, `GET /users/me/favorite-events` returns 200 OK and a JSON array of `EventSummary` objects in this order: `event-C` then `event-A`.

**Acceptance Scenarios**:

1. **Given** an authenticated organiser with no favorites, **When** they `GET /users/me/favorite-events`, **Then** the server returns 200 OK with an empty JSON array `[]`.
2. **Given** an authenticated organiser with two favorited events in two different organisations, **When** they `GET /users/me/favorite-events`, **Then** the server returns 200 OK with a JSON array of two `EventSummary` objects ordered by `start_time` ascending.
3. **Given** two different organisers (user A and user B) where user A has favorited `event-A`, **When** user B calls `GET /users/me/favorite-events`, **Then** the server returns 200 OK with `[]` (user B does not see user A's favorites).
4. **Given** an unauthenticated request, **When** the route is called, **Then** the server returns 401 Unauthorized.

---

### User Story 3 - Remove an event from my favorites (Priority: P2)

As an authenticated organiser, I can `DELETE /users/me/favorite-events/{eventSlug}` to remove an event from my personal favorites.

**Why this priority**: P2 because the list and add stories are independently shippable; removal is the natural cleanup but the feature is not unusable without it for a first iteration. It is still expected to ship in the same PR — the priority is only about story ordering, not scope.

**Independent Test**: With an organiser who has previously favorited `event-A`, `DELETE /users/me/favorite-events/event-A` returns 204 No Content and a follow-up `GET /users/me/favorite-events` no longer contains `event-A`.

**Acceptance Scenarios**:

1. **Given** an authenticated organiser who has `event-A` in their favorites, **When** they `DELETE /users/me/favorite-events/event-A`, **Then** the server returns 204 No Content with an empty body.
2. **Given** an authenticated organiser whose favorites do **not** include `event-A` (event exists in one of their orgs), **When** they `DELETE /users/me/favorite-events/event-A`, **Then** the server returns 404 Not Found.
3. **Given** an authenticated organiser and an event slug that does not exist, **When** they call DELETE on that slug, **Then** the server returns 404 Not Found (same response as the "known but not favorited" case — see FR-009).
4. **Given** an authenticated organiser of org A and an event `event-B` belonging to org B (caller has no permission on org B), **When** they `DELETE /users/me/favorite-events/event-B`, **Then** the server returns 403 Forbidden.
5. **Given** an unauthenticated request, **When** they call DELETE on any slug, **Then** the server returns 401 Unauthorized.

---

### Edge Cases

- **Caller has no organiser permission anywhere**: any of the three endpoints called by a user who has zero rows in `organisation_permissions` returns 200 OK with `[]` for GET, and 403 Forbidden for PUT/DELETE on any concrete event slug (because the event resolves to an org the caller has no permission on). The route does not pre-emptively refuse based on "is the caller any kind of organiser" — the per-event org check is sufficient.
- **Concurrent PUT of the same favorite**: the unique index on `(user_id, event_id)` makes one of the inserts fail at the DB layer. The repository's `singleFavorite` check is a best-effort fast path; the `ExposedSQLException` on duplicate insert is caught and surfaced as a 409 Conflict to keep the contract uniform.
- **Event is deleted after being favorited**: the `event_id` foreign key uses Exposed's default `ON DELETE RESTRICT`, so an event with active favorites cannot be deleted until the favorites are cleared. This is consistent with how the rest of the codebase handles event references (no cascading deletes). Out of scope: a follow-up could decide to cascade.
- **User is revoked from an org while having favorites in it**: existing favorites remain in the table but become **invisible** in the GET response — the implementation filters by the caller's current org permissions, so revoked-org favorites are silently hidden. They can be re-revealed if the user is re-granted. The PUT/DELETE on such an event would then return 403. This avoids implicit data loss on revoke and matches how `/users/me/events` already behaves.
- **Empty event slug** (e.g. trailing slash `PUT /users/me/favorite-events/`): Ktor returns 404 Not Found from the routing layer; no special handling needed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose `GET /users/me/favorite-events` returning a JSON array of `EventSummary` objects, ordered by event `start_time` ascending. The empty case MUST return `[]`, not 404.
- **FR-002**: System MUST expose `PUT /users/me/favorite-events/{eventSlug}` that adds an event to the caller's favorites and returns 201 Created with an empty body on first add, 409 Conflict if the caller has already favorited the event.
- **FR-003**: System MUST expose `DELETE /users/me/favorite-events/{eventSlug}` that removes an event from the caller's favorites and returns 204 No Content on success.
- **FR-004**: All three endpoints MUST identify the caller from the bearer token via `AuthRepository.getUserInfo(token)` (same pattern used by `/users/me/events` and `/users/me/orgs`). They MUST NOT install `AuthorizedOrganisationPlugin` because the routes are not scoped to a single organisation.
- **FR-005**: Missing or invalid bearer token MUST result in 401 Unauthorized, raised by the existing `AuthRepository` failure path (`UnauthorizedException` → 401 via `App.kt`'s `configureStatusPage()`).
- **FR-006**: For PUT, when the event slug does not exist in the database, the system MUST throw `NotFoundException` ("Event \<slug\> not found"), mapped to 404 Not Found via `App.kt`. The DELETE behavior for unknown slugs is covered by FR-009 (collapsed into the uniform "not in your favorites" 404).
- **FR-007**: For PUT and DELETE, when the event exists but belongs to an organisation the caller has no permission on, the system MUST throw `ForbiddenException` (existing class under `internal/infrastructure/api/`) mapped to 403 Forbidden via `App.kt`. Permission is checked with the existing `OrganisationPermissionEntity.hasPermission(orgId, userId)` helper.
- **FR-008**: PUT MUST return 409 Conflict (via `ConflictException`) when a favorite already exists for `(caller, eventSlug)`. The repository SHOULD detect this via a `singleFavorite` lookup before insert; concurrent inserts that bypass the lookup MUST also be surfaced as 409 (catch and translate `ExposedSQLException` on the unique-index violation).
- **FR-009**: DELETE MUST return 404 Not Found when no favorite exists for `(caller, eventSlug)`. This collapses two cases into one response — "event slug unknown" and "event slug known but caller has not favorited it" — to keep the contract uniform.
- **FR-010**: The list endpoint MUST filter out favorites whose underlying event belongs to an organisation the caller no longer has permission on. Permission status is read live at request time (not snapshotted at favorite-creation time).
- **FR-011**: The OpenAPI source spec at `application/src/main/resources/openapi/openapi.yaml` MUST be updated to document the three new endpoints. The bundled `application/src/main/resources/openapi/documentation.yaml` MUST be regenerated via `npm run bundle`. Both `npm run validate` and `npm run bundle` MUST pass. The endpoints MUST reuse the existing `EventSummary` schema component for the GET response, and the existing shared `ErrorResponse` schema component for 4xx response bodies, mirroring the inline-status-code style used by `/orgs/{orgSlug}/ai/chat` (the project does not define `components/responses`; each path documents its status codes inline with `description:` and a `$ref` to `ErrorResponse`). **No new `*.schema.json` files** are added under `application/src/main/resources/schemas/` — PUT and DELETE have no request body and GET has no body either.
- **FR-012**: The new route function `Route.favoriteEventRoutes()` MUST be mounted in `App.kt`'s `routing { }` block alongside the other `*Routes()` calls.
- **FR-013**: The new `FavoriteEventRepositoryExposed` MUST be registered as `FavoriteEventRepository` in the existing `events/infrastructure/bindings/EventModule.kt` — no new top-level Koin module is created.
- **FR-014**: All new code MUST pass `./gradlew ktlintCheck detekt test --no-daemon`.

### Key Entities

- **`FavoriteEventsTable`** (`events/infrastructure/db/FavoriteEventsTable.kt`): `UUIDTable("favorite_events")` with columns `user_id` (reference to `UsersTable`), `event_id` (reference to `EventsTable`), `favorited_at` (datetime, defaulted to `Clock.System.now()` in UTC via `clientDefault`). `init { uniqueIndex(userId, eventId) }`. `favorited_at` is stored but not exposed in the API; it preserves the option to switch ordering or surface "recently favorited" in a follow-up without a migration.
- **`FavoriteEventEntity`** (`events/infrastructure/db/FavoriteEventEntity.kt`): `UUIDEntity` mapping with `var user by UserEntity referencedOn FavoriteEventsTable.userId`, `var event by EventEntity referencedOn FavoriteEventsTable.eventId`, `var favoritedAt by FavoriteEventsTable.favoritedAt`. Companion-object query helpers, as extension functions on `UUIDEntityClass<FavoriteEventEntity>`, are colocated in the same file:
  - `fun singleFavorite(userId: UUID, eventId: UUID): FavoriteEventEntity?` — used by PUT (409 detection) and DELETE (404 detection).
  - `fun listByUserOrderByEventStartTime(userId: UUID): List<FavoriteEventEntity>` — used by GET. Eager-loads `event` via `.with(FavoriteEventEntity::event)` and sorts client-side by `event.startTime`.
- **`FavoriteEventRepository`** (`events/domain/FavoriteEventRepository.kt`): the single business contract. Methods:
  - `fun listByUserEmail(userEmail: String): List<EventSummary>` — throws `NotFoundException` if no user matches the email (defensive; should not happen in practice because the bearer-token flow upserts the user).
  - `fun addFavorite(userEmail: String, eventSlug: String): Boolean` — returns `true` if a new row was inserted, `false` if the favorite already existed (caller maps `false` to 409). Throws `NotFoundException` on unknown event, `ForbiddenException` on cross-org.
  - `fun removeFavorite(userEmail: String, eventSlug: String): Boolean` — returns `true` if a row was deleted, `false` if no favorite existed (caller maps `false` to 404). Throws `ForbiddenException` on cross-org. Unknown-event deletion is also surfaced as `false` so that the route returns the uniform 404 from FR-009.
- **`FavoriteEventRepositoryExposed`** (`events/application/FavoriteEventRepositoryExposed.kt`): the Exposed-backed implementation. Each method runs inside a single `transaction { }`. Uses `UserEntity.singleUserByEmail`, `EventEntity.singleEventBySlug`, and `OrganisationPermissionEntity.hasPermission` (all existing helpers).
- **`Route.favoriteEventRoutes()`** (`events/infrastructure/api/FavoriteEventRoutes.kt`): the Ktor route. Mounts under `/users/me/favorite-events`. Each handler resolves the email from the bearer token then delegates to the repository. The handler translates the repository's `Boolean` returns into HTTP status codes (201 vs 409 for PUT, 204 vs 404 for DELETE).
- **`EventEntity.toEventSummary()`** (`events/application/mappers/Event.ext.kt`, **new file**): small mapper extracted from the inline mapping already used in `EventRepositoryExposed.findByUserEmail` (`EventRepositoryExposed.kt:249-256`). Shared between the existing `findByUserEmail` and the new `listByUserEmail` so the two endpoints stay in lockstep.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All three endpoints are reachable on a running server and return the documented status codes for the twelve cases enumerated in the testing plan (see `plan.md`).
- **SC-002**: All quality gates pass in CI: `./gradlew ktlintCheck detekt test --no-daemon` and `npm run validate`.
- **SC-003**: Contract-test coverage for the new `FavoriteEventRoutes` is **100 % of documented status codes** — one test per row of the response semantics table (twelve tests total).
- **SC-004**: One integration test verifies cross-user / cross-org isolation end-to-end (user A's favorites are invisible to user B; user A cannot favorite an event belonging to org B if they lack permission on B).
- **SC-005**: After deploy, an authenticated organiser can `PUT /users/me/favorite-events/<eventSlug>`, then `GET /users/me/favorite-events`, then `DELETE /users/me/favorite-events/<eventSlug>` against the preprod server and observe the documented response codes (201 → 200 with the event → 204 → 200 with `[]`).

## Out of scope for v1

- **Bulk operations** (PUT/DELETE multiple favorites in one call). The current scope is one event per call.
- **Pagination of the list endpoint**. Mirrors `/users/me/events`, which is also non-paginated; favorites are expected to be a small set (≤ tens).
- **Exposing `favorited_at` in the response payload**. The field is stored for future use only.
- **"Recently favorited" ordering**. Picked `start_time` ascending; can be revisited once UX feedback lands without a schema change.
- **Per-organisation favorites view** (e.g. `/orgs/{orgSlug}/users/me/favorite-events`). Favorites are user-global; if a per-org filter is needed later it can be a query parameter on the existing endpoint.
- **Shared / team favorites** (favorites visible to all members of an org). The current model is strictly per-user; a team-favorites feature would be a separate spec with a different table shape.
- **Cascading delete of favorites when an event is deleted**. Existing project convention is `ON DELETE RESTRICT` for event references; revisit in a follow-up if event deletion ever becomes a routine operation.
- **OpenAPI tagging** (the three endpoints are added without a `tags:` section, matching the convention the user requested for this feature).

## Cross-references

- **Implementation plan**: see `plan.md` in this folder — contains the file-by-file structure (table, entity, repository, route, Koin wiring, mapper extraction), the contract-test enumeration, and the integration-test workflow.
- **Related routes**: `/users/me/events` and `/users/me/orgs` in `users/infrastructure/api/UserRoutes.kt` — the new endpoints follow the same auth pattern (bearer-token email resolution, no `AuthorizedOrganisationPlugin`).
- **Permission helper**: `OrganisationPermissionEntity.hasPermission(orgId, userId)` in `users/infrastructure/db/OrganisationPermissionEntity.kt:36-42` — reused as-is for the cross-org check on PUT and DELETE.
- **Error mapping**: `App.kt`'s `configureStatusPage()` already maps `UnauthorizedException` → 401, `ForbiddenException` → 403, `NotFoundException` → 404, `ConflictException` → 409. No new exception classes or handlers are needed.
