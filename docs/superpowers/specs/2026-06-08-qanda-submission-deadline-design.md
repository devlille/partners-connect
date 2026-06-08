# Q&A Submission Deadline — Design

**Date:** 2026-06-08
**Status:** Approved (pending spec review)

## Problem

Organisers want to stop partners from changing their Scanzee Q&A content after a
cutoff. Today Q&A submission is gated only by `qanda_enabled` and the
max-questions / max-answers limits — there is no time boundary. We need:

- An **organiser-side configuration**: an optional date+hour after which Q&A is
  closed for an event.
- A **partner-side effect**: once the deadline has passed, the partner can no
  longer add, edit, or delete questions, and sees a clear message explaining why.

## Decisions (resolved during brainstorming)

1. **Message:** a standard, built-in (hardcoded French) message that includes the
   formatted deadline — *not* an organiser-authored custom message. Keeps the data
   model lean (deadline only, no message column) and matches the existing
   `scanzee.vue` / `EventForm.vue` convention, which use literal French strings
   rather than the i18n `mergeLocaleMessage` machinery.
2. **Scope:** **frozen at deadline** — create, edit, *and* delete are all blocked
   once the deadline passes. Partners can still view their questions.
3. **Storage model:** a **dedicated, optional** Q&A deadline on the event config
   (nullable). `null` = no deadline = today's behaviour (fully backward compatible).
   Rejected alternatives: reusing the general `submission_end_time` (wrong
   semantics — that window governs partnership material, not Q&A), and a
   per-partnership deadline (all Q&A config is event-level today).
4. **Schema scope:** **close the pre-existing gap.** `qanda_config` is read by the
   front but was never added to the OpenAPI *response* schema, so
   `EventDisplaySchema` is untyped for it. We add a proper response
   `qanda_config.schema.json` (covering `max_questions`, `max_answers`,
   `submission_deadline`) and regenerate the client so the field is typed and
   documented end-to-end.

## Timezone convention (important)

The deadline is stored as a `datetime` (`LocalDateTime`), exactly like
`submission_start_time` / `submission_end_time`. Enforcement compares it against
`Clock.System.now().toLocalDateTime(TimeZone.UTC)` — the same UTC basis used by
`EventsTable.createdAt`. There is no per-event timezone in the system today, so the
organiser effectively enters a **UTC** cutoff. This matches existing behaviour for
all other event datetimes; introducing an event timezone is explicitly out of scope.

Submissions are allowed **up to and including** the deadline instant; strictly
after is blocked (`now > deadline`).

## Architecture & data flow

```
Organiser EventForm (datetime-local)
  → create.vue / information.vue map nested qanda_config → flat CreateEventSchema
  → PUT/POST /orgs/{orgSlug}/events/{eventSlug}  (Event input model, flat field)
  → EventRepositoryExposed.updateEvent writes EventEntity.qandaSubmissionDeadline
  → EventsTable column qanda_submission_deadline (datetime, nullable)

Partner scanzee.vue
  → getEventsPartnershipDetailed → DetailedPartnershipResponse.event (EventDisplaySchema)
  → event.qanda_config.submission_deadline  (nested output field)
  → usePublicPartnership exposes qandaSubmissionDeadline
  → isDeadlinePassed → disable all mutations + show message

Server enforcement (authoritative, defence-in-depth)
  → QandaRepositoryExposed.create / update / delete
  → verifyDeadlineNotPassed(event) → 403 ForbiddenException if past
```

The server is the authoritative gate (returns `403`); the frontend disabled-state
is the friendly mirror. A bypassed client still gets a clear error because the
existing scanzee save/delete handlers surface `err.response.data.message`.

---

## Changes — Server (Kotlin / Ktor / Exposed)

All paths under
`server/application/src/main/kotlin/fr/devlille/partners/connect/`.

### Data model (infrastructure/db)
- **`events/infrastructure/db/EventsTable.kt`** — add
  `val qandaSubmissionDeadline = datetime("qanda_submission_deadline").nullable()`.
- **`events/infrastructure/db/EventEntity.kt`** — add
  `var qandaSubmissionDeadline by EventsTable.qandaSubmissionDeadline`.

### Domain models
- **`events/domain/Event.kt`** (flat *input* model) — add
  `@SerialName("qanda_submission_deadline") val qandaSubmissionDeadline: LocalDateTime? = null`.
