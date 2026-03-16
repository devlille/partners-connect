# Feature Specification: Schedule Standalone Communication

**Feature Branch**: `019-standalone-communication`  
**Created**: 2026-03-16  
**Status**: Draft  
**Input**: User description: "As an organiser, I should be able to schedule a communication not attached to a partnership but that will be displayed in the communication planning"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Schedule a Standalone Communication (Priority: P1)

An organiser wants to create a communication entry (e.g., a welcome message to all sponsors, a general logistics update, or a post-event newsletter) that is not linked to any specific partnership. They provide a title, a scheduled date, and optionally a description. The entry is saved and immediately visible on the communication planning timeline alongside any partnership-specific communications.

**Why this priority**: This is the core value of the feature — without the ability to create these standalone entries, nothing else is possible. It also unblocks organisers from having a complete and accurate view of all planned communications in one place.

**Independent Test**: Can be fully tested by creating a standalone communication for an event and verifying it appears in the communication planning view with the correct date and title.

**Acceptance Scenarios**:

1. **Given** an authenticated organiser on a specific event, **When** they create a standalone communication with a title and a future scheduled date, **Then** the communication is saved and appears in the communication planning view at the correct date position.
2. **Given** an organiser creating a standalone communication, **When** they omit the title, **Then** the system rejects the request with a validation error.
3. **Given** an organiser creating a standalone communication, **When** they omit the scheduled date, **Then** the system rejects the request with a validation error.
4. **Given** an organiser on event A, **When** they create a standalone communication, **Then** the communication is scoped to event A only and does not appear for event B.

---

### User Story 2 - View Standalone Communications in Planning (Priority: P1)

An organiser opens the communication planning view for an event and sees all scheduled communications in chronological order, including both partnership-linked communications and standalone ones. Standalone communications are clearly distinguishable from partnership-linked ones.

**Why this priority**: This is equally critical to User Story 1 — a communication that cannot be seen in the planning is not useful. Both stories together form the minimum viable feature.

**Independent Test**: Can be tested independently by seeding a standalone communication and verifying it appears in the planning list response with correct metadata.

**Acceptance Scenarios**:

1. **Given** an event with both partnership communications and standalone communications scheduled, **When** the organiser retrieves the communication planning, **Then** all communications appear sorted chronologically.
2. **Given** an event with standalone communications, **When** the organiser retrieves the planning, **Then** each standalone communication entry includes at minimum: title, scheduled date, and an indicator that it is not tied to a partnership.
3. **Given** an event with no communications at all, **When** the organiser retrieves the planning, **Then** an empty list is returned without error.

---

### User Story 3 - Edit Any Communication Plan Entry (Priority: P2)

An organiser realises that the scheduled date or title of a communication plan entry (standalone or partnership-linked) needs to change. They update the entry via the new communication plan endpoints, and the planning view reflects the new information.

**Why this priority**: Communications planning is inherently iterative — organisers need to adjust dates and details as the event evolves. Without edit capability, entries quickly become stale.

**Independent Test**: Can be tested by creating a standalone communication, updating its scheduled date, and verifying the planning view shows the updated date. Also verified by checking that a migrated partnership-linked entry can be updated.

**Acceptance Scenarios**:

1. **Given** an existing standalone communication, **When** the organiser updates the title or scheduled date, **Then** the changes are persisted and the planning view reflects the updated values.
2. **Given** an existing partnership-linked communication plan entry, **When** the organiser updates its scheduled date via the new endpoint, **Then** the changes are persisted and reflected in the planning view.
3. **Given** an organiser trying to edit a communication plan entry from a different event, **When** they submit the update, **Then** the system rejects the request.
4. **Given** an organiser updating a communication plan entry, **When** they clear the title, **Then** the system rejects the request with a validation error.
5. **Given** an existing entry with a scheduled date, **When** the organiser sends an update with `scheduled_date: null`, **Then** the entry is accepted, the date is cleared, and the entry moves to the `unplanned` group in the planning view.

---

### User Story 4 - Delete Any Communication Plan Entry (Priority: P2)

