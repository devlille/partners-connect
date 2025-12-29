# Implementation Plan: Email Partnership Contacts via Mailing Integration

**Branch**: `014-email-partnerships` | **Date**: 2025-12-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/014-email-partnerships/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Enable event organizers to send bulk emails to filtered partnership groups using the existing Mailjet integration. Organizers can apply the same partnership filters available in the listing endpoint (`filter[validated]`, `filter[paid]`, etc.) to select recipients, provide email subject and HTML body, and send emails through a new POST endpoint. The system handles email deduplication, validates content, groups emails by assigned partnership organizer (using organizer email as From with event email in CC), and returns success with recipient count once Mailjet accepts the batch.

## Technical Context

**Language/Version**: Kotlin 1.9.x with JVM 21 (Amazon Corretto)
**Primary Dependencies**: Ktor 2.x, Exposed ORM 0.41+, kotlinx.serialization, Koin (DI), Mailjet API v3.1
**Storage**: PostgreSQL database (H2 in-memory for tests)
**Testing**: Ktor test framework with H2 in-memory database, contract tests for API schema validation, integration tests for HTTP routes
**Target Platform**: JVM server application (Linux/Docker)
**Project Type**: Web backend (Kotlin/Ktor REST API)
**Performance Goals**: <10 seconds for 100 recipients, <30 seconds timeout for up to 500 recipients
**Constraints**: 30-second HTTP timeout, Mailjet API rate limits, no audit logging (ephemeral operation)
**Scale/Scope**: Single new POST endpoint, reuses existing partnership repository filtering, new email grouping/deduplication logic

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Code Quality Standards (PASSED)
- **ktlint/detekt compliance**: Will be enforced via `./gradlew ktlintCheck detekt`
- **Documentation**: All public repository interfaces and route handlers will include KDoc documentation
- **Code review**: Standard PR process with architectural review for repository separation patterns

### ✅ Comprehensive Testing Strategy (PASSED)
- **Contract tests**: Will be written BEFORE implementation for POST `/orgs/{orgSlug}/events/{eventSlug}/partnerships/email` endpoint
- **Integration tests**: HTTP route tests validating email grouping by organizer, deduplication logic, error scenarios
- **Test coverage**: Target 80%+ for new email repository and route handler
- **H2 in-memory database**: All tests use H2, no external dependencies
- **Mock factories**: Will reuse existing `insertMockedPartnership()`, `insertMockedEvent()`, `insertMockedOrganisation()` factories

### ✅ Clean Modular Architecture (PASSED - Critical Pattern Compliance)
- **Repository separation**: New `PartnershipEmailRepository` will NOT depend on other repositories (data fetching only)
- **Route orchestration**: Email sending routes will inject `PartnershipEmailRepository`, `EventRepository`, and `NotificationRepository` separately
- **Notification pattern**: Routes orchestrate email grouping by organizer, then call `NotificationRepository.sendMail()` per group (email sending abstracted to notification layer)
- **NotificationRepository usage**: Existing `NotificationRepository` enhanced with `sendMail()` function that internally handles Mailjet gateway configuration and provider calls
- **Domain module**: Email functionality stays within `partnership` module (no new modules needed)
- **Database pattern**: Will use Exposed ORM with UUIDTable/UUIDEntity dual-layer structure (NO new tables needed - uses existing `company_emails` and `partnerships` tables)

### ✅ API Consistency & User Experience (PASSED)
- **JSON Schema validation**: Will create `send_partnership_email_request.schema.json` for request body validation
- **OpenAPI documentation**: Will update `openapi.yaml` with new POST endpoint specification
- **`call.receive<T>(schema)` pattern**: Will be used for automatic request validation
- **Consistent error responses**: Leverages existing StatusPages exception mapping
- **Authorization**: Will use `AuthorizedOrganisationPlugin` for permission enforcement (NO manual checks)
- **Parameter extraction**: Will use `call.parameters.eventSlug` and `call.parameters.orgSlug` extensions

