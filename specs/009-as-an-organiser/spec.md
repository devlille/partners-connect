# Feature Specification: Provider Management Enhancement

**Feature Branch**: `009-as-an-organiser`  
**Created**: 8 November 2025  
**Status**: Draft  
**Input**: User description: "As an organiser, I can use create, update or delete a provider and I can attach and detach an existing provider to an event and as a user, I can list providers for an organisation to get provider informations. For now, when you are creating a new provider, the provider isn't attached to an organisation but we should update this to reference an organisation to the provider table and then, when an organisation want to attach an existing provider to an event, the provider should exist in its organisation. When an organiser want to delete an existing provider, we should detach the provider from all existing event and when it is done, then we can delete it from the database. For the public list endpoint, we should keep existing query parameters but add the org slug query parameter to be able to get providers of an organisation. That will allow organisers to share professional contact to companies interested to use our providers for their booth or services."

## Execution Flow (main)
```
1. Parse user description from Input
   → Provider management enhancement with organisation relationships
2. Extract key concepts from description
   → Actors: organisers, users; Actions: CRUD operations, attach/detach; Data: providers, organisations, events
3. For each unclear aspect:
   → [NEEDS CLARIFICATION: What types of provider information should be stored?]
   → [NEEDS CLARIFICATION: What specific query parameters should be preserved in public list endpoint?]
4. Fill User Scenarios & Testing section
   → Clear user flows identified for CRUD and attachment operations
5. Generate Functional Requirements
   → Each requirement is testable and measurable
6. Identify Key Entities
   → Provider, Organisation, Event relationships
7. Run Review Checklist
   → WARN "Spec has uncertainties about provider data schema and existing query parameters"
8. Return: SUCCESS (spec ready for planning with noted clarifications)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers

---

## Clarifications

### Session 2025-11-08
- Q: What specific information should be stored for each provider? → A: Use existing table structure (name, type, website, phone, email, createdAt)
- Q: What are the current query parameters for the provider list endpoint that need to be preserved? → A: query, sort, direction, page, page_size
- Q: How should the system handle permission validation when organisers try to manage providers? → A: Check user's organisation membership only
- Q: What should happen when a provider is attached to an event that gets deleted? → A: Events cannot be deleted
- Q: How should provider type validation work when creating/updating providers? → A: Free text - any string allowed for type field

---

## User Scenarios & Testing *(mandatory)*

### Primary User Story
As an organiser, I need to manage providers within my organisation and attach them to events so that companies participating in events can access relevant professional services. As a user, I need to view providers for an organisation to find suitable services for my event participation.

### Acceptance Scenarios
1. **Given** an organiser is logged in to their organisation, **When** they create a new provider, **Then** the provider is automatically linked to their organisation
2. **Given** an organiser has providers in their organisation, **When** they attach a provider to an event, **Then** the provider becomes available for that event
3. **Given** a provider is attached to multiple events, **When** an organiser deletes the provider, **Then** the provider is first detached from all events before being deleted
4. **Given** a user visits the public provider list, **When** they filter by organisation slug, **Then** they see only providers belonging to that organisation
5. **Given** an organiser wants to update provider information, **When** they modify provider details, **Then** the changes are reflected across all attached events

### Edge Cases
- What happens when trying to attach a provider from a different organisation to an event?
- How does the system handle deletion of a provider that is actively being used by an event?
- What occurs when filtering by a non-existent organisation slug?
- How does the system respond when an organiser tries to delete a provider they don't have permission to manage?

## Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: System MUST link every new provider to the creating organiser's organisation automatically
- **FR-002**: System MUST allow organisers to create, update, and delete providers within their own organisation only
- **FR-003**: System MUST allow organisers to attach existing providers from their organisation to events they manage
- **FR-004**: System MUST allow organisers to detach providers from events they manage
- **FR-005**: System MUST detach a provider from all events before allowing deletion
- **FR-006**: System MUST prevent deletion of providers until all event attachments are removed
- **FR-007**: System MUST allow public listing of providers filtered by organisation slug
- **FR-008**: System MUST preserve existing query parameters (query, sort, direction, page, page_size) in the public provider list
- **FR-009**: System MUST prevent organisers from attaching providers that don't belong to their organisation to events
- **FR-010**: System MUST allow users to view provider name, type, website, phone, and email for any organisation
- **FR-011**: System MUST provide organisers with the ability to share provider contact information with companies for event services
- **FR-012**: System MUST validate organiser permissions by checking user's organisation membership before allowing provider management operations
- **FR-013**: System MUST accept any string value for provider type field without validation constraints

### Key Entities *(include if feature involves data)*
- **Provider**: Service provider with name, type (service category), optional website, phone, email, and creation timestamp, now linked to a specific organisation
- **Organisation**: The entity that owns and manages providers, establishes the boundary for provider management permissions
- **Event**: Can have multiple providers attached, providers can be attached/detached as needed
- **Provider-Event Relationship**: Represents the attachment between a provider and an event, must be removed before provider deletion

---

## Review & Acceptance Checklist
*GATE: Automated checks run during main() execution*

### Content Quality
- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain (all clarifications resolved)
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
- [x] Review checklist passed (all clarifications completed)
