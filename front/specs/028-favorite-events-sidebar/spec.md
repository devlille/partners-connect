# Feature Specification: Server-backed favorites + sidebar widget

**Feature Branch**: `028-favorite-events-sidebar`
**Created**: 2026-05-24
**Status**: Draft
**Input**: User description: "I would like to actual implementation based on local browser cache to use our new endpoints and I would like to display last 5 favorites events in the sidebar when we are in /orgs page."

## Clarifications

### Session 2026-05-24

- Q: Sync strategy between localStorage and the new server endpoints? → A: Drop localStorage entirely. Server is the source of truth. Use a Pinia store as an in-memory session cache; optimistic UI on add/remove with rollback on error. Full-page reload re-fetches.
- Q: The server's `GET /users/me/favorite-events` returns `EventSummary` which has no `org_slug` / `org_name`, but the UI needs both to build navigation URLs. How? → A: Extend `EventSummary` itself (in place) with flat `org_slug` and `org_name` fields. Every endpoint returning `EventSummary` gets the new fields for free (the `toEventSummary()` mapper is the single point of change). Non-breaking JSON addition.
- Q: Order of the sidebar's "last 5" list? → A: By event `start_time` ascending (next event first). Server's natural order. Take `items.slice(0, 5)`.
- Q: Sidebar location? → A: New "Mes favoris" section below the existing nav links, above the divider/footer. Existing static "Événements Favoris" link is preserved.
- Q: Which routes show the sidebar widget? → A: `/orgs` (index), `/orgs/favorites`, `/orgs/create`, and `/orgs/{slug}` (org overview). NOT inside `/orgs/{slug}/events/{eventSlug}/*` (deep event workflow). Implemented via a route-path check inside the widget component — pages do not opt in.
- Q: Empty state for the sidebar widget? → A: Hide the entire section when zero favorites OR when the store has not yet hydrated. No skeleton, no "empty state" copy. The existing fixed "Événements Favoris" nav link is the empty-state entry point.
- Q: What happens to the `addedAt` field on the existing favorites page? → A: Server does not expose `favorited_at` in the response. The "Ajouté le" column is replaced by "Date de l'événement" (`start_time`).
- Q: What happens to the existing 100-favorite cap and Zod validation in `useFavoriteEvents.ts`? → A: Both are dropped. The server is the authoritative validator. The generated Orval types from `utils/api.ts` are the contract.
- Q: i18n? → A: New UI strings ("Mes favoris", "Date de l'événement") use hardcoded French text matching the existing favorites page's style — the sidebar's existing labels are also hardcoded French.
- Q: Tests? → A: Two new Vitest specs — `stores/favorites.spec.ts` (store actions with mock API) and `components/DashboardFavorites.spec.ts` (rendering + route conditional). Match the existing `NoticeBlock.spec.ts` / `PartnershipForm.spec.ts` Vitest style.

## User Scenarios & Testing _(mandatory)_

### User Story 1 - See my upcoming favorites in the sidebar (Priority: P1)

As an organiser, when I navigate within the `/orgs` section of the app, I can see up to 5 of my favorite events directly in the sidebar — sorted by event start time, soonest first — so I can jump straight to any of them without going through the dedicated favorites page or the events list of each org.

**Why this priority**: This is the headline visible feature — the quick-access widget. The underlying server-sync refactor only exists to enable this.

**Independent Test**: As an authenticated organiser with at least one favorited event, navigate to `/orgs`; the sidebar shows a "Mes favoris" section below the main nav links with up to 5 event names ordered by `start_time` ascending, each linking to `/orgs/{org_slug}/events/{event_slug}`.

**Acceptance Scenarios**:

1. **Given** an organiser with 7 favorited events spanning 3 different organisations, **When** they navigate to `/orgs`, **Then** the sidebar's "Mes favoris" section shows the 5 events with the soonest `start_time`, each labeled with event name and a formatted start date, each clickable to the org-scoped event page.
2. **Given** an organiser with 0 favorited events, **When** they navigate to `/orgs`, **Then** the "Mes favoris" section is not rendered at all (no header, no divider, no empty-state copy).
3. **Given** an organiser whose favorites are still loading on first paint, **When** they navigate to `/orgs`, **Then** the sidebar's "Mes favoris" section is hidden until the GET resolves, then pops in (no skeleton).
4. **Given** an organiser inside `/orgs/{slug}/events/{eventSlug}/budget`, **When** they look at the sidebar, **Then** the "Mes favoris" section is hidden (deep-event routes are excluded).
5. **Given** an organiser on `/orgs/{slug}` (org overview), **When** they look at the sidebar, **Then** the "Mes favoris" section is visible.