- **`events/domain/QandaConfig.kt`** (nested *output* model) — add
  `@SerialName("submission_deadline") val submissionDeadline: LocalDateTime? = null`.

### Write / read paths (application)
- **`events/application/EventRepositoryExposed.kt`**
  - `updateEvent`: when `qandaEnabled`, set
    `eventEntity.qandaSubmissionDeadline = event.qandaSubmissionDeadline` (optional —
    no required-check, unlike the max fields); when disabled, set it to `null`
    alongside the maxes.
  - `EventDisplay` mapping: pass
    `submissionDeadline = eventEntity.qandaSubmissionDeadline` into the
    `QandaConfig(...)` it already builds (the `if (eventEntity.qandaEnabled)` branch).

### Enforcement (application)
- **`partnership/application/QandaRepositoryExposed.kt`** — add a private helper and
  call it in all three mutating methods:

  ```kotlin
  // imports: kotlin.time.Clock, kotlinx.datetime.TimeZone, kotlinx.datetime.toLocalDateTime
  private fun verifyDeadlineNotPassed(event: EventEntity) {
      val deadline = event.qandaSubmissionDeadline ?: return
      val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
      if (now > deadline) {
          throw ForbiddenException("Q&A submission deadline has passed")
      }
  }
  ```

  - `create`: call after the `qandaEnabled` check, before `validateAnswers`.
  - `update`: call on the already-fetched `eventEntity`.
  - `delete`: assign the fetched partnership to a `val` and call
    `verifyDeadlineNotPassed(partnership.event)` (uses the existing
    `PartnershipEntity.event` relation — **no interface/route signature change**).

  `ForbiddenException` is already imported and maps to HTTP `403` via StatusPages.
  `listByPartnership` / `listByEvent` stay unblocked so partners and organisers can
  still view the frozen set.

### OpenAPI schemas (resources)
Paths under `server/application/src/main/resources/`.
- **`schemas/create_event.schema.json`** (input; `$id` is `event.schema.json`) — add:
  ```json
  "qanda_submission_deadline": { "type": ["string", "null"], "format": "datetime" }
  ```
  (stays out of `required`; `format: "datetime"` matches the other date fields in
  this codebase — *not* `date-time`).
- **`schemas/qanda_config.schema.json`** — new response schema:
  ```json
  {
    "$id": "qanda_config.schema.json",
    "type": "object",
    "properties": {
      "max_questions": { "type": "integer", "minimum": 1 },
      "max_answers": { "type": "integer", "minimum": 2 },
      "submission_deadline": { "type": ["string", "null"], "format": "datetime" }
    },
    "required": ["max_questions", "max_answers"]
  }
  ```
- **`schemas/event_display.schema.json`** — add a nullable `qanda_config` property
  (kept out of `required`, null when Q&A disabled):
  ```json
  "qanda_config": {
    "anyOf": [
      { "$ref": "qanda_config.schema.json" },
      { "type": "null" }
    ]
  }
  ```
  This single change flows to both the organiser edit page and the partner page,
  because `DetailedPartnershipResponseSchema.event` reuses `EventDisplaySchema`.
- **`openapi/openapi.yaml`** — register the new schema under `components/schemas`:
  ```yaml
  QandaConfig:
    $ref: "../schemas/qanda_config.schema.json"
  ```
  (so Orval emits a named `QandaConfigSchema`). No runtime loader registration in
  `ApplicationCall.ext.kt` is needed — `qanda_config` is response-only; only request
  schemas validated via `call.receive(schema = …)` are registered, and
  `create_event.schema.json` is already registered.

### Validation workflow (server/)
After server + schema edits: `npm run validate` → `./gradlew check --no-daemon`
→ `npm run bundle` (regenerates `documentation.yaml`; never hand-edited).

---

## Changes — Frontend (Nuxt 4 / Vue 3 / Pinia)

All paths under `front/`.

### Generated client
- Regenerate **`utils/api.ts`** from the local bundled spec (via the
  `regenerate-front-api` skill). Expected output:
  - `CreateEventSchema` gains `qanda_submission_deadline?: string | null`.
  - New `QandaConfigSchema { max_questions; max_answers; submission_deadline?: string | null }`.
  - `EventDisplaySchema` gains `qanda_config?: QandaConfigSchema | null` — which makes
    the existing `event.qanda_config` / `data.qanda_config` reads type-safe.

