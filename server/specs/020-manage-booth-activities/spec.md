# Feature Specification: Manage Booth Activities

**Feature Branch**: `020-manage-booth-activities`
**Created**: 2026-03-20
**Status**: Draft
**Input**: User description: "As a partnership owner, I should be able to manage activities on my partnership space if the partnership has an option with a booth. I'd be able to add, edit, delete and get activities, and for each activity, I should be able to encode a title, description and an optional start time and end time. These new endpoints should call the webhook integration if exist, thanks to the dedicated ktor plugin."

## Clarifications

### Session 2026-03-21

- Q: What is the URL path structure for activity endpoints? → A: `/events/{eventSlug}/partnerships/{partnershipId}/activities` — `/orgs` prefix is reserved for authenticated organiser routes; the path parameter is a UUID named `{partnershipId}` (not `{partnershipSlug}`) because `WebhookPartnershipPlugin` hard-codes `call.parameters.partnershipId`.
- Q: How is a "booth option" identified on a partnership? → A: `partnership_options` has a FK to `sponsoring_options`; if any linked `sponsoring_options` row has `selectable_descriptor = 'booth'`, the partnership has a booth.
- Q: What HTTP status code is returned when a partnership has no booth option? → A: `403 Forbidden`.
- Q: What webhook event type/payload must be sent for activity mutations? → A: None — install `WebhookPartnershipPlugin` on the route group; it handles dispatch automatically.
- Q: What HTTP status code for non-existent partnership or activity? → A: `404 Not Found`.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add an Activity to a Booth (Priority: P1)

A partnership owner whose partnership includes a booth option wants to schedule an activity that will take place at their booth during the event. They provide a title, a description, and optionally a start time and end time. The activity is saved and immediately associated with their partnership.

**Why this priority**: This is the foundational capability of the feature. Without the ability to create activities, nothing else (editing, deleting, retrieving) has meaning. It unlocks the most direct user value.

**Independent Test**: Can be fully tested by creating an activity on a partnership with a booth option and verifying the activity is returned in the activity list for that partnership.

**Acceptance Scenarios**:

1. **Given** a partnership owner whose partnership has a booth option, **When** they submit a new activity with a title, description, and both start and end times, **Then** the activity is saved and returned with a unique identifier, title, description, start time, end time, and creation date.
2. **Given** a partnership owner whose partnership has a booth option, **When** they submit a new activity with only a title and description (no times), **Then** the activity is saved successfully with null start and end times.
3. **Given** a partnership owner whose partnership has a booth option, **When** they submit an activity without a title, **Then** the request is rejected with a validation error.
4. **Given** a partnership owner whose partnership has a booth option, **When** they submit an activity without a description, **Then** the request is rejected with a validation error.
5. **Given** a partnership owner whose partnership does NOT have a booth option, **When** they attempt to create an activity, **Then** the system rejects the request with `403 Forbidden`.
6. **Given** a partnership owner creating an activity, **When** the partnership has a configured webhook integration, **Then** the system sends a webhook notification after the activity is created.

---

### User Story 2 - List Activities for a Booth Partnership (Priority: P1)

A partnership owner wants to review all activities they have scheduled for their booth. They request the activity list for their partnership and receive all activities in a consistent order.

**Why this priority**: Reading existing activities is as essential as creating them — without a retrieval endpoint, the owner cannot confirm their entries or display them to event attendees.

**Independent Test**: Can be fully tested by seeding activities on a partnership and verifying the list endpoint returns them all with correct fields.

**Acceptance Scenarios**:

1. **Given** a partnership with a booth option and several activities saved, **When** the partnership owner requests the activity list, **Then** all activities are returned sorted by start time in ascending order (activities with no start time appear last).
2. **Given** a partnership with a booth option and no activities, **When** the partnership owner requests the activity list, **Then** an empty list is returned without error.
3. **Given** a request for activities using a partnership identifier, **When** the partnership does not exist, **Then** the system rejects the request with `404 Not Found`.

