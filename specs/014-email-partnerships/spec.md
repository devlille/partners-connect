# Feature Specification: Email Partnership Contacts via Mailing Integration

**Feature Branch**: `014-email-partnerships`  
**Created**: December 19, 2025  
**Status**: Draft  
**Input**: User description: "As an organiser, I want to use the mailing integration to send an email to contact a selected list of partnerships. We should use same filters than the existing GET endpoint to list partnership and get email specified on these partnership. In this new endpoint, the organiser give filters to get partnership contacts, the header of the email and the body."

## Clarifications

### Session 2025-12-19

- Q: How should the system sanitize HTML content in email body to prevent XSS attacks while preserving organizers' formatting needs? → A: No sanitization - trust organizers to provide safe HTML (validation only checks structure)
- Q: How should the system handle scenarios where Mailjet accepts the send request but some individual recipients fail (invalid email format, mailbox full, etc.)? → A: Return success if Mailjet accepts the request; let Mailjet handle per-recipient failures (industry standard)
- Q: User Story 3 describes email preview functionality - should a preview endpoint be implemented in v1, or is this out of scope? → A: Exclude preview endpoint from v1 scope; organizers can use existing GET partnerships endpoint to check filter counts
- Q: Should the system maintain an audit trail of email send operations, and if so, what level of detail should be logged? → A: No audit logging - treat email sends as ephemeral actions (no record kept)
- Q: How should the system handle scenarios where the organization has exhausted their Mailjet sending quota or hit rate limits? → A: Return specific error when organization hits Mailjet quota; organizers must wait or upgrade plan
- Q: How should the system determine the sender (From) email address for bulk emails? → A: If partnership has an assigned organizer, use organizer's email as From with event contact email in CC; if no organizer assigned, use event contact email as From (no CC)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Send Bulk Email to Filtered Partnerships (Priority: P1)

Event organizers need to communicate important information to specific groups of partners (e.g., all validated partnerships, all paid partnerships, specific sponsorship pack holders). Instead of manually copying emails and using external tools, organizers should be able to compose and send emails directly through the platform using the same filtering capabilities available in the partnership listing view.

**Why this priority**: This is the core functionality that delivers immediate value - enabling organizers to efficiently communicate with partner segments without leaving the platform. This is the MVP that makes the feature useful.

**Independent Test**: Can be fully tested by authenticating as an organizer, applying partnership filters (e.g., `filter[validated]=true`), providing email subject and body, and verifying that all matching partnerships receive the email via their registered contact emails. Delivers standalone value for basic bulk communication needs.

**Acceptance Scenarios**:

1. **Given** an organizer is authenticated and has edit permissions for an event, **When** they send an email to partnerships filtered by `filter[validated]=true` with subject "Partnership Update" and HTML body content, **Then** the system sends the email to all contact emails registered for validated partnerships and returns success confirmation with count of recipients
2. **Given** an organizer sends an email to partnerships with multiple filters (`filter[paid]=true&filter[agreement-signed]=true`), **When** the request is processed, **Then** only partnerships matching ALL filter criteria receive the email
3. **Given** a partnership has multiple contact emails registered, **When** an email is sent to that partnership, **Then** all registered contact emails for that partnership receive the email
4. **Given** an organizer sends an email to partnerships but no partnerships match the filters, **When** the request is processed, **Then** the system returns an error indicating no recipients were found
5. **Given** the Mailjet integration is not configured for the organization, **When** an organizer attempts to send an email, **Then** the system returns an error indicating the mailing integration is not available

---

### User Story 2 - Email Delivery Status and Error Handling (Priority: P2)

When sending bulk emails, organizers need clear feedback about delivery success or failures. The system should handle scenarios where the mailing service is unavailable, API credentials are invalid, or some recipients fail while others succeed.

**Why this priority**: Essential for production reliability but not required for initial testing. Organizers need to know if their communication was successful, but basic "send and hope" functionality is sufficient for P1.