### ✅ Performance & Observability Requirements (PASSED)
- **Database queries**: Reuses existing optimized partnership filtering via `PartnershipRepository.listByEvent()`
- **Structured logging**: Standard Ktor logging with correlation IDs (inherited from existing infrastructure)
- **Resource monitoring**: Production monitoring handled by ops team (not in implementation scope)
- **Performance testing**: NOT included per constitution - functional validation only in quickstart

### Constitution Compliance Summary
**Status**: ✅ ALL GATES PASSED - No violations

- Repository layer will be pure data access (email grouping logic in route handlers)
- Authorization via plugin (no manual permission checks)
- Exception handling via StatusPages (no try-catch in routes)
- Schema validation via JSON schemas and `call.receive<T>(schema)`
- OpenAPI documentation will include complete endpoint specification with external schema references

## Project Structure

### Documentation (this feature)

```text
specs/014-email-partnerships/
├── spec.md              # Feature specification (COMPLETE)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (COMPLETE)
├── data-model.md        # Phase 1 output (COMPLETE)
├── quickstart.md        # Phase 1 output (COMPLETE)
├── contracts/           # Phase 1 output (COMPLETE)
│   ├── send-email-api.md
│   └── email-grouping-logic.md
│   ├── send-email-api.md
│   └── email-grouping-logic.md
├── checklists/
│   └── requirements.md  # Quality validation checklist (COMPLETE)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/
├── domain/
│   ├── PartnershipEmailRepository.kt          # NEW - Interface for fetching partnerships with emails
│   └── SendPartnershipEmailRequest.kt         # NEW - Request data class
├── application/
│   └── PartnershipEmailRepositoryExposed.kt   # NEW - Repository implementation
└── infrastructure/api/
    └── PartnershipEmailRoutes.kt              # NEW - HTTP route handlers

server/application/src/main/kotlin/fr/devlille/partners/connect/notifications/
└── domain/
    └── NotificationRepository.kt              # MODIFIED - Add sendMail() function for email sending

server/application/src/main/resources/
├── openapi/
│   └── openapi.yaml                           # MODIFIED - Add POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/email
└── schemas/
    ├── send_partnership_email_request.schema.json   # NEW - Request schema
    └── send_partnership_email_response.schema.json  # NEW - Response schema

server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/
├── PartnershipEmailRoutesContractTest.kt      # NEW - Contract tests for API schema validation
├── PartnershipEmailIntegrationTest.kt         # NEW - Integration tests for email grouping logic
└── factories/
    └── PartnershipFactory.kt                  # MODIFIED - Add mock organizer assignment helpers
```

**Structure Decision**: This feature extends the existing `partnership` domain module with email sending capabilities. Following the established pattern:
- **Domain layer**: `PartnershipEmailRepository` interface defines contract for fetching partnerships with email data (data access only, no sending logic)
- **Application layer**: Exposed ORM implementation handles database queries joining partnerships, company_emails, and users tables
- **Infrastructure/API layer**: Route handlers orchestrate three repositories:
  - `PartnershipEmailRepository.getPartnershipsWithEmails()` - Fetch data
  - `EventRepository.findBySlug()` - Get event details
  - `NotificationRepository.sendMail()` - Send emails (internally uses MailjetProvider)
- **Email sending**: Routes group partnerships by organizer, then loop groups calling `NotificationRepository.sendMail()` per group
- **NotificationRepository enhancement**: Existing notification module extended with `sendMail()` function that handles Mailjet gateway configuration lookup and email sending
- **No new database tables**: Reuses existing `partnerships`, `company_emails`, `events`, and `users` tables
- **No new modules**: Email functionality is partnership-related, stays in partnership module

## Complexity Tracking

> **No constitution violations - this section is not applicable**

All implementation follows established constitutional patterns with zero violations.
