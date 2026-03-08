# Research: Morning Organiser Daily Digest

**Branch**: `001-morning-organizer-digest` | **Date**: 2026-03-05

All [NEEDS CLARIFICATION] items from the spec and Technical Context are resolved below.

---

## Decision 1 — Scheduling Mechanism

**Question**: How should the 08:00 UTC daily job be scheduled? Options: in-process `kotlinx-coroutines` delay loop, `java.util.concurrent.ScheduledExecutorService`, Quartz Scheduler, OS-level cron, or an external HTTP trigger.

**Decision**: Expose a dedicated HTTP endpoint (`POST /orgs/{orgSlug}/events/{eventId}/jobs/digest`) that an external scheduler service calls at 08:00 UTC, once per event. The application contains no built-in scheduler.

**Rationale**:
- An in-process coroutine delay loop couples the schedule to the JVM process lifetime. On infrastructure platforms that can scale to zero or restart containers, the loop silently stops firing — a failure mode that is invisible and hard to alert on.
- An HTTP trigger makes the schedule an explicit, observable, independently configurable concern. The external caller (a cloud scheduler, cron job, CI pipeline, etc.) can be monitored, retried, and modified without a server redeployment.
- The endpoint is stateless: a single `POST` with the event ID in the path is all that is needed, making it trivially testable with `curl` during development.
- This aligns with the 12-factor app principle of separating admin/scheduled processes from the web process by treating them as individually invocable actions.
- All in-process alternatives (coroutine loop, `ScheduledExecutorService`, Quartz) were rejected because they share the same infrastructure fragility and add complexity without observability benefits.

**Endpoint**:
```
POST /orgs/{orgSlug}/events/{eventId}/jobs/digest

Headers:
  Accept-Language: fr   (optional; accepted values: "fr", "en"; defaults to "fr" if absent or unrecognised)

Response: 204 No Content (digest sent, or no items found — either way succeeds)
          404 Not Found   (event does not exist)
```

**Implementation sketch**:
```kotlin
// DigestRoutes.kt — registered under routing {}
post("/orgs/{orgSlug}/events/{eventId}/jobs/digest") {
    val digestRepository by inject<DigestRepository>()
    val notificationRepository by inject<NotificationRepository>()

    val eventId = call.parameters.eventId   // UUID extension, throws on missing
    val language = when (call.request.headers[HttpHeaders.AcceptLanguage]) {
        "en" -> "en"
        else -> "fr"   // default for any absent or unrecognised value
    }
    val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    val digest = digestRepository.queryDigest(eventId, today)  // throws NotFoundException if event not found
    if (digest.hasItems) {
        val variables = NotificationVariables.MorningDigest(
            language = language,
            event = digest.event,
            agreementItems = digest.agreementItems,
            quoteItems = digest.quoteItems,
            socialMediaItems = digest.socialMediaItems,
        )
        notificationRepository.sendMessageFromMessaging(variables)
    }
    call.respond(HttpStatusCode.NoContent)
}
```

**Testability**: The route is tested via `testApplication`. Date pinning is achieved by inserting partnerships with `communicationPublicationDate` set to the desired test date.

---

## Decision 2 — Digest Notification Architecture

**Question**: Should the digest reuse the existing `NotificationRepository` / `NotificationVariables` pipeline, or introduce a dedicated notification repository?

**Decision**: Add `NotificationVariables.MorningDigest` as a new subclass of the existing sealed interface. The route builds a `MorningDigest` instance and calls `notificationRepository.sendMessageFromMessaging(variables)`. No new repository or gateway changes are needed.

**Rationale**:
- `sendMessageFromMessaging(variables)` already handles the full Slack dispatch pipeline: it looks up the Slack integration for the event, calls `slackGateway.send(integrationId, variables)`, which loads `/notifications/slack/{usageName}/{language}.md` via `readResourceFile()`, then calls `variables.populate(content)`. Adding a new subclass is the idiomatic extension point.
- `MorningDigest.usageName = "digest"` causes the gateway to load `notifications/slack/digest/{language}.md` automatically — no custom loading code required.
- `MorningDigest.populate()` performs the same `{{variable}}` substitution as all other subclasses, building the three section strings inline from its `DigestEntry` lists.
- The sealed interface requires a `company: Company` property. The digest is event-scoped and does not reference a single company; `populate()` does not use it. The property is implemented as `error("Not applicable for MorningDigest")` so any accidental access fails loudly.
- The `SlackNotificationGateway.send(integrationId, header, body, destination)` TODO overload does **not** need to be implemented for this feature.

