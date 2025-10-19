# Feature Specification: Job Offer Promotion with Approval Workflow

**Feature Branch**: `004-i-would-like`  
**Created**: October 18, 2025  
**Status**: Draft  
**Input**: User description: "I would like to create services that will let a user promote a job offer attached to a company with a partnership with an event and services in the organization (so that will require permission to an event) to accept or decline job offer submitted by the user. Note that these new services should use the notification integration to notify partnership contacts when a new offer has been created and when it has been accepted or declined (mailjet) and organization (slack)."

## Execution Flow (main)
```
1. Parse user description from Input
   → Extracted: actors (company users, event organizers), actions (promote, accept, decline), data (job offers, partnerships, notifications)
2. Extract key concepts from description
   → Job offer promotion workflow, approval/rejection process, notification integration
3. For each unclear aspect:
   → [NEEDS CLARIFICATION: What happens to job offers after event ends?]
   → [NEEDS CLARIFICATION: Can a declined job offer be re-submitted?]
   → [NEEDS CLARIFICATION: Is there a limit on job offers per partnership?]
   → [NEEDS CLARIFICATION: What permission level is required for approval (owner only, or any event admin)?]
4. Fill User Scenarios & Testing section
   → Primary flow: company promotes → organizer reviews → notification sent
5. Generate Functional Requirements
   → Promotion, approval, notification requirements identified
6. Identify Key Entities
   → PromotedJobOffer, JobOfferPromotionRequest, NotificationVariables
7. Run Review Checklist
   → WARN "Spec has uncertainties" - 4 clarification markers present
8. Return: SUCCESS (spec ready for planning with clarifications needed)
```

---

## ⚡ Quick Guidelines
- ✅ Focus on WHAT users need and WHY
- ❌ Avoid HOW to implement (no tech stack, APIs, code structure)
- 👥 Written for business stakeholders, not developers

---

## Clarifications

### Session 2025-10-18
- Q: What happens to job offers after event ends? → A: System returns 403 Forbidden when attempting to promote after event end date
- Q: When a declined job offer needs to be re-submitted to the same event, what should the system allow? → A: Allow immediate re-promotion without changes (declined status reset to pending)
- Q: What permission level is required for a user to approve or decline job offer promotions? → A: Any user with event edit permissions (canEdit=true)
- Q: When a job offer with active promotions is deleted, what should happen to those promotions? → A: Cascade delete all promotions (removed from system entirely)
- Q: When a partnership is terminated while job offer promotions are pending approval, what should happen? → A: Keep promotions in pending state (orphaned, waiting indefinitely)
- Q: Is there a maximum number of job offers that can be promoted per partnership? → A: No limit (unrestricted promotions)

---

## User Scenarios & Testing *(mandatory)*

### Primary User Story
As a company owner with an active partnership, I want to promote my job offers to an event so that event participants can discover employment opportunities. As an event organizer, I need to review and approve or decline promoted job offers to ensure quality and relevance to my event audience. When job offer statuses change, both the company contacts and organization team should receive automated notifications.

### Acceptance Scenarios

1. **Given** I am a company owner with an active partnership and existing job offers, **When** I promote a job offer to the partnership, **Then** the job offer is marked as "pending approval" and event organizers receive notifications via email (Mailjet) and Slack

2. **Given** I am an event organizer with permissions, **When** I review a pending job offer promotion and approve it, **Then** the job offer status changes to "approved", the company contacts receive approval notification via email, and the organization team receives Slack notification

3. **Given** I am an event organizer with permissions, **When** I review a pending job offer promotion and decline it, **Then** the job offer status changes to "declined", the company contacts receive decline notification via email with reason, and the organization team receives Slack notification

4. **Given** I am a company owner, **When** I attempt to promote a job offer without an active partnership, **Then** the system prevents the promotion and returns a permission error

5. **Given** I am a user without event permissions, **When** I attempt to approve or decline a job offer promotion, **Then** the system denies access and returns an authorization error

6. **Given** a job offer is promoted to multiple events through different partnerships, **When** one event approves and another declines, **Then** each promotion maintains its independent status and notifications are sent to respective parties

### Edge Cases

