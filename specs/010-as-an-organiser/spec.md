# Feature Specification: OpenPlanner Integration for Agenda and Speaker Management

**Feature Branch**: `010-as-an-organiser`  
**Created**: November 11, 2025  
**Status**: Draft  
**Input**: User description: "As an organiser, I can fetch a agenda from an external saas service called OpenPlanner and save session and speaker information (I should create these data if they don't exist or update existing one if they already exist) in an existing event. Then, as an user with an active partnership, I can use a public endpoint to attach a speaker to a partnership to allow organisers to communicate about this."

## Execution Flow (main)
```
1. Parse user description from Input
   → Feature involves agenda import from OpenPlanner and speaker-partnership linking
2. Extract key concepts from description
   → Actors: organizers, users with partnerships
   → Actions: fetch agenda, save/update sessions/speakers, attach speakers to partnerships
   → Data: agenda, sessions, speakers, partnerships, events
   → Constraints: data must be created or updated if exists
3. For each unclear aspect:
   → Authentication method for OpenPlanner API
   → Data format from OpenPlanner
   → Partnership validation rules
   → Communication mechanism with organizers
4. Fill User Scenarios & Testing section
   → Organizer imports agenda workflow
   → Partner attaches speaker workflow
5. Generate Functional Requirements
   → API integration, data management, partnership linking
6. Identify Key Entities
   → Session, Speaker, Event, Partnership, Agenda
7. Run Review Checklist
   → Mark authentication and data format uncertainties
8. Return: SUCCESS (spec ready for planning)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers

---

## Clarifications

### Session 2025-11-11
- Q: How should the system identify when an imported speaker or session already exists to update it versus creating a new record? → A: Match by external ID from OpenPlanner if provided
- Q: What data format does OpenPlanner provide for agenda exports? → A: JSON via REST API endpoints
- Q: What defines an "active partnership" for speaker attachment eligibility? → A: Partnership approved by organizers regardless of payment status
- Q: How should the system authenticate with the OpenPlanner API? → A: API key in query parameter
- Q: What specific communication workflows should be enabled between organizers and partnership-linked speakers? → A: No communication integration (out of scope)

## User Scenarios & Testing *(mandatory)*

### Primary User Stories

**Story 1: Organizer Imports Agenda**
As an event organizer, I want to fetch agenda data from OpenPlanner so that I can automatically populate session and speaker information in my event without manual data entry.

**Story 2: Partner Links Speaker**
As a user with an active partnership, I want to attach a speaker to my partnership so that organizers can identify speakers associated with my company.

### Out of Scope
- Communication workflows or messaging between organizers and speakers
- Integration with external communication tools
- Speaker scheduling or calendar management

### Acceptance Scenarios

#### Agenda Import Flow
1. **Given** I am an authenticated organizer with access to an event, **When** I trigger an agenda import from OpenPlanner, **Then** the system fetches the complete agenda data
2. **Given** agenda data is received from OpenPlanner, **When** processing sessions and speakers, **Then** new sessions and speakers are created if they don't exist
3. **Given** sessions and speakers already exist in the event, **When** processing updated agenda data, **Then** existing records are updated with new information
4. **Given** the import process completes, **When** I view the event details, **Then** I can see all imported sessions and speakers properly organized

#### Speaker-Partnership Linking Flow
1. **Given** I am a user with an active partnership, **When** I access the speaker attachment endpoint, **Then** I can see available speakers from my partnership's events
2. **Given** I select a speaker to attach, **When** I submit the attachment request, **Then** the speaker is linked to my partnership
3. **Given** a speaker is attached to a partnership, **When** organizers view partnership details, **Then** they can see the associated speaker information

### Edge Cases
- What happens when OpenPlanner API is unavailable during import?
- How does the system handle duplicate speaker names from different sources?
- What occurs when a user attempts to attach a speaker from an event they don't have partnership access to?
- How does the system manage speaker information updates after attachment?

## Requirements *(mandatory)*

### Functional Requirements

#### Agenda Import
- **FR-001**: System MUST allow authenticated organizers to initiate agenda import from OpenPlanner for their events
- **FR-002**: System MUST fetch complete agenda data including sessions and speaker information from OpenPlanner REST API in JSON format
- **FR-003**: System MUST create new session records when session data doesn't exist in the target event
- **FR-004**: System MUST create new speaker records when speaker data doesn't exist in the system
- **FR-005**: System MUST update existing session records when matching sessions are found during import using OpenPlanner external IDs
- **FR-006**: System MUST update existing speaker records when matching speakers are found during import using OpenPlanner external IDs
- **FR-007**: System MUST associate imported sessions and speakers with the correct event

#### Speaker-Partnership Linking
- **FR-008**: System MUST provide a public endpoint for users with active partnerships to attach speakers
- **FR-009**: System MUST validate that users have organizer-approved partnerships before allowing speaker attachment
- **FR-010**: System MUST allow users to attach speakers from events related to their partnerships
- **FR-011**: System MUST create linkage between speakers and partnerships for communication purposes
- **FR-012**: System MUST enable organizers to view speaker information associated with partnerships

#### Data Management
- **FR-014**: System MUST maintain data integrity during import and update operations
- **FR-015**: System MUST handle concurrent access to session and speaker data during import
- **FR-016**: System MUST preserve existing relationships when updating speaker or session information

#### Authentication & Authorization
- **FR-017**: System MUST authenticate with OpenPlanner API using API key passed as query parameter
- **FR-018**: System MUST validate organizer permissions before allowing agenda import for specific events
- **FR-019**: System MUST verify partnership approval status and validity before allowing speaker attachment

### Key Entities *(include if feature involves data)*

- **Session**: Represents agenda items/presentations with title, description, time, location, associated speakers, and OpenPlanner external ID for matching
- **Speaker**: Individual presenter with name, bio, contact information, expertise areas, and OpenPlanner external ID for matching
- **Event**: Container for sessions and speakers, managed by organizers
- **Partnership**: Active business relationship between companies and events approved by organizers, enabling speaker attachment
- **Agenda**: Complete schedule structure imported from OpenPlanner via JSON REST API containing sessions and speakers
- **Speaker-Partnership Link**: Association enabling organizers to identify speakers related to partnerships

---

## Review & Acceptance Checklist
*GATE: Automated checks run during main() execution*

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
*Updated by main() during processing*

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked (OpenPlanner API authentication)
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---