# Research: Email Partnership Contacts via Mailing Integration

**Feature**: 014-email-partnerships  
**Date**: 2025-12-19  
**Status**: Complete

## Technical Decisions

### 1. Email Sending Strategy (Updated)

**Decision**: Send individual emails per partnership (no grouping). Each partnership gets its own destination with From/CC determined by assigned organizer.

**Rationale**:
- Partnerships can have different assigned organizers (or no organizer)
- Each Mailjet message requires consistent From/CC addresses
- From email logic: Organizer email when assigned, event contact email when not assigned
- CC logic: Event contact email added only when organizer is From
- Grouping by organizer ensures consistent sender identity per batch

**Implementation Approach**:
```kotlin
// Route handler orchestration (injects 2 repositories)
val partnershipEmailRepository by inject<PartnershipEmailRepository>()
val notificationRepository by inject<NotificationRepository>()

// Get destinations for all partnerships (gateway creates provider-specific destinations)
val destinations = partnershipEmailRepository.getPartnershipDestination(eventSlug, filters)
if (destinations.isEmpty()) throw NotFoundException("No partnerships match filters")

// Send to each destination
destinations.forEach { destination ->
    notificationRepository.sendMessage(
        eventSlug = eventSlug,
        destination = destination,
        subject = request.subject,
        htmlBody = request.body
    )
}

// Return total destinations count
SendPartnershipEmailResponse(recipients = destinations.size)
```

**Alternatives Considered**:
- **Single batch with mixed From addresses**: Rejected - Mailjet requires consistent From per message
- **Group partnerships by organizer**: Simplified to individual sending for clearer tracking and simpler architecture
- **Separate API call per contact email**: Rejected - Too many HTTP requests, inefficient

---

### 2. Email Deduplication Strategy (Updated)

**Decision**: Each partnership sends to its own contact emails (no cross-partnership deduplication).

**Rationale**:
- Each partnership is treated independently
- Contact emails are stored per partnership in partnership_emails table  
- If same email appears in multiple partnerships, they receive separate emails (acceptable for per-partnership tracking)
- Simplified logic without cross-partnership coordination

**Implementation Approach**:
```kotlin
// Each destination contains contact emails for one partnership
val destinations = partnershipEmailRepository.getPartnershipDestination(eventSlug, filters)

// Gateway creates destination with From/CC logic and partnership emails
// Each partnership sends independently
destinations.forEach { destination ->
    notificationRepository.sendMessage(
        eventSlug = eventSlug,
        destination = destination,
        subject = request.subject,
        htmlBody = request.body
    )
}
```

**Alternatives Considered**:
- **Cross-partnership deduplication**: Rejected - Adds complexity, per-partnership tracking is acceptable
- **Database-level DISTINCT**: Rejected - Not needed for individual partnership sending

---

### 3. Email Sending Integration Pattern

**Decision**: Use `NotificationRepository.sendMail()` to handle email sending via configured email gateway (Mailjet).

**Rationale**:
- NotificationRepository is the correct abstraction for all notification sending (email, Slack, etc.)
- NotificationGateway provides generic interface for different notification types
- Gateway implementations (Mailjet, Slack) handle their own config lookup from integration tables
- Routes can ONLY depend on repositories (architectural constraint)
- Abstraction allows changing email provider (Mailjet → SendGrid, etc.) without touching routes or NotificationRepository
- Maintains clean separation: Routes orchestrate, NotificationRepository adapts, Gateway sends

**NotificationRepository Interface**:
```kotlin
interface NotificationRepository {
    /**
     * Sends an email via the configured email gateway (Mailjet).
     * 
     * Checks if email gateway is configured for organization and uses it to send.
     * Handles provider authentication, API interaction, and error mapping.
     * 
     * @param orgSlug Organization slug for gateway config lookup
     * @param from Sender email address and name
     * @param to Recipient email addresses
     * @param cc CC recipient email addresses (optional)
     * @param subject Email subject line
     * @param htmlBody Email body in HTML format
     * @return Boolean indicating send success
     * @throws NotFoundException if email gateway not configured
     * @throws ServiceUnavailableException if email gateway fails
     */
    suspend fun sendMail(
        orgSlug: String,
        from: EmailContact,
        to: List<EmailContact>,
        cc: List<EmailContact>?,
        subject: String,
        htmlBody: String
    ): Boolean
}

data class EmailContact(
    val email: String,
    val name: String? = null
)
```

