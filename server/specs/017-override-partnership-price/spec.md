# Feature Specification: Override Partnership Pricing

**Feature Branch**: `017-override-partnership-price`  
**Created**: 2026-02-27  
**Status**: Draft  
**Input**: User description: "As an organiser, you should be able to provide an optional price parameter on an existing partnership to override the price of the sponsoring pack validated and on one or more options. This should be able to do when you want to update an existing partnership."

## Clarifications

### Session 2026-02-27

- Q: Does the price override feed into billing/quotes, or is it informational only? → A: Override price replaces the catalogue price for billing and quote generation when set
- Q: Can price overrides be set regardless of partnership status, or only when approved/validated? → A: Price overrides can be set regardless of partnership status
- Q: What numeric type and precision should override prices use? → A: Integer, same type/unit as existing catalogue prices
- Q: How should prices be exposed in API responses? → A: Front-end receives both catalogue price and override price as separate fields; billing system receives only the effective price (override when set, catalogue otherwise)
- Q: Should setting or changing a price override trigger any notification to the partner? → A: No notification; price overrides are an internal organiser operation

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Override Sponsoring Pack Price (Priority: P1)

As an organiser, I need to set a custom price on the sponsoring pack of an existing partnership so that I can accommodate negotiated deals or special pricing agreements with partners.

**Why this priority**: This is the primary ask — the ability to assign a negotiated price to the validated sponsoring pack of a partnership. Without this, all other pricing overrides are irrelevant.

**Independent Test**: Can be fully tested by updating an existing partnership with a custom pack price and verifying the override price is returned on subsequent queries while the original pack catalogue price remains unchanged.

**Acceptance Scenarios**:

1. **Given** I am an authenticated organiser for an organisation, **When** I update an existing partnership providing a custom price for the sponsoring pack, **Then** the partnership stores the override price and returns it in subsequent responses
2. **Given** I have previously set a custom pack price, **When** I update the partnership and omit the price override field, **Then** the existing override price is preserved unchanged
3. **Given** I have previously set a custom pack price, **When** I update the partnership and explicitly clear the price override (set to null), **Then** the override is removed and the original catalogue pack price applies
4. **Given** I provide a price of zero, **When** I update the partnership, **Then** the system accepts it as a valid override (free sponsoring package)

---

### User Story 2 - Override Prices on One or More Options (Priority: P2)

As an organiser, I need to set custom prices on one or more options attached to an existing partnership so that individual option pricing can be adjusted independently from catalogue prices.

**Why this priority**: Options are separate from the pack and may also be subject to individual negotiation. This extends the override capability to the option level while keeping it independent of the pack override.

**Independent Test**: Can be fully tested by updating a partnership with custom prices for a subset of its options and verifying that only those specific options reflect the override, while unchanged options retain their catalogue prices.

**Acceptance Scenarios**:

1. **Given** a partnership has multiple options, **When** I update the partnership providing a price override for one specific option, **Then** only that option reflects the custom price; all other options retain their original prices
2. **Given** a partnership has multiple options, **When** I provide price overrides for all options in a single update, **Then** all options reflect their respective custom prices
3. **Given** I have previously set a custom price on an option, **When** I update the partnership and explicitly clear the override for that option, **Then** the override is removed and the original catalogue option price applies
4. **Given** I provide an option ID that does not belong to the partnership, **When** I submit the update, **Then** the system returns an error indicating the option is not associated with the partnership

---

### User Story 3 - Combined Pack and Option Price Overrides (Priority: P3)

As an organiser, I need to set custom prices on both the sponsoring pack and one or more options in a single update so that a complete custom pricing deal can be recorded in one operation.

**Why this priority**: Organisers will often negotiate an all-in deal covering both the pack and specific options. Doing this in one request reduces friction and ensures atomicity.

**Independent Test**: Can be fully tested by submitting an update that contains both a pack price override and option price overrides, then verifying all overrides are persisted and correctly returned together.

**Acceptance Scenarios**:

1. **Given** an existing partnership with a validated pack and options, **When** I update it with a custom pack price and custom prices for two options simultaneously, **Then** all three price overrides are saved and returned in subsequent responses
2. **Given** I submit a combined override, **When** the pack price is valid but one option price is invalid (e.g., negative), **Then** the entire update is rejected with a clear error identifying the invalid value

---

### Edge Cases

- What happens when the override price is negative? → The system must reject it with a validation error.
- What happens when no price override is provided in the update payload? → No change is made to existing overrides (partial update semantics).
- What happens if the partnership has no validated sponsoring pack yet? → Pack price override cannot be set; the system returns a clear error.
- What happens if two concurrent updates set different price overrides on the same partnership? → Last write wins; no partial state should be persisted.
- What happens when the override price is a non-numeric value? → The system must reject it with a validation error.
- What happens when an organiser sets a price override on a rejected or pending partnership? → The override is accepted; partnership status does not gate the operation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow an authenticated organiser to provide an optional custom price for the validated sponsoring pack when updating an existing partnership
- **FR-002**: System MUST allow an authenticated organiser to provide optional custom prices for one or more options when updating an existing partnership
- **FR-003**: System MUST allow pack and option price overrides to be submitted independently or together in a single update request
- **FR-004**: System MUST persist price overrides per partnership — overrides are stored alongside the partnership record, not modifying the catalogue pack or option prices
- **FR-005**: System MUST support clearing a price override (restoring the original catalogue price) by explicitly setting the override to null
- **FR-006**: System MUST apply partial update semantics — omitting a price override field in the request leaves the existing override unchanged
- **FR-007**: System MUST validate that override prices are non-negative integers, using the same unit as catalogue prices (consistent with existing pack and option price representation)
- **FR-008**: System MUST reject an option price override if the referenced option is not associated with the partnership
- **FR-009**: System MUST reject a pack price override if the partnership has no validated sponsoring pack
- **FR-010**: The partnership API response MUST include both the original catalogue price and the override price as separate nullable fields so that front-end consumers can display negotiated pricing transparently
- **FR-011**: Only authenticated organisers with access to the organisation owning the event may set price overrides
- **FR-012**: System MUST return appropriate error messages for all validation failures
- **FR-013**: When a price override is active on the sponsoring pack or an option, the billing and quote generation system MUST use only the effective price (override price when set, catalogue price otherwise); billing consumers do not receive the split representation
- **FR-014**: Price overrides MAY be set on a partnership in any lifecycle status (pending, approved, rejected); partnership status MUST NOT block the override operation
- **FR-015**: Setting, updating, or clearing a price override MUST NOT trigger any notification to the partner company; price overrides are an internal organiser operation

### Key Entities

- **Partnership**: The relationship between a partner company and an event; extended with optional price overrides for the validated sponsoring pack and its selected options
- **Sponsoring Pack**: The catalogue package chosen by the partner and validated by the organiser; has a catalogue price that may be overridden per partnership deal
- **Option**: An individual add-on attached to a partnership; has a catalogue price that may be overridden independently per partnership deal
- **Price Override**: An optional non-negative integer stored on a partnership (or a specific option within a partnership) that supersedes the catalogue price for that specific deal; uses the same unit as the catalogue price

## Assumptions

- Price values are expressed in the same currency as the original catalogue price; currency selection is out of scope for this feature
- A price of zero is a valid override (e.g., a complimentary package or option)
- The update endpoint for partnerships already exists; this feature extends its request payload with optional price override fields
- Organisers are authenticated via the existing organisation authorisation mechanism; no new authentication is introduced
- When an override price is set, it supersedes the catalogue price everywhere, including billing and quote generation; the catalogue price is never modified

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An organiser can set or update a price override on a partnership in under 30 seconds from initiating the request
- **SC-002**: The API response always exposes both the catalogue price and the override price as distinct fields with 100% accuracy; the billing system always receives only the correct effective price
- **SC-003**: 100% of requests with negative or otherwise invalid price values are rejected with a clear, actionable error message
- **SC-004**: Partial update semantics work correctly 100% of the time — unspecified price fields never inadvertently clear existing overrides
- **SC-005**: Zero impact on catalogue data integrity — original pack and option catalogue prices remain unchanged after any override operation
