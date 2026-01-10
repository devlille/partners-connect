# Implementation Plan: Partnership Email History

**Branch**: `016-partnership-email-history` | **Date**: January 10, 2026 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/016-partnership-email-history/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Create a comprehensive email history tracking system for partnership communications. Every email sent to a partnership will be automatically logged with full details (sender, recipients, subject, body, per-recipient delivery status). An organiser-only API endpoint will provide paginated access to this history, ensuring complete audit trail and transparency in partner communications.

## Technical Context

**Language/Version**: Kotlin 1.9.x with JVM 21 (Amazon Corretto)  
**Primary Dependencies**: Ktor 2.x, Exposed 0.41+, kotlinx.serialization, Koin DI  
**Storage**: PostgreSQL (H2 in-memory for tests)  
**Testing**: JUnit 5, Ktor test application, 80%+ coverage via HTTP route tests  
**Target Platform**: Linux server (Docker containerized)
**Project Type**: Single backend server with REST API  
**Performance Goals**: <2 seconds for history retrieval (per spec SC-001), <500ms for email logging  
**Constraints**: Per-recipient delivery tracking, indefinite retention, paginated responses using PaginatedResponse model  
**Scale/Scope**: Supports multiple events/organizations, ~100-500 emails per partnership over event lifetime

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Code Quality Standards
- ✅ **ktlint & detekt**: Will run on all new code
- ✅ **80% test coverage**: Contract tests + integration tests planned for all endpoints
- ✅ **KDoc documentation**: All public APIs will be documented
- ✅ **No TODO comments**: No production TODOs without GitHub issues

### Testing Strategy
- ✅ **Integration tests via HTTP routes**: Will test complete workflow through API endpoints
- ✅ **Contract tests**: Will validate API schemas for GET /partnerships/{id}/email-history
- ✅ **Shared database pattern**: Will use `moduleSharedDb(userId)` with single transaction initialization
- ✅ **Factory functions**: Will create `insertMockedPartnershipEmailHistory()` with UUID-based defaults
- ✅ **External service testing**: Mailjet response parsing will be tested via HTTP routes

### Clean Architecture
- ✅ **Repository separation**: `PartnershipEmailHistoryRepository` will NOT depend on other repositories
- ✅ **Logging in routes**: Email logging will occur in route handlers AFTER calling notification gateway (not in gateway itself to maintain module separation)
- ✅ **Module boundaries**: New code in `partnership/` module (email history is partnership-scoped)

### API Consistency
- ✅ **JSON schema validation**: Will create `partnership_email_history_response.schema.json`
- ✅ **OpenAPI documentation**: Will document GET endpoint in `openapi.yaml`
- ✅ **Response time**: <2 seconds per spec (pagination ensures scalability)
- ✅ **Error handling**: Standard StatusPages error mapping

### Database Schema Standards
- ✅ **UUIDTable**: New `PartnershipEmailHistoryTable` will extend `UUIDTable`
- ✅ **datetime()**: Will use `datetime()` for timestamp column (NOT `timestamp()`)
- ✅ **Entity pattern**: Will create `PartnershipEmailHistoryEntity` extending `UUIDEntity`
- ✅ **Immutability**: No update/delete operations on history records

**Overall Status**: ✅ **PASS** - No constitution violations. All standards will be followed.

## Project Structure

### Documentation (this feature)

```text
specs/016-partnership-email-history/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── get_partnership_email_history.yaml  # GET endpoint contract
│   └── schemas/         # JSON schemas for request/response
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
server/application/src/
├── main/kotlin/fr/devlille/partners/connect/
│   ├── partnership/
│   │   ├── domain/
│   │   │   ├── PartnershipEmailHistory.kt           # Domain model
│   │   │   └── PartnershipEmailHistoryRepository.kt  # Repository interface
│   │   ├── application/
│   │   │   └── PartnershipEmailHistoryRepositoryExposed.kt  # Repository implementation
│   │   └── infrastructure/
│   │       ├── db/
│   │       │   ├── PartnershipEmailHistoryTable.kt           # Database schema
│   │       │   ├── PartnershipEmailHistoryEntity.kt          # Exposed entity
│   │       │   └── RecipientDeliveryStatusTable.kt           # Per-recipient status
│   │       └── api/
│   │           └── PartnershipEmailHistoryRoutes.kt          # GET endpoint
│   └── notifications/
│       ├── domain/
│       │   ├── EmailDeliveryResult.kt                # Domain model for delivery results
│       │   └── NotificationGateway.kt                # Updated interface
│       └── infrastructure/
│           ├── gateways/
│           │   └── MailjetNotificationGateway.kt     # Updated to log history
│           └── providers/
│               └── MailjetProvider.kt                # Updated to return detailed response
└── main/resources/
    ├── openapi/
    │   └── openapi.yaml                              # Updated with new endpoint
    └── schemas/
        ├── partnership_email_history_response.schema.json
        ├── partnership_email_history_list_response.schema.json
        └── recipient_delivery_status.schema.json

server/application/src/test/kotlin/fr/devlille/partners/connect/
└── partnership/
    ├── PartnershipEmailHistoryRoutesTest.kt          # Integration tests
    ├── infrastructure/api/
    │   └── PartnershipEmailHistoryRouteGetTest.kt   # Contract tests
    └── factories/
        └── PartnershipEmailHistory.factory.kt        # Test data factory
```

**Structure Decision**: Email history is partnership-scoped, so new code belongs in `partnership/` module following existing domain structure. The `notifications/` module will be updated to return structured delivery results (domain-agnostic) rather than boolean, and `MailjetProvider` will parse Mailjet-specific response format.

## Complexity Tracking

> No violations of constitution constraints. This section is not needed.
