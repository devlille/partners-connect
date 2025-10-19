# Research: Job Offer Promotion Implementation

## Overview
This document consolidates research findings for implementing the job offer promotion workflow, covering database design patterns, notification integration, permission validation, and testing strategies within the existing partners-connect codebase.

---

## Database Schema Design

### Decision: Exposed ORM with Foreign Key Cascades
**Rationale**: 
- Existing codebase uses Exposed ORM throughout (see `CompanyJobOffersTable`, `PartnershipsTable`)
- FR-024 requires cascade delete when job offers are deleted
- FR-032 requires preserving promotions when partnerships terminate (no cascade on partnership FK)
- Exposed supports granular cascade control per foreign key

**Date/Time Column Type**: Use `datetime()` function (maps to `DATETIME` in PostgreSQL, `LocalDateTime` in Kotlin). This is the project standard - `timestamp()` is not used.

**Pattern** (from existing `CompanyJobOffersTable`):
```kotlin
object CompanyJobOfferPromotionsTable : UUIDTable("company_job_offer_promotions") {
    val jobOfferId = reference("job_offer_id", CompanyJobOffersTable, onDelete = ReferenceOption.CASCADE)
    val partnershipId = reference("partnership_id", PartnershipsTable, onDelete = ReferenceOption.NO_ACTION)
    val eventId = reference("event_id", EventsTable, onDelete = ReferenceOption.NO_ACTION)
    val status = enumerationByName<PromotionStatus>("status", 20)
    val promotedAt = datetime("promoted_at")
    val reviewedAt = datetime("reviewed_at").nullable()
    val reviewedBy = reference("reviewed_by", UsersTable).nullable()
}
```

**Alternatives Considered**:
- **Manual cascade in repository**: Rejected - error-prone, inconsistent with codebase patterns
- **Database triggers**: Rejected - Exposed doesn't integrate well with custom triggers, harder to test

---

## Notification Integration

### Decision: Extend Existing NotificationVariables Pattern
**Rationale**:
- Existing notification system already handles partnership events (see `SuggestionApproved`, `PartnershipValidated`)
- FR-013 to FR-021 require dual notifications (Mailjet email + Slack)
- System already has MailjetNotificationGateway and SlackNotificationGateway
- Language support via partnership language field already implemented

**Pattern** (from `NotificationVariables.kt`):
```kotlin
data class JobOfferPromoted(
    override val language: String,
    override val event: EventWithOrganisation,
    override val company: Company,
    val partnership: Partnership,
    val jobOffer: JobOfferResponse,
) : NotificationVariables {
    override val usageName: String = "job_offer_promoted"
    override fun populate(content: String): String {
        val partnershipLink = partnership.link(event)
        return content
            .replace("{{event_name}}", event.event.name)
            .replace("{{company_name}}", company.name)
            .replace("{{job_title}}", jobOffer.title)
            .replace("{{job_url}}", jobOffer.url)
            .replace("{{partnership_link}}", partnershipLink)
    }
}
```

**Notification Files Required** (per existing convention):
- `resources/notifications/email/{usage_name}/content.{lang}.html`
- `resources/notifications/email/{usage_name}/header.{lang}.txt`
- `resources/notifications/slack/{usage_name}/{lang}.md`

**Alternatives Considered**:
- **New notification service**: Rejected - unnecessary duplication, existing infrastructure sufficient
- **Single notification type**: Rejected - FR-013/FR-014 explicitly require both email and Slack

---

## Permission Validation

### Decision: Use AuthorizedOrganisationPlugin for Route Protection
**Rationale**:
- FR-037 requires automatic JWT token validation and permission checking
- Existing Ktor plugin `AuthorizedOrganisationPlugin` handles this automatically
- Plugin extracts token, calls `getUserInfo()`, checks `hasEditPermissionByEmail()`, throws `UnauthorizedException` if unauthorized
- Used consistently across all organization-protected routes (events, sponsoring, partnerships, etc.)
- Eliminates manual permission checking in route handlers

**Pattern** (from existing routes like `EventRoutes.kt`, `SponsoringRoutes.kt`):
```kotlin
route("/orgs/{orgSlug}/events/{eventSlug}/partnership/{partnershipId}/job-offers/promotions/{promotionId}") {
    install(AuthorizedOrganisationPlugin)  // Automatically validates JWT + canEdit permission
    
    post("/approve") {
        // No manual permission checking needed - plugin handles it
        val partnershipId = call.parameters.partnershipId
        val promotionId = call.parameters.promotionId
        val userInfo = authRepository.getUserInfo(call.token)  // User already validated by plugin
        
        // Business logic only
        promotionRepository.approve(promotionId, userInfo.id)
        call.respond(HttpStatusCode.OK)
    }
    
    post("/decline") {
        // Plugin ensures user has canEdit=true before reaching this code
        val promotionId = call.parameters.promotionId
        val reason = call.receive<DeclineRequest>()
        promotionRepository.decline(promotionId, reason)
        call.respond(HttpStatusCode.OK)
    }
}
```

