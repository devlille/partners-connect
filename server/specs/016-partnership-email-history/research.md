# Research: Partnership Email History

**Feature**: 016-partnership-email-history  
**Date**: January 10, 2026

## Research Questions & Findings

### 1. Mailjet API Response Format for Per-Recipient Delivery Status

**Question**: How does Mailjet API return delivery status for emails with multiple recipients?

**Decision**: Parse Mailjet's response JSON structure with "Sent" array containing successful recipients

**Rationale**: 
- Mailjet v3.1 `/send` endpoint returns JSON with structure: `{"Sent": [{"Email": "...", "MessageID": ...}]}`
- Recipients present in "Sent" array succeeded; absent recipients failed
- This provides per-recipient granularity needed for FR-004 compliance
- Mailjet response includes MessageID for tracking/debugging

**Alternatives considered**:
- Using Mailjet webhooks for delivery tracking - Rejected: adds complexity, async nature complicates immediate history recording
- Storing only boolean success/failure - Rejected: loses per-recipient granularity required by spec clarification

**Implementation Impact**:
- `MailjetProvider.send()` must return structured response object (not boolean)
- Parse response JSON to extract per-recipient success/failure
- Map Mailjet-specific response to domain-agnostic `EmailDeliveryResult` model

---

### 2. Domain-Agnostic Delivery Result Abstraction

**Question**: How should `NotificationGateway` interface return delivery results without Mailjet-specific details?

**Decision**: Create `EmailDeliveryResult` domain model with provider-agnostic structure

**Rationale**:
- Maintains clean architecture: notification domain doesn't depend on Mailjet specifics
- Enables future support for alternative email providers (SendGrid, AWS SES, etc.)
- `EmailDeliveryResult` contains: overall status (sent/failed/partial), per-recipient status array, failure reasons
- Gateway implementations (Mailjet, future providers) map their specific responses to this model

**Alternatives considered**:
- Exposing Mailjet response directly - Rejected: violates domain separation, creates tight coupling
- Keeping boolean return type - Rejected: insufficient for per-recipient tracking requirement

**Implementation Impact**:
- Define `EmailDeliveryResult` in `notifications/domain/`
- Update `NotificationGateway.send()` interface to return `EmailDeliveryResult`
- `MailjetNotificationGateway` maps Mailjet response to domain model
- All existing callers of `send()` must be updated to handle new return type

---

### 3. Database Schema for Per-Recipient Status

**Question**: Should per-recipient delivery status be stored in separate table or JSON column?

**Decision**: Separate normalized table `RecipientDeliveryStatusTable` with one-to-many relationship

**Rationale**:
- Enables efficient querying by recipient email (e.g., "find all emails sent to specific address")
- Better data integrity with proper foreign key constraints
- Follows Exposed ORM patterns used throughout codebase
- Avoids JSON column which would be less maintainable and harder to query

**Alternatives considered**:
- JSON column in `PartnershipEmailHistoryTable` - Rejected: less queryable, no referential integrity, not idiomatic for Exposed
- Single status column with comma-separated values - Rejected: extremely poor data model, no querying capability

**Implementation Impact**:
- Create `RecipientDeliveryStatusTable` extending `UUIDTable`
- Foreign key to `PartnershipEmailHistoryTable`
- Fields: `emailAddress`, `deliveryStatus` (enum: SENT, FAILED, PENDING), `failureReason` (nullable)
- `PartnershipEmailHistoryEntity` has `recipients` relationship via `referredOn`

---

### 4. Email Content Storage Strategy

**Question**: How should email body content be stored?

**Decision**: Store email content as-is without any HTML processing

**Rationale**:
- Preserves original email content exactly as sent
- No data loss or transformation
- Simpler implementation - no additional dependencies
- Content can be HTML, plain text, or mixed - system doesn't care
- Frontend/clients can handle rendering as needed

**Alternatives considered**:
- HTML to plain text conversion - Rejected: unnecessary processing, loses formatting information
- Storing both HTML and plain text - Rejected: redundant storage, added complexity

**Implementation Impact**:
- No additional dependencies needed
- In `MailjetNotificationGateway`, store `htmlPart` directly in history
- Database TEXT column stores content as-is

---

### 5. Pagination Implementation Pattern

**Question**: What's the standard pagination pattern in this codebase for list endpoints?

**Decision**: Use existing `PaginatedResponse<T>` model with `page` and `page_size` parameters