**Model change** (`notifications/domain/NotificationVariables.kt`):
```kotlin
data class MorningDigest(
    override val language: String,
    override val event: EventWithOrganisation,
    val agreementItems: List<DigestEntry>,
    val quoteItems: List<DigestEntry>,
    val socialMediaItems: List<DigestEntry>,
) : NotificationVariables {
    override val usageName: String = "digest"

    // Sealed interface requires company; morning digest is event-scoped and does not use it.
    override val company: Company get() = error("Not applicable for MorningDigest")

    override fun populate(content: String): String = content
        .replace("{{event_name}}", event.event.name)
        .replace("{{agreement_section}}", buildSection(agreementItems, language, SectionType.AGREEMENT))
        .replace("{{quote_section}}", buildSection(quoteItems, language, SectionType.QUOTE))
        .replace("{{social_media_section}}", buildSection(socialMediaItems, language, SectionType.SOCIAL_MEDIA))
}
```

The `buildSection()` helper is a top-level function in the same file (or a companion). It returns `""` when the list is empty, or a mrkdwn block with a language-specific heading and one Slack link per `DigestEntry` (`<{partnershipLink}|{companyName}>`).

---

## Decision 3 — Slack Message Language

**Question**: The existing notification templates have `fr.md` and `en.md` per notification type, selected by `partnership.language`. The digest is per-event, not per-partnership. Which language should the digest use?

**Decision**: Use `fr` as the default language for the digest. Produce both `fr.md` and `en.md` template files at `application/src/main/resources/notifications/slack/digest/`; select based on the `Accept-Language` request header, defaulting to `fr` if absent or unrecognised.

**Rationale**:
- The application's primary audience is French-speaking event organisers (devlille, etc.).
- Both template files are created so the infrastructure is consistent and ready for English callers.
- The language is read directly from the `Accept-Language` HTTP request header by the route handler. No database access is needed to determine the language; the caller supplies it.
- The language string directly maps to the template file name: `"fr"` → `/notifications/slack/digest/fr.md`, `"en"` → `/notifications/slack/digest/en.md`. This matches the exact same file-selection pattern used by `SlackNotificationGateway.send(integrationId, variables)` for all other notification types.
- `NotificationVariables.MorningDigest` receives the `language` parameter from the route (read from the `Accept-Language` request header, defaulting to `"fr"`), loads the template via `readResourceFile("/notifications/slack/digest/$language.md")`, and performs `{{variable}}` substitution inline, mirroring how every other `NotificationVariables` subclass works.

---

## Decision 4 — `communicationPublicationDate` Column Type

**Question**: `PartnershipsTable.communicationPublicationDate` is defined as `datetime()` in the schema (maps to `LocalDateTime`), but semantically it represents a calendar date. How should "today" matching be implemented?

**Decision**: Compare only the date portion: `communicationPublicationDate.date == today` where `today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date`.

**Rationale**:
- The column stores a `LocalDateTime`. The time component is irrelevant for the social-media reminder; only the date portion is meaningful.
- Extracting `.date` (a `LocalDate`) from the `LocalDateTime` and comparing it to today's UTC date is the idiomatic kotlinx.datetime approach.
- This avoids a time-of-day dependency and correctly handles any partnerships where the datetime was stored at midnight or at any other time.

**Exposed query pattern**:
```kotlin
val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
// In the Exposed query:
PartnershipsTable.communicationPublicationDate.date() eq today
// or via kotlin-datetime Exposed extension
```

---

## Field Verification (Schema Audit)

All fields referenced in the spec have been confirmed to exist in the current schema:

| Spec Reference | Table | Column | Kotlin Type | Nullable |
|---|---|---|---|---|
| `agreement_url` | `partnerships` | `agreementUrl` | `String` | Yes |
| `validated_at` | `partnerships` | `validatedAt` | `LocalDateTime` | Yes |
| `declined_at` | `partnerships` | `declinedAt` | `LocalDateTime` | Yes |
| `communication_publication_date` | `partnerships` | `communicationPublicationDate` | `LocalDateTime` | Yes |
| `contact_name` | `partnerships` | `contactName` | `String` | No |
| `contact_role` | `partnerships` | `contactRole` | `String` | No |
| `quote_pdf_url` | `billings` | `quotePdfUrl` | `String` | Yes |
| `start_time` | `events` | `startTime` | `LocalDateTime` | No |
| `name` | `companies` | `name` | `String` | No |
| `siret` | `companies` | `siret` | `String` | Yes |
| `address` | `companies` | `address` | `String` | Yes |
| `zip_code` | `companies` | `zipCode` | `String` | Yes |
| `city` | `companies` | `city` | `String` | Yes |
| `country` | `companies` | `country` | `String(2)` | Yes |

**Note on agreement readiness — `siret` nullable**: The spec requires `siret` to be non-null for agreement and quote readiness. The column is nullable in the DB. The readiness query must include `siret IS NOT NULL` as a filter condition.

**Note on partnership link**: The `partnership.link(event)` helper function is defined in `Partnership.kt` and uses `SystemVarEnv.frontendBaseUrl`. The `DigestEntry.partnershipLink` field should be built the same way, using the partnership UUID and the event slugs.