**Plugin Implementation** (from `AuthorizedOrganisationPlugin.kt`):
```kotlin
val AuthorizedOrganisationPlugin = createRouteScopedPlugin(name = "AuthorizedOrganisationPlugin") {
    val authRepository by application.inject<AuthRepository>()
    val userRepository by application.inject<UserRepository>()

    onCall { call ->
        val token = call.token
        val orgSlug = call.parameters.orgSlug
        val userInfo = authRepository.getUserInfo(token)
        val canEdit = userRepository.hasEditPermissionByEmail(userInfo.email, orgSlug)
        if (!canEdit) throw UnauthorizedException("You are not allowed to edit this event")
    }
}
```

**Alternatives Considered**:
- **Manual permission checking in each route**: Rejected - duplicates code, error-prone, inconsistent with codebase
- **Owner-only restriction**: Rejected - clarification specified canEdit suffices
- **New permission level**: Rejected - existing permission model adequate

---

## Event Date Validation

### Decision: Check Event End Date Before Promotion
**Rationale**:
- FR-030: System MUST reject job offer promotion attempts after event end date with 403 Forbidden
- Clarification confirmed: "System returns 403 Forbidden when attempting to promote after event end date"
- Event entity has `endTime` field (from existing EventEntity)
- Validation should happen in repository layer before INSERT

**Pattern**:
```kotlin
suspend fun promote(companyId: UUID, jobOfferId: UUID, partnershipId: UUID): UUID = transaction {
    val partnership = PartnershipEntity.findById(partnershipId) 
        ?: throw NotFoundException("Partnership not found")
    val event = EventEntity.findById(partnership.eventId.value)
        ?: throw NotFoundException("Event not found")
    
    // Check event end date
    val now = Clock.System.now()
    if (event.endTime < now) {
        throw ForbiddenException("Cannot promote job offers after event has ended")
    }
    
    // ... continue with promotion
}
```

**Alternatives Considered**:
- **Validation in route**: Rejected - violates separation of concerns, harder to test
- **Soft validation (warning)**: Rejected - requirement explicitly states 403 Forbidden error

---

## Status Transition Logic

### Decision: Allow Re-Promotion by Upserting Existing Records
**Rationale**:
- FR-031: System MUST allow re-promotion of declined job offers, resetting status to pending
- Clarification: "Allow immediate re-promotion without changes (declined status reset to pending)"
- Prevents duplicate entries for same job offer + partnership combination
- Maintains history via `promotedAt` timestamp update

**Pattern**:
```kotlin
// Check for existing promotion
val existing = CompanyJobOfferPromotionEntity.find {
    (CompanyJobOfferPromotionsTable.jobOfferId eq jobOfferId) and
    (CompanyJobOfferPromotionsTable.partnershipId eq partnershipId)
}.singleOrNull()

if (existing != null) {
    // Re-promotion: reset status and timestamp
    existing.status = PromotionStatus.PENDING
    existing.promotedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    existing.reviewedAt = null
    existing.reviewedBy = null
    return existing.id.value
} else {
    // New promotion
    return CompanyJobOfferPromotionEntity.new {
        // ... initialize fields
    }.id.value
}
```

**Alternatives Considered**:
- **Block duplicate promotions**: Rejected - conflicts with FR-031
- **Soft delete + new record**: Rejected - loses audit trail, increases storage

---

## Testing Strategy

### Decision: Integration Tests via HTTP Routes (No Repository Tests)
**Rationale**:
- Constitution: "Focus on HTTP route integration tests that validate API behavior, NOT repository tests"
- User requirement: "I don't want unit test on repository, just developed integration tests"
- Existing test pattern: `CompanyJobOfferRoutesTest.kt` uses Ktor test engine with H2 in-memory DB
- Tests must cover all FR requirements through endpoint calls

**Test Structure** (per existing pattern):
```kotlin
class CompanyJobOfferPromotionRoutesTest : BaseIntegrationTest() {
    @Test
    fun `POST promote job offer - success with notifications`() {
        // Setup: Create company, partnership, job offer, event
        // Execute: POST /companies/{companyId}/partnerships/{partnershipId}/promote
        // Assert: 201 Created, promotion ID returned, notifications sent
    }
    
    @Test
    fun `POST promote job offer - 403 when event ended`() {
        // Setup: Event with end date in past
        // Execute: POST promotion endpoint
        // Assert: 403 Forbidden with appropriate error message
    }
    
    @Test
    fun `POST promote job offer - allow re-promotion of declined`() {
        // Setup: Existing declined promotion
        // Execute: POST promotion again
        // Assert: 201 Created, status reset to pending
    }
    
    // ... more scenarios covering FR-001 to FR-032
}
```

**Coverage Requirements**:
- All 32 functional requirements (FR-001 to FR-032)
- All 6 acceptance scenarios from spec
- Edge cases: cascade delete, partnership termination, missing permissions
- Notification integration (mock Mailjet/Slack gateways)

**Alternatives Considered**:
- **Repository unit tests**: Rejected - explicit user requirement and constitution guidance
- **Manual testing only**: Rejected - constitution requires 80% coverage