- What happens when a job offer is promoted but then deleted before approval/decline? System cascades deletion to all associated promotions, removing them entirely from the system.
- How does system handle job offers after the event date has passed? System prevents new promotions and returns 403 Forbidden error when attempting to promote after event end date.
- What happens if a partnership is terminated while job offers are pending approval? Promotions remain in pending state without automatic decline or deletion, allowing manual review if partnership is restored.
- Can a company re-promote a declined job offer to the same event? Yes, system allows immediate re-promotion which resets status from declined to pending.
- What if notification services (Mailjet/Slack) are unavailable during status changes? Should the status change be rolled back or proceed with failed notification logging?
- How are notifications handled when partnership has no contact emails defined?
- Is there a maximum number of job offers that can be promoted per partnership? No, system allows unlimited job offer promotions per partnership without restrictions.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Promotion Requirements
- **FR-001**: System MUST allow company owners to promote existing job offers to their active event partnerships
- **FR-002**: System MUST verify that a valid active partnership exists between the company and event before allowing promotion
- **FR-003**: System MUST prevent promotion of job offers that are already promoted to the same partnership
- **FR-004**: System MUST initialize promoted job offers with "pending" status upon creation
- **FR-005**: System MUST link promoted job offers to specific partnerships and events
- **FR-030**: System MUST reject job offer promotion attempts after event end date with 403 Forbidden status code
- **FR-031**: System MUST allow re-promotion of declined job offers, resetting their status from declined to pending

#### Approval/Decline Requirements
- **FR-006**: System MUST allow users with event permissions to approve pending job offer promotions
- **FR-007**: System MUST allow users with event permissions to decline pending job offer promotions
- **FR-008**: System MUST validate user has event organization edit permissions (canEdit=true) before allowing approval/decline actions
- **FR-037**: System MUST use AuthorizedOrganisationPlugin on approval/decline routes to automatically check JWT token and verify user permissions in database
- **FR-009**: System MUST update job offer promotion status to "approved" when approved by authorized user
- **FR-010**: System MUST update job offer promotion status to "declined" when declined by authorized user
- **FR-011**: System MUST record timestamp of approval/decline actions
- **FR-012**: System MUST record which user performed the approval/decline action

#### Notification Requirements
- **FR-013**: System MUST send email notification (via Mailjet) to all partnership contact emails when a job offer is promoted
- **FR-014**: System MUST send Slack notification to organization channel when a job offer is promoted
- **FR-015**: System MUST send email notification (via Mailjet) to all partnership contact emails when a job offer promotion is approved
- **FR-016**: System MUST send Slack notification to organization channel when a job offer promotion is approved
- **FR-017**: System MUST send email notification (via Mailjet) to all partnership contact emails when a job offer promotion is declined
- **FR-018**: System MUST send Slack notification to organization channel when a job offer promotion is declined
- **FR-019**: System MUST include relevant details in notifications: event name, company name, job offer title, partnership link
- **FR-020**: System MUST support multiple languages for email notifications based on partnership language preference
- **FR-021**: System MUST log notification failures without blocking the approval/decline action

#### Data Integrity Requirements
- **FR-022**: System MUST maintain referential integrity between promoted job offers and their source job offers
- **FR-023**: System MUST maintain referential integrity between promoted job offers and partnerships
- **FR-024**: System MUST cascade delete all promotions when source job offer is deleted
- **FR-025**: System MUST persist promotion history including all status changes
- **FR-032**: System MUST preserve pending promotions when partnership is terminated without automatic status changes

#### Database Implementation Requirements
- **FR-033**: System MUST implement database schema using Exposed ORM with both Table object (UUIDTable) and Entity class (UUIDEntity)
- **FR-034**: System MUST create CompanyJobOfferPromotionsTable as Exposed table object extending UUIDTable
- **FR-035**: System MUST create CompanyJobOfferPromotionEntity as Exposed entity class extending UUIDEntity with companion object
- **FR-036**: System MUST use datetime() column type for all timestamp fields (promoted_at, reviewed_at, created_at, updated_at) mapping to LocalDateTime in Kotlin

#### Query Requirements
- **FR-026**: System MUST allow retrieval of all promoted job offers for a specific event
- **FR-027**: System MUST allow filtering promoted job offers by status (pending, approved, declined)
- **FR-028**: System MUST allow retrieval of all promotions for a specific company's job offer
- **FR-029**: System MUST allow retrieval of pending promotions requiring organizer action

### Key Entities *(include if feature involves data)*

- **Promoted Job Offer**: Represents a job offer that has been submitted for promotion to an event through a partnership. Contains reference to original job offer, partnership, event, current status (pending/approved/declined), submission timestamp, approval/decline timestamp, approving/declining user reference, and status change history.

- **Job Offer Promotion Request**: Request initiated by company owner to promote a job offer. Contains job offer ID, partnership ID, event ID, requesting user, and request timestamp.

- **Promotion Status**: Enumeration representing lifecycle states: "pending" (awaiting organizer review), "approved" (accepted by organizer), "declined" (rejected by organizer).

- **Notification Variables for Promotion**: Data structure containing information for notification templates: event name, company name, job offer title, job offer URL, partnership link, event contact email, language preference, action type (promoted/approved/declined).

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
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed (all clarifications resolved)

---