**Independent Test**: Can be tested independently by simulating Mailjet API failures, invalid credentials, or network errors, and verifying appropriate error messages are returned to the organizer. Delivers value by improving user confidence and troubleshooting capabilities.

**Acceptance Scenarios**:

1. **Given** the Mailjet API returns an error during email sending, **When** the organizer sends an email, **Then** the system returns a clear error message indicating the mailing service failed
2. **Given** the Mailjet API credentials are invalid or expired, **When** the organizer sends an email, **Then** the system returns an authentication error before attempting to send
3. **Given** an email send request succeeds and Mailjet accepts the batch, **When** the response is returned, **Then** the system returns success with recipient count (individual delivery failures are handled by Mailjet)
4. **Given** Mailjet accepts the request but some recipients later fail delivery, **When** organizers check status, **Then** they must use Mailjet's dashboard for per-recipient delivery tracking (outside this system)

---

### Edge Cases

- What happens when a partnership has no contact emails registered? (Skip that partnership and continue sending to others)
- What happens when some recipient email addresses are invalid after Mailjet accepts the batch? (Mailjet handles per-recipient delivery failures - our system reports success once batch is accepted)
- What happens when organization exhausts their Mailjet sending quota? (Return specific error message indicating quota exceeded; organizers must wait for quota reset or upgrade their plan)
- What happens when partnership filters match thousands of partnerships? (Mailjet API has rate limits - batch sending may be required)
- How does the system handle malicious HTML content in email body? (Organizers are trusted users with edit permissions - no sanitization applied, only structure validation)
- What happens if Mailjet API is slow or times out? (Request should timeout gracefully with error message after 30 seconds)
- What happens when organizer has no edit permissions for the event? (Authorization plugin blocks request before processing)
- What happens when the same email address appears in multiple partnerships? (Send email only once per unique email address to avoid spam)
- What happens when filtered partnerships have different assigned organizers? (Send separate email batches grouped by organizer, each with appropriate From/CC addresses)
- What happens when some filtered partnerships have organizers and others don't? (Send separate batches: one from each organizer with event in CC, one from event for unassigned partnerships)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a new POST endpoint `/orgs/{orgSlug}/events/{eventSlug}/partnerships/email` that accepts partnership filters, email subject, and email body
- **FR-002**: System MUST reuse the exact same filter parameters as the existing GET `/orgs/{orgSlug}/events/{eventSlug}/partnerships` endpoint (`filter[pack_id]`, `filter[validated]`, `filter[suggestion]`, `filter[paid]`, `filter[agreement-generated]`, `filter[agreement-signed]`)
- **FR-003**: System MUST apply all provided filters using AND logic (partnerships must match all specified criteria)
- **FR-004**: System MUST retrieve all contact emails from the `company_emails` table for each matching partnership
- **FR-005**: System MUST deduplicate email addresses if the same email appears across multiple matching partnerships
- **FR-006**: System MUST validate that email subject is provided and not empty
- **FR-007**: System MUST validate that email body is provided and not empty
- **FR-008**: System MUST validate that email subject does not exceed 500 characters
- **FR-009**: System MUST send emails via `NotificationRepository.sendMail()` which uses the generic `NotificationGateway` interface (Mailjet implementation handles config lookup and sending)
- **FR-010**: System MUST determine the "From" email address based on partnership organizer assignment: if partnership has an assigned organizer, use the organizer's email address; if no organizer is assigned, use the event contact email
- **FR-010a**: System MUST add event contact email to "CC" field when partnership has an assigned organizer (organizer is From, event is CC)
- **FR-010b**: System MUST use event contact email as "From" with no "CC" when partnership has no assigned organizer
- **FR-011**: System MUST use the organizer's name (when assigned) or event name (when no organizer) as the "From" name in sent emails
- **FR-012**: System MUST prefix email subject with `[{event_name}]` for consistency with existing notification patterns
- **FR-013**: System MUST accept HTML content in the email body and send it as HTMLPart in Mailjet API
- **FR-014**: System MUST require authentication (valid JWT bearer token) for the endpoint
- **FR-015**: System MUST enforce authorization using `AuthorizedOrganisationPlugin` (user must have canEdit permission for the event)
- **FR-016**: System MUST return error if no Mailjet integration is configured for the organization
- **FR-017**: System MUST return error if Mailjet API credentials are invalid or expired
- **FR-018**: System MUST return error if no partnerships match the provided filters
- **FR-019**: System MUST return error if matching partnerships have zero contact emails registered
- **FR-020**: System MUST return success response including total count of unique email recipients
- **FR-021**: System MUST handle Mailjet API rate limits gracefully with appropriate error messages
- **FR-021a**: System MUST return specific error message when organization exhausts Mailjet sending quota, indicating organizers must wait for quota reset or upgrade their Mailjet plan (quota reset timestamp included only if available from Mailjet API response)
- **FR-022**: System MUST complete email sending within 30 seconds or return timeout error
- **FR-023**: System MUST validate HTML structure in email body but does NOT sanitize content (organizers are trusted to provide safe HTML)
- **FR-024**: Request body MUST include `subject` (string, required), `body` (string, required, HTML format)
- **FR-025**: System MUST return 400 Bad Request if subject or body validation fails
- **FR-026**: System MUST return 404 Not Found if organization or event does not exist
- **FR-027**: System MUST return 403 Forbidden if user lacks edit permissions for the event
- **FR-028**: System MUST return 503 Service Unavailable if Mailjet service is unreachable or fails

