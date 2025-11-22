# Feature Specification: Assign Organiser to Partnership

**Feature Branch**: `011-assign-partnership-organiser`  
**Created**: November 21, 2025  
**Status**: Draft  
**Input**: User description: "As organiser, I want to be able to attach an organiser (myself or someone else in the team member of the organisation) to a partnership in an event. This will allow the partner to know who will follow its partnership during the process if he needs to contact someone directly and not always only the main generic email address."

## Clarifications

### Session 2025-11-22

- Q: Data retention for deleted organiser users - how should the system handle user account deletion when assigned to partnerships? → A: Users cannot be deleted from the system - organiser assignments always maintain valid references
- Q: Assignment timestamp recording - should the system track when assignments are created or modified? → A: No timestamp tracking - assignment is timeless, only current state matters
- Q: API response time performance target - should there be explicit performance targets for assignment operations? → A: No explicit API performance target - rely on general system performance expectations
- Q: Concurrent assignment modification handling - how should the system resolve conflicts when multiple administrators modify the same assignment simultaneously? → A: Last write wins - most recent assignment update takes effect, no conflict detection

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Assign Organiser to New Partnership (Priority: P1)

When creating or managing a partnership for an event, an organisation administrator assigns a specific team member (organiser) as the primary contact point for that partnership. This provides partners with a direct point of contact instead of relying only on generic organisational email addresses.

**Why this priority**: This is the core value proposition - enabling personal contact assignment to improve partner communication and relationship management.

**Independent Test**: Can be fully tested by creating a partnership, assigning an organiser from the organisation's team members, and verifying the assignment is stored and displayed correctly. Delivers immediate value by establishing a dedicated contact person.

**Acceptance Scenarios**:

1. **Given** I am an administrator of an organisation with an event containing a partnership, **When** I assign one of my organisation's team members as the organiser for that partnership, **Then** the assignment is saved and the organiser's contact information becomes associated with that partnership
2. **Given** I am an administrator managing a partnership, **When** I view the partnership details, **Then** I can see who the currently assigned organiser is (if any)
3. **Given** I am an administrator with edit permissions for an event, **When** I assign myself as the organiser for a partnership, **Then** my user details are linked to that partnership
4. **Given** a partnership has an assigned organiser, **When** a partner views the partnership information, **Then** they can see the organiser's name and contact details for direct communication

---

### User Story 2 - Change Assigned Organiser (Priority: P2)

As partnerships evolve or team responsibilities change, administrators need the ability to reassign partnerships to different organisers. This ensures continuity when team members change roles or leave.

**Why this priority**: Critical for operational flexibility, but secondary to the initial assignment capability.

**Independent Test**: Assign an organiser to a partnership, then change the assignment to a different team member, verifying the update is reflected and historical context is maintained appropriately.

**Acceptance Scenarios**:

1. **Given** a partnership already has an assigned organiser, **When** I change the assignment to a different organisation team member, **Then** the new organiser replaces the previous one and the change is reflected immediately
2. **Given** I need to unassign an organiser temporarily, **When** I remove the organiser assignment, **Then** the partnership no longer has a designated contact person
3. **Given** a partnership with an assigned organiser, **When** the organiser's user details change (name, email), **Then** the partnership automatically reflects the updated information

---

### User Story 3 - Bulk Organiser Assignment (Priority: P3)

For events with multiple partnerships, administrators can assign organisers to multiple partnerships simultaneously, streamlining team workload distribution.

**Why this priority**: Efficiency improvement for larger events, but not essential for MVP functionality.

**Independent Test**: Select multiple partnerships for an event and assign the same organiser to all of them in a single operation, verifying all assignments complete successfully.

**Acceptance Scenarios**:

1. **Given** an event with multiple partnerships, **When** I select several partnerships and assign the same organiser to all of them, **Then** all selected partnerships receive the organiser assignment
2. **Given** I want to distribute partnerships among team members, **When** I use bulk assignment features, **Then** I can efficiently allocate partnerships to different organisers based on their capacity or expertise

---

### Edge Cases

