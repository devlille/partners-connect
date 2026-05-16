# Feature Specification: Partnership Flyer Generation (Frontend)

**Feature Branch**: `024-partnership-flyer-generation`
**Created**: 2026-05-17
**Status**: Draft
**Backend Spec**: `../../../server/specs/024-partnership-flyer-generation/spec.md`
**Input**: User description: "Frontend integration for the partnership flyer generation feature. Organisers attach a PNG flyer template + logo zone to each sponsoring pack; an organiser-triggered action then composes the company logo into that zone and stores the result on the partnership's communication support. The backend endpoints are already shipped; this spec covers only the frontend surfaces."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Organiser uploads a flyer template and defines the logo zone (Priority: P1)

As an organiser editing a sponsoring pack, I want to upload a PNG flyer template, see it rendered in the form, and draw the rectangular zone where partner logos will appear by clicking and dragging on the rendered template — refining the coordinates in numeric fields if needed — so that flyers can be generated for partnerships in that pack.

**Why this priority**: No flyer can be generated for any partnership until the pack has a template and zone configured. This is the prerequisite surface for the entire feature.

**Independent Test**: Can be fully tested by opening the pack create or edit page, uploading a PNG file, dragging a rectangle on the preview, saving the template, and verifying the pack reflects the template URL and zone after a reload.

**Acceptance Scenarios**:

1. **Given** a sponsoring pack with no flyer template, **When** the organiser uploads a PNG (≤ 10 MB), drags a rectangle on the preview, and clicks "Save template", **Then** the template is uploaded via the backend `PUT /flyer-template` endpoint and the pack form reflects the template + zone after the response succeeds.
2. **Given** a pack with an existing flyer template, **When** the organiser opens the pack edit page, **Then** the form displays the current template image and the configured zone overlaid on it, with the four numeric coordinates populated.
3. **Given** a pack with an existing flyer template, **When** the organiser clicks "Clear template", **Then** the backend `DELETE /flyer-template` endpoint is called, the local form state is reset to "no template", and the pack form no longer shows the section preview.
4. **Given** an uploaded template, **When** the organiser fine-tunes a coordinate in the numeric input (e.g. changes the width from 800 to 760), **Then** the rectangle overlay on the preview updates in real time, and the saved zone matches the numeric inputs.
5. **Given** an uploaded template of 1200 × 800 px, **When** the organiser tries to set a zone that extends beyond those bounds (e.g. x=900, width=400), **Then** a client-side validation error is shown next to the zone inputs and the "Save template" button is disabled until corrected.
6. **Given** a non-PNG file (e.g. JPG), **When** the organiser drops or selects it, **Then** a client-side error is shown ("PNG only") and the file is not accepted.

---

### User Story 2 - Organiser generates a flyer for a partnership (Priority: P2)

As an organiser browsing the communication management page, I want to see a "Generate flyer" button on each partnership's communication card, click it, and have the generated flyer appear in the existing communication-support area within seconds.

**Why this priority**: This is the action surface that turns a configured template into a partner-ready asset.

**Independent Test**: Can be fully tested by configuring a flyer-enabled pack, validating a partnership with a company logo, opening the communication page, clicking "Generate flyer" on that partnership's card, and verifying the flyer URL appears on the card after the request succeeds.

**Acceptance Scenarios**:

1. **Given** a validated partnership with a company logo and a flyer-enabled pack, **When** the organiser clicks "Generate flyer" on the partnership's card, **Then** the backend `POST /flyer` endpoint is called, a loading indicator is shown on the button, and on success the partnership's `communication_support_url` is updated in the local store and the card re-renders with the new flyer link.
2. **Given** a partnership that is not yet validated, **When** the organiser views the communication page, **Then** the "Generate flyer" button on the partnership's card is disabled with a tooltip explaining "Partnership is not yet validated".
3. **Given** a partnership whose company has no logo, **When** the organiser views the communication page, **Then** the "Generate flyer" button is disabled with a tooltip explaining "Company has no logo yet".
4. **Given** a partnership whose pack is not flyer-enabled, **When** the organiser clicks "Generate flyer", **Then** the request returns 409 from the backend, and the partnership card surfaces a toast with the backend's error message (e.g. "Pack is not flyer-enabled"). The button is not disabled in advance because pack-flyer-enabled status is not loaded with the partnership.
5. **Given** a partnership with an existing `communication_support_url`, **When** the organiser clicks "Generate flyer" again, **Then** the request proceeds without a confirmation prompt and the new flyer URL overwrites the previous one on the card (the backend confirms a single standard notification is sent regardless of first-generation vs. regeneration).