**Implementation Approach**:
```kotlin
class NotificationRepositoryImpl(
    private val notificationGateway: NotificationGateway  // Generic gateway (Mailjet, Slack, etc.)
) : NotificationRepository {
    
    override suspend fun sendMail(
        orgSlug: String,
        from: EmailContact,
        to: List<EmailContact>,
        cc: List<EmailContact>?,
        subject: String,
        htmlBody: String
    ): Boolean {
        // Convert email-specific data to generic notification format
        val destinations = to.map { Destination(identifier = it.email, metadata = mapOf("name" to (it.name ?: ""))) }
        val ccDestinations = cc?.map { Destination(identifier = it.email, metadata = mapOf("name" to (it.name ?: ""))) }
        
        val fromMetadata = mapOf(
            "email" to from.email,
            "name" to (from.name ?: "")
        )
        
        // Send via generic notification gateway
        return notificationGateway.send(
            orgSlug = orgSlug,
            header = subject,
            body = htmlBody,
            destinations = destinations,
            metadata = mapOf(
                "from" to fromMetadata,
                "cc" to (ccDestinations ?: emptyList())
            )
        )
    }
}

// Generic NotificationGateway interface
interface NotificationGateway {
    /**
     * Sends a generic notification via the configured gateway.
     * 
     * @param orgSlug Organization slug for gateway config lookup
     * @param header Notification header (email subject, Slack title, etc.)
     * @param body Notification body content (HTML for email, markdown for Slack, etc.)
     * @param destinations List of notification recipients
     * @param metadata Additional gateway-specific data (from address, cc, priority, etc.)
     * @return Boolean indicating send success
     * @throws NotFoundException if gateway not configured
     * @throws ServiceUnavailableException if gateway fails
     */
    suspend fun send(
        orgSlug: String,
        header: String,
        body: String,
        destinations: List<Destination>,
        metadata: Map<String, Any>? = null
    ): Boolean
}

data class Destination(
    val identifier: String,  // Email address, Slack user ID, phone number, etc.
    val metadata: Map<String, String>? = null  // Name, display name, etc.
)
```

**Route Usage Pattern**:
```kotlin
// Route orchestrates: fetch, group, send per group
for ((organiserContact, partnerships) in groupedByOrganiser) {
    val recipients = partnerships.flatMap { it.emailContacts }.distinct()
    
    val fromContact = organiserContact ?: EmailContact(
        email = event.contactEmail,
        name = event.name
    )
    
    val ccContacts = if (organiserContact != null) {
        listOf(EmailContact(email = event.contactEmail))
    } else null
    
    val success = notificationRepository.sendMail(
        orgSlug = orgSlug,
        from = fromContact,
        to = recipients,  // Already EmailContact objects
        cc = ccContacts,
        subject = "[${event.name}] ${request.subject}",
        htmlBody = request.body
    )
    
    if (!success) throw ServiceUnavailableException("Email service unavailable")
}
```

**Alternatives Considered**:
- **Use IntegrationRepository for sending**: Rejected - IntegrationRepository should only handle integration registration, not sending
- **Routes call NotificationGateway directly**: Rejected - Violates architecture (routes can only depend on repositories)
- **Create separate EmailService**: Rejected - NotificationRepository is the correct abstraction

---

### 4. Repository Layer Responsibility

**Decision**: `PartnershipEmailRepository` handles ONLY data fetching (partnerships + emails). Route handler orchestrates Mailjet integration.

**Rationale**:
- Constitution CRITICAL requirement: Repositories must NOT depend on other repositories
- Email sending is cross-domain concern (partnerships + Mailjet + events + users)
- Route handlers inject multiple repositories and orchestrate operations
- Follows existing patterns in `PartnershipRoutes.kt` for notification sending

**Repository Interface**:
```kotlin
interface PartnershipEmailRepository {
    /**
     * Fetches partnerships with their contact emails and organizer info for bulk emailing.
     * 
     * Returns structured domain objects with EmailContact objects pre-created for:
     * - Recipient email addresses (from company_emails table)
     * - Organizer contact info (from users table via partnership.organiser_user_id)
     * 
     * Uses identical filter logic to PartnershipRepository.listByEvent() to ensure
     * consistency with partnership listing endpoint.
     * 
     * @param eventSlug Event identifier
     * @param filters Partnership filters (validated, paid, etc.)
     * @param direction Sort direction
     * @return List of partnerships with pre-structured email contacts
     */
    suspend fun getPartnershipsWithEmails(
        eventSlug: String,
        filters: PartnershipFilters,
        direction: String
    ): List<PartnershipWithEmails>
}

data class PartnershipWithEmails(
    val partnershipId: UUID,
    val organiserContact: EmailContact?,  // null if no organizer assigned, pre-structured from User
    val emailContacts: List<EmailContact>  // from company_emails table, pre-structured
)
```