---

## OpenAPI Documentation

### Decision: Update openapi.yaml with JSON Schema References
**Rationale**:
- Constitution: "All new or modified REST API endpoints MUST be documented in openapi.yaml"
- Existing pattern: Schemas in `resources/schemas/*.json`, referenced in openapi.yaml
- User requirement: "update the openapi file with new services based on jsonschema"
- Must pass `npm run validate` (Redocly lint)

**Required Schemas**:
1. `promote_job_offer.schema.json` - Request for POST /companies/{companyId}/partnerships/{partnershipId}/promote
2. `approve_job_offer_promotion.schema.json` - Request for POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/job-offers/{promotionId}/approve
3. `decline_job_offer_promotion.schema.json` - Request for POST .../decline (may include reason field)
4. `job_offer_promotion_response.schema.json` - Response with promotion details

**OpenAPI Requirements** (per constitution):
- All endpoints must have `operationId`, `summary`, `security` definitions
- Company endpoints: `security: - {}` (public, no authentication required)
- Partnership endpoints: `security: - bearerAuth: []` (requires JWT + edit permission via AuthorizedOrganisationPlugin)
- Proper 2xx/4xx response definitions with error schemas

**Alternatives Considered**:
- **Inline schemas in openapi.yaml**: Rejected - inconsistent with codebase, harder to validate
- **Skip OpenAPI**: Rejected - constitution makes this NON-NEGOTIABLE

---

## Error Handling

### Decision: Use StatusPages Exception Mapping (No Try-Catch in Routes)
**Rationale**:
- Constitution: "Route handlers MUST NOT include try-catch blocks for exception-to-HTTP conversion"
- Existing pattern: All routes throw domain exceptions, StatusPages converts to HTTP responses
- Required exceptions already defined: `NotFoundException`, `ForbiddenException`, `ConflictException`

**Exception Mapping**:
- `NotFoundException` → 404 Not Found
- `ForbiddenException` → 403 Forbidden (event ended, no permission)
- `ConflictException` → 409 Conflict (duplicate promotion - though we upsert)
- `ValidationException` → 400 Bad Request (invalid input)

**Pattern** (from constitution):
```kotlin
// CORRECT - No try-catch, throw domain exception
suspend fun promote(...): UUID = transaction {
    val event = EventEntity.findById(eventId) ?: throw NotFoundException("Event not found")
    if (event.endTime < now) {
        throw ForbiddenException("Cannot promote after event ended")
    }
    // ...
}

// WRONG - Manual exception handling
try {
    val result = repository.promote(...)
    call.respond(HttpStatusCode.Created, result)
} catch (e: Exception) {
    call.respond(HttpStatusCode.InternalServerError, ...)
}
```

**Alternatives Considered**:
- **Manual try-catch**: Rejected - violates constitution, creates inconsistent error responses
- **Result wrapper types**: Rejected - not used in codebase, StatusPages sufficient

---

## Summary of Key Decisions

| Area | Decision | Constitutional Alignment |
|------|----------|-------------------------|
| Database | Exposed ORM with selective FK cascades | ✓ Clean Architecture (modular) |
| Notifications | Extend NotificationVariables pattern | ✓ API Consistency (reuse existing) |
| Permissions | Use existing canEdit validation | ✓ Clean Architecture (DRY) |
| Testing | HTTP integration tests only | ✓ Testing Strategy (route-focused) |
| OpenAPI | JSON schemas + openapi.yaml update | ✓ API Consistency (mandatory docs) |
| Error Handling | StatusPages exception mapping | ✓ Exception Handling Pattern (no try-catch) |

---

## Dependencies & Integration Points

### Existing Services to Use
- **NotificationRepository**: Send dual notifications (Mailjet + Slack)
- **OrganisationPermissionEntity**: Validate canEdit permission
- **CompanyJobOfferRepository**: Retrieve job offer details for promotion
- **PartnershipRepository**: Validate active partnership exists
- **EventRepository**: Check event end date

### No New External Dependencies
All functionality uses existing infrastructure:
- Ktor web framework
- Exposed ORM
- Koin DI
- kotlinx-serialization
- Notification gateways (Mailjet, Slack)

---

## Risk Mitigation

### Database Migration
- **Risk**: Schema changes break existing deployments
- **Mitigation**: New table creation is additive (no breaking changes), foreign key constraints ensure referential integrity

### Notification Failures
- **Risk**: FR-021 requires logging failures without blocking
- **Mitigation**: Existing notification gateways return Boolean, wrap in try-catch only for logging (not for status change rollback)

### Permission Edge Cases
- **Risk**: Partnership without organization link (orphaned data)
- **Mitigation**: Existing foreign key constraints prevent orphans, repository validation will catch missing relations

### Event Date Timezone Issues
- **Risk**: Event end date comparison affected by timezone
- **Mitigation**: Use `Clock.System.now()` with UTC for consistent comparisons, existing EventEntity stores timestamps in UTC

---

*All research complete. No NEEDS CLARIFICATION remain. Ready for Phase 1 design.*