---

### User Story 3 - Edit an Existing Activity (Priority: P2)

A partnership owner needs to update an activity they previously created — for example, to correct the title, update the description, or adjust the start and end times. They submit the update and the activity reflects the new values.

**Why this priority**: Activities are planned in advance and details often change. Without edit capability, owners would need to delete and recreate entries, creating friction and potential data loss.

**Independent Test**: Can be fully tested by creating an activity, updating one or more fields, and verifying the activity list reflects the new values.

**Acceptance Scenarios**:

1. **Given** an existing activity on a booth partnership, **When** the owner updates the title, description, start time, or end time, **Then** the changes are persisted and the updated activity is returned.
2. **Given** an existing activity with a start time and end time, **When** the owner updates the activity setting start time and end time to null, **Then** the activity is saved with no times.
3. **Given** an existing activity, **When** the owner submits an update with an empty title, **Then** the system rejects the request with a validation error.
4. **Given** an existing activity, **When** the owner submits an update with an empty description, **Then** the system rejects the request with a validation error.
5. **Given** an existing activity on a booth partnership with a configured webhook integration, **When** the owner updates the activity, **Then** the system sends a webhook notification after the update.
6. **Given** an owner attempting to edit an activity belonging to a different partnership or a non-existent activity ID, **When** they submit the update, **Then** the system rejects the request with `404 Not Found`.

---

### User Story 4 - Delete an Activity (Priority: P2)

A partnership owner decides that a previously created activity should be removed — for example, because the activity was cancelled. They delete the activity and it is no longer returned in the activity list.

**Why this priority**: Maintaining an accurate activity programme requires the ability to remove entries that are no longer valid.

**Independent Test**: Can be fully tested by creating an activity, deleting it, and confirming it no longer appears in the activity list.

**Acceptance Scenarios**:

1. **Given** an existing activity on a booth partnership, **When** the owner deletes it, **Then** the activity is removed and no longer appears in the activity list for that partnership.
2. **Given** an owner attempting to delete an activity belonging to a different partnership or a non-existent activity ID, **When** they submit the deletion, **Then** the system rejects the request with `404 Not Found`.
3. **Given** an existing activity on a booth partnership with a configured webhook integration, **When** the owner deletes it, **Then** the system sends a webhook notification after the deletion.

---

### Edge Cases

- What happens when the start time is after the end time? The system rejects the request with a validation error.
- What happens when a partnership's booth option is removed after activities have been added? Existing activities remain stored; the write endpoints (create, update, delete) return `403 Forbidden` until the booth option is restored, but the list (`GET`) endpoint remains accessible and returns the stored activities.
- What happens when a webhook integration is configured but the webhook call fails? The activity operation is still considered successful; the webhook failure is logged but does not roll back the data change.
- What happens when two activities have the same start time? Both are returned; ordering within the same start time is by creation date ascending.
- What happens when accessing the list endpoint for a partnership that has no booth option? The list endpoint is accessible regardless of booth eligibility — it returns the stored activities (or an empty list). Only write operations require the booth option.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Write activity endpoints (POST, PUT, DELETE) MUST only be accessible for partnerships where at least one `sponsoring_options` row linked via `partnership_options` has `selectable_descriptor = 'booth'`; write requests on ineligible partnerships MUST be rejected with `403 Forbidden`. The read endpoint (GET list) is exempt from this gate and is accessible for any existing partnership.
- **FR-002**: Partnership owners MUST be able to create an activity by providing a title and a description; start time and end time are optional.
- **FR-003**: Partnership owners MUST be able to retrieve the complete list of activities associated with their partnership.
- **FR-004**: Partnership owners MUST be able to update the title, description, start time, and end time of an existing activity.
- **FR-005**: Partnership owners MUST be able to delete an existing activity by its identifier.
- **FR-006**: The system MUST reject activity creation or update requests where the title is missing or empty.
- **FR-007**: The system MUST reject activity creation or update requests where the description is missing or empty.
- **FR-008**: The system MUST reject activity creation or update requests where a start time and end time are both provided and the start time is after the end time.
- **FR-009**: Activities MUST be scoped to a single partnership and MUST NOT be visible or accessible from other partnerships.
- **FR-010**: After every successful create, update, or delete operation, the system MUST trigger the webhook via the `WebhookPartnershipPlugin` installed on the activity route group; no manual event type or payload construction is required.
- **FR-011**: A webhook invocation failure MUST NOT cause the corresponding activity operation to fail; the failure MUST be logged without rolling back the data change.
- **FR-012**: Activity management endpoints MUST be public (no authentication required); access is scoped to the partnership identified in the request path.
- **FR-013**: The activity list MUST be returned sorted by start time in ascending order; activities with no start time MUST appear at the end of the list.
- **FR-014**: Each activity MUST be uniquely identified and MUST expose at minimum: identifier, title, description, start time (nullable), end time (nullable), and creation date.
- **FR-015**: The system MUST return `404 Not Found` when the partnership identifier in the path does not resolve to an existing partnership, or when an activity identifier does not exist within the addressed partnership.

