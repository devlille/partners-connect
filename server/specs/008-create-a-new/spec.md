# Feature Specification: Public Partnership Information Endpoint

**Feature Branch**: `008-create-a-new`  
**Created**: 8 November 2025  
**Status**: Draft  
**Input**: User description: "create a new public endpoint to get partnership information by its id. The partnership object should have company, event and partnership entity information that allow me to know where is located the partnership in the process."

## Execution Flow (main)
```
1. Parse user description from Input
   → Extract: public endpoint, partnership info by ID, process status
2. Extract key concepts from description
   → Identify: public API access, partnership entity, company info, event info, process tracking
3. For each unclear aspect:
   → [NEEDS CLARIFICATION: authentication requirements for "public" endpoint]
   → [NEEDS CLARIFICATION: specific process status fields needed]
4. Fill User Scenarios & Testing section
   → Main flow: external user retrieves partnership details by ID
5. Generate Functional Requirements
   → Endpoint must return partnership with related entities
6. Identify Key Entities: Partnership, Company, Event
7. Run Review Checklist
   → WARN "Spec has uncertainties about authentication and process status details"
8. Return: SUCCESS (spec ready for planning with clarifications)
```

---

## Clarifications

### Session 2025-11-08
- Q: What authentication method should be used for this endpoint? → A: Completely public - no authentication required
- Q: What level of process status detail should be included in the response? → A: Detailed workflow stages with timestamps
- Q: What level of data privacy should be maintained for the public endpoint? → A: All partnership data (including private contact details)
- Q: What level of abuse prevention should be implemented for this public endpoint? → A: No rate limiting - unlimited access
- Q: What response structure should be used for the partnership information? → A: Nested structure (company, event, partnership objects)

---

## User Scenarios & Testing

### Primary User Story
As an external stakeholder (company representative, event attendee, or integration system), I need to retrieve comprehensive partnership information using a partnership ID to understand the current status and details of a partnership between a company and an event.

### Acceptance Scenarios
1. **Given** a valid event slug and partnership ID, **When** I call GET /events/{eventSlug}/partnerships/{partnershipId}, **Then** I receive partnership details including company info, event info, and current process status
2. **Given** an invalid partnership ID or event slug, **When** I call the endpoint, **Then** I receive a 404 Not Found response
3. **Given** a valid partnership ID but wrong event slug, **When** I call the endpoint, **Then** I receive a 404 Not Found response
4. **Given** a valid event slug and partnership ID, **When** I retrieve the information, **Then** the response includes enough detail to understand where the partnership stands in the approval/billing/communication process

### Edge Cases
- What happens when partnership ID is malformed (not UUID format)? We should return a 400 status code but this is already handled by the extension `toUUID` function.
- What happens when event slug is invalid or doesn't exist? We should return a 404 status code.
- What happens when partnership exists but doesn't belong to the specified event? We should return a 404 status code.
- How does system handle partnerships that are in archived/deleted state? We should return a 404 status code.
- What information is available for partnerships in different process stages (pending, approved, declined, completed)?

## Requirements

### Functional Requirements
- **FR-001**: System MUST provide a public GET endpoint at `/events/{eventSlug}/partnerships/{partnershipId}`
- **FR-002**: System MUST return partnership details including associated company and event information
- **FR-003**: System MUST include process status indicators showing partnership workflow state
- **FR-004**: System MUST validate partnershipId as a valid UUID format
- **FR-005**: System MUST validate eventSlug exists and is accessible
- **FR-006**: System MUST verify partnership belongs to the specified event
- **FR-007**: System MUST return 404 error for non-existent partnership IDs
- **FR-008**: System MUST return 404 error for non-existent event slugs
- **FR-009**: System MUST return 404 error when partnership exists but doesn't belong to specified event
- **FR-010**: System MUST return 400 error for malformed partnership IDs
- **FR-011**: Response MUST include complete company details (name, contact information, address, business data including SIRET/VAT, website)
- **FR-012**: Response MUST include complete event details (name, slug, dates, location, organization information)
- **FR-013**: Response MUST include partnership process status with detailed workflow stages and timestamps (suggestion sent/approved/declined, validation, billing, agreement generation/signing, communication phases)
- **FR-014**: System MUST allow access without authentication (completely public endpoint)
- **FR-015**: Response MUST include all partnership contact details (phone, email, contact name and role)
- **FR-016**: System MUST allow unlimited access with no rate limiting restrictions
- **FR-017**: System MUST handle partnerships in any state (pending, approved, declined) consistently
- **FR-018**: Response format MUST use nested JSON structure with separate company, event, and partnership objects

### Key Entities

- **Partnership**: Core entity containing contact information, language preferences, selected/suggested packs, and process timestamps (suggestionSentAt, suggestionApprovedAt, validatedAt, etc.)
- **Company**: Associated company entity with name, contact details, address, and business information (SIRET, VAT, website)
- **Event**: Associated event entity with name, slug, dates, location, and organization information
- **Process Status**: Detailed workflow stage information with timestamps including suggestion lifecycle (sent/approved/declined dates), validation status and date, billing status, agreement generation and signing dates, and communication publication phases

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
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---