- What happens when an assigned organiser loses organisation membership or permissions? **Backend system maintains the assignment unchanged (no server-side status marking). Any visual indicators or warnings about inactive organisers are UI layer concerns outside this feature's scope. Administrators can reassign at any time via the API**
- How does the system handle assigning an organiser from a different organisation than the event? **System prevents this through validation (FR-003, FR-010) - only organisation members can be assigned**
- Can a partnership have multiple organisers, or only one at a time? **Only one organiser per partnership for MVP (P1), supporting clear single point of contact**
- What information is visible to partners versus internal team members about the assigned organiser? **Partners see: name, email, role (if available). Internal team members see: full user profile details, current assignment state (note: no historical tracking per FR-015 - "assignment history" refers to viewing who is currently assigned)**
- How are notifications handled when an organiser is assigned or changed? **No notifications are sent - assignments are silent operations. Organisers and partners discover assignments by viewing partnership details**
- What happens when multiple administrators try to assign organisers to the same partnership simultaneously? **Last write wins - the most recent assignment update takes effect without conflict detection or warnings**

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow administrators with edit permissions for an event to assign any member of their organisation as an organiser to a partnership within that event
- **FR-002**: System MUST store the organiser assignment as a relationship between the partnership and a user entity from the organisation
- **FR-003**: System MUST validate that the assigned organiser is a member of the organisation that owns the event before allowing assignment
- **FR-004**: System MUST allow administrators to view the currently assigned organiser for any partnership
- **FR-005**: System MUST allow administrators to change the assigned organiser to a different organisation member at any time
- **FR-006**: System MUST allow administrators to remove an organiser assignment, leaving the partnership without a designated contact
- **FR-007**: System MUST display the assigned organiser's name and contact information to partners viewing their partnership details
- **FR-008**: Partners MUST be able to see the assigned organiser's email address for direct communication purposes
- **FR-009**: System MUST handle cases where an assigned organiser loses organisation membership by maintaining the assignment unchanged (backend does not mark as inactive - any visual indicators or warnings are UI layer concerns outside this feature's scope)
- **FR-010**: System MUST prevent assigning users who are not members of the partnership's event organisation
- **FR-011**: System MUST support optional organiser assignment - partnerships without an assigned organiser should remain valid and functional
- **FR-012**: System MUST maintain organiser assignment independently from other partnership contact information (contact_name, contact_role fields which represent partner-side contacts)
- **FR-013**: System MUST NOT send notifications when an organiser is assigned or changed - assignments are discovered through viewing partnership details
- **FR-014**: System MUST maintain valid foreign key references to user accounts - user accounts cannot be deleted, ensuring organiser assignments remain valid
- **FR-015**: System MUST store only the current organiser assignment state without timestamps - no creation or modification time tracking required
- **FR-016**: System MUST use last-write-wins strategy for concurrent assignment modifications - no conflict detection or optimistic locking required

### Key Entities *(include if feature involves data)*

- **Partnership**: Represents the sponsorship relationship between a company and an event; will now optionally reference an assigned organiser
- **User**: Represents organisation team members who can be assigned as organisers for partnerships
- **Organisation**: The entity that owns events and has team members who can be assigned as organisers
- **Event**: Context within which partnerships exist and organisers are assigned
- **Organiser Assignment**: The relationship linking a user (organiser) to a partnership; stores only current state without timestamps

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Partners can identify their assigned organiser contact within 5 seconds of viewing partnership details
- **SC-002**: Administrators can assign an organiser to a partnership in under 30 seconds
- **SC-003**: 80% of partnerships have an assigned organiser within the first week after feature deployment
- **SC-004**: Partner satisfaction scores for "ease of contacting event team" improve by 25% within 3 months
- **SC-005**: Direct email communication with assigned organisers reduces generic support email volume by 40%
- **SC-006**: Organiser reassignment operations complete without data loss or communication disruption in 100% of cases

## Assumptions

- Organisation membership and permissions systems are already in place and can be queried to validate organiser eligibility
- Users table contains sufficient contact information (email, name) to be displayed to partners
- Partnership entities already exist with proper relationships to events and organisations
- The existing contact_name and contact_role fields on partnerships represent partner-side contacts, not organisation-side contacts
- Email is the primary communication channel for direct partner-organiser contact
- Single organiser per partnership is sufficient for MVP; multiple organiser support can be added later if needed
- Partners have access to view partnership details through an existing interface that can be extended to show organiser information
- No notifications are required for organiser assignment changes - users discover assignments by viewing partnership details
- User accounts cannot be deleted from the system, ensuring organiser assignment references remain valid permanently
- API performance for assignment operations follows general system performance expectations without feature-specific targets

## Scope

### In Scope

- Assigning a single organiser (user) to a partnership
- Viewing the assigned organiser for a partnership (both administrator and partner views)
- Changing/updating the assigned organiser
- Removing an organiser assignment
- Validating that assigned users are members of the event's organisation
- Displaying organiser contact information to partners
- Handling edge cases where organisers lose membership or are deleted

### Out of Scope

- Multiple organisers per partnership (future enhancement)
- Organiser workload tracking or capacity management
- Automatic organiser assignment based on rules or algorithms
- Historical tracking of organiser assignment changes (beyond current assignment)
- Organiser performance metrics or analytics
- Integration with external communication tools beyond email display
- Bulk reassignment across multiple events simultaneously
- Organiser availability or scheduling features
- Notification system for organiser assignments (silent assignments only)