### Key Entities

- **Activity**: Represents a scheduled programme item at a partnership's booth. Attributes: unique identifier, partnership reference, title (required), description (required), start time (optional, date-time), end time (optional, date-time), creation date. Scoped to a single partnership.
- **Partnership** (existing): Carries a set of options via the `partnership_options` join table. A partnership is considered to have a booth when at least one linked `sponsoring_options` row has `selectable_descriptor = 'booth'`. This is evaluated at the time of each request to gate access to the activity endpoints.
- **SponsoringOption** (existing): Represents a sponsoring option. Has a nullable `selectable_descriptor` field; when its value is `'booth'`, the associated partnership is eligible for booth activity management.
- **WebhookPartnershipPlugin** (existing): A Ktor plugin that, when installed on a route group, automatically dispatches webhook notifications for the partnership after each successful mutation. No manual invocation or payload construction is needed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Partnership owners can create a new booth activity with all required fields in a single request without needing additional steps.
- **SC-002**: 100% of activities created for a partnership are returned by the activity list endpoint with accurate field values.
- **SC-003**: Webhook notifications are dispatched for 100% of successful activity mutations when an integration is configured, independently of the outcome of the webhook call itself.
- **SC-004**: Activities are strictly scoped to the partnership identified in the request path — no activity from one partnership is ever returned or modified via another partnership's endpoints.
- **SC-005**: Activity write endpoints (create, update, delete) are not accessible for partnerships without a booth option — 100% of such write attempts are rejected with `403 Forbidden`. The list endpoint is always accessible for valid partnerships.

## Assumptions

- A "booth option" is identified via the `sponsoring_options` table: a partnership has a booth when at least one `sponsoring_options` row linked through `partnership_options` has `selectable_descriptor = 'booth'`. The `selectable_descriptor` field is nullable; a null value means no booth designation.
- The `WebhookPartnershipPlugin` Ktor plugin already exists and MUST be installed on the activity route group; this feature requires no other webhook integration work.
- Start time and end time are full date-time values (not time-only), carrying implicit date context.
- There is no pagination requirement for the activity list at this stage; all activities for a partnership are returned in a single response.
- Concurrent edits to the same activity are not a concern at this scope; last-write-wins is acceptable.
- Activities are free-form entries owned entirely by the partnership owner and are not linked to event-level session management systems.
- Activity endpoints follow the path pattern `/events/{eventSlug}/partnerships/{partnershipId}/activities`. The path parameter is a UUID named `{partnershipId}` (not `{partnershipSlug}`). The `/orgs/{orgSlug}` prefix is reserved for authenticated organiser routes and MUST NOT be used here.
