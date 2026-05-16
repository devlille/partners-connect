# Feature Specification: Partnership Flyer Generation

**Feature Branch**: `024-partnership-flyer-generation`
**Created**: 2026-05-16
**Status**: Draft
**Input**: User description: "Allow organisers to attach a flyer template (PNG) to every sponsoring pack and specify a logo zone (in template pixels). Expose a backend endpoint that composes the partner's company logo into that zone and stores the result as the partnership's communication support. Generation is restricted to validated partnerships whose company has a logo. On success, partnership contacts receive an email, organisers receive a Slack message, and partnerships still missing a flyer are surfaced in the morning organiser digest."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Organiser configures a flyer template on a pack (Priority: P1)

As an organiser, I want to upload a PNG flyer template on a sponsoring pack and draw the rectangular zone where partner logos should appear, so that the backend can produce per-partnership flyers automatically.

**Why this priority**: Without a configured template and zone, no flyer can ever be generated for partnerships of that pack. This is the foundational prerequisite for the entire feature.

**Independent Test**: Can be fully tested by uploading a PNG template and zone coordinates against a pack, then retrieving the pack and verifying the template URL and zone are persisted and visible.

**Acceptance Scenarios**:

1. **Given** a sponsoring pack with no flyer template, **When** the organiser uploads a PNG template and provides zone coordinates `(x, y, width, height)`, **Then** the template is stored, the pack reflects the template URL and zone, and the pack is flagged as flyer-enabled.
2. **Given** a sponsoring pack with an existing flyer template, **When** the organiser uploads a new template, **Then** the old template file is deleted and the pack reflects the new template URL and zone.
3. **Given** a sponsoring pack with a flyer template, **When** the organiser clears the template, **Then** the template URL and all four zone coordinates are nulled, the template file is deleted, and the pack is no longer flyer-enabled.

---

### User Story 2 - Organiser generates a flyer for a validated partnership (Priority: P2)

As an organiser, I want to trigger flyer generation for a specific partnership, so that the partner immediately has a branded communication asset usable on their channels.

**Why this priority**: This is the core value of the feature — turning a configured template into a partner-ready asset.

**Independent Test**: Can be fully tested by configuring a flyer-enabled pack, validating a partnership with a company logo, calling the generate endpoint, and verifying the partnership's `communication_support_url` points to the new flyer image.

**Acceptance Scenarios**:

1. **Given** a validated partnership whose company has a logo and whose pack is flyer-enabled, **When** the organiser calls the flyer generation endpoint, **Then** a JPG flyer is composed (company logo centred inside the configured zone, preserving aspect ratio with a 20px margin), uploaded to storage, and the partnership's `communication_support_url` is set to the new flyer URL.
2. **Given** a validated partnership with an existing `communication_support_url`, **When** the organiser regenerates the flyer, **Then** the new flyer URL overwrites the existing value (no special notification beyond the standard one).
3. **Given** a partnership that is not yet validated, **When** the organiser calls the flyer generation endpoint, **Then** the request is rejected with a 4xx error indicating the partnership must be validated first.
4. **Given** a validated partnership whose company has no logo (neither 1000px variant nor original), **When** the organiser calls the flyer generation endpoint, **Then** the request is rejected with a 4xx error indicating the company logo is missing.
5. **Given** a validated partnership whose pack does not have a configured flyer template, **When** the organiser calls the flyer generation endpoint, **Then** the request is rejected with a 4xx error indicating the pack is not flyer-enabled.

---

### User Story 3 - Partnership contacts and organisers are notified (Priority: P3)

As a partnership contact, I want to receive an email when my flyer is generated so I can use it on my channels. As an organiser, I want a Slack message in the organiser channel so the team has visibility on what was generated.

**Why this priority**: Notification is what closes the loop between organiser action and partner awareness. Without it, partners would not know an asset is ready.

**Independent Test**: Can be fully tested by triggering generation on a validated partnership and verifying both an email is delivered to the partnership contacts and a Slack message is delivered to the configured organiser channel.

**Acceptance Scenarios**:

1. **Given** a successful flyer generation, **When** the response is returned, **Then** an email is dispatched to every partnership contact in their preferred language, containing the company name, event name, partnership link, and flyer URL.
2. **Given** a successful flyer generation on an event with a configured Slack integration, **When** the response is returned, **Then** a Slack message is posted to the configured organiser channel naming the partnership and linking to the generated flyer.
3. **Given** a successful flyer generation on an event with no Slack integration configured, **When** the response is returned, **Then** the email is still delivered and the Slack delivery is reported as skipped (no failure).

---

### User Story 4 - Morning digest lists partnerships missing a flyer (Priority: P4)

As an organiser, I want the morning digest to remind me which validated partnerships still need a flyer, so that I can batch generation without manually scanning the partnership list.

**Why this priority**: The digest is the existing daily checklist for organisers. Surfacing missing flyers there fits the established workflow.

**Independent Test**: Can be fully tested by setting up validated partnerships in mixed states (some with flyers, some without, on flyer-enabled vs. non-flyer-enabled packs) and verifying the digest response includes only those needing a flyer.

**Acceptance Scenarios**:

1. **Given** validated partnerships, some with flyer-enabled packs and no `communication_support_url`, some with `communication_support_url` already set, **When** the digest endpoint is called, **Then** the response includes a `flyer_items` section listing only those partnerships eligible for flyer generation but not yet generated.
2. **Given** validated partnerships on packs without a flyer template, **When** the digest endpoint is called, **Then** those partnerships do NOT appear in `flyer_items` (they would be flagged regardless of generation).
3. **Given** no partnerships eligible for flyer generation, **When** the digest endpoint is called, **Then** `flyer_items` is an empty list and the digest's `has_items` flag is computed only over the other sections.

---

### Edge Cases

- What happens when the company logo is larger than the configured zone? The logo is scaled down to fit, preserving aspect ratio, with a 20px margin inside the zone (matching the reference algorithm).
- What happens when the company logo is smaller than the configured zone? The logo is scaled up to fit, preserving aspect ratio, with a 20px margin inside the zone.
- What happens when the configured zone's coordinates fall outside the template's pixel dimensions? The upload endpoint validates that `(x + width) ≤ template_width` and `(y + height) ≤ template_height`; out-of-bounds coordinates are rejected with a 4xx error.
- What happens when the template upload is not a PNG (e.g., JPG, SVG)? The upload is rejected with a 4xx error specifying PNG only.
- What happens when the template URL becomes unreachable at generation time (e.g., storage outage)? The endpoint fails with a 5xx error and no partial state is persisted (`communication_support_url` is not overwritten).
- What happens when the partnership's pack is changed after a flyer was generated? The existing flyer stays attached to `communication_support_url`. Regenerating uses the new pack's template.
- What happens when two organisers click "generate" at the same time? Both calls run independently; the second one's flyer overwrites the first one's URL. No locking required given org-only access and low frequency.
- What happens when the email delivery fails but the flyer was successfully generated and stored? The flyer URL is persisted and returned to the organiser; email failure is recorded in the partnership email history (consistent with existing notification behaviour).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow organisers to upload a PNG flyer template against a sponsoring pack via a dedicated endpoint that accepts the template file and the four zone coordinates `(x, y, width, height)` in template pixels.
- **FR-002**: System MUST reject template uploads that are not PNG.
- **FR-003**: System MUST validate that all four zone coordinates are non-negative integers and that the zone fits entirely inside the template's pixel dimensions.
- **FR-004**: System MUST allow organisers to clear a pack's flyer template, deleting the stored file and nulling all five flyer-related columns.
- **FR-005**: System MUST persist the flyer template URL and zone coordinates as five nullable columns on the sponsoring packs table.
- **FR-006**: System MUST treat a pack as "flyer-enabled" only when ALL five flyer-related columns are non-null.
- **FR-007**: System MUST expose a new endpoint, restricted to authorised organisers of the event, that generates a flyer for a single partnership.
- **FR-008**: System MUST reject flyer generation requests when the partnership is not validated (`validated_at` is null).
- **FR-009**: System MUST reject flyer generation requests when the partnership's company has neither a 1000px logo variant nor an original logo.
- **FR-010**: System MUST reject flyer generation requests when the partnership's selected pack is not flyer-enabled.
- **FR-011**: System MUST compose the flyer by downloading the pack's template, downloading the company logo (preferring the 1000px variant, falling back to the original), resizing the logo to fit inside the configured zone with a 20px internal margin while preserving aspect ratio, centring the resized logo within the zone, and exporting the result as JPG.
- **FR-012**: System MUST upload the generated JPG to the same storage backend used for other partnership assets and store the resulting URL in the partnership's `communication_support_url` column, overwriting any existing value.
- **FR-013**: System MUST dispatch an email notification to every partnership contact in their preferred language, containing the event name, company name, partnership link, and flyer URL.
- **FR-014**: System MUST dispatch a Slack message to the event's configured organiser channel (if any), containing the partnership identifier and the flyer URL.
- **FR-015**: System MUST NOT dispatch a distinct notification when an existing `communication_support_url` is overwritten — the standard generation notification covers both first-time and overwrite cases.
- **FR-016**: System MUST extend the morning organiser digest with a `flyer_items` section listing partnerships that are validated, have a flyer-enabled pack, and do not yet have `communication_support_url` set.
- **FR-017**: System MUST include the `flyer_items` count in the digest's `has_items` evaluation that controls whether the digest is dispatched.