**Rationale**:
- Codebase has standard `PaginatedResponse<T>` model in `internal.infrastructure.api` package
- Model includes: `items` (array), `page` (int), `page_size` (int), `total` (long), optional `metadata`
- Simple to implement with Exposed: `.limit(pageSize, offset = page * pageSize)`
- Default page size: 20 (per `DEFAULT_PAGE_SIZE` constant), customizable via query param
- Consistent with all other paginated endpoints in the system

**Alternatives considered**:
- Custom pagination response - Rejected: violates consistency, reinventing existing solution
- Cursor-based pagination - Rejected: more complex, unnecessary for this use case

**Implementation Impact**:
- GET `/orgs/{orgSlug}/events/{eventSlug}/partnerships/{id}/email-history?page=0&page_size=20`
- Query parameters: page ≥ 0, page_size 1-100 (default 20)
- Response: `PaginatedResponse<PartnershipEmailHistoryResponse>`
- Use existing `PaginatedResponse` from `internal.infrastructure.api`

---

### 6. Authorization Strategy for Email History Endpoint

**Question**: How should authorization be enforced for organiser-only access?

**Decision**: Use existing `AuthorizedOrganisationPlugin` Ktor plugin

**Rationale**:
- Existing pattern used throughout codebase (partnership routes, event routes)
- Plugin automatically validates: user authenticated, user has permission for organization/event
- Throws appropriate 401/403 exceptions if unauthorized
- Zero boilerplate in route handler

**Alternatives considered**:
- Manual permission checking in route - Rejected: violates DRY, error-prone, inconsistent with codebase
- Custom middleware - Rejected: reinventing existing solution

**Implementation Impact**:
- Install `AuthorizedOrganisationPlugin` on email history route
- No manual authorization logic needed in handler
- Plugin ensures only organisers with event permissions can access

---

### 7. Testing Strategy for Email History Logging

**Question**: How to test that emails are automatically logged when sent?

**Decision**: Integration test that sends email via `NotificationRepository`, then verifies history via GET endpoint

**Rationale**:
- Tests complete workflow: send email → history logged → history retrieved
- Validates both automatic logging (FR-001) and retrieval (FR-002)
- Uses actual API endpoints (not repository methods directly) per testing constitution
- H2 in-memory database ensures test isolation

**Alternatives considered**:
- Unit test on repository - Rejected: constitution mandates HTTP route testing
- Mock email provider - Rejected: doesn't test real integration, false confidence

**Implementation Impact**:
- `PartnershipEmailHistoryRoutesTest` integration test
- Setup: Create partnership, configure Mailjet integration (or mock provider)
- Action: Call existing email send endpoint, then GET /email-history
- Assert: History contains sent email with correct details

---

### 8. Handling Email Body Storage

**Question**: How to store email bodies without imposing artificial size limits?

**Decision**: Use PostgreSQL TEXT column type (unlimited size)

**Rationale**:
- TEXT column supports unlimited content (up to 1GB in PostgreSQL)
- No need for arbitrary character limits that could truncate content
- Most emails are <10KB, but system can handle exceptional cases
- Database handles storage efficiently (TOAST for large values)
- Rejection of oversized emails is NOT acceptable (loses audit trail data)

**Alternatives considered**:
- VARCHAR(50000) with hard limit - Rejected: artificial limit, could lose data
- Automatic truncation - Rejected: data loss unacceptable for audit trail
- Splitting into chunks - Rejected: over-engineering, adds complexity

**Implementation Impact**:
- `PartnershipEmailHistoryTable.bodyPlainText = text("body_plain_text")`
- No application-level validation needed for size
- PostgreSQL TOAST automatically handles storage optimization

---

## Technology Decisions Summary

| Component | Technology | Version | Justification |
|-----------|-----------|---------|---------------|
| Pagination | Offset-based | N/A | Existing codebase pattern, simple, sufficient |
| Authorization | AuthorizedOrganisationPlugin | N/A | Existing Ktor plugin, zero boilerplate |
| Database | Exposed ORM | 0.41+ | Project standard, type-safe |
| Testing | Ktor testApplication | N/A | Project standard, HTTP route testing |

---

## Dependencies to Add

No additional dependencies required.

---

## Open Questions (None)

All technical unknowns have been researched and resolved. Ready to proceed to Phase 1 (Design).