An organiser decides that a previously planned communication entry (standalone or partnership-linked) is no longer needed. They delete it via the new endpoint and it is removed from the communication planning view.

**Why this priority**: Keeping the planning clean and accurate requires the ability to remove entries that are no longer relevant.

**Independent Test**: Can be tested by creating a standalone communication, deleting it, and verifying it no longer appears in the planning view.

**Acceptance Scenarios**:

1. **Given** an existing standalone communication, **When** the organiser deletes it, **Then** it is removed from the communication planning view.
2. **Given** an existing partnership-linked communication plan entry, **When** the organiser deletes it, **Then** it is removed from the planning view (the partnership itself is unaffected).
3. **Given** an organiser trying to delete a communication plan entry from a different event, **When** they submit the deletion, **Then** the system rejects the request.

---

### Edge Cases

- What happens when a standalone communication is scheduled in the past? The system accepts past dates (useful for retroactively logging sent communications and keeping the history accurate in the planning view).
- What happens when multiple standalone communications share the same scheduled date? All appear in the planning; ordering within the same date is by creation time.
- What happens when an event is archived or closed and a standalone communication is still scheduled? The communication remains visible in the planning but cannot be modified once the event is locked.
- What if an organiser attempts to create a standalone communication for an event they do not manage? The system rejects the request with an authorisation error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Organisers MUST be able to create a standalone communication for an event by providing at minimum a title and a scheduled date.
- **FR-002**: Standalone communications MUST be scoped to a single event and not visible across other events.
- **FR-003**: The communication planning view MUST include standalone communications alongside partnership-linked communications, sorted chronologically.
- **FR-004**: Each standalone communication entry in the planning view MUST expose: title, scheduled date, and an indicator that it is not linked to any partnership.
- **FR-005**: Organisers MUST be able to update the title, scheduled date, and description of any communication plan entry (standalone or partnership-linked) via the new communication plan endpoints.
- **FR-006**: Organisers MUST be able to delete any communication plan entry (standalone or partnership-linked) via the new communication plan endpoints.
- **FR-006b**: The existing `PUT .../partnerships/{id}/communication/publication` and `PUT .../partnerships/{id}/communication/support` routes MUST be updated to write to the new `communication_plans` table rather than `PartnershipsTable`.
- **FR-007**: The system MUST reject creation or update requests where the title is empty or missing.
- **FR-008**: The system MUST reject **creation** (`POST`) requests where the scheduled date is missing. For **update** (`PUT`) requests, setting `scheduled_date` to null is permitted and moves the entry to the `unplanned` group in the planning view.
- **FR-009**: Only organisers authorised for the event MUST be able to create, edit, or delete standalone communications for that event.
- **FR-010**: A standalone communication MUST support an optional description field for free-text notes about the communication content or intent.
- **FR-011**: A data migration MUST be written that reads all rows in `PartnershipsTable` where `communicationPublicationDate` is not null, and creates a corresponding row in the new `communication_plans` table with `partnershipId` set, `title` set to the company name of that partnership, `scheduledDate` populated from `communicationPublicationDate`, and `supportUrl` from `communicationSupportUrl`.
- **FR-012**: After migration, the `communicationPublicationDate` and `communicationSupportUrl` columns in `PartnershipsTable` MUST be marked as deprecated and no longer written to by the application code; the columns remain in the database schema but are frozen.
- **FR-013**: The new `communication_plans` table schema MUST be designed to allow a nullable `integrationId` foreign key column to be added in a future migration without requiring structural changes to existing columns.
- **FR-014**: The new CRUD endpoints MUST follow the route pattern: `POST /orgs/{orgSlug}/events/{eventSlug}/communication-plan` (create), `PUT /orgs/{orgSlug}/events/{eventSlug}/communication-plan/{id}` (update), `DELETE /orgs/{orgSlug}/events/{eventSlug}/communication-plan/{id}` (delete). The existing `GET /orgs/{orgSlug}/events/{eventSlug}/communication` planning view endpoint is a separate read-only route and is NOT replaced by this feature — only its data source changes.
- **FR-015**: Communication plan entries MUST NOT include a target audience field. Audience selection is determined at send time, outside the scope of this feature.

