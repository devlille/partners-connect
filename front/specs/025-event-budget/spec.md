# Feature Specification: Front Budget Screen

**Feature Branch**: `025-event-budget`
**Created**: 2026-05-22
**Status**: Draft
**Input**: User description: "Add a new Budget item in the sidebar under Dashboard. In this new screen, display a card with global totals price and the whole partnership list below. Add a filter by pack which edits the card price and the partnership list with the pack selected."

## Clarifications

### Session 2026-05-22

- Q: Filter control shape? → A: USelect dropdown above the totals card. Single-select. Default option is "All packs".
- Q: Totals card layout? → A: Three primary numbers (paid, validated, total) with the two derived numbers (validated − paid, total − validated) shown as smaller hints under the corresponding primaries.
- Q: Partnership list columns? → A: Pack + Company + Price + Status. The Pack column is hidden when a single pack is selected (redundant with the filter).
- Q: Price format? → A: French locale via `Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })`. The `currency` field on the API response drives the formatter's currency code.
- Q: Status visualization? → A: `UBadge` with semantic colors — `paid` = success (green), `validated` = primary (blue), `submitted` = neutral (gray).
- Q: Pagination? → A: None. Data is bounded by event size (≤200 partnerships) and the API returns the full payload.
- Q: i18n? → A: Page body uses `$t('budget.*')` keys added to fr-FR, en-US, es-ES. Sidebar label stays hardcoded "Budget" matching the existing `useEventLinks.ts` style.
- Q: Tests? → A: Out of scope for v1 — sibling pages (sponsors, agenda, etc.) have no unit tests; visual verification via `pnpm dev` is the practical gate.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View global event budget (Priority: P1)

As an event organiser, I need a dedicated screen that shows my event's total budget at a glance — what's been paid, what's contractually committed, and what's still in the pipeline — so I can track cash flow and pipeline health without piecing it together from the partnerships list.

**Why this priority**: Headline numbers are the core ask; the per-pack filter only refines what's already useful on its own.

**Independent Test**: From the sidebar, click "Budget"; the screen displays a totals card with non-zero values for an event that has at least one partnership in any state, and the partnership list shows all non-declined partnerships.

**Acceptance Scenarios**:

1. **Given** an event with mixed-state partnerships (some paid, some validated, some submitted), **When** I navigate to the Budget screen, **Then** the totals card shows the three primary amounts (paid, validated, total) and the two derived hints (validated−paid, total−validated) for the whole event.
2. **Given** an event with no partnerships, **When** I navigate to the Budget screen, **Then** the totals card shows zeros for all five metrics and the partnership list shows an empty state.
3. **Given** an event whose data is still loading, **When** I open the Budget screen, **Then** a skeleton placeholder is shown until the API responds.
4. **Given** the API returns an error, **When** I open the Budget screen, **Then** a red banner with the error message is shown and the user can retry by reloading.

---

### User Story 2 - Filter the budget by pack (Priority: P2)

As an event organiser, I need to switch between "All packs" and a specific pack so that I can verify per-pack revenue and spot partnerships that may be priced inconsistently within a pack.

**Why this priority**: Augments US1 without blocking it. Useful for events with several packs.

**Independent Test**: Select a specific pack from the dropdown — the totals card numbers change to that pack's `totals` and the partnership list narrows to that pack's partnerships only.

**Acceptance Scenarios**:

