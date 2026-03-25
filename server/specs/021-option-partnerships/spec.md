# Feature Specification: Option Partnerships

**Feature Branch**: `021-option-partnerships`  
**Created**: 2026-03-24  
**Status**: Draft  
**Input**: User description: "In the existing route which is list the options of an event, I would like to return the list of partnership which has been validated with the option. I would like to return the exact same partner item object that is already returned in the existing partnership list route."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Validated Partnerships per Option (Priority: P1)

As an organizer viewing a single sponsoring option for an event, I want the option detail to include the list of partnerships that have been validated with that option, so I can quickly see which partners have committed to this option without navigating away.

**Why this priority**: This is the core and only feature — enriching the existing single option detail response with validated partnership data. Without it, organizers must cross-reference options and partnerships manually.

**Independent Test**: Can be fully tested by calling the single option detail endpoint and verifying that it includes validated partnerships using the same partner item structure as the partnership list route.

**Acceptance Scenarios**:

1. **Given** an event with a sponsoring option and some partnerships validated with that option, **When** an organizer requests the detail of that option, **Then** the response includes a list of validated partnerships using the same partner item structure as the partnership list endpoint.
2. **Given** a sponsoring option with no validated partnerships, **When** an organizer requests the option detail, **Then** the response includes an empty list of partnerships.
3. **Given** a partnership that has selected an option but has not been validated, **When** an organizer requests the option detail, **Then** that unvalidated partnership does NOT appear in the option's partnership list.

---

### User Story 2 - Consistent Partner Item Format (Priority: P1)

As an organizer, I want the partner items returned within each option to use the exact same structure as those returned in the existing partnership list, so I can rely on a single consistent data format across the application.

**Why this priority**: Data format consistency is essential for frontend consumers to reuse existing rendering logic. This is inseparable from Story 1.

**Independent Test**: Can be tested by comparing the partner item structure in the single option detail response against the partnership list response and verifying they are identical.

**Acceptance Scenarios**:

1. **Given** validated partnerships associated with an option, **When** the single option detail is returned, **Then** each partner item contains all the same fields as the partner item in the partnership list endpoint (id, contact, company name, event name, selected pack, suggested pack, validated pack, language, phone, emails, organiser, creation date).
2. **Given** a partner item in the single option detail response, **When** compared to the same partner item from the partnerships list endpoint, **Then** the field values are identical.

---

### Edge Cases

- What happens when an option exists but has zero validated partnerships? The response returns an empty partnerships list for that option.
- What happens when a partnership is validated with multiple options? The partnership appears in the list of each option it is validated with.
- What happens when a partnership was validated but later declined? `validatedPack()` returns null for such partnerships, so they are naturally excluded.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The existing organizer single option detail endpoint MUST include a list of validated partnerships in the response.
- **FR-002**: Each partner item in the option response MUST use the exact same data structure as the partner item returned by the existing partnership list endpoint.
- **FR-003**: Only partnerships whose validated pack (determined by `validatedPack()`) contains a given option MUST appear in that option's partnerships list.
- **FR-004**: Partnerships whose `validatedPack()` returns null (unvalidated, declined, or suggestion-declined) MUST NOT appear in the option's partnerships list. No separate declined check is required.
- **FR-005**: Options with no validated partnerships MUST return an empty partnerships list.
- **FR-006**: The existing fields and behavior of the single option detail endpoint MUST remain unchanged; the partnerships list is an addition to the existing response. The options list endpoint MUST remain completely unchanged.

### Key Entities

- **Sponsoring Option**: A configurable sponsoring offering for an event (text, quantitative, number, or selectable type) with translations and an optional price. Each option can be associated with multiple partnerships.
- **Partnership Item**: A summary representation of a partnership including contact details, company name, event name, selected/suggested/validated pack references, language, phone, emails, assigned organiser, and creation date.
- **Validated Partnership-Option Relationship**: Derived from the partnership's validated pack (via `validatedPack()`) and the pack's attached options (via `PackOptionsTable`). A partnership is associated with an option if its validated pack contains that option.

## Clarifications

### Session 2026-03-24

- Q: Should partnerships be matched to options via direct selection (PartnershipOptionsTable) or via validated pack membership (PackOptionsTable)? → A: Via validated pack membership — use the existing `validatedPack()` function to determine the partnership's validated pack, then resolve options attached to that pack.
- Q: Should partnerships be added to both the list and single option endpoints, or only one? → A: Only to the single option detail endpoint (`GET /orgs/{orgSlug}/events/{eventSlug}/options/{optionId}`).
- Q: Should declined partnerships be explicitly filtered out, or rely solely on `validatedPack()`? → A: Rely solely on `validatedPack()` — if it returns null the partnership is excluded; no additional declined check needed.

## Assumptions

- "Validated" is determined exclusively by `validatedPack()` returning a non-null pack. This already handles all edge cases (declined, suggestion-declined, unvalidated).
- A partnership appears under an option if its validated pack contains that option (resolved via `PackOptionsTable`).
- The existing partnership list endpoint returns `PartnershipItem` objects, and this same structure will be reused without modification.
- The change only affects the authenticated organizer route (`/orgs/{orgSlug}/events/{eventSlug}/options`), not any public routes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Organizers can view all validated partnerships for an option in a single request to the option detail endpoint, eliminating the need for separate cross-referencing.
- **SC-002**: 100% of partner item fields in the options response match the structure and values of the partnership list endpoint.
- **SC-003**: Options with no validated partnerships return an empty list without errors.
- **SC-004**: Existing options list consumers experience no breaking changes — all previous response fields remain present and unchanged.