---

### Edge Cases

- What happens when the template PNG file is larger than 10 MB? The upload is rejected client-side with a clear message before the request is sent. The 10 MB cap mirrors typical pack-asset sizes; the backend has no hard cap but oversized uploads waste bandwidth and slow generation.
- What happens when the user uploads a PNG, draws a zone, but does not click "Save template" before navigating away? The template and zone are discarded (consistent with how the rest of the pack form handles unsaved changes — no autosave).
- What happens when the user resizes the browser window while the zone picker is open? The display ratio is recomputed; the rectangle overlay scales smoothly. The emitted template-pixel coordinates remain stable across window sizes.
- What happens when the network drops mid-upload? The upload fails with the standard error toast; the local form state stays at "pending upload" so the user can retry without re-selecting the file.
- What happens when the user clicks "Generate flyer" twice quickly? The button enters a loading state on the first click and rejects subsequent clicks until the response arrives (no debouncing needed beyond the button's `:loading` state).
- What happens when an organiser clicks "Generate flyer" on a partnership whose pack template was deleted between page-load and click? The backend returns 409 ("pack is not flyer-enabled") and the toast surfaces this. No client-side polling for pack changes is added.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The pack form (`SponsoringPackForm.vue`) MUST expose a "Flyer template" section that allows the organiser to upload a PNG template file, preview it, draw a logo zone, and save or clear the template via dedicated buttons.
- **FR-002**: The "Save template" action MUST be separate from the pack's basic-fields save action and MUST call the backend `PUT /orgs/{orgSlug}/events/{eventSlug}/packs/{packId}/flyer-template` endpoint with `file` (PNG bytes) and `zone` (JSON `{x, y, width, height}`) multipart parts.
- **FR-003**: The "Clear template" action MUST call the backend `DELETE /orgs/{orgSlug}/events/{eventSlug}/packs/{packId}/flyer-template` endpoint and reset the form section to "no template" on success.
- **FR-004**: The flyer template upload MUST reject non-PNG files client-side with a clear error message before any network call is made.
- **FR-005**: The flyer template upload MUST reject files larger than 10 MB client-side with a clear error message.
- **FR-006**: The zone picker MUST support drag-to-draw on the rendered template image as the primary interaction.
- **FR-007**: The zone picker MUST expose four numeric inputs (`x`, `y`, `width`, `height`) that are kept in sync with the drag interaction in both directions (changing a numeric value updates the rectangle overlay, and dragging the rectangle updates the numeric values).
- **FR-008**: The zone picker MUST emit zone coordinates in template-pixel space (the image's natural width × natural height), regardless of the display size at which the template is rendered.
- **FR-009**: The zone picker MUST disable the "Save template" button and show an inline error when the configured zone has non-positive width or height, negative coordinates, or extends beyond the template's natural dimensions.
- **FR-010**: The communication page (`pages/orgs/[slug]/events/[eventSlug]/communication.vue`) MUST expose a "Generate flyer" action on each partnership's `CommunicationCard` linked to a partnership (cards with `partnership_id` set).
- **FR-011**: The "Generate flyer" button MUST be disabled with a tooltip explaining the reason when the partnership is not yet validated.
- **FR-012**: The "Generate flyer" button MUST be disabled with a tooltip explaining the reason when the partnership's company has no logo.
- **FR-013**: The "Generate flyer" button MUST remain enabled for partnerships whose pack-flyer-enabled status is not known client-side. On click, if the backend returns 409 with the relevant precondition error, the frontend MUST surface the backend's error message as a toast.
- **FR-014**: The "Generate flyer" click MUST call the backend `POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/flyer` endpoint and show a loading state on the button until the response arrives.
- **FR-015**: On a successful flyer generation, the local Pinia store entry for the partnership MUST be patched with the new `communication_support_url` so the communication card re-renders with the new flyer link without a page reload.
- **FR-016**: On a failed flyer generation (4xx/5xx), the user MUST see a toast describing the failure; the local store MUST NOT be mutated.
- **FR-017**: All user-facing strings introduced by this feature MUST be added to `locales/{fr-FR,en-US,es-ES}.json` under a new top-level `flyer` namespace.
- **FR-018**: The backend's auto-generated client functions (via orval) MUST be the only path to calling the three new endpoints from frontend code — no hand-written `fetch`/axios calls.

### Key Entities

- **Flyer Template Configuration (form state)**: An in-memory composition of `{file: File | null, templateUrl: string | null, zone: {x, y, width, height} | null, naturalWidth: number | null, naturalHeight: number | null}` used by `FlyerTemplateConfig.vue`. Lives only in the pack form; persisted to the backend via the dedicated PUT/DELETE endpoints.
- **Generated Flyer (URL on the partnership)**: The existing `communication_support_url` string field on a partnership. The frontend mutates only its display, never its meaning. Already rendered in the partner-facing communication tab.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An organiser can upload a PNG, define the zone via drag, and save the template in under 1 minute on a typical pack edit page.
- **SC-002**: The zone picker's emitted coordinates remain accurate (≤ 1 px error) across browser window resizes between 320 px and 2560 px wide.
- **SC-003**: An organiser can trigger flyer generation from the communication page with a single click; the new flyer URL appears on the card within 12 seconds (10 s backend SLO + ~2 s rendering buffer).
- **SC-004**: 100% of precondition failures known client-side (not validated, no logo) result in a disabled button with a tooltip; 100% of precondition failures known only server-side (pack not flyer-enabled) result in a toast with the backend's error message.
- **SC-005**: No flyer-related code makes hand-rolled API calls; all three endpoints are consumed via orval-generated functions.

## Clarifications

### Session 2026-05-17

- Q: Should the partner-facing flyer download surface be redesigned as part of this feature? → A: No. The public communication-support link is already displayed in the existing partner-side communication tab; the flyer feature reuses it as-is.
- Q: Should the "Save template" action be tied to the pack's basic-fields save? → A: No. The flyer template upload is a separate, explicit action with its own button.
- Q: Drag-to-draw or numeric inputs only for the zone picker? → A: Drag-to-draw as primary, with numeric inputs as a secondary refinement; both bidirectional.
- Q: Should the "Generate flyer" button preemptively detect that the pack is not flyer-enabled? → A: No. Disable only for client-known preconditions (validated, logo). Rely on the backend 409 + toast for pack-flyer-enabled.
- Q: Should the frontend spec live at the same path as the backend spec? → A: Mirror the backend convention at `front/specs/024-partnership-flyer-generation/spec.md`.

## Assumptions

- The backend OpenAPI spec at `server/application/src/main/resources/openapi/openapi.yaml` is the source of truth for the three new endpoints (`PUT/DELETE .../packs/{packId}/flyer-template`, `POST .../partnerships/{partnershipId}/flyer`). The frontend regenerates its API client via orval against this spec; no hand-written client code is added.
- The existing `useSponsorsStore()` (`stores/sponsors.ts`) entry for a partnership carries enough fields to determine "validated" and "has logo" client-side. If a field is missing, the partnership list query is extended to include it as part of this work; no new backend endpoint is added.
- The partner-facing display of the generated flyer is the existing communication tab's public communication-support link. No new partner-side route or component is introduced.
- The zone picker is implemented from scratch using HTML5 `<canvas>` + mouse-event listeners rather than pulling in a third-party crop/picker library. The interaction is simple enough (one rectangle, one image) that a dependency is not justified.
- The flyer template upload size cap is 10 MB client-side, chosen to match typical PNG flyer template sizes. The backend has no explicit cap on template size (its 500 MB cap is for the support-video endpoint, not this one).
- The "Generate flyer" button does not show a confirmation prompt before regenerating. Regeneration is cheap, the backend notifications are non-spammy (single standard email + Slack), and a prompt adds friction without preventing meaningful mistakes.
- Pinia store mutation after successful generation patches only the partnership's `communication_support_url` field. No refetch of the full partnership list is triggered.
- The three new orval-generated functions (`putOrgsEventsPacksFlyerTemplate`, `deleteOrgsEventsPacksFlyerTemplate`, `postOrgsEventsPartnershipsFlyer`) are the only path to the three new endpoints. Their generated names are determined by the backend's `operationId` values and are stable across orval re-runs.
- No tests for the flyer generation algorithm are added on the frontend. The composition is server-side; the frontend tests cover only its own UX (component rendering, zone picker math, store mutation on success).