1. **Given** the Budget screen is loaded with multiple packs available, **When** I open the pack dropdown, **Then** I see "All packs" followed by every pack name that has at least one non-declined partnership.
2. **Given** the dropdown is set to "All packs", **When** I select "Gold", **Then** the totals card shows Gold's `totals` (not the event-level totals) and the partnership list shows only Gold's partnerships; the "Pack" column is hidden because it would be redundant.
3. **Given** the dropdown is set to a specific pack, **When** I switch it back to "All packs", **Then** the card and the list both return to the event-wide view and the "Pack" column reappears.
4. **Given** a pack has no partnerships, **When** I look at the dropdown, **Then** that pack does NOT appear (the API doesn't return packs with zero partnerships).

---

### User Story 3 - Navigate to the Budget screen from the sidebar (Priority: P3)

As an event organiser, I need a clearly labelled Budget entry in the event sidebar so the feature is discoverable.

**Why this priority**: Discoverability is a prerequisite, but trivial — it's one line in `useEventLinks.ts`.

**Independent Test**: Open any page under `/orgs/[slug]/events/[eventSlug]/...`; the sidebar lists "Budget" with a money/currency icon immediately below "Dashboard".

**Acceptance Scenarios**:

1. **Given** I am on any event-scoped page, **When** I look at the sidebar, **Then** "Budget" appears as the second entry, right below "Dashboard".
2. **Given** I am on the Budget screen, **When** I look at the sidebar, **Then** the "Budget" entry is visually marked as active (matches the existing active-class styling).

---

### Edge Cases

- Event has zero partnerships → totals card renders all zeros; empty-state message for the partnership list; pack dropdown contains only "All packs".
- Pack dropdown contains only one pack → "All packs" is still the default; selecting the single pack hides the Pack column and behaves identically to "All packs" minus the column.
- A partnership has `status = "submitted"` (no validatedAt yet) but is paid (data inconsistency) → status displays as `paid` (server-side precedence ensures paid wins over validated/submitted; the front trusts the field as returned).
- Locale switching → all formatted prices and labels respect the active i18n locale. The `currency` field is read from the API response, not hardcoded.
- A user navigates to `/budget` by URL without prior authentication → `authMiddleware` redirects to the auth flow, identical to sibling pages.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST add a "Budget" entry to `useEventLinks.ts` immediately after the "Dashboard" entry, with route `/orgs/${orgSlug}/events/${eventSlug}/budget` and the `i-heroicons-banknotes` icon.
- **FR-002**: The system MUST create a new page at `pages/orgs/[slug]/events/[eventSlug]/budget.vue`, wrapped in the existing `Dashboard` layout with `eventLinks` and `footerLinks`.
- **FR-003**: On mount, the page MUST call `getOrgsEventsBudget(orgSlug, eventSlug)` from `~/utils/api.ts` and store the response in a local `ref`.
- **FR-004**: While the API call is in flight, the page MUST display a skeleton placeholder; on error, a red banner with the error message; on success, the totals card and the partnership list.
- **FR-005**: The page MUST render a `BudgetTotalsCard` component that receives `BudgetTotalsSchema` and a currency code (string), and displays the three primary numbers (`paid`, `validated`, `total`) with `validated_minus_paid` and `total_minus_validated` shown as smaller hints under the matching primaries.
- **FR-006**: All monetary values MUST be formatted via `Intl.NumberFormat(locale, { style: 'currency', currency, maximumFractionDigits: 0 })` where `locale` matches the i18n active locale (`fr-FR` by default) and `currency` comes from the API response.
- **FR-007**: The page MUST render a pack filter via `USelect` placed above the totals card. The select options are `[{ label: $t('budget.allPacks'), value: null }, ...budget.packs.map(p => ({ label: p.pack_name, value: p.pack_id }))]`. Default selection is `null`.
- **FR-008**: When the selected pack ID is `null`, the totals card MUST display `budget.totals` and the partnership list MUST contain the flat union of every pack's `partnerships`. Order: packs in the API order (alphabetical by pack name), with each pack's partnerships preserving their company-name-asc sort. The list MUST include a "Pack" column showing the originating pack name.
- **FR-009**: When the selected pack ID is non-null, the totals card MUST display `selectedPack.totals` and the partnership list MUST contain only that pack's `partnerships`. The "Pack" column MUST be hidden.
- **FR-010**: The partnership list MUST render via `UTable` with these columns: (when applicable) Pack name (string), Company name (string), Price applied (formatted currency), Status (`StatusBadge` component).
- **FR-011**: The `StatusBadge` component MUST accept a `status` prop with value `paid` | `validated` | `submitted` and render a `UBadge` with colors `success` | `primary` | `neutral` respectively. Label text MUST come from `$t('budget.status.paid'|.validated|.submitted)`.
- **FR-012**: When the partnership list is empty (no rows for the current filter selection), the page MUST display an empty-state message using the same pattern as the sponsors page (`text-center py-12`).
- **FR-013**: The page MUST set `definePageMeta({ middleware: authMiddleware, ssr: false })` matching sibling pages.
- **FR-014**: New i18n keys MUST be added in `fr-FR.json`, `en-US.json`, and `es-ES.json` under a `budget.*` namespace covering: `title`, `allPacks`, columns (`pack`, `company`, `price`, `status` headers), status labels (`status.paid`, `status.validated`, `status.submitted`), `totals.paid`, `totals.validated`, `totals.total`, `totals.toCome`, `totals.pipeline`, `empty`, `loading`, `error`.
- **FR-015**: A `utils/formatPrice.ts` helper MUST be created exporting `formatPrice(value: number, currency: string, locale?: string): string` that wraps `Intl.NumberFormat`. It is consumed by `TotalsCard.vue` and the Price column cell renderer.

### Key Entities

- **Sidebar Link**: One entry in `useEventLinks.ts` — `{ label, icon, to }`.
- **Budget Response**: `EventBudgetSchema` from `~/utils/api.ts` — already generated.
- **Pack Filter State**: A single nullable string (`packId | null`) held in a `ref` on the page.
- **Totals Card Props**: `{ totals: BudgetTotalsSchema, currency: string }` — derived from the page state.
- **Partnership Row**: Either `PartnershipBudgetItemSchema` directly (when filtered) or augmented with `pack_name` (when "All packs" is selected).

## Assumptions

- The Orval-generated client already exports `getOrgsEventsBudget`, `EventBudgetSchema`, `BudgetTotalsSchema`, `PackBudgetSchema`, `PartnershipBudgetItemSchema`, and `PartnershipBudgetItemSchemaStatus` (verified in `utils/api.ts` on this branch).
- The `Dashboard` component, `eventLinks` composable, `footerLinks` composable, `useRouteParams`, and `authMiddleware` follow the patterns used by `pages/orgs/[slug]/events/[eventSlug]/sponsors/index.vue` (verified during exploration).
- The active i18n locale is determined by `@nuxtjs/i18n`; the formatter reads it via `useI18n().locale`.
- The pack dropdown contains only packs that the API returned (packs with zero non-declined partnerships are absent from `budget.packs[]`).
- French is the primary language; English and Spanish translations are added with reasonable equivalents but no professional review is required for v1.
- No client-side caching layer (no Pinia store) — the page fetches on every mount, matching the existing sponsors/dashboard pattern.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An organiser can land on the Budget screen and read the event-level paid / validated / total amounts within 1 second after the API response, with correctly formatted currency strings.
- **SC-002**: Switching the pack filter updates both the totals card and the partnership list in under 50ms (client-side only — no API roundtrip).
- **SC-003**: The "Pack" column appears in the list when and only when the filter is "All packs"; it never appears when a specific pack is selected.
- **SC-004**: 100% of monetary values rendered on the screen are produced by `formatPrice()` — no raw integers or ad-hoc string concatenation in the templates.
- **SC-005**: The Budget sidebar entry appears immediately below Dashboard on every event-scoped page and routes correctly when clicked.
- **SC-006**: Empty event (zero partnerships) renders zeros + empty-state without any console errors or runtime warnings.
