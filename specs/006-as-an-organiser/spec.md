# Feature Specification: Partnership Validation with Customizable Package Details

**Feature Branch**: `006-as-an-organiser`  
**Created**: 26 October 2025  
**Status**: Draft  
**Input**: User description: "As an organiser, I would like to save the number of tickets, the number of job offer and the configuration of the booth when a partnership is validated. By default, we get these information from the pack, so the number of tickets is an optional parameter but the number of job offer and the booth configuration become required when an organiser valid a partnership."

## Execution Flow (main)
```
1. Parse user description from Input
   → Identified: organizer validation of partnership with custom package details
2. Extract key concepts from description
   → Identified: partnership validation, ticket count override, job offers, booth configuration
3. For each unclear aspect:
   → Marked booth configuration details
4. Fill User Scenarios & Testing section
   → User flow is clear: organizer validates partnership with custom parameters
5. Generate Functional Requirements
   → Each requirement must be testable
6. Identify Key Entities (if data involved)
   → Partnership, SponsoringPack, validated package snapshot
7. Run Review Checklist
   → One clarification needed on booth configuration format
8. Return: SUCCESS (spec ready for planning with minor clarification)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers

---

## Clarifications

### Session 2025-10-26
- Q: What format should booth configuration use? → A: Predefined options (dropdown of standard booth types)
- Q: Can a validated partnership be re-validated with different custom values? → A: Yes, but only before agreement is signed
- Q: What are the maximum allowed values for ticket count and job offer count per partnership? → A: No upper limit (unlimited)
- Q: What happens if two organizers attempt to validate the same partnership simultaneously? → A: First validation wins (second attempt fails)
- Q: For existing partnerships validated before this feature, what values should be shown for the new fields? → A: Show null/empty (require re-validation)
- Q: Booth configuration refinement → A: Booth configuration represents booth SIZE only (not a boolean). Each sponsoring pack defines its booth size. The "withBooth" property is removed from packs. During validation, the booth size defaults to the pack's booth size but can be overridden, with validation ensuring the override value exists in at least one pack for the event. This prepares for future location selection on floor plans based on booth size.

---

## User Scenarios & Testing

### Primary User Story
As an event organizer reviewing partnership requests, I need to customize the package details when validating a partnership. While sponsoring packs provide default values for tickets, job offers, and booth size, I want to adjust these at validation time to accommodate special agreements, negotiate different terms, or correct initial package selections. The booth size configuration prepares for future functionality where partners will select their booth location on a floor plan based on the validated booth size. This allows me to maintain flexibility while preserving the base package structure.

### Acceptance Scenarios

1. **Given** I am an organizer reviewing a partnership request with a "Gold Pack" (default: 5 tickets, booth size "3x3m"), **When** I validate the partnership and specify 10 tickets, 3 job offers, and booth size "6x3m", **Then** the partnership is validated with these custom values saved.

2. **Given** I am an organizer validating a partnership, **When** I do not specify a custom ticket count, **Then** the system uses the default ticket count from the selected sponsoring pack.

3. **Given** I am an organizer validating a partnership, **When** I do not specify a custom booth size, **Then** the system uses the default booth size from the selected sponsoring pack.

4. **Given** I am an organizer validating a partnership, **When** I do not specify the number of job offers, **Then** the system rejects the validation request with an error indicating job offer count is required.

5. **Given** I am an organizer validating a partnership and I specify a booth size override, **When** the booth size value does not exist in any sponsoring pack for this event, **Then** the system rejects the validation request with an error indicating the booth size is invalid.

6. **Given** I am an organizer validating a partnership with a "Silver Pack" (default: 2 tickets, booth size "3x3m"), **When** I validate with 2 job offers and booth size "6x3m" (which exists in another pack), **Then** the system accepts the partnership validation and saves all details including the overridden booth size.

7. **Given** a partnership has been validated with custom details (8 tickets, 2 job offers, booth size "3x3m"), **When** I view the partnership details, **Then** I see these validated values, not the original pack defaults.

8. **Given** a company wants to generate tickets for their validated partnership, **When** the system checks ticket allocation, **Then** it uses the validated ticket count (not the pack default) to determine ticket availability.

9. **Given** I am an organizer viewing multiple partnerships for an event, **When** I filter by validated partnerships, **Then** I can see the customized package details including booth sizes for each validated partnership.

10. **Given** a partnership has been validated with custom values but the agreement is not yet signed, **When** I re-validate with different custom values, **Then** the system updates the validated package details with the new values.

11. **Given** a partnership has been validated and the agreement is signed, **When** I attempt to re-validate with different custom values, **Then** the system rejects the request with an error indicating the partnership cannot be modified after agreement signature.

12. **Given** a partnership was validated before this feature existed (legacy partnership), **When** I view the partnership details, **Then** I see null/empty values for validated ticket count, job offer count, and booth size fields, and I can re-validate to set these values.

### Edge Cases

- What happens when I specify a ticket count of 0? System should accept 0 as valid (some partnerships might not include tickets).
- What happens when I specify a negative ticket count? System should reject with validation error for negative values.
- What happens if I specify a booth size that doesn't exist in any pack for this event? System should reject the validation request with an error indicating the booth size is invalid.
- What happens if a pack has no booth size defined (null)? The pack can be validated without a booth size, and partners won't be able to select booth locations in the future floor plan feature.
- What happens when I try to view a partnership's validated details before it's validated? System should show the partnership is pending validation with no custom details stored yet.
- What happens if I validate a partnership with 0 job offers? System should accept 0 as a valid value (some partnerships might not promote job offers).
- Can I re-validate a partnership to change the custom values? Yes, but only before the agreement is signed. Once signed, the validated values become immutable.
- What happens if two organizers attempt to validate the same partnership simultaneously? The first validation succeeds, and any subsequent validation attempt fails (first-wins concurrency control).
- How are legacy partnerships (validated before this feature) handled? They show null/empty values for the new validated package detail fields and can be re-validated to populate these fields (subject to the same rules about agreement signature status).

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST allow organizers to validate a partnership with custom package details including ticket count, job offer count, and booth size.

- **FR-002**: System MUST make job offer count a required field when validating a partnership.

- **FR-003**: System MUST make booth size an optional field when validating a partnership, defaulting to the sponsoring pack's booth size if not specified.

- **FR-004**: System MUST make ticket count an optional field when validating a partnership.

- **FR-005**: System MUST use the default ticket count from the sponsoring pack when no custom ticket count is provided during validation.

- **FR-006**: System MUST use the default booth size from the sponsoring pack when no custom booth size is provided during validation.

- **FR-007**: System MUST validate that any custom booth size provided during validation exists as a booth size in at least one sponsoring pack for the event.

- **FR-008**: System MUST persist the validated package details (ticket count, job offer count, booth size) separately from the base pack configuration.

- **FR-008**: System MUST persist the validated package details (ticket count, job offer count, booth size) separately from the base pack configuration.

- **FR-009**: System MUST use the validated ticket count (not the pack default) when determining ticket allocation and generation for the partnership.

- **FR-010**: System MUST display validated partnership details showing the custom values rather than the original pack defaults.

- **FR-011**: System MUST validate that ticket count, when provided, is a non-negative integer with no upper limit.

- **FR-012**: System MUST validate that job offer count is a non-negative integer with no upper limit.

- **FR-013**: System MUST maintain backward compatibility with existing partnerships that were validated before this feature existed by showing null/empty values for the new validated package detail fields.

- **FR-014**: System MUST allow organizers to validate partnerships where custom ticket counts differ from pack defaults (both higher and lower values).

- **FR-015**: System MUST preserve the relationship to the original sponsoring pack while storing the validated custom values.

- **FR-016**: System MUST include validated package details in partnership listings and detail views for organizers.

- **FR-017**: System MUST allow organizers to re-validate a partnership with different custom values only before the agreement is signed.

- **FR-018**: System MUST prevent re-validation of partnerships once the agreement has been signed.

- **FR-019**: System MUST prevent concurrent validation attempts by rejecting subsequent validation requests if a partnership is already validated (first validation wins).

- **FR-020**: System MUST allow organizers to re-validate legacy partnerships (validated before this feature) to set the validated package detail fields that were previously null/empty.

### Key Entities

- **Partnership**: Represents the agreement between a company and an event. Now stores validated package details including custom ticket count, job offer count, and booth size at validation time.

- **ValidatedPackageDetails**: The snapshot of package configuration at partnership validation time, including validated ticket count (defaults to pack value if not specified), validated job offer count (required), and validated booth size (defaults to pack value if not specified). These values override the base pack configuration for this specific partnership.

- **SponsoringPack**: The base package template that provides default values for tickets, booth size, and other package features. The "withBooth" boolean property is removed and replaced with a booth size field (e.g., "3x3m", "6x3m", or null). These defaults are used as starting points but can be customized during partnership validation.

- **BoothSize**: A string value representing the physical dimensions of a booth (e.g., "3x3m", "6x3m", "9x3m"). Each pack defines its booth size. During validation, the booth size must exist in at least one pack for the event. This prepares for future functionality where partners select booth locations on floor plans based on validated booth sizes.

- **TicketAllocation**: Uses the validated ticket count from the partnership's ValidatedPackageDetails to determine how many tickets can be generated for the partnership.

- **JobOfferPromotion**: Uses the validated job offer count to determine how many job offers the company can promote through this partnership.

---

## Review & Acceptance Checklist

### Content Quality
- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous  
- [x] Success criteria are measurable
- [x] Scope is clearly bounded (validation-time customization of package details)
- [x] Dependencies and assumptions identified (existing partnership validation flow, sponsoring pack structure)

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked (booth configuration details, re-validation behavior)
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed with clarifications noted

---
