# Feature Specification: User Permission Revocation for Organisations

**Feature Branch**: `005-as-an-organizer`  
**Created**: 24 October 2025  
**Status**: Draft  
**Input**: User description: "As an organizer, I can revoke a user from an organisation where I have edit permission. The service will take a list of email address like the grant service but instead of adding new user in the organisation with edit permission, the revoke service will remove these users from the organisation to remove their permission for all events inside the organisation."

## Execution Flow (main)
```
1. Parse user description from Input
   → If empty: ERROR "No feature description provided"
2. Extract key concepts from description
   → Identified: organizer (actor), revoke action, user emails (data), edit permission (constraint)
3. For each unclear aspect:
   → No major ambiguities identified - feature mirrors existing grant functionality
4. Fill User Scenarios & Testing section
   → User flow is clear: authorized user revokes permissions via email list
5. Generate Functional Requirements
   → Each requirement must be testable
6. Identify Key Entities (if data involved)
   → Organisations, Users, OrganisationPermissions
7. Run Review Checklist
   → No implementation details included
8. Return: SUCCESS (spec ready for planning)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers

---

## Clarifications

### Session 2025-10-24
- Q: When a user revokes their own access (self-revocation), what should the system do? → A: Allow only if not the last user with edit permission
- Q: When the request includes email addresses of users who don't exist in the system, what should happen? → A: Return partial success with list of non-existent emails in response
- Q: Which permission levels can be revoked through this feature? → A: Only edit permissions
- Q: Should the system maintain an audit log of revocation actions? → A: No audit logging required
- Q: What is the maximum number of email addresses allowed in a single revocation request? → A: No limitation

---

## User Scenarios & Testing

### Primary User Story
As an organizer with edit permissions on an organisation, I need to revoke user access from that organisation to remove their permissions across all events within it. This allows me to manage team changes, remove former collaborators, or adjust access control as the organisation's needs evolve.

### Acceptance Scenarios

1. **Given** I am an organizer with edit permission on "DevLille 2025" organisation, **When** I revoke access for users "alice@example.com" and "bob@example.com", **Then** both users lose their edit permissions for the organisation and all its associated events, and I receive confirmation of successful revocation.

2. **Given** I am an organizer with edit permission on "DevLille 2025" organisation, **When** I revoke access for a user who already has no permissions, **Then** the system processes the request without error and confirms completion (idempotent operation).

3. **Given** I am an organizer without edit permission on "DevLille 2025" organisation, **When** I attempt to revoke user access, **Then** the system denies my request with an unauthorized error message.

4. **Given** I am an organizer with edit permission on "DevLille 2025" organisation, **When** I submit a revocation request with an empty email list, **Then** the system validates the request and returns an appropriate response without making changes.

5. **Given** I am not authenticated, **When** I attempt to revoke user access from any organisation, **Then** the system denies access with an authentication error.

6. **Given** I am an authenticated user not registered in the system, **When** I attempt to revoke user access, **Then** the system returns a not found error indicating I am not a registered user.

### Edge Cases

- What happens when I try to revoke my own access? The system allows self-revocation only if the user is not the last user with edit permissions on the organisation. If they are the last editor, the system returns an error preventing orphaned organisations.
- What happens when I provide email addresses of users who don't exist in the system? The system processes the revocation for all existing users and returns a partial success response that includes a list of non-existent email addresses that were skipped.
- What happens when I provide duplicate email addresses in the list? The system should deduplicate and process each unique email once.
- What happens when the organisation doesn't exist? The system should return a not found error.
- What happens when I revoke the last user with edit permissions? The system should allow this but potentially flag it as a warning since the organisation would have no managers.

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST allow authenticated users with edit permission on an organisation to revoke access for other users from that organisation.

- **FR-002**: System MUST accept a list of email addresses identifying the users whose access should be revoked.

- **FR-002a**: System MUST accept an unlimited number of email addresses in a single revocation request (no batch size limit).

- **FR-003**: System MUST remove all organisation-level permissions for the specified users, which automatically removes their access to all events within that organisation.

- **FR-003a**: System MUST only revoke edit permissions; view-only permissions are out of scope for this feature.

- **FR-004**: System MUST verify that the requesting user has edit permission on the target organisation before processing any revocation.

- **FR-005**: System MUST deny revocation requests from unauthenticated users.

- **FR-006**: System MUST deny revocation requests from authenticated users who are not registered in the system.

- **FR-007**: System MUST deny revocation requests from users who lack edit permission on the target organisation.

- **FR-008**: System MUST return success confirmation when the revocation operation completes successfully.

- **FR-008a**: System MUST return partial success response when some users are revoked successfully but others are not found, including a list of non-existent email addresses.

- **FR-009**: System MUST handle revocation requests for users who already lack permissions without error (idempotent operation).

- **FR-010**: System MUST validate the revocation request payload to ensure it contains required fields.

- **FR-011**: System MUST identify the target organisation for the revocation operation.

- **FR-012**: System MUST process revocation operations atomically to ensure data consistency.

- **FR-013**: System MUST prevent users from revoking their own access if they are the last user with edit permission on the organisation.

### Key Entities

- **Organisation**: Represents a group or team that manages multiple events. Has a unique identifier (slug) and contains users with varying permission levels.

- **User**: Represents an individual account identified by email address. Can have permissions on one or more organisations.

- **OrganisationPermission**: Represents the access relationship between a user and an organisation. Includes permission level (edit/view). This feature specifically revokes edit permissions; view permissions are not affected.

- **Event**: Represents an occurrence or activity managed within an organisation. Users inherit access to events through their organisation permissions.

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
- [x] Scope is clearly bounded (mirrors existing grant functionality with opposite effect)
- [x] Dependencies and assumptions identified (existing grant service pattern, organisation permission model)

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked (none identified)
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---