---

### User Story 2 - Star and un-star events with the server (Priority: P1)

As an organiser, when I click the star button on any event in the events list of an organisation I belong to, the event is added to my server-side favorites and persists across browsers and sessions. Clicking the star again removes it. The UI feels instant — the star fills immediately, even before the server confirms.

**Why this priority**: Without server-backed star/unstar, the sidebar widget would have nothing to show across devices. This is the necessary plumbing.

**Independent Test**: On `/orgs/{slug}/events`, click the star (☆) next to an event; the star fills (⭐) immediately; reload the page in a new browser session; the star is still filled and the event appears in `/orgs/favorites` and in the sidebar widget.

**Acceptance Scenarios**:

1. **Given** an organiser looking at an event with an empty star, **When** they click the star, **Then** the star fills immediately (optimistic), and a successful `PUT /users/me/favorite-events/{eventSlug}` is sent in the background.
2. **Given** an organiser looking at an event with a filled star, **When** they click the star, **Then** the star empties immediately, and a successful `DELETE /users/me/favorite-events/{eventSlug}` is sent in the background.
3. **Given** a `PUT` race where the event is already favorited (server returns 409), **When** the response arrives, **Then** the UI keeps the star filled (the optimistic update is correct anyway). No error toast.
4. **Given** a `DELETE` race where the event is already not favorited (server returns 404), **When** the response arrives, **Then** the UI keeps the star empty. No error toast.
5. **Given** a server error other than 409/404 on `PUT` or `DELETE`, **When** the call fails, **Then** the optimistic UI change is rolled back and a toast notifies the user.

---

### User Story 3 - Favorites page works with the server (Priority: P2)

As an organiser, the existing `/orgs/favorites` page continues to work but now reflects my server-side favorites (cross-device). The columns are slightly different: the "Ajouté le" column is replaced by an event "Date de l'événement" column since the server does not expose the timestamp at which I starred the event.

**Why this priority**: The favorites page is a secondary surface (the sidebar is the headline). It must remain functional, but a slight column reshape is acceptable.

**Independent Test**: Visit `/orgs/favorites`; the table shows my favorited events with columns `Événement`, `Organisation`, `Date de l'événement`, `Actions` (with a trash button). The trash button removes the favorite both from the table and from `GET /users/me/favorite-events`.

**Acceptance Scenarios**:

1. **Given** an organiser with 3 favorited events, **When** they navigate to `/orgs/favorites`, **Then** the table shows 3 rows ordered by event `start_time` ascending.
2. **Given** an organiser on the favorites page who clicks the trash icon on a row, **When** the click resolves, **Then** the row disappears optimistically and a successful `DELETE` is sent in the background.
3. **Given** an organiser with 0 favorited events, **When** they navigate to `/orgs/favorites`, **Then** the existing empty-state placeholder (star icon + "Aucun événement favori" copy) is shown.

---

### Edge Cases