**Note**: Email sending moved to `NotificationRepository.sendMail()` - PartnershipEmailRepository only handles data fetching.

**Route Handler Orchestration**:
```kotlin
fun Route.partnershipEmailRoutes() {
    val partnershipEmailRepository by inject<PartnershipEmailRepository>()
    val eventRepository by inject<EventRepository>()
    val notificationRepository by inject<NotificationRepository>()
    
    post("/email") {
        val request = call.receive<SendPartnershipEmailRequest>(SendPartnershipEmailSchema)
        
        // 1. Fetch partnerships with emails
        val partnershipsWithEmails = partnershipEmailRepository.getPartnershipsWithEmails(
            eventSlug = eventSlug,
            filters = filters,
            direction = direction
        )
        
        // 2. Validate recipients exist
        if (partnershipsWithEmails.isEmpty()) {
            throw NotFoundException("No partnerships found matching the filters")
        }
        
        // 3. Get event details
        val event = eventRepository.findBySlug(eventSlug)
            ?: throw NotFoundException("Event not found: $eventSlug")
        
        // 4. Group by organizer contact (business logic in route)
        val groupedByOrganizer = partnershipsWithEmails.groupBy { it.organiserContact }
        
        // 5. Send email batches via NotificationRepository
        var totalRecipients = 0
        
        for ((organiserContact, partnerships) in groupedByOrganizer) {
            val recipients = partnerships.flatMap { it.emailContacts }.distinct()
            if (recipients.isEmpty()) continue
            
            totalRecipients += recipients.size
            
            // Use organizer contact if present, otherwise use event contact
            val fromContact = organiserContact ?: EmailContact(
                email = event.contactEmail,
                name = event.name
            )
            
            val ccContacts = if (organiserContact != null) {
                listOf(EmailContact(email = event.contactEmail))
            } else null
            
            // Send via notification repository
            val success = notificationRepository.sendMail(
                orgSlug = orgSlug,
                from = fromContact,
                to = recipients,  // Already EmailContact objects from repository
                cc = ccContacts,
                subject = "[${event.name}] ${request.subject}",
                htmlBody = request.body
            )
            
            if (!success) {
                throw ServiceUnavailableException("Email service is currently unavailable")
            }
        }
        
        call.respond(HttpStatusCode.OK, SendPartnershipEmailResponse(recipients = totalRecipients))
    }
}
```

**Alternatives Considered**:
- **Routes call email provider directly**: Rejected - Violates architecture (routes can only depend on repositories)
- **Create separate EmailService**: Rejected - Repository abstraction is cleaner and follows existing patterns
- **Repository only fetches, route sends**: Rejected - Violates architecture (routes cannot depend on providers)

---

### 5. HTML Content Validation Strategy

**Decision**: Validate HTML structure via basic well-formedness check, NO sanitization (organizers are trusted).

**Rationale**:
- Spec clarification: Option A selected - trust organizers, no sanitization
- Organizers are authenticated users with edit permissions (not public input)
- HTML sanitization would strip valid formatting (CSS, complex tables, embedded images)
- Mailjet itself handles email-safe HTML rendering

**Implementation Approach**:
```kotlin
// Simple well-formedness validation
fun validateHtmlStructure(html: String): Boolean {
    return try {
        // Basic checks for balanced tags
        val openTags = html.split("<").filter { it.contains(">") }.count()
        val closeTags = html.split("</").filter { it.contains(">") }.count()
        openTags > 0 && closeTags > 0
    } catch (e: Exception) {
        false
    }
}
```

**Validation Requirements**:
- Subject not empty, not exceeding 500 characters (FR-006, FR-008)
- Body not empty (FR-007)
- HTML basic structure check (optional, for user experience)
- All validation via JSON schema where possible

**Alternatives Considered**:
- **Use Jsoup HTML parser**: Rejected - Over-engineering for trust model
- **Whitelist safe tags**: Rejected - Spec explicitly chose no sanitization
- **Preview rendering**: Rejected - Out of scope per spec clarification

---

### 6. Error Handling & HTTP Status Codes

**Decision**: Leverage Ktor StatusPages plugin for consistent error responses, throw domain exceptions in repository/route layers.

**Rationale**:
- Constitution requirement: NO try-catch in route handlers
- StatusPages handles exception-to-HTTP mapping automatically
- Consistent error format across entire API

