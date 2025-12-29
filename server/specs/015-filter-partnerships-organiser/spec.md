# Feature Specification: Filter Partnerships by Assigned Organiser

**Feature Branch**: `015-filter-partnerships-organiser`  
**Created**: December 29, 2025  
**Status**: Draft  
**Input**: User description: "As an organiser, I want to filter partnership of an event which are assigned to an organiser by his email address but the api should return also the display name of the organiser for frontend experience purpose."

**Note**: Partnership responses already include organiser information (email and displayName). This feature adds filtering capability and pagination metadata with available organisers.

## Clarifications

### Session 2025-12-29

- Q: Under what conditions should the API include the `filters` and `sorts` metadata in the response? → A: Always include metadata in every partnership list response
- Q: What permission level must a user have to be included in the available organisers list in the metadata? → A: Users with edit permissions or higher on the organisation
- Q: What HTTP status code should be returned when the email endpoint is called with filters that match zero partnerships? → A: HTTP 204 No Content

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Filter Partnerships by Organiser Email (Priority: P1)

Event organizers need to view partnerships assigned to specific team members to track workload distribution, review progress, and ensure proper follow-up. When viewing the partnership list, organizers should be able to filter by a specific organiser's email address and see which partnerships are assigned to them, including the organiser's full name for easy identification in the frontend interface.

**Why this priority**: This is the core functionality that enables workload visibility and partnership management oversight. It's essential for team coordination and ensures organizers can track who is responsible for which partnerships.

**Independent Test**: Can be fully tested by authenticating as an organizer with view permissions, applying the organiser email filter (`filter[organiser]=email@example.com`) to the partnership list endpoint, and verifying that only partnerships assigned to that specific organiser are returned, with each partnership including the organiser's display name. Delivers standalone value for team management and workload tracking.

**Acceptance Scenarios**:

1. **Given** an organizer is authenticated with view permissions for an event that has partnerships with assigned organizers, **When** they request the partnership list with `filter[organiser]=john.doe@example.com`, **Then** the system returns only partnerships where the assigned organiser's email matches exactly, and each partnership response includes the organiser's display name
2. **Given** an organizer filters partnerships by organiser email, **When** some partnerships have no assigned organiser, **Then** those partnerships are excluded from the results
3. **Given** an organizer filters by an email address that has no assigned partnerships, **When** the request is processed, **Then** the system returns an empty list with HTTP 200 status
4. **Given** an organizer applies multiple filters including organiser email (e.g., `filter[organiser]=john@example.com&filter[validated]=true`), **When** the request is processed, **Then** only partnerships matching ALL criteria are returned

---

### User Story 2 - Pagination Metadata with Available Filters and Organisers (Priority: P2)

When viewing the partnership list, organizers need to understand what filtering and sorting options are available, and specifically for the organiser filter, they need to see which organisers in their organisation can be used as filter values. This helps frontend applications build dynamic filter UIs without hardcoding options.

**Why this priority**: This enhances the API's discoverability and enables frontend applications to dynamically render filter options based on available organisers in the organisation. While the core filtering works without this (P1), the metadata significantly improves developer experience and UI capabilities.

**Independent Test**: Can be tested by requesting the partnership list and verifying that the response includes pagination metadata with `filters` containing all filter options (including `organiser` with a list of organisation members who can be used as filter values) and `sorts` listing sortable fields. Delivers standalone value for API discoverability and dynamic UI generation.

**Acceptance Scenarios**:

1. **Given** an organizer requests the partnership list for an event, **When** the response is returned, **Then** it includes pagination metadata with `filters` array containing filter definitions for all supported filters
2. **Given** pagination metadata includes available filters, **When** the `organiser` filter is present, **Then** it includes a `values` array containing objects with `email` and `displayName` for all users in the organisation who have edit permissions
3. **Given** pagination metadata includes available filters, **When** an organiser has no partnerships assigned, **Then** they are still included in the available organisers list
4. **Given** an organizer requests the partnership list, **When** pagination metadata is included, **Then** it includes `sorts` array listing fields that can be used for sorting

---

### User Story 3 - Filter Partnerships by Organiser in Email Endpoint (Priority: P3)

When sending bulk emails to partnerships, organizers need the ability to filter recipients by assigned organiser to send targeted communications to partnerships managed by specific team members. This enables organizers to send emails only to partnerships they are responsible for or to coordinate communications across the team.

**Why this priority**: This extends the filtering capability to the email workflow, enabling more targeted communication. While useful, it depends on the core filtering functionality (P1) and is less critical than the ability to view and filter partnerships in the list view.