- **First-paint hydration**: The sidebar widget is hidden until the Pinia store's `load()` resolves. There is intentionally no skeleton (favorites are a secondary surface). Once the GET resolves, the widget renders without page reflow because the surrounding `<div v-if>` collapses to nothing when items are empty.
- **Authenticated but unfavorited**: GET returns `[]`. Store is hydrated with empty items. Widget stays hidden. Star buttons are still functional (clicking adds the first favorite).
- **Network failure on initial GET**: Store stays in `loaded = false` state, no items. Widget hidden. Existing global error-handling (`useErrorHandler`) surfaces a toast. The user can still see the static "Événements Favoris" nav link.
- **`PUT` succeeds but UI was already on a stale view** (e.g. event removed from server-side org list): The `PUT` returns 404 from the server side ("Event slug unknown"). The store rollback removes the optimistic entry and toasts. The star button reverts to empty.
- **403 on `PUT`** (the favoriting user no longer has permission on the target event's org): rollback + toast.
- **Multiple stars clicked quickly** on different events: each fires its own `PUT`. The store handles concurrent in-flight requests by indexing into `items` via slug. No queue, no debounce — REST conflicts are resolved by 409 → silent success.
- **Logout**: existing logout flow clears `localStorage.auth_token`. The Pinia store is not explicitly cleared but will be discarded on full-page reload (the auth middleware redirects to `/login`). Acceptable for v1.
- **Hard refresh on `/orgs/favorites`**: page-mount triggers `useFavoriteEvents()`, which lazy-hydrates the store on first read. While loading, the table shows the existing empty-state until items arrive.

## Requirements _(mandatory)_

### Functional Requirements

#### Server-side (the small EventSummary edit)

- **FR-001**: `EventSummary` (in `server/application/src/main/kotlin/fr/devlille/partners/connect/events/domain/EventSummary.kt`) MUST gain two new fields:
  - `@SerialName("org_slug") val orgSlug: String`
  - `@SerialName("org_name") val orgName: String`
- **FR-002**: The `EventEntity.toEventSummary()` mapper at `events/application/mappers/Event.ext.kt` MUST populate the new fields from `organisation.slug` and `organisation.name` (existing lazy DAO traversal within the surrounding transaction).
- **FR-003**: The four inline `EventSummary(...)` literals in `EventRepositoryExposed.kt` (`getAllEvents`, `findByOrgSlug`, `findByOrgSlugPaginated`, and any other site that constructs the type directly) MUST be updated to either pass the new fields or be replaced by `event.toEventSummary()` calls. The mapper is the single source of truth.
- **FR-004**: The OpenAPI source `server/application/src/main/resources/openapi/openapi.yaml` MUST be updated so the `EventSummary` schema declares the two new properties (`org_slug` and `org_name`, both required strings). `npm run validate` and `npm run bundle` MUST pass.
- **FR-005**: Existing server contract tests for endpoints returning `EventSummary` MUST be updated so any test that asserts the full JSON body recognizes the new fields (most tests assert by-field and will keep passing unmodified).
- **FR-006**: All server quality gates (`./gradlew ktlintCheck detekt test`) MUST pass.

#### Front-side (the bulk of the work)

- **FR-010**: Regenerate `front/utils/api.ts` against the updated local OpenAPI bundle so the generated `EventSummary` type includes `org_slug` and `org_name`.
- **FR-011**: Add a new Pinia store at `front/stores/favorites.ts` extending `EntityState<EventSummary>` (options-style, matching `stores/packs.ts`). State: `items: EventSummary[]`, `loading`, `error`, plus a `loaded: boolean` action flag. Actions:
  - `load()` — calls `getUsersMeFavoriteEvents`, populates `items`, sets `loaded = true`.
  - `add(eventSlug: string, optimistic?: EventSummary)` — if `optimistic` is provided, push it to `items` immediately; call `putUsersMeFavoriteEvent`; on non-409 error, rollback the optimistic push and toast.
  - `remove(eventSlug: string)` — splice from `items` immediately; call `deleteUsersMeFavoriteEvent`; on non-404 error, refetch via `load()` and toast.
  - `isFavorite(eventSlug: string): boolean` — getter backed by a `slugSet` computed.
- **FR-012**: Replace `front/composables/useFavoriteEvents.ts` with a thin wrapper over the store. Drop the Zod schema, drop the 100-favorite cap, drop the localStorage read/write/load. Drop the `addedAt` field everywhere. Public surface:
  - `favorites: ComputedRef<EventSummary[]>`
  - `top5: ComputedRef<EventSummary[]>` (= `favorites.slice(0, 5)`)
  - `isFavorite(eventSlug: string): boolean`
  - `addFavorite(eventSlug: string, optimistic?: EventSummary): Promise<void>`
  - `removeFavorite(eventSlug: string): Promise<void>`
  - `toggleFavorite(eventSlug: string, optimistic?: EventSummary): Promise<void>`
- **FR-013**: First call to `useFavoriteEvents()` in a client-side context MUST trigger `store.load()` if the store has not yet been hydrated (`store.loaded === false`). Subsequent calls within the same session reuse the cached `items`.
- **FR-014**: Create a new component `front/components/DashboardFavorites.vue` that:
  - Reads `top5` from `useFavoriteEvents()`.
  - Renders a "Mes favoris" section (header + nav list) only when `top5.length > 0` AND the current route is in scope (FR-015).
  - Each entry shows the event name (truncated if long) and a formatted `start_time`. The entry navigates to `/orgs/{org_slug}/events/{event_slug}`.
- **FR-015**: `DashboardFavorites.vue` MUST evaluate route eligibility inline using `useRoute().path`. Eligible if the path starts with `/orgs` AND does NOT contain `/events` in any segment. Examples:
  - Eligible: `/orgs`, `/orgs/favorites`, `/orgs/create`, `/orgs/foo`, `/orgs/foo/users`
  - Not eligible: `/orgs/foo/events`, `/orgs/foo/events/bar`, `/orgs/foo/events/bar/budget`
- **FR-016**: `Dashboard.vue` MUST mount `<DashboardFavorites />` between the main `<nav>` block and the footer divider/links. No prop or slot — the widget self-decides whether to render.
- **FR-017**: Refactor `pages/orgs/[slug]/events/index.vue` to call the new composable API: `isFavorite(event.slug)` (no `orgSlug` argument), `toggleFavorite(event.slug, eventSummaryForThisRow)`. The local row data already contains the `EventSummary` shape — pass it as the `optimistic` argument so the star fills immediately.
- **FR-018**: Refactor `pages/orgs/favorites.vue`:
  - Replace the "Ajouté le" column with "Date de l'événement" (`start_time`).
  - Adjust the row click handler to navigate to `/orgs/${fav.org_slug}/events/${fav.slug}` (using the new server-provided fields).
  - Adjust the trash button to call `removeFavorite(fav.slug)` (no `orgSlug` argument).
  - `getFavorites()` is replaced by reading `favorites.value` from the composable.
- **FR-019**: Add Vitest specs at `front/stores/favorites.spec.ts` and `front/components/DashboardFavorites.spec.ts` covering the behavioral semantics described in the User Stories (happy paths + 409/404 silent-success + rollback on other errors + route eligibility + empty-state hide).
- **FR-020**: All front quality gates (`pnpm lint && pnpm test:run && pnpm format:check`) MUST pass.

### Key Entities

- **`EventSummary`** (server domain + generated front type): existing data class, gains `orgSlug` + `orgName`. Used by the new favorites flow and four other event-list endpoints. The favorites flow does not introduce a new type.
- **`useFavoritesStore`** (`front/stores/favorites.ts`): options-style Pinia store extending `EntityState<EventSummary>`. The single source of truth for the user's favorites within a session.
- **`useFavoriteEvents`** (`front/composables/useFavoriteEvents.ts`): thin reactive wrapper around `useFavoritesStore`. Preserves the existing function names so the two existing callers see only signature changes, not a method rename.
- **`DashboardFavorites`** (`front/components/DashboardFavorites.vue`): new sidebar widget. Route-conditional, content-conditional, self-contained.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: With at least one favorited event, the sidebar "Mes favoris" section is visible on `/orgs`, `/orgs/favorites`, `/orgs/create`, `/orgs/{slug}` and any non-events sub-route — and hidden on every `/orgs/{slug}/events/*` route.
- **SC-002**: A new favorite created from `/orgs/{slug}/events` is reflected on the sidebar widget (after the next navigation/render) AND on `/orgs/favorites` AND survives a hard page refresh. The reverse is also true for un-starring.
- **SC-003**: The star button updates the UI in under one frame (16 ms) on click — the API roundtrip is hidden by the optimistic update.
- **SC-004**: A `PUT` or `DELETE` 5xx/3xx/other-4xx (excluding the documented 409/404) results in the UI reverting to the prior state and a toast.
- **SC-005**: `./gradlew ktlintCheck detekt test` AND `pnpm lint && pnpm test:run && pnpm format:check` AND `npm run validate && npm run bundle` (server-side) ALL pass.

## Out of scope for this PR

- **Favorites count badge** anywhere in the UI.
- **Drag-to-reorder** of favorites in the sidebar or page.
- **Bulk star/unstar** operations.
- **Pagination** of the favorites endpoint (server returns all; UI displays top-5 in sidebar and full list in page).
- **Star toggle on pages other than `/orgs/{slug}/events/index.vue`** (e.g. event detail pages don't get a star button). Existing single entry point preserved.
- **"Voir tout" link inside the sidebar widget** — the static "Événements Favoris" link in the main nav already serves that.
- **Tracking `favorited_at` in the UI** — the server stores it but does not expose it; the column is dropped from the favorites page.
- **i18n keys** — French strings hardcoded to match the rest of the sidebar's existing French-only labels.
- **Animation** on star fill, sidebar widget pop-in, or list reordering.
- **Skeleton loaders** for the sidebar or favorites page — both rely on hide-until-loaded for the first render.

## Cross-references

- **Server-side feature (already merged)**: `server/specs/027-favorite-events/` introduced the three endpoints. This spec adds the `org_slug`/`org_name` enrichment on the response.
- **Existing front composable being replaced**: `front/composables/useFavoriteEvents.ts` (localStorage-only, ~175 lines).
- **Existing front page being updated**: `front/pages/orgs/favorites.vue`.
- **Existing front page being updated**: `front/pages/orgs/[slug]/events/index.vue` (the star button site).
- **Server contract under enhancement**: `server/application/src/main/resources/openapi/openapi.yaml` — the `EventSummary` schema and the three favorite-events response refs.
