# Feature Specification: Complete CRUD Operations for Companies

**Feature Branch**: `006-as-a-partner`  
**Created**: 30 October 2025  
**Status**: Draft  
**Input**: User description: "As a partner, I should be able to update company information with a public endpoint to complete CRUD operations on the /companies resource."

## Execution Flow (main)
```
1. Parse user description from Input
   → Feature clearly requests missing company update functionality
2. Extract key concepts from description
   → Actors: partners/companies, Actions: update company info, Data: company information, Constraints: public endpoint
3. For each unclear aspect:
   → [NEEDS CLARIFICATION: What constitutes "partner" - is this any authenticated user or specific role?]
   → [NEEDS CLARIFICATION: Should company deletion be included to truly "complete" CRUD operations?]
4. Fill User Scenarios & Testing section
   → Clear user flow for company information updates
5. Generate Functional Requirements
   → Each requirement is testable and specific
6. Identify Key Entities
   → Company entity with update capabilities
7. Run Review Checklist
   → Spec ready for planning
8. Return: SUCCESS (spec ready for planning)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers

---

## Clarifications

### Session 2025-10-30
- Q: What type of user can update company information? → A: Public endpoint, no authentication required
- Q: Should Delete functionality also be included for complete CRUD? → A: Add DELETE but make it a soft delete (mark inactive)
- Q: Should soft-deleted companies still appear in search and listing results? → A: Show but clearly marked as inactive/deleted with status filter options
- Q: What should be the default behavior when no status filter is applied to company listings? → A: Show all companies (both active and inactive)

---

## User Scenarios & Testing

### Primary User Story
As a partner company, I need to update or delete my company information after initial registration to maintain complete control over my profile. This includes updating business details like address, contact information, VAT number, and company description as my business evolves, or removing my company profile entirely when no longer needed.

### Acceptance Scenarios
1. **Given** I am a public user with knowledge of an existing company ID, **When** I submit updated company information via the API, **Then** the company profile is updated with the new information and I receive confirmation
2. **Given** I provide partial company information updates, **When** I submit the update request, **Then** only the specified fields are updated while other fields remain unchanged
3. **Given** I attempt to update a company that doesn't exist, **When** I submit the update request, **Then** I receive a "not found" error response
4. **Given** I search for companies with a status filter, **When** I specify "active", "inactive", or no filter, **Then** I receive appropriately filtered results with status clearly indicated
5. **Given** I provide invalid data format (e.g., malformed email, invalid country code), **When** I submit the update, **Then** I receive validation error messages specifying what needs to be corrected
6. **Given** I am a public user with knowledge of an existing company ID, **When** I submit a delete request for that company, **Then** the company is marked as inactive but data is preserved for historical integrity

### Edge Cases
- What happens when multiple users attempt to update the same company simultaneously?
- How does system handle updates to companies that are referenced in active partnerships?
- How should the system handle concurrent updates to the same company status (active/inactive transitions)?
- What validation occurs for business registration numbers (SIRET, VAT) during updates?
- Should there be audit logging of company information changes?

## Requirements

### Functional Requirements
- **FR-001**: System MUST provide a public REST endpoint to update existing company information
- **FR-002**: System MUST support partial updates where only specified fields are modified
- **FR-003**: System MUST validate all company data according to business rules (e.g., SIRET format, VAT format, country codes)
- **FR-004**: System MUST return updated company information upon successful update
- **FR-005**: System MUST return appropriate error responses for invalid data or non-existent companies
- **FR-006**: System MUST preserve company relationships (partnerships, job offers) when updating company information
- **FR-007**: System MUST provide public access to update company information without authentication requirements, consistent with existing /companies endpoints
- **FR-008**: System MUST handle concurrent updates gracefully to prevent data corruption
- **FR-009**: System MUST provide a DELETE endpoint that performs soft deletion by marking companies as inactive rather than permanently removing records
- **FR-010**: System MUST preserve all company relationships (partnerships, job offers) when soft deleting companies to maintain data integrity
- **FR-011**: System MUST provide status filtering in company listings (active, inactive, or all) and clearly mark inactive companies in results
- **FR-012**: System MUST show all companies (active and inactive) by default when no status filter is specified

### Key Entities
- **Company**: Core business entity containing name, address, business identifiers (SIRET, VAT), website, description, media assets, and status (active/inactive); supports partial updates, soft deletion, and status filtering while maintaining data integrity and business relationships

---

## Review & Acceptance Checklist

### Content Quality
- [ ] No implementation details (languages, frameworks, APIs)
- [ ] Focused on user value and business needs
- [ ] Written for non-technical stakeholders
- [ ] All mandatory sections completed

### Requirement Completeness
- [ ] No [NEEDS CLARIFICATION] markers remain
- [ ] Requirements are testable and unambiguous  
- [ ] Success criteria are measurable
- [ ] Scope is clearly bounded
- [ ] Dependencies and assumptions identified

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [ ] Review checklist passed

---
