# Feature Specification: Option Usage Count

**Feature Branch**: `022-option-usage-count`
**Created**: 2026-03-25
**Status**: Draft
**Input**: User description: "Now, I would like to create a new spec to return the usage count in the options list endpoint. The idea is to be able to see from the option list screen if there is at least one partner which is using the option to be able to open the option to see the full list of partnership that we just added in the previous spec."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Partnership Usage Count per Option (Priority: P1)

As an event organizer viewing the options list screen, I want to see how many validated partnerships are using each sponsoring option, so I can quickly identify which options are in use and decide whether to open an option's detail view to see the full list of partnerships.

**Why this priority**: This is the core and only feature — it provides the at-a-glance indicator that tells organizers whether an option has active partnerships, enabling them to navigate to the detail view (added in feature 021) for the full partnership list.

**Independent Test**: Can be fully tested by calling the options list endpoint and verifying each option in the response includes a count of validated partnerships. Delivers immediate value by showing option utilization without requiring navigation to each option's detail page.

**Acceptance Scenarios**:

1. **Given** an event with sponsoring options where some options are included in validated partnership packs, **When** the organizer requests the options list, **Then** each option in the response includes a count representing the number of validated partnerships using that option.
2. **Given** an event with sponsoring options where no partnerships have been validated for a particular option, **When** the organizer requests the options list, **Then** that option's usage count is `0`.
3. **Given** an event with multiple validated partnerships whose validated packs all contain the same option, **When** the organizer requests the options list, **Then** the usage count for that option equals the total number of distinct validated partnerships using it.
4. **Given** a partnership that has been declined (no validated pack), **When** the organizer requests the options list, **Then** that partnership is NOT counted in any option's usage count.

---

### Edge Cases

- **Option not attached to any pack**: Usage count should be `0` since no pack references it.
- **Partnership with a suggested pack (not yet approved)**: Should NOT be counted — only partnerships with a validated pack (as determined by the existing `validatedPack()` logic) are included.
- **Multiple packs containing the same option**: A partnership validated with any of those packs counts once toward the option's usage count.
- **Empty event (no partnerships)**: All options should have a usage count of `0`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The options list endpoint (`GET /orgs/{orgSlug}/events/{eventSlug}/options`) MUST return a usage count for each option representing the number of validated partnerships associated with that option.
- **FR-002**: The usage count MUST be determined using the same partnership-option association logic as the option detail endpoint (feature 021): a partnership is associated with an option when its validated pack contains that option.
- **FR-003**: The usage count MUST be an integer value of `0` or greater.
- **FR-004**: The existing option fields (id, translations, price, type-specific fields) MUST continue to be returned unchanged inside a wrapper object — the usage count is a sibling field alongside the option object. The response changes from a flat array of options to an array of wrapper objects.
- **FR-005**: The usage count MUST only count each validated partnership once, even if multiple packs containing the same option exist.

### Key Entities

- **SponsoringOptionWithTranslations**: Existing sealed class (Text, TypedQuantitative, TypedNumber, TypedSelectable) representing a sponsoring option with all its translations. NOT modified — shared by other endpoints (e.g., sponsoring pack endpoint).
- **Option list item wrapper**: New wrapper object containing an `option` (SponsoringOptionWithTranslations) and a `partnership_count` (integer). The list endpoint returns an array of these wrappers.
- **Partnership**: Existing entity — counted when its validated pack (via `validatedPack()`) contains the option (via `PackOptionsTable`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The options list response returns an array of wrapper objects, each containing an `option` field and a `partnership_count` field (integer `0` or greater).
- **SC-002**: The `partnership_count` value matches the number of partnerships visible in the option detail endpoint (feature 021) for the same option.
- **SC-003**: Organizers can distinguish between options with active partnerships (count > 0) and unused options (count = 0) directly from the list view.
- **SC-004**: Existing option fields are preserved inside the wrapper object — no fields are removed or renamed. The response structure changes from a flat array to an array of wrappers (breaking change documented in OpenAPI).

## Clarifications

### Session 2026-03-25

- Q: Should `partnership_count` be added inline to the sealed class subtypes (flat array) or via a wrapper per item? → A: Wrapper per item (`{"option":{...}, "partnership_count":3}`), because the sealed class is shared by the sponsoring pack endpoint where partnership count should not appear.

## Assumptions

- "Validated partnership" uses the same definition as feature 021: a partnership whose `validatedPack()` returns a non-null pack.
- The partnership-option association follows the same path as feature 021: `validatedPack() → PackOptionsTable → option`.
- The count is scoped to the event — only partnerships belonging to the same event are counted.
- This feature only affects the authenticated organizer endpoint (`/orgs/{orgSlug}/events/{eventSlug}/options`), not the public packs endpoint.

## Scope

### In Scope

- Adding a partnership count field to each option in the list endpoint response.
- Updating the JSON schema and OpenAPI spec for the list endpoint.

### Out of Scope

- Changing the option detail endpoint (already handled in feature 021).
- Adding filtering or sorting by usage count.
- Real-time or live-updating counts.