### Key Entities *(include if feature involves data)*

- **Email Request**: Contains filter parameters (reused from partnership listing), email subject, and email body (HTML)
- **Partnership Contact Email**: Email addresses stored in `company_emails` table, linked to partnerships via `partnership_id`
- **EmailContact**: Domain object containing email address and optional name, pre-structured by `PartnershipEmailRepository` (returned within PartnershipWithEmails objects)
- **PartnershipWithEmails**: Domain object returned by `PartnershipEmailRepository.getPartnershipsWithEmails()` containing `partnershipId`, `organiserContact` (EmailContact?), and `emailContacts` (List<EmailContact>)
- **Partnership Organizer**: Optional user assigned to manage specific partnership; determines sender email (organizer's email if assigned, event email otherwise); represented as `organiserContact: EmailContact?` in domain objects
- **NotificationGateway**: Generic interface for sending notifications (email, Slack, etc.); Mailjet implements this and handles its own config lookup from `mailjet_integrations` table
- **Destination**: Generic notification recipient model used by NotificationGateway (identifier + metadata)
- **Email Recipient**: Deduplicated list of EmailContact objects extracted from matching partnerships (includes CC recipients when organizer is assigned)
- **Email Send Response**: Contains success/failure status and count of unique recipients

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Organizers can send bulk emails to filtered partnership groups in under 10 seconds for up to 100 recipients
- **SC-002**: System successfully deduplicates email addresses across partnerships with 100% accuracy (no recipient receives duplicate emails)
- **SC-003**: Email filtering uses identical logic to partnership listing endpoint with zero discrepancies in matched partnerships
- **SC-004**: 95% of email send requests complete successfully when Mailjet service is available and configured correctly
- **SC-005**: Organizers receive clear error messages within 3 seconds when email send fails due to configuration or validation issues
- **SC-005a**: Quota exhaustion errors clearly indicate the specific issue (rate limit vs daily quota) and include next available send time when Mailjet API provides this information
- **SC-006**: System handles up to 500 unique recipients per email send request without timeout
- **SC-007**: HTML structure validation catches malformed HTML with 100% accuracy before sending

## Assumptions *(mandatory)*

- **ASM-001**: Organizations have already configured Mailjet integration via existing integration management endpoints
- **ASM-002**: Partnership contact emails are validated and stored correctly in the `company_emails` table during partnership creation
- **ASM-003**: Organizers will compose email HTML content using external tools or basic HTML knowledge (no WYSIWYG editor provided in this feature)
- **ASM-004**: The existing Mailjet API v3.1 bulk send endpoint (`/v3.1/send`) supports multiple recipients in a single request
- **ASM-005**: Email delivery tracking and bounce management are handled by Mailjet service, not within this system
- **ASM-006**: Organizers understand that email sending is asynchronous and Mailjet handles actual delivery after API success
- **ASM-007**: The existing `NotificationGateway` interface and Mailjet implementation can be reused for this feature; gateway handles its own config lookup from integration tables
- **ASM-008**: Rate limiting and batch processing will be implemented in a future iteration if needed (initial version assumes <500 recipients per request and returns clear quota errors when limits are exceeded)
- **ASM-009**: Email reply-to address matches the From address (organizer email when assigned, event contact email otherwise; organizers cannot specify custom reply-to in v1)
- **ASM-009a**: When sending to multiple partnerships with different assigned organizers, the system sends separate email batches grouped by organizer (each batch has consistent From/CC addresses)
- **ASM-010**: This feature does not track individual email opens, clicks, or delivery status (Mailjet handles this externally)
- **ASM-011**: Email send operations are not logged or persisted in the system - no audit trail or historical record is maintained (ephemeral actions)

## Out of Scope *(mandatory)*

- Email preview endpoint for validating content and checking recipient count before sending (organizers use existing GET partnerships endpoint for filter verification)
- Audit logging or historical record of email send operations (who sent what to whom and when)
- Email template management system with predefined templates
- WYSIWYG HTML editor for composing emails in the UI
- Email scheduling (send at specific date/time)
- Email delivery tracking dashboard showing opens/clicks/bounces
- A/B testing for email content
- Personalization variables in email content (e.g., `{{company_name}}`, `{{contact_name}}`)
- Ability to attach files to emails
- Custom reply-to addresses different from event contact email
- Email recipient management (adding/removing individual email addresses manually)
- Integration with mailing services other than Mailjet
- Batch processing for requests with >500 recipients (will be added if needed)
- Email content templates library or reusable snippets
- Unsubscribe link management (assumed Mailjet handles this if required by law)

## Dependencies *(optional)*

- **NotificationRepository & Gateway**: Requires `NotificationRepository.sendMail()` to be enhanced and `NotificationGateway` interface to support generic notifications; Mailjet implementation (`MailjetNotificationGateway`) handles config lookup from `mailjet_integrations` table
- **Partnership Filtering Logic**: Depends on existing `PartnershipRepository.listByEvent()` filter logic (reused by new `PartnershipEmailRepository`)
- **PartnershipEmailRepository**: New repository that fetches partnerships with pre-structured EmailContact objects for recipients and organizers
- **Partnership Organizer Assignment**: Depends on partnerships having optional organizer assignment; repository converts User data to EmailContact domain objects
- **Authorization Plugin**: Requires `AuthorizedOrganisationPlugin` to enforce permissions
- **Partnership Email Table**: Depends on `PartnershipEmailsTable` (company_emails) schema being correctly populated during partnership creation
- **Event Contact Information**: Requires event records to have valid contact email addresses for "From" field fallback

## Technical Constraints *(optional)*

- **Mailjet API Limits**: Mailjet v3.1 API may have rate limits or daily sending quotas that could restrict bulk sends
- **Database Query Performance**: Filtering partnerships and joining email addresses across potentially thousands of records requires optimized queries
- **Request Timeout**: HTTP requests should complete within 30 seconds to avoid gateway timeouts
- **HTML Validation Only**: Email body HTML structure is validated but content is not sanitized - relies on organizers being trusted authenticated users with edit permissions
- **Memory Limits**: Loading thousands of partnership records and email addresses into memory may require pagination or streaming
- **Concurrent Requests**: Multiple organizers sending emails simultaneously should not cause Mailjet API quota conflicts