### Key Entities

- **Communication Plan Entry** (`communication_plans` table): The unified entity that replaces the current `communicationPublicationDate` / `communicationSupportUrl` fields on `PartnershipsTable`. Attributes: unique identifier, event reference, optional partnership reference (null for standalone), title, scheduled date, optional description, optional support URL (visual material), creation date, last updated date. No audience field. The schema must remain extensible for a future nullable `integrationId` FK.
- **Communication Planning View**: The unified projection returned by `GET /orgs/{orgSlug}/events/{eventSlug}/communication`. After this feature, it reads exclusively from `communication_plans` and groups entries into `done` (past scheduled date), `planned` (future scheduled date), and `unplanned` (no scheduled date). The `unplanned` group contains migrated partnership-linked entries that had no date set before migration, and any entry whose date has been cleared via `PUT`. New standalone entries require a scheduled date at creation, so they enter the `planned` or `done` group immediately.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Organisers can create a standalone communication and see it appear in the communication planning view in fewer than 3 interactions (e.g., fill form, confirm).
- **SC-002**: The communication planning view loads all entries (standalone and partnership-linked) for an event in a single request.
- **SC-003**: 100% of standalone communications created for an event appear in that event's communication planning view with accurate date and title.
- **SC-004**: Organisers can update or delete a standalone communication and see the planning view reflect the change immediately without a full page reload.

## Assumptions

- A "communication planning" view already exists (`GET /orgs/{orgSlug}/events/{eventSlug}/communication`) and currently builds its data from `PartnershipsTable.communicationPublicationDate` and `communicationSupportUrl`. This feature migrates that view to a new dedicated table.
- A standalone communication is a planning artefact (a scheduled entry) and does not automatically trigger email delivery — actual sending remains a separate action or workflow.
- Organisers are already authenticated and have event-scoped permissions enforced by the existing `AuthorizedOrganisationPlugin`.
- Past dates are valid for standalone communications to support retroactive planning records.
- The new `communication_plans` table will have an optional `partnershipId` foreign key, making it the single source of truth for all communications in the planning view — both partnership-linked and standalone.
- The architecture must remain extensible for a future optional `integrationId` FK (to auto-trigger communications via an external SaaS service), but this is NOT part of the current implementation.

## Clarifications

### Session 2026-03-16

- Q: Does an existing communication entity exist or should this create a new standalone entity? → A: Create a brand-new dedicated `communication_plans` table with an optional `partnershipId` reference. Existing `communicationPublicationDate` and `communicationSupportUrl` fields on `PartnershipsTable` will be deprecated. A data migration class MUST be written to move all existing scheduled communications from `PartnershipsTable` into the new table. The new table architecture should accommodate a future nullable `integrationId` FK without requiring breaking changes, but that column MUST NOT be added now.
- Q: What should the `title` be for partnership communications migrated from `PartnershipsTable`? → A: Use the company name (e.g., `"Acme Corp"`) as the title for all migrated entries.
- Q: Can organisers edit partnership-linked communication plan entries via the new endpoints, or only via existing partnership routes? → A: Any communication plan entry (standalone or partnership-linked) can be edited or deleted via the new endpoints. The existing `PUT .../communication/publication` and `PUT .../communication/support` partnership routes must be updated to write to the new `communication_plans` table instead of `PartnershipsTable`.
- Q: Where should the new CRUD endpoints live in the route hierarchy? → A: Dedicated event-level resource: `POST /orgs/{orgSlug}/events/{eventSlug}/communication-plan` for create; `GET /PUT /DELETE` on `…/communication-plan/{id}` for individual entry operations. The existing `GET /communication` planning view endpoint is unaffected.
- Q: Should standalone communications support an explicit target audience field? → A: No audience field — audience is determined at send time, not at the planning stage. The planning entry captures scheduling intent only.