### Key Entities

- **Flyer Template Configuration**: A bundle of five values attached to a sponsoring pack — the URL of the uploaded template image and four pixel coordinates `(x, y, width, height)` describing the logo zone. All five MUST be non-null for the pack to be flyer-enabled.
- **Partnership Communication Support**: An existing single-URL property on a partnership (`communication_support_url`). The generated flyer is stored here; this feature does not introduce a new entity for the generated artefact.
- **Flyer Generation Notification**: A new notification variant dispatched via both email (Mailjet) and Slack channels, carrying event name, company name, partnership link, and flyer URL.
- **Digest Flyer Item**: A new entry type in the existing event digest, containing the company name and the partnership link, listed alongside agreement, billing, social media, job offer, and support video items.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An organiser can configure a flyer template on a pack (upload + zone) in under 2 minutes.
- **SC-002**: Flyer generation completes within 10 seconds for templates up to 4 MB and logos up to 2 MB.
- **SC-003**: 100% of flyer generation requests with a missing precondition (not validated, no logo, pack not flyer-enabled) are rejected with a specific 4xx error identifying which precondition failed.
- **SC-004**: Generated flyers preserve logo aspect ratio with a 20px margin from the zone edges, validated by automated pixel inspection of the output.
- **SC-005**: Email and Slack notifications are delivered (or accurately reported as skipped/failed in the email history) within the same latency envelope as existing partnership notifications.

## Clarifications

### Session 2026-05-16

- Q: Who can trigger flyer generation? → A: Authorised organisers only. Partners do not have a self-serve endpoint.
- Q: Which template formats should be accepted? → A: PNG only.
- Q: Which logo variant should be composited onto the template? → A: The 1000px variant when available, falling back to the original. The 250px and 500px variants are not used.
- Q: How should an existing `communication_support_url` be handled when regenerating? → A: Overwrite without a distinct "overwritten" notification. The standard generation notification is sent regardless of whether it is a first generation or a regeneration.

## Assumptions

- The flyer template is a raster PNG, not SVG. Pixel-based zone coordinates are meaningful only against a raster template.
- The reference algorithm from `communication-generator-kotlin/src/main/kotlin/com/devlille/communication/partners/FlyerGenerator.kt` is the authoritative behaviour for composition: scale to fit with 20px margin, preserve aspect ratio, centre within the zone, output as JPG.
- The `image-processing` Gradle bundle already on the project (`imgscalr-lib 4.2`) is the preferred high-quality scaler. The reference algorithm's manual `Graphics2D` rendering hints are not required when `imgscalr.Scalr.resize` is used.
- The existing `Storage` abstraction (Google Cloud Storage) is the destination for both uploaded templates and generated flyers. No new storage backend is introduced.
- The notification fan-out reuses the existing `NotificationPartnershipPlugin` route plugin pattern. A new `NotificationVariables` data class is added for `FlyerGenerated`; new Markdown templates live under `notifications/{mailjet,slack}/flyer-generated/{en,fr}.md`.
- The digest extension follows the existing `EventDigest` structure: `flyer_items` is a `List<DigestEntry>` with `company_name` and `partnership_link`, consistent with other digest sections.
- No flyer review/approval state is introduced. The generated flyer is deterministic from validated inputs and is immediately published as the partnership's communication support.
- No batch generation endpoint is introduced. Per-partnership generation is sufficient at the expected event scale; the digest surfaces what remains to generate.
- No template versioning is introduced. Replacing a template does not regenerate existing flyers; only future generations use the new template.
- Concurrent generation requests on the same partnership are not locked. The cost of a race (one flyer overwrites another) is acceptable given the org-only access and low frequency.