**Independent Test**: Can be tested by using the email partnerships endpoint with the `filter[organiser]` parameter and verifying that only partnerships assigned to the specified organiser receive the email. Delivers standalone value for targeted bulk communication to partnership subsets based on team member assignments.

**Acceptance Scenarios**:

1. **Given** an organizer is authenticated and has edit permissions for an event, **When** they send an email to partnerships using `filter[organiser]=john@example.com`, **Then** only partnerships assigned to that organiser receive the email
2. **Given** an organizer uses the email endpoint with multiple filters including organiser (e.g., `filter[organiser]=john@example.com&filter[validated]=true`), **When** the email is sent, **Then** only partnerships matching ALL filter criteria receive the email
3. **Given** an organizer filters by an organiser email that has no assigned partnerships, **When** the email request is processed, **Then** the system returns HTTP 204 No Content (no recipients match the filter criteria, no email sent)
4. **Given** the organiser filter is used in the email endpoint, **When** partnerships are retrieved for emailing, **Then** the same filter logic as the list endpoint is applied (case-insensitive email matching, exclude unassigned partnerships)

---

### Edge Cases

- What happens when the filter email parameter contains invalid characters or is not a valid email format? System should still perform the exact string match (no email validation required for filtering)
- What happens when the same organiser is assigned to multiple partnerships? All matching partnerships should be returned
- What happens when combining organiser filter with other filters that result in no matches? Return empty list with HTTP 200
- What happens when an organiser is removed from a partnership after being assigned? The filter should no longer return that partnership for that email

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST add a new query parameter `filter[organiser]` to the existing GET `/orgs/{orgSlug}/events/{eventSlug}/partnerships` endpoint that accepts an email address as a string value
- **FR-002**: System MUST filter partnerships to return only those where the assigned organiser's email exactly matches the provided filter value (case-insensitive comparison)
- **FR-003**: System MUST apply the organiser filter using AND logic with any other active filters (`filter[validated]`, `filter[paid]`, etc.)
- **FR-004**: System MUST exclude partnerships with no assigned organiser when the organiser filter is applied
- **FR-005**: System MUST enhance the PaginatedResponse model to include pagination metadata containing `filters` and `sorts` arrays in every response
- **FR-006**: System MUST include a `filters` array in pagination metadata listing all supported filter parameters for the endpoint
- **FR-007**: System MUST define each filter in `filters` with properties: `name` (filter parameter name), `type` (data type), and optionally `values` (array of valid options)
- **FR-008**: System MUST include the `organiser` filter in `filters` with `type: "string"` and a `values` array containing all organisation members
- **FR-009**: System MUST populate the `organiser` filter's `values` array with objects containing `email` (string) and `displayName` (string) for each user in the organisation who has edit permissions or higher
- **FR-010**: System MUST include users in the available organisers list regardless of whether they currently have any partnerships assigned
- **FR-011**: System MUST include a `sorts` array in pagination metadata listing all fields that can be used for sorting
- **FR-012**: System MUST return HTTP 200 with an empty array when no partnerships match the organiser filter criteria
- **FR-013**: System MUST add the `filter[organiser]` query parameter to the email partnerships endpoint (POST `/orgs/{orgSlug}/events/{eventSlug}/partnerships/email`)
- **FR-014**: System MUST apply the same organiser filter logic in the email endpoint as in the list endpoint (case-insensitive matching, exclude unassigned partnerships)
- **FR-015**: System MUST combine the organiser filter with other filters using AND logic when selecting partnerships to email
- **FR-016**: System MUST return HTTP 204 No Content when the email endpoint is called with filters that match zero partnerships (no recipients)

### Key Entities *(include if feature involves data)*

- **Partnership**: Core entity linking company to event, contains optional reference to assigned organiser user (organiser information already included in response)
- **Organiser (User)**: User entity assigned to manage specific partnerships, contains email, first name, and last name
- **PaginatedResponse**: Response wrapper containing partnership data array and pagination metadata (page, size, total, plus new optional `filters` and `sorts`)
- **Pagination Metadata**: Optional metadata object containing `filters` (array of filter definitions) and `sorts` (array of sortable fields)
- **Filter Definition**: Object describing a filter parameter with properties: `name` (parameter name like "organiser"), `type` (data type like "string" or "boolean"), and optional `values` (array of valid options)
- **Organiser Filter**: Query parameter (`filter[organiser]`) accepting email address string for filtering partnerships by assigned organiser
- **Email Partnerships Endpoint**: Existing POST endpoint for sending bulk emails to filtered partnerships, now supporting organiser filter

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Organizers can retrieve all partnerships assigned to a specific team member in under 2 seconds
- **SC-002**: System accurately filters partnerships with 100% precision (no false positives or false negatives) when organiser filter is applied
- **SC-003**: Pagination metadata includes complete list of available filters and organisers without requiring additional API calls, reducing frontend API requests by 50%
- **SC-004**: Frontend applications can dynamically generate filter UI components based on available filters metadata without hardcoding filter options
- **SC-005**: Filter combines seamlessly with existing partnership filters without breaking current functionality
- **SC-006**: Organizers can send targeted bulk emails to partnerships assigned to specific team members using the organiser filter in the email endpoint