**Exception Mapping**:
| Exception | HTTP Status | Scenario |
|-----------|-------------|----------|
| `NotFoundException` | 404 Not Found | Event, organization, or Mailjet integration not found; zero partnerships match filters |
| `BadRequestException` | 400 Bad Request | Invalid request body (JSON schema validation failure); subject/body empty; subject too long |
| `ForbiddenException` | 403 Forbidden | User lacks edit permission (caught by AuthorizedOrganisationPlugin) |
| `ServiceUnavailableException` | 503 Service Unavailable | Mailjet API unreachable or returns error; quota exhausted |
| `UnauthorizedException` | 401 Unauthorized | Invalid or missing JWT token (caught by AuthorizedOrganisationPlugin) |

**Quota Exhaustion Handling**:
```kotlin
// In NotificationGateway (Mailjet implementation)
val success = notificationGateway.send(
    orgSlug = orgSlug,
    header = subject,
    body = htmlBody,
    destinations = destinations,
    metadata = metadata
)
// Gateway throws NotFoundException if no integration configured
// Gateway throws ServiceUnavailableException if quota exceeded or API fails
return success
```

**Alternatives Considered**:
- **Return success=false boolean**: Rejected - StatusPages pattern requires exceptions
- **Custom error codes**: Rejected - HTTP status codes are sufficient
- **Retry logic**: Deferred - Operational concern per constitution

---

### 7. JSON Schema Design

**Decision**: Create two external JSON schemas referenced by OpenAPI: request schema and response schema.

**Rationale**:
- Constitution requirement: All request bodies must use `call.receive<T>(schema)` pattern
- OpenAPI components must reference external schema files (not inline definitions)
- Schema files enable automatic validation without manual code

**Request Schema** (`send_partnership_email_request.schema.json`):
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["subject", "body"],
  "properties": {
    "subject": {
      "type": "string",
      "minLength": 1,
      "maxLength": 500,
      "description": "Email subject line (will be prefixed with [Event Name])"
    },
    "body": {
      "type": "string",
      "minLength": 1,
      "description": "Email body in HTML format"
    }
  },
  "additionalProperties": false
}
```

**Response Schema** (`send_partnership_email_response.schema.json`):
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["recipients"],
  "properties": {
    "recipients": {
      "type": "integer",
      "minimum": 1,
      "description": "Total number of unique email recipients across all batches"
    }
  }
}
```

**Alternatives Considered**:
- **Inline schemas in openapi.yaml**: Rejected - Constitution requires external schema files
- **Kotlin data validation**: Rejected - JSON schemas provide better OpenAPI integration
- **Reuse existing schemas**: Rejected - This is a unique request/response pair

---

### 8. Mailjet Quota Error Handling

**Decision**: Return quota exhaustion error messages with best-effort inclusion of quota reset timestamp if Mailjet API provides it in error response.

**Rationale**:
- FR-021a requires specific error messages for quota exhaustion
- Mailjet API may or may not include quota reset timestamp in error responses
- System should provide helpful guidance without depending on timestamp availability
- Best-effort approach balances user experience with API limitations

**Implementation Approach**:
```kotlin
// In NotificationGateway Mailjet implementation
catch (e: MailjetApiException) {
    when {
        e.statusCode == 429 && e.message.contains("quota") -> {
            // Try to extract reset timestamp from error response
            val resetTime = extractQuotaResetTime(e.response)
            val message = if (resetTime != null) {
                "Mailjet sending quota exceeded. Quota resets at $resetTime. Contact your administrator to upgrade your plan."
            } else {
                "Mailjet sending quota exceeded. Contact your administrator to upgrade your plan or wait for quota reset."
            }
            throw ServiceUnavailableException(message)
        }
        // ... other error cases
    }
}
```

**Error Message Variants**:
- With timestamp: "Mailjet sending quota exceeded. Quota resets at 2025-12-20T14:00:00Z. Contact your administrator to upgrade your plan."
- Without timestamp: "Mailjet sending quota exceeded. Contact your administrator to upgrade your plan or wait for quota reset."

**Alternatives Considered**:
- **Always require timestamp**: Rejected - Mailjet API may not provide this in all error scenarios
- **Poll Mailjet for quota status**: Rejected - Adds unnecessary complexity and API calls
- **Generic quota error**: Rejected - Less helpful to organizers who need actionable guidance

---

### 9. Testing Strategy

**Decision**: Write contract tests BEFORE implementation (TDD), separate from integration tests.

**Rationale**:
- Constitution CRITICAL requirement: Contract tests written before implementation
- Contract tests focus on API schema, integration tests on business logic
- H2 in-memory database for all tests (no external dependencies)

