# Feature Specification: Job Offers Management for Companies

**Feature Branch**: `003-as-a-company`  
**Created**: October 15, 2025  
**Status**: Draft  
**Input**: User description: "As a company owner, I can register job offers that I can promote to a partnership in progress with an event. Job offers at the company level can be created, updated, removed or retrieved. A job offer is an url, a title, a location, a publication date and optionally an end date, experienced year required, a location and a salary."

---

## User Scenarios & Testing *(mandatory)*

### Primary User Story
As a company owner, I want to manage job offers for my company so that I can promote them to potential partners during active event partnerships. I need to create new job offers with all relevant details, update existing ones when requirements change, remove obsolete positions, and view all my company's current job offers. When I have an active partnership with an event, I want to be able to promote these job offers to increase visibility among event participants.

### Acceptance Scenarios
1. **Given** I am a user and open a company space, **When** I create a new job offer with title, URL, location, and publication date, **Then** the job offer is saved and appears in my company's job offers list
2. **Given** I have existing job offers, **When** I update a job offer's details (title, URL, location, end date, experience requirements, or salary), **Then** the changes are saved and reflected in the system
3. **Given** I have job offers in my company, **When** I delete a job offer, **Then** it is permanently removed from my company's offerings
4. **Given** I have created job offers, **When** I view my company's job offers list, **Then** I see all active job offers with their complete details
5. **Given** I have an active partnership with an event and available job offers, **When** I promote job offers to the partnership, **Then** event participants can view these job opportunities

### Edge Cases
- What happens when a job offer URL becomes invalid or unreachable? when we fetch the job offer list for a company, we have two list: one active, other inactive.
- How does the system handle job offers with past end dates? They are saved until someone delete them.
- What validation occurs for experience years (negative values, unrealistic ranges)? should be positive and between 1 and 20
- How are job offers handled when a company partnership with an event ends? They are just kept for history.
- What happens if a company tries to promote job offers without an active event partnership? An error 403 is returned

## Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: System MUST allow user to create job offers with mandatory fields: URL, title, location, and publication date
- **FR-002**: System MUST allow users to specify optional job offer fields: end date, required experience years, and salary
- **FR-003**: System MUST allow users to update all job offer details after creation
- **FR-004**: System MUST allow users to delete job offers they have created
- **FR-005**: System MUST allow users to retrieve and view all job offers for their company
- **FR-006**: System MUST validate that job offer URLs are properly formatted web addresses
- **FR-007**: System MUST validate that publication dates are not in the future
- **FR-008**: System MUST validate that end dates (when provided) are after publication dates
- **FR-009**: System MUST restrict job offer management to authenticated company owners for their own companies
- **FR-010**: System MUST allow job offers to be promoted to active event partnerships
- **FR-011**: System MUST persist all job offer data including creation and modification timestamps
- **FR-012**: System MUST validate experience years as non-negative integers
- **FR-013**: System MUST handle salary information currency, range

### Key Entities *(include if feature involves data)*
- **Job Offer**: Represents an employment opportunity posted by a company, containing URL (link to detailed posting), title (position name), location (work location), publication date (when posted), optional end date (application deadline), optional experience years required (minimum experience), and optional salary information
- **Company**: Business entity that creates and manages job offers, has ownership relationship with job offers
- **Event Partnership**: Active collaboration between a company and an event, enables job offer promotion to event participants
- **Event**: Organized gathering where companies can promote their job offers to participants through partnerships

---

## Review & Acceptance Checklist
*GATE: Automated checks run during main() execution*

### Content Quality
- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [ ] No [NEEDS CLARIFICATION] markers remain (4 clarifications needed)
- [x] Requirements are testable and unambiguous  
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status
*Updated by main() during processing*

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---
