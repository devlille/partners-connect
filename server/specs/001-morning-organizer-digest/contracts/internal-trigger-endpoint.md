# Contract: Internal Trigger Endpoint — Morning Organiser Daily Digest

**Branch**: `001-morning-organizer-digest` | **Date**: 2026-03-06

## Overview

A single HTTP endpoint allows an external scheduler to trigger the morning digest at a configured time (08:00 UTC). The application contains no built-in scheduler; the schedule is fully owned and observable by the external caller.

---

## Endpoint

```
POST /orgs/{orgSlug}/events/{eventId}/jobs/digest
```

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `orgSlug` | `String` | Organisation slug |
| `eventId` | `UUID` | Event identifier |

### Request Headers

| Header | Required | Values | Default |
|--------|----------|--------|---------|
| `Accept-Language` | No | `fr`, `en` | `fr` (any absent or unrecognised value falls back to `fr`) |

No body required.

### Responses

| Status | Condition |
|--------|-----------|
| `204 No Content` | Digest ran successfully. A Slack message was sent if there were actionable items; nothing was sent if there were none. |
| `404 Not Found` | Event does not exist. |

---

## Implementation

### Route (`DigestRoutes.kt`)

The route handler injects repositories directly and orchestrates calls — no service layer, per the clean architecture constitution.

```kotlin
// Registered in App.kt under routing {}
fun Application.digestRoutes() {
    routing {
        post("/orgs/{orgSlug}/events/{eventId}/jobs/digest") {
            val digestRepository by inject<DigestRepository>()
            val notificationRepository by inject<NotificationRepository>()

            val eventId = call.parameters.eventId
            val language = when (call.request.headers[HttpHeaders.AcceptLanguage]) {
                "en" -> "en"
                else -> "fr"
            }
            val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
            val digest = digestRepository.queryDigest(eventId, today)  // throws NotFoundException if event missing
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
    }
}
```

---

## External Scheduler Configuration Examples

### cURL (testing / manual trigger)

```bash
# French digest (default — header can be omitted)
curl -X POST https://your-server.example.com/orgs/devlille/events/{eventId}/jobs/digest \
  -H "Accept-Language: fr"

# English digest
curl -X POST https://your-server.example.com/orgs/devlille/events/{eventId}/jobs/digest \
  -H "Accept-Language: en"
```

### GitHub Actions scheduled workflow

```yaml
on:
  schedule:
    - cron: '0 8 * * *'   # 08:00 UTC every day
jobs:
  trigger-digest:
    runs-on: ubuntu-latest
    steps:
      - run: curl -X POST ${{ secrets.SERVER_URL }}/orgs/${{ vars.ORG_SLUG }}/events/${{ vars.EVENT_ID }}/jobs/digest
```

### Heroku Scheduler / EasyCron / any HTTP cron service

Configure one `POST` request per event to `https://your-server.example.com/orgs/{orgSlug}/events/{eventId}/jobs/digest` at 08:00 UTC.

---

## Test Contract

```kotlin
@Test
fun `POST digest for event with actionable items sends Slack message and returns 204`() = testApplication {
    application { moduleSharedDb(userId = userId) /* + test data */ }
    
    client.post("/orgs/$orgSlug/events/$eventId/jobs/digest").apply {
        assertEquals(HttpStatusCode.NoContent, status)
    }
}

@Test
fun `POST digest for unknown event returns 404`() = testApplication {
    application { moduleSharedDb(userId = userId) }
    
    client.post("/orgs/devlille/events/${UUID.randomUUID()}/jobs/digest").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}
```