### Organiser config — `components/EventForm.vue`
- Add `qanda_submission_deadline: string | null` to `FormState`, initialised from
  `data.qanda_config?.submission_deadline ?? null`.
- Add a `datetime-local` `UInput` inside the existing `v-if="form.qanda_enabled"`
  block, labelled *"Date limite de soumission (optionnel)"*, bound with the same
  empty-string-to-null pattern used by the max fields.
- In `onSave`, include `submission_deadline` in the emitted `qanda_config` object
  when enabled.

### Organiser pages — map nested → flat
- **`pages/orgs/[slug]/events/create.vue`** and
  **`pages/orgs/[slug]/events/[eventSlug]/information.vue`** — in each `onSave`'s
  `eventPayload`, add
  `qanda_submission_deadline: <payload>.qanda_config?.submission_deadline ?? null`
  next to the existing `qanda_max_*` mappings.
- `create.vue` `initialData` already sets `qanda_config: null` — no change needed.

### Partner state — `composables/usePublicPartnership.ts`
- Add `const qandaSubmissionDeadline = useState<string | null>("qanda-submission-deadline", () => null)`.
- In `loadPartnership`, set
  `qandaSubmissionDeadline.value = event.qanda_config?.submission_deadline ?? null`.
- Export it.

### Partner page — `pages/[eventSlug]/[partnershipId]/scanzee.vue`
- Pull `qandaSubmissionDeadline` from the composable.
- `const isDeadlinePassed = computed(() => { const d = qandaSubmissionDeadline.value; return d ? new Date() > new Date(d) : false })`
  (matches the server's strictly-after rule; this client check is approximate UX —
  the server is authoritative).
- Add a `UAlert` (or styled banner consistent with the page) at the top when
  `isDeadlinePassed`, with a hardcoded French message interpolating the formatted
  deadline, e.g. *"La période de soumission est terminée (depuis le {date}). Vous ne
  pouvez plus ajouter, modifier ou supprimer de questions."* (format via
  `toLocaleString('fr-FR', …)`).
- Disable every mutating control when `isDeadlinePassed`, combined with their
  existing `:disabled` conditions: "Ajouter une question", per-answer "Ajouter",
  question/answer `UInput`s, answer `UCheckbox`es, "Sauvegarder", remove-answer and
  delete-question buttons.

---

## Testing

### Server contract tests (`partnership/infrastructure/api/`)
Q&A routes are public (no auth); the deadline is a business rule → `403`.
- Extend the event factories so deadline scenarios can be built:
  **`events/factories/EventEntity.factory.kt`** — add defaulted params to
  `insertMockedFutureEvent` (and `insertMockedPastEvent` if useful):
  `qandaEnabled: Boolean = false`, `qandaMaxQuestions: Int? = null`,
  `qandaMaxAnswers: Int? = null`, `qandaSubmissionDeadline: LocalDateTime? = null`,
  writing them onto the entity.
- Add deadline cases to the Q&A create/update/delete contract tests:
  - past deadline → `403` on create, update, delete;
  - future deadline (or `null`) → success path unaffected.

### Event config contract tests (`events/infrastructure/api/`)
- Extend the existing event PUT/GET tests
  (`EventQandaConfigRoutePut/GetTest`) to assert the deadline round-trips:
  set `qanda_submission_deadline` on PUT, read `qanda_config.submission_deadline`
  back on GET; and that it's `null` when Q&A is disabled.

### Schema / build
- `npm run validate` passes; `./gradlew check` passes; `npm run bundle` regenerates
  `documentation.yaml`.

## Backward compatibility & edge cases
- Deadline `null` → no enforcement; existing events behave exactly as before.
- New nullable DB column → no migration risk for existing rows (defaults to null).
- `qanda_config` newly added to the response schema is non-`required` and nullable,
  so existing consumers are unaffected; it only *adds* a (previously untyped) field.
- A past deadline set by the organiser immediately closes Q&A (intended — lets an
  organiser close early).

## Out of scope
- Per-event timezones (UTC convention retained, consistent with all event datetimes).
- Organiser-authored custom close messages.
- i18n migration of the Scanzee pages (they remain hardcoded French).
- Per-partnership deadlines.
