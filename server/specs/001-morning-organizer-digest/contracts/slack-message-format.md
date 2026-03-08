# Contract: Slack Message Format — Morning Organiser Daily Digest

**Branch**: `001-morning-organizer-digest` | **Date**: 2026-03-05

## Overview

The morning digest produces one outbound Slack message per event, sent to the event's configured Slack channel via the existing Slack integration. The digest is triggered by the `POST /orgs/{orgSlug}/events/{eventId}/jobs/digest` endpoint (see [internal-trigger-endpoint.md](internal-trigger-endpoint.md)).

---

## Slack Message Templates

Templates live at:
```
application/src/main/resources/notifications/slack/digest/fr.md
application/src/main/resources/notifications/slack/digest/en.md
```

This follows the same folder convention used by every other Slack notification type (e.g. `notifications/slack/partnership_validated/fr.md`). The file is loaded at runtime via `readResourceFile("/notifications/slack/digest/$language.md")`.

### French template (`fr.md`)

```
🌅 *Bonjour ! Voici les actions du jour pour {{event_name}}.*
{{agreement_section}}{{quote_section}}{{social_media_section}}
```

### English template (`en.md`)

```
🌅 *Good morning! Here are today's actions for {{event_name}}.*
{{agreement_section}}{{quote_section}}{{social_media_section}}
```

---

## Template Variables and Section Rendering

`NotificationVariables.MorningDigest.populate()` calls `String.replace()` for each placeholder — the same mechanism used by every other `NotificationVariables` subclass.

| Placeholder | Source | Behaviour when empty |
|-------------|--------|----------------------|
| `{{event_name}}` | `digest.event.event.name` | Always present |
| `{{agreement_section}}` | `buildSection(digest.agreementItems, ...)` | Replaced with `""` — section fully omitted |
| `{{quote_section}}` | `buildSection(digest.quoteItems, ...)` | Replaced with `""` — section fully omitted |
| `{{social_media_section}}` | `buildSection(digest.socialMediaItems, ...)` | Replaced with `""` — section fully omitted |

### Section format (produced by `buildSection()`)

When a list is **non-empty**, `buildSection()` returns a mrkdwn block with a language-specific heading and one Slack link per entry:

**French — agreement:**
```

📄 *Conventions à générer :*
• <https://…/partnerships/uuid-1|ACME Corp>
• <https://…/partnerships/uuid-2|Startup SAS>
```

**French — quote:**
```

💶 *Devis à générer :*
• <https://…/partnerships/uuid-3|BigCo SA>
```

**French — social media:**
```

📢 *Communication prévue aujourd'hui :*
• <https://…/partnerships/uuid-4|DevTools Ltd>
```

**English equivalents** use the same structure with translated headings: *Agreements to generate:*, *Quotes to generate:*, *Scheduled communication today:*.

When a list is **empty**, `buildSection()` returns `""` and the placeholder disappears from the rendered message.

---

## Example Rendered Message (French, all three sections)

```
🌅 *Bonjour ! Voici les actions du jour pour DevLille 2026.*

📄 *Conventions à générer :*
• <https://app.example.com/orgs/devlille/events/devlille-2026/partnerships/uuid-1|ACME Corp>
• <https://app.example.com/orgs/devlille/events/devlille-2026/partnerships/uuid-2|Startup SAS>

💶 *Devis à générer :*
• <https://app.example.com/orgs/devlille/events/devlille-2026/partnerships/uuid-3|BigCo SA>

📢 *Communication prévue aujourd'hui :*
• <https://app.example.com/orgs/devlille/events/devlille-2026/partnerships/uuid-4|DevTools Ltd>
```

---

## `NotificationVariables.MorningDigest`

```kotlin
// notifications/domain/NotificationVariables.kt — new subclass added to the sealed interface
data class MorningDigest(
    override val language: String,
    override val event: EventWithOrganisation,
    val agreementItems: List<DigestEntry>,
    val quoteItems: List<DigestEntry>,
    val socialMediaItems: List<DigestEntry>,
) : NotificationVariables {
    override val usageName: String = "digest"

    // Required by the sealed interface; not used in this template.
    override val company: Company get() = error("Not applicable for MorningDigest")

    override fun populate(content: String): String = content
        .replace("{{event_name}}", event.event.name)
        .replace("{{agreement_section}}", buildSection(agreementItems, language, SectionType.AGREEMENT))
        .replace("{{quote_section}}", buildSection(quoteItems, language, SectionType.QUOTE))
        .replace("{{social_media_section}}", buildSection(socialMediaItems, language, SectionType.SOCIAL_MEDIA))
}
```

The `usageName = "digest"` makes `SlackNotificationGateway.send(integrationId, variables)` automatically load `/notifications/slack/digest/{language}.md` via `readResourceFile()` — the same mechanism used by every other notification type. The route calls `notificationRepository.sendMessageFromMessaging(variables)`, which dispatches through the existing Slack gateway pipeline.

The `buildSection()` helper returns `""` when the list is empty (placeholder disappears), or a mrkdwn block with a language-specific heading and one Slack link per `DigestEntry` (`<{partnershipLink}|{companyName}>`).

---

## Delivery Behaviour

| Condition | Behaviour |
|-----------|-----------|
| `EventDigest.hasItems == false` | Route handler skips `sendMessageFromMessaging`; no Slack call is made |
| Slack integration not configured for event | `NotificationRepositoryExposed.sendMessageFromMessaging` throws `NotFoundException`; StatusPages maps to HTTP 404 |
| Slack API error | `SlackNotificationGateway` returns `SlackDeliveryResult(FAILED)`; route returns 204 (fire-and-forget, no retry) |
| All sections empty after filtering | Same as `hasItems == false` |
