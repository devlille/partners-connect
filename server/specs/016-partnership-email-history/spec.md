# Feature Specification: Partnership Email History

**Feature Branch**: `016-partnership-email-history`  
**Created**: January 10, 2026  
**Status**: Draft  
**Input**: User description: "I would like to save history of all notifications sent by organisers to a partnership with partnership emails used, subject and body. I should provide an endpoint for a partnership, protected for organisers only, to list these emails."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Email History for a Partnership (Priority: P1)

As an event organiser, I need to view the complete history of all emails that have been sent to a specific partnership, including the email addresses used, subject lines, and message content, so I can track communication history and avoid duplicate or conflicting messages.

**Why this priority**: This is the core value proposition of the feature - providing visibility into past communications. Without this, organisers have no way to reference what was previously communicated to partners.

**Independent Test**: Can be fully tested by sending test emails to a partnership, then retrieving the history via the API endpoint and verifying all sent emails are listed with correct details.

**Acceptance Scenarios**:

1. **Given** an organiser is authenticated and has permissions for an event, **When** they request email history for a partnership they manage, **Then** they receive a chronologically ordered list of all emails sent to that partnership
2. **Given** multiple emails have been sent to a partnership, **When** the organiser views the history, **Then** each email record includes sender email address, recipient email addresses, subject, body content, and timestamp
3. **Given** no emails have been sent to a partnership, **When** the organiser requests the history, **Then** they receive an empty list with a success response
4. **Given** an organiser without permissions for an event, **When** they attempt to access email history for a partnership in that event, **Then** they receive an authorization error
5. **Given** a partnership has more than 20 email records, **When** the organiser requests the history without specifying page size, **Then** they receive the first 20 records with pagination metadata (total count, page info)

---

### User Story 2 - Automatic Email History Logging (Priority: P1)

As a system administrator, I need all emails sent through the partnership notification system to be automatically logged with full details, so that the email history is complete and accurate without requiring manual intervention.

**Why this priority**: This is foundational - without automatic logging, the history feature cannot function. This must work transparently whenever emails are sent.

**Independent Test**: Can be tested by triggering email notifications through existing partnership workflows, then verifying that the email history endpoint returns the newly sent emails with all required fields populated.

**Acceptance Scenarios**:

1. **Given** an organiser sends an email to a partnership through the system, **When** the email is successfully sent, **Then** a history record is automatically created with the email details
2. **Given** an email fails to send, **When** the sending process completes, **Then** the email history record reflects the failure status
3. **Given** multiple recipients are included in an email, **When** the email is sent, **Then** all recipient addresses are captured in the history record

---

### Edge Cases

- What happens when an email has a very large body content (e.g., over 10,000 characters)?
- How does the system handle emails sent to partnerships that are later deleted?
- What happens when requesting history for a partnership ID that doesn't exist?
- How should the overall status be determined when the email service returns partial success (some recipients succeeded, others failed)? (Answered: mark as "partial" with per-recipient status details)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST automatically capture and store a history record for every email sent to a partnership, including timestamp, sender email, recipient email(s), subject, and body content
- **FR-002**: System MUST provide an API endpoint to retrieve email history for a specific partnership, accessible only to authenticated organisers with permissions for that partnership's event
- **FR-003**: System MUST return email history records in reverse chronological order (newest first)
- **FR-004**: System MUST include the delivery status of each email (sent, failed, or partial) and track per-recipient delivery status for emails with multiple recipients
- **FR-005**: System MUST preserve email history records indefinitely with no retention time limit, surviving partnership deletions and system restarts
- **FR-006**: System MUST handle multiple recipient email addresses in a single email notification and track individual delivery status for each recipient
- **FR-007**: System MUST store email body content as-is (whether HTML or plain text) with no size limit
- **FR-008**: Organisers MUST NOT be able to access email history for partnerships in events they don't have permissions for
- **FR-009**: System MUST include metadata about who triggered the email (organiser user ID or system)
- **FR-010**: Email history records MUST be immutable once created (no updates or deletions allowed)
- **FR-011**: System does NOT support email attachments; only email body text is captured in history
- **FR-012**: System MUST support pagination for email history retrieval using existing PaginatedResponse model with a default page size of 20 records and allow clients to configure page size

### Key Entities

- **Partnership Email History**: Represents a single email sent to a partnership, containing:
  - Unique identifier for the history record
  - Partnership identifier (reference to the partnership)
  - Timestamp when the email was sent
  - Sender email address (the "from" address used)
  - Recipient delivery details (array of recipient email address + individual delivery status)
  - Email subject line
  - Email body content (stored as-is, whether HTML or plain text)
  - Overall delivery status (sent if all succeeded, failed if all failed, partial if mixed)
  - Triggered by (user ID of the organiser who initiated the email, or "system" for automated emails)

- **Partnership**: The existing entity representing a partnership between a company and an event (referenced by Partnership Email History)

## Clarifications

### Session 2026-01-10

- Q: Email body format handling - should the system store plain text, HTML, or both formats? → A: Store plain text no matter if the content contains html or not
- Q: Email attachment handling - should attachments be stored in history? → A: Our system doesn't support attachment so don't store attachment in the history also.
- Q: Pagination for email history listing - return all records or support pagination? → A: Support pagination (default 20 records per page using existing PaginatedResponse model, configurable)
- Q: Retention policy for email history - any time limit on storage? → A: No retention limit (indefinite storage)
- Q: Partial email delivery handling - how to record mixed success/failure states? → A: Record per-recipient delivery status (array of recipient+status pairs)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Organisers can retrieve complete email history for any partnership within 2 seconds
- **SC-002**: 100% of emails sent through the partnership notification system are automatically logged in the history
- **SC-003**: Email history data persists indefinitely, surviving partnership deletions and system restarts
- **SC-004**: Unauthorized access attempts to email history endpoints result in proper authentication/authorization errors
- **SC-005**: The system accurately captures emails with up to 10 recipients without data loss
- **SC-006**: Organisers report increased confidence in partnership communication (qualitative feedback)