**Contract Test Scope**:
- Request schema validation (subject required, maxLength 500, body required)
- Response schema validation (recipients integer ≥1)
- HTTP status codes (200 OK, 400 Bad Request, 404 Not Found, 503 Service Unavailable)
- Parameter extraction (orgSlug, eventSlug)
- Authorization via plugin (no manual checks)

**Integration Test Scope**:
- Email grouping by organizer (multiple batches sent)
- Email deduplication within groups
- From/CC address logic (organizer vs event)
- Empty filter results (404 error)
- No Mailjet integration configured (404 error)
- Mailjet API failure (503 error)

**Mock Factory Usage**:
```kotlin
// Reuse existing factories
val orgId = UUID.randomUUID()
val eventId = UUID.randomUUID()
val companyId = UUID.randomUUID()
val userId = UUID.randomUUID()

insertMockedOrganisationEntity(orgId)
insertMockedEventWithAdminUser(eventId, orgId, "test-event")
insertMockedCompany(companyId)
insertMockedUser(userId, email = "organizer@example.com")
insertMockedPartnership(
    partnershipId = UUID.randomUUID(),
    eventId = eventId,
    companyId = companyId,
    organiserUserId = userId  // Assign organizer
)
insertMockedPartnershipEmail(partnershipId, "contact@company.com")
```

**Alternatives Considered**:
- **Integration tests only**: Rejected - Constitution requires contract tests
- **Manual Mailjet testing**: Rejected - All tests use mocks/stubs
- **Repository unit tests**: Rejected - Constitution prefers HTTP route integration tests

---

## Best Practices Applied

### Kotlin/Ktor Patterns
- **Koin dependency injection**: Route handlers inject all required repositories
- **Ktor plugins**: Use `AuthorizedOrganisationPlugin` for authorization, StatusPages for error handling
- **Suspend functions**: Mailjet API calls are async via `suspend fun`
- **Exposed ORM**: Query partnerships and emails via Exposed entities
- **Extension functions**: Use `call.parameters.eventSlug` for parameter extraction

### Partnership Module Patterns
- **Filter reuse**: Use existing `PartnershipFilters` data class
- **Route naming**: Follow `Partnership{Purpose}Routes.kt` convention
- **Repository interfaces**: Pure data access, no business logic
- **No new tables**: Reuse `partnerships`, `company_emails`, `events`, `users`

### Email/Notification Patterns
- **Template-free**: No notification templates (user provides HTML)
- **Gateway abstraction**: Use existing `NotificationGateway` interface (Mailjet implementation)
- **No audit logging**: Per spec clarification (ephemeral operation)
- **Batch sending**: Group by organizer for consistent From/CC

---

## Dependencies & Prerequisites

### Existing Code (Reused)
- `PartnershipRepository.listByEvent()` - Partnership filtering logic
- `NotificationRepository.sendMail()` - Email sending abstraction (to be enhanced with new function)
- `NotificationGateway` - Generic notification gateway interface (Mailjet, Slack, etc. implement this)
- `EventRepository.findBySlug()` - Event data fetching (called by route)
- `UserRepository` - Organizer data (via partnership relationships)
- `AuthorizedOrganisationPlugin` - Authorization enforcement
- `PartnershipEmailsTable` - Database schema for contact emails

**Note**: Mailjet gateway implementation internally fetches configuration from `mailjet_integrations` table using `orgSlug`.

### External Services
- Mailjet API v3.1 (`/v3.1/send` endpoint)
- PostgreSQL database (partnerships, company_emails, events, users tables)

### Build Tools
- Gradle 8.13+ with `--no-daemon` flag
- ktlint formatting validation
- detekt static analysis
- npm for OpenAPI validation (`npm run validate`)

---

## Implementation Sequence

1. **Phase 0 (Current)**: Research complete
2. **Phase 1 (Next)**: 
   - Create data-model.md
   - Create contracts/ (send-email-api.md, email-grouping-logic.md)
   - Create quickstart.md with manual testing scenarios
3. **Phase 2 (Future - /speckit.tasks)**: 
   - Task breakdown for implementation
   - Estimation and sequencing
   - Acceptance criteria mapping

---

## Open Questions (Resolved)

All technical uncertainties have been resolved through research:
- ✅ Email grouping strategy decided
- ✅ Deduplication approach confirmed
- ✅ Repository responsibilities clarified
- ✅ Mailjet integration pattern established
- ✅ Error handling strategy defined
- ✅ Testing approach documented
- ✅ Schema design completed

**No blockers for Phase 1 design work.**