## Assumptions *(mandatory)*

1. **Email Uniqueness**: Each organiser has a unique email address in the system (enforced at database level)
2. **User Entity Exists**: The existing User entity contains email, firstname, and lastname fields with non-null values
3. **Partnership-User Relationship**: The existing Partnership entity has an optional foreign key reference to User (organiser_user_id), and organiser information is already included in partnership list responses
4. **Existing Partnership Endpoint**: The GET `/orgs/{orgSlug}/events/{eventSlug}/partnerships` endpoint exists and supports multiple filter parameters
5. **PaginatedResponse Model Exists**: The response uses a PaginatedResponse wrapper that includes metadata in every response
6. **Case Insensitivity**: Email comparison should be case-insensitive to match common user expectations (emails are case-insensitive by standard)
7. **Organisation Membership**: Users eligible to be organisers are those with edit permissions or higher on the organisation
8. **Performance**: Fetching organisation members for metadata can be done efficiently without impacting response time
9. **Authorization**: Existing authentication and authorization mechanisms apply (organizers must have view permissions for the event)

## Out of Scope *(mandatory)*

1. **Partial Email Matching**: Filtering by partial email strings or wildcards (only exact email matches are supported)
2. **Multiple Organiser Filtering**: Filtering by multiple organiser emails in a single request (e.g., `filter[organiser]=email1,email2`)
3. **Organiser Assignment/Unassignment**: Creating, updating, or removing organiser assignments (existing functionality in `/partnerships/{id}/organiser` endpoints)
4. **Organiser List Endpoint**: Dedicated endpoint to list all organizers or filter by organiser name
5. **Email Validation**: Validating email format in the filter parameter (accepts any string for backwards compatibility)
6. **Organiser Profile Details**: Including additional organiser information beyond email and display name in pagination metadata (e.g., profile picture, phone number)
7. **Filtering by Organiser Display Name**: Filtering partnerships by organiser's first or last name (only email-based filtering)
8. **Sorting by Organiser**: Adding sort options specifically for organiser name or email (though can be listed in `availableSorts` if already supported)
9. **Aggregation/Statistics**: Counting partnerships per organiser or generating workload statistics in pagination metadata
10. **Dynamic Filter Values for Other Filters**: Only the `organiser` filter includes dynamic `values` array; other filters (validated, paid, etc.) remain as boolean or string types without enumerated values

## Dependencies *(optional)*

1. **Existing Partnership Repository**: Must reuse or extend existing partnership query logic to maintain consistency with other filters
2. **User Repository**: Must access User entity data for fetching organisation members with their email and display name
3. **Organisation Permissions**: Must query organisation permissions to determine which users are eligible organisers
4. **Database Schema**: Depends on existing `partnerships.organiser_user_id` foreign key relationship to `users.id`
5. **PaginatedResponse Model**: Must extend existing PaginatedResponse to support optional metadata field
6. **Email Partnerships Endpoint**: The existing POST `/orgs/{orgSlug}/events/{eventSlug}/partnerships/email` endpoint must support the same filter parameters as the list endpoint
7. **OpenAPI Specification**: Must update existing partnership list endpoint schema to include new filter parameter and pagination metadata structure, plus update email endpoint schema

## Technical Constraints *(optional)*

1. **Database Performance**: Query must remain performant even with organiser filter applied alongside other filters (target: sub-2-second response time for typical partnership list sizes of up to 1000 records)
2. **Backwards Compatibility**: Adding pagination metadata fields to PaginatedResponse must not break existing API consumers (existing clients can ignore new fields)
3. **Null Handling**: Frontend must handle null organiser values gracefully for partnerships without assigned organizers (already implemented)
4. **Query Parameter Naming**: Must follow existing filter parameter convention (`filter[organiser]` matching `filter[validated]`, `filter[paid]`, etc.)
5. **Metadata Performance**: Fetching available organisers for metadata should not significantly impact response time (consider caching organisation members)
6. **Response Size**: Including metadata with all organisation members should not exceed reasonable response size limits (typical organisations have 10-100 members)
