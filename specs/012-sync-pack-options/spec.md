# Feature Specification: Synchronize Pack Options

**Feature Branch**: `012-sync-pack-options`  
**Created**: November 24, 2025  
**Status**: Draft  
**Input**: User description: "Improve the existing endpoint that assign options to a pack: POST /orgs/{orgSlug}/events/{eventSlug}/packs/{packId}/options. For now, this endpoint only assign options to a pack but we want to delete also existing options which aren't in input of the endpoint."

## Clarifications

### Session 2025-11-24

- Q: When validation fails (e.g., duplicate option in both required and optional lists, or non-existent option ID), what HTTP status code and error structure should the endpoint return? → A: Specific status codes: 409 Conflict for duplicates, 403 Forbidden for options not in event, 404 Not Found for non-existent options/pack
- Q: When multiple organizers attempt to modify the same pack's options simultaneously, how should the system handle concurrent modifications? → A: Last write wins - accept all concurrent modifications, most recent request overwrites
- Q: Should the system create an audit trail or log entry when pack options are modified, and if so, what information should be captured? → A: No audit logging required - rely on standard application logs only
- Q: What is the maximum number of options (combined required and optional) that should be allowed in a single pack configuration update? → A: No limit
- Q: When the synchronization operation fails partway through (e.g., database error after some options deleted but before new ones added), what cleanup or rollback behavior is expected? → A: Automatic rollback via database transaction - all changes reverted on any failure

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Complete Pack Configuration Update (Priority: P1)

An event organizer needs to update which sponsoring options (both required and optional) are included in a sponsoring pack. When they submit the new configuration, the system should reflect exactly what was submitted - removing options that are no longer selected and adding newly selected ones in a single operation.

**Why this priority**: This is the core functionality that allows organizers to maintain accurate pack configurations without manual cleanup steps. Without this, organizers must delete unwanted options individually before adding new ones, creating opportunity for errors.

**Independent Test**: Can be fully tested by submitting a complete options list for a pack and verifying the pack contains exactly those options (no more, no less), delivering a single-action pack configuration update.

**Acceptance Scenarios**:

1. **Given** a pack has 3 existing options attached (2 required, 1 optional), **When** an organizer submits a new configuration with 2 different options (1 required, 1 optional), **Then** the pack contains exactly the 2 new options and the 3 old options are removed
2. **Given** a pack has 5 existing options, **When** an organizer submits a configuration with only 2 of those existing options plus 1 new option, **Then** the pack contains exactly those 3 options (2 retained, 1 new, 3 removed)
3. **Given** a pack has existing options, **When** an organizer submits an empty configuration (no required or optional options), **Then** all existing options are removed from the pack

---

### User Story 2 - Option Requirement Status Update (Priority: P2)

An event organizer needs to change an option's status from optional to required (or vice versa) for a specific pack. This should be possible by simply including the option in the appropriate list in the new configuration.

**Why this priority**: Enables flexible pack configuration management where organizers can adjust option requirements as sponsorship terms evolve, without needing to delete and re-add options.

**Independent Test**: Can be tested by submitting a configuration where a previously required option is now optional (or vice versa), and verifying the option's requirement status changes correctly.

**Acceptance Scenarios**:

1. **Given** a pack has option X as required, **When** an organizer submits a configuration with option X in the optional list instead, **Then** option X remains attached but is now marked as optional
2. **Given** a pack has option Y as optional, **When** an organizer submits a configuration with option Y in the required list instead, **Then** option Y remains attached but is now marked as required

---

### User Story 3 - Add New Options Without Manual Cleanup (Priority: P2)

An event organizer wants to completely replace the options in a pack configuration without having to first manually delete existing options. They should be able to submit the desired final state in one request.

**Why this priority**: Streamlines the workflow for organizers who want to reconfigure packs, reducing the number of API calls and potential for partial configuration states.

**Independent Test**: Can be tested by adding new options to a pack that already has options, and verifying all options reflect the submitted state without requiring prior deletion calls.

**Acceptance Scenarios**:

1. **Given** a pack has existing options A and B, **When** an organizer submits a configuration with only option C, **Then** the pack contains only option C (A and B are removed)
2. **Given** a pack has existing options, **When** an organizer submits a configuration with completely different options, **Then** the operation completes in a single request without requiring prior deletions

---

### Edge Cases

- What happens when the submitted configuration contains duplicate option IDs in required and optional lists? → Return 409 Conflict with error message identifying the duplicate option IDs
- What happens when the submitted configuration contains option IDs that don't exist in the event? → Return 403 Forbidden with error message indicating which option IDs do not belong to the event
- What happens when the submitted configuration contains option IDs that belong to a different event? → Return 403 Forbidden with error message indicating which option IDs do not belong to the event
- What happens when attempting to sync options for a pack that doesn't exist? → Return 404 Not Found with error message indicating the pack was not found
- What happens when attempting to sync options for a pack that belongs to a different event? → Return 404 Not Found with error message indicating the pack was not found for this event
- What happens when the submitted configuration contains non-existent option IDs? → Return 404 Not Found with error message identifying which option IDs do not exist
- What happens when the submitted configuration has no changes (identical to current state)? → Process normally and return success (idempotent operation)
- What happens when submitting an empty configuration (both required and optional lists are empty)? → Remove all existing options from the pack and return success
- What happens when a database error occurs during synchronization? → Automatic rollback of all changes via database transaction, return 500 Internal Server Error

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept a complete list of required and optional option IDs for a pack (no limit on number of options)
- **FR-002**: System MUST remove any existing pack options that are not included in the submitted lists
- **FR-003**: System MUST add any options from the submitted lists that are not currently attached to the pack
- **FR-004**: System MUST update the requirement status (required vs optional) for options that remain attached but change status
- **FR-005**: System MUST validate that submitted option IDs belong to the same event as the pack
- **FR-006**: System MUST reject configurations where the same option ID appears in both required and optional lists (return 409 Conflict)
- **FR-007**: System MUST reject configurations containing option IDs that don't exist in the system (return 404 Not Found)
- **FR-008**: System MUST validate that the pack exists and belongs to the specified event before processing (return 404 Not Found if pack not found)
- **FR-009**: System MUST perform the entire synchronization operation atomically using database transactions (all changes succeed or all fail, with automatic rollback on any failure)
- **FR-010**: System MUST maintain existing authorization requirements (organization membership and permissions)
- **FR-011**: System MUST handle concurrent modifications using last-write-wins strategy (most recent request overwrites previous changes)

### Key Entities

- **Sponsoring Pack**: Represents a sponsorship tier/package for an event, contains a collection of options
- **Sponsoring Option**: A benefit or service included in sponsorship (e.g., logo placement, speaking slot), can be required or optional within a pack
- **Pack-Option Attachment**: The relationship between a pack and an option, including whether the option is required or optional
- **Event**: The context that owns both packs and options, ensuring options can only be attached to packs within the same event

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Organizers can update a pack's complete option configuration in a single API request
- **SC-002**: Pack configurations reflect exactly the submitted state with 100% accuracy (no orphaned options remain)
- **SC-003**: Synchronization operation completes within 500ms for packs with up to 50 options (performance scales linearly for larger configurations)
- **SC-004**: All validation errors provide clear feedback about which option IDs caused the failure
- **SC-005**: Zero partial configuration states occur - operations are fully atomic (all changes applied or none)
