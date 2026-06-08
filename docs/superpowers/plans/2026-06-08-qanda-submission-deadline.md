# Q&A Submission Deadline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let organisers set an optional Q&A submission deadline on an event; once it passes, partners can no longer create, edit, or delete Scanzee questions and see a message explaining why.

**Architecture:** A nullable `qanda_submission_deadline` datetime is added to the event config. The server is the authoritative gate — `QandaRepositoryExposed` rejects create/update/delete with `403` once the deadline passes (compared in UTC, matching `created_at`). The frontend mirrors this with a disabled state + message. The pre-existing untyped `qanda_config` response is closed by adding a proper response schema so the field is typed end-to-end.

**Tech Stack:** Kotlin/Ktor + Exposed ORM (server), Redocly/OpenAPI 3.1 + Orval (contract), Nuxt 4 / Vue 3 / Pinia + @nuxt/ui (front).

**Spec:** `docs/superpowers/specs/2026-06-08-qanda-submission-deadline-design.md`

**Conventions:** Server skills `exposed-entities`, `openapi-schemas`, `contract-tests`, `api-routing`, `clean-architecture`, `test-factories`; front skill `regenerate-front-api`. Run server commands from `server/`. `documentation.yaml` is generated — never hand-edit.

**Refinement vs spec:** The spec suggested extending `insertMockedFutureEvent` with Q&A params. The existing Q&A tests instead set `event.qandaEnabled = true` directly on the entity after creation, so this plan sets `event.qandaSubmissionDeadline` inline the same way and leaves the entity factory untouched. Only the `createEvent` *domain* factory gains the new parameter (it builds the PUT request body).

---

## File map

**Server (modify):**
- `events/infrastructure/db/EventsTable.kt` — add column
- `events/infrastructure/db/EventEntity.kt` — add property
- `events/domain/Event.kt` — add flat input field
- `events/domain/QandaConfig.kt` — add nested output field
- `events/application/EventRepositoryExposed.kt` — write (updateEvent + createEvent) & read (EventDisplay mapping)
- `partnership/application/QandaRepositoryExposed.kt` — deadline enforcement
- `application/src/main/resources/schemas/create_event.schema.json` — input field
- `application/src/main/resources/schemas/qanda_config.schema.json` — **new** response schema
- `application/src/main/resources/schemas/event_display.schema.json` — reference qanda_config
- `application/src/main/resources/openapi/openapi.yaml` — register `QandaConfig` component

**Server tests (modify):**
- `events/factories/Event.factory.kt` — add `qandaSubmissionDeadline` param
- `events/infrastructure/api/EventQandaConfigRoutePutTest.kt` — PUT accepts deadline
- `events/infrastructure/api/EventQandaConfigRouteGetTest.kt` — GET returns deadline
- `partnership/infrastructure/api/QandaQuestionRoutePostTest.kt` — 403 after deadline
- `partnership/infrastructure/api/QandaQuestionRoutePutTest.kt` — 403 after deadline
- `partnership/infrastructure/api/QandaQuestionRouteDeleteTest.kt` — 403 after deadline

**Front (modify):**
- `front/utils/api.ts` — regenerated
- `front/components/EventForm.vue` — deadline input
- `front/pages/orgs/[slug]/events/create.vue` — map deadline → payload
- `front/pages/orgs/[slug]/events/[eventSlug]/information.vue` — map deadline → payload
- `front/composables/usePublicPartnership.ts` — expose deadline
- `front/pages/[eventSlug]/[partnershipId]/scanzee.vue` — disabled state + message

---

## Task 1: Add the database column and entity property

**Files:**
- Modify: `server/application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/db/EventsTable.kt`
- Modify: `server/application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/db/EventEntity.kt`

- [ ] **Step 1: Add the column to `EventsTable`**

In `EventsTable.kt`, add the new column immediately after the `qandaMaxAnswers` line (line 28):

```kotlin
    val qandaMaxAnswers = integer("qanda_max_answers").nullable()
    val qandaSubmissionDeadline = datetime("qanda_submission_deadline").nullable()
```

(`datetime` is already imported via `org.jetbrains.exposed.v1.datetime.datetime`.)

- [ ] **Step 2: Add the property to `EventEntity`**

In `EventEntity.kt`, add the delegated property next to the other `qanda*` properties:

```kotlin
    var qandaSubmissionDeadline by EventsTable.qandaSubmissionDeadline
```

- [ ] **Step 3: Compile**

Run: `cd server && ./gradlew compileKotlin --no-daemon`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add server/application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/db/EventsTable.kt server/application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/db/EventEntity.kt
git commit -m "feat(server): add qanda_submission_deadline column to events"
```

---

## Task 2: Add the deadline to the domain models

**Files:**
- Modify: `server/application/src/main/kotlin/fr/devlille/partners/connect/events/domain/Event.kt`
- Modify: `server/application/src/main/kotlin/fr/devlille/partners/connect/events/domain/QandaConfig.kt`

- [ ] **Step 1: Add the flat input field to `Event`**

In `Event.kt`, add after the `qandaMaxAnswers` field (keep `@SerialName`, default null):

```kotlin
    @SerialName("qanda_max_answers")
    val qandaMaxAnswers: Int? = null,
    @SerialName("qanda_submission_deadline")
    val qandaSubmissionDeadline: LocalDateTime? = null,
```

(`LocalDateTime` is already imported in this file via `kotlinx.datetime.LocalDateTime`.)

- [ ] **Step 2: Add the nested output field to `QandaConfig`**

Replace the body of `QandaConfig.kt` with:

```kotlin
package fr.devlille.partners.connect.events.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QandaConfig(
    @SerialName("max_questions")
    val maxQuestions: Int,
    @SerialName("max_answers")
    val maxAnswers: Int,
    @SerialName("submission_deadline")
    val submissionDeadline: LocalDateTime? = null,
)
```

- [ ] **Step 3: Compile**

Run: `cd server && ./gradlew compileKotlin --no-daemon`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add server/application/src/main/kotlin/fr/devlille/partners/connect/events/domain/Event.kt server/application/src/main/kotlin/fr/devlille/partners/connect/events/domain/QandaConfig.kt
git commit -m "feat(server): add submission deadline to event Q&A domain models"
```

---

## Task 3: Persist & expose the deadline (TDD)

**Files:**
- Modify: `server/application/src/test/kotlin/fr/devlille/partners/connect/events/factories/Event.factory.kt`
- Modify: `server/application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/EventQandaConfigRoutePutTest.kt`
- Modify: `server/application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/EventQandaConfigRouteGetTest.kt`
- Modify: `server/application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventRepositoryExposed.kt`

- [ ] **Step 1: Add the deadline param to the `createEvent` domain factory**

In `Event.factory.kt`, add the parameter and wire it into the `Event(...)` constructor:

```kotlin
@Suppress("LongParameterList")
fun createEvent(
    name: String = "DevLille 2025",
    startTime: LocalDateTime = LocalDateTime.parse("2025-06-13T18:00:00"),
    endTime: LocalDateTime = LocalDateTime.parse("2025-06-12T09:00:00"),
    submissionStartTime: LocalDateTime = LocalDateTime.parse("2025-01-01T00:00:00"),
    submissionEndTime: LocalDateTime = LocalDateTime.parse("2025-03-01T23:59:59"),
    address: String = "Lille Grand Palais, Lille, France",
    phone: String = "+33 6 12 34 56 78",
    email: String = "contact@mail.com",
    qandaEnabled: Boolean = false,
    qandaMaxQuestions: Int? = null,
    qandaMaxAnswers: Int? = null,
    qandaSubmissionDeadline: LocalDateTime? = null,
): Event = Event(
    name = name,
    startTime = startTime,
    endTime = endTime,
    submissionStartTime = submissionStartTime,
    submissionEndTime = submissionEndTime,
    address = address,
    contact = Contact(phone = phone, email = email),
    qandaEnabled = qandaEnabled,
    qandaMaxQuestions = qandaMaxQuestions,
    qandaMaxAnswers = qandaMaxAnswers,
    qandaSubmissionDeadline = qandaSubmissionDeadline,
)
```

- [ ] **Step 2: Write the failing PUT test (accepts the deadline)**

In `EventQandaConfigRoutePutTest.kt`, add this test method inside the class:

```kotlin
    @Test
    fun `PUT updates event with Q&A submission deadline and returns 200`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val event = createEvent(
            qandaEnabled = true,
            qandaMaxQuestions = 3,
            qandaMaxAnswers = 4,
            qandaSubmissionDeadline = kotlinx.datetime.LocalDateTime.parse("2026-12-01T18:30:15"),
        )
        val response = client.put("/orgs/$orgId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(event))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
```

- [ ] **Step 3: Write the failing GET test (returns the deadline)**

In `EventQandaConfigRouteGetTest.kt`, add this test method inside the class:

```kotlin
    @Test
    fun `GET event returns qanda_config submission_deadline when set`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 3
                event.qandaMaxAnswers = 4
                event.qandaSubmissionDeadline =
                    kotlinx.datetime.LocalDateTime.parse("2026-12-01T18:30:15")
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val response = client.get("/events/$eventId")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val eventObj = body["event"]!!.jsonObject
        val qandaConfig = eventObj["qanda_config"]?.jsonObject
        assertNotNull(qandaConfig)
        assertTrue(qandaConfig["submission_deadline"].toString().contains("2026-12-01T18:30:15"))
    }
```

- [ ] **Step 4: Run the new tests to verify they fail**

Run: `cd server && ./gradlew test --no-daemon --tests "fr.devlille.partners.connect.events.infrastructure.api.EventQandaConfigRouteGetTest" --tests "fr.devlille.partners.connect.events.infrastructure.api.EventQandaConfigRoutePutTest"`
Expected: the GET test FAILS (`submission_deadline` absent from `qanda_config`). The PUT test passes (the field is currently ignored, so 200 still returns) — that is acceptable; it locks the contract.

- [ ] **Step 5: Write the deadline on update and create**

In `EventRepositoryExposed.kt` `updateEvent` (the `if (event.qandaEnabled)` / `else` block, lines ~185-195), set the deadline in both branches:

```kotlin
        eventEntity.qandaEnabled = event.qandaEnabled
        if (event.qandaEnabled) {
            val maxQuestions = event.qandaMaxQuestions
                ?: throw BadRequestException("qanda_max_questions is required when Q&A is enabled")
            val maxAnswers = event.qandaMaxAnswers
                ?: throw BadRequestException("qanda_max_answers is required when Q&A is enabled")
            eventEntity.qandaMaxQuestions = maxQuestions
            eventEntity.qandaMaxAnswers = maxAnswers
            eventEntity.qandaSubmissionDeadline = event.qandaSubmissionDeadline
        } else {
            eventEntity.qandaMaxQuestions = null
            eventEntity.qandaMaxAnswers = null
            eventEntity.qandaSubmissionDeadline = null
        }
        eventSlug
```

In the same file's `createEvent` (the `entity.new { ... }` block, lines ~164-166), add the deadline line:

```kotlin
            this.qandaEnabled = event.qandaEnabled
            this.qandaMaxQuestions = if (event.qandaEnabled) event.qandaMaxQuestions else null
            this.qandaMaxAnswers = if (event.qandaEnabled) event.qandaMaxAnswers else null
            this.qandaSubmissionDeadline = if (event.qandaEnabled) event.qandaSubmissionDeadline else null
```

- [ ] **Step 6: Expose the deadline in the `EventDisplay` mapping**

In the same file's `EventDisplay` construction (lines ~126-133), pass the deadline into `QandaConfig`:

```kotlin
            qandaConfig = if (eventEntity.qandaEnabled) {
                QandaConfig(
                    maxQuestions = eventEntity.qandaMaxQuestions ?: 1,
                    maxAnswers = eventEntity.qandaMaxAnswers ?: 2,
                    submissionDeadline = eventEntity.qandaSubmissionDeadline,
                )
            } else {
                null
            },
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `cd server && ./gradlew test --no-daemon --tests "fr.devlille.partners.connect.events.infrastructure.api.EventQandaConfigRouteGetTest" --tests "fr.devlille.partners.connect.events.infrastructure.api.EventQandaConfigRoutePutTest"`
Expected: PASS (all methods, including the two new ones).

- [ ] **Step 8: Commit**

```bash
git add server/application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventRepositoryExposed.kt server/application/src/test/kotlin/fr/devlille/partners/connect/events/factories/Event.factory.kt server/application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/EventQandaConfigRoutePutTest.kt server/application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/EventQandaConfigRouteGetTest.kt
git commit -m "feat(server): persist and expose Q&A submission deadline in event config"
```

---

## Task 4: Enforce the deadline on Q&A mutations (TDD)

**Files:**
- Modify: `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaQuestionRoutePostTest.kt`
- Modify: `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaQuestionRoutePutTest.kt`
- Modify: `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaQuestionRouteDeleteTest.kt`
- Modify: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/QandaRepositoryExposed.kt`

A past deadline is represented by `kotlinx.datetime.LocalDateTime.parse("2000-01-01T00:00:00")` (always in the past, deterministic).

- [ ] **Step 1: Write the failing POST test (403 after deadline)**

In `QandaQuestionRoutePostTest.kt`, add the import `import kotlinx.datetime.LocalDateTime` to the import block, then add this test method inside the class:

```kotlin
    @Test
    fun `POST returns 403 when submission deadline has passed`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 3
                event.qandaMaxAnswers = 4
                event.qandaSubmissionDeadline = LocalDateTime.parse("2000-01-01T00:00:00")
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val body = """
            {
                "question": "Test?",
                "answers": [
                    {"answer": "A", "is_correct": false},
                    {"answer": "B", "is_correct": true}
                ]
            }
        """.trimIndent()
        val response = client.post("/events/$eventId/partnerships/$partnershipId/qanda/questions") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
```

- [ ] **Step 2: Write the failing DELETE test (403 after deadline)**

In `QandaQuestionRouteDeleteTest.kt`, add the import `import kotlinx.datetime.LocalDateTime`, then add this test method inside the class:

```kotlin
    @Test
    fun `DELETE returns 403 when submission deadline has passed`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val questionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 3
                event.qandaMaxAnswers = 4
                event.qandaSubmissionDeadline = LocalDateTime.parse("2000-01-01T00:00:00")
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedQandaQuestion(id = questionId, partnershipId = partnershipId)
                insertMockedQandaAnswer(questionId = questionId, answer = "A", isCorrect = true)
                insertMockedQandaAnswer(questionId = questionId, answer = "B", isCorrect = false)
            }
        }

        val response =
            client.delete("/events/$eventId/partnerships/$partnershipId/qanda/questions/$questionId")

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
```

- [ ] **Step 3: Write the failing PUT test (403 after deadline)**

Open `QandaQuestionRoutePutTest.kt`. It follows the same structure (create event + partnership + an existing question, then `client.put(".../qanda/questions/{questionId}")`). Add the import `import kotlinx.datetime.LocalDateTime`, then add a test method that mirrors the existing successful PUT test in that file but adds `event.qandaSubmissionDeadline = LocalDateTime.parse("2000-01-01T00:00:00")` in the transaction and asserts `HttpStatusCode.Forbidden`:

```kotlin
    @Test
    fun `PUT returns 403 when submission deadline has passed`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val questionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 3
                event.qandaMaxAnswers = 4
                event.qandaSubmissionDeadline = LocalDateTime.parse("2000-01-01T00:00:00")
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedQandaQuestion(id = questionId, partnershipId = partnershipId)
                insertMockedQandaAnswer(questionId = questionId, answer = "A", isCorrect = true)
                insertMockedQandaAnswer(questionId = questionId, answer = "B", isCorrect = false)
            }
        }

        val body = """
            {
                "question": "Updated?",
                "answers": [
                    {"answer": "A", "is_correct": true},
                    {"answer": "B", "is_correct": false}
                ]
            }
        """.trimIndent()
        val response = client.put(
            "/events/$eventId/partnerships/$partnershipId/qanda/questions/$questionId",
        ) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
```

> If `QandaQuestionRoutePutTest.kt` does not already import `insertMockedQandaQuestion` / `insertMockedQandaAnswer` / `io.ktor.client.request.put` / `io.ktor.client.request.setBody` / `io.ktor.http.ContentType` / `io.ktor.http.contentType`, add the missing imports (mirror `QandaQuestionRouteDeleteTest.kt` and `QandaQuestionRoutePostTest.kt`).

- [ ] **Step 4: Run the new tests to verify they fail**

Run: `cd server && ./gradlew test --no-daemon --tests "fr.devlille.partners.connect.partnership.infrastructure.api.QandaQuestionRoutePostTest" --tests "fr.devlille.partners.connect.partnership.infrastructure.api.QandaQuestionRoutePutTest" --tests "fr.devlille.partners.connect.partnership.infrastructure.api.QandaQuestionRouteDeleteTest"`
Expected: the three new tests FAIL (they currently return 201 / 200 / 204 because no deadline check exists).

- [ ] **Step 5: Add the enforcement helper and call it**

In `QandaRepositoryExposed.kt`:

1. Add these three imports next to the existing ones:

```kotlin
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
```

(`EventEntity` and `ForbiddenException` are already imported in this file — do not re-add them.)

2. Add this private helper at the bottom of the class (next to `validateAnswers`):

```kotlin
    private fun verifyDeadlineNotPassed(event: EventEntity) {
        val deadline = event.qandaSubmissionDeadline ?: return
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        if (now > deadline) {
            throw ForbiddenException("Q&A submission deadline has passed")
        }
    }
```

3. In `create`, call it right after the `qandaEnabled` check (after line 76, before `validateAnswers`):

```kotlin
        if (!eventEntity.qandaEnabled) {
            throw ForbiddenException("Q&A is not enabled for this event")
        }
        verifyDeadlineNotPassed(eventEntity)

        validateAnswers(request, eventEntity.qandaMaxAnswers)
```

4. In `update`, call it on the already-fetched `eventEntity` (after line 113, before `validateAnswers`):

```kotlin
        val eventEntity = EventEntity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        verifyDeadlineNotPassed(eventEntity)

        validateAnswers(request, eventEntity.qandaMaxAnswers)
```

5. In `delete`, capture the partnership and check its event (replace the existing `PartnershipEntity.findById(partnershipId) ?: throw ...` line at the top of `delete`):

```kotlin
    override fun delete(partnershipId: UUID, questionId: UUID): Unit = transaction {
        val partnership = PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")

        verifyDeadlineNotPassed(partnership.event)

        val questionEntity = QandaQuestionEntity.findById(questionId)
            ?.takeIf { it.partnership.id.value == partnershipId }
            ?: throw NotFoundException("Question not found")

        questionEntity.delete()
    }
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `cd server && ./gradlew test --no-daemon --tests "fr.devlille.partners.connect.partnership.infrastructure.api.QandaQuestionRoutePostTest" --tests "fr.devlille.partners.connect.partnership.infrastructure.api.QandaQuestionRoutePutTest" --tests "fr.devlille.partners.connect.partnership.infrastructure.api.QandaQuestionRouteDeleteTest"`
Expected: PASS (all methods — pre-existing tests still green because they set no deadline).

- [ ] **Step 7: Commit**

```bash
git add server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/QandaRepositoryExposed.kt server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaQuestionRoutePostTest.kt server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaQuestionRoutePutTest.kt server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaQuestionRouteDeleteTest.kt
git commit -m "feat(server): block Q&A create/update/delete after submission deadline"
```

---

## Task 5: Update the OpenAPI schemas

**Files:**
- Modify: `server/application/src/main/resources/schemas/create_event.schema.json`
- Create: `server/application/src/main/resources/schemas/qanda_config.schema.json`
- Modify: `server/application/src/main/resources/schemas/event_display.schema.json`
- Modify: `server/application/src/main/resources/openapi/openapi.yaml`

- [ ] **Step 1: Add the input field to `create_event.schema.json`**

Add the `qanda_submission_deadline` property after `qanda_max_answers` (it stays out of `required`):

```json
    "qanda_max_answers": {
      "type": ["integer", "null"],
      "minimum": 2
    },
    "qanda_submission_deadline": {
      "type": ["string", "null"],
      "format": "datetime"
    }
```

- [ ] **Step 2: Create the response schema `qanda_config.schema.json`**

```json
{
  "$id": "qanda_config.schema.json",
  "type": "object",
  "properties": {
    "max_questions": {
      "type": "integer",
      "minimum": 1
    },
    "max_answers": {
      "type": "integer",
      "minimum": 2
    },
    "submission_deadline": {
      "type": ["string", "null"],
      "format": "datetime"
    }
  },
  "required": ["max_questions", "max_answers"]
}
```

- [ ] **Step 3: Reference `qanda_config` from `event_display.schema.json`**

Add the property after `booth_plan_image_url` (keep it out of `required`, nullable via `anyOf`):

```json
    "booth_plan_image_url": {
      "type": ["string", "null"]
    },
    "qanda_config": {
      "anyOf": [
        { "$ref": "qanda_config.schema.json" },
        { "type": "null" }
      ]
    }
```

- [ ] **Step 4: Register the `QandaConfig` component in `openapi.yaml`**

In the `components/schemas` block (near the other `qanda*` entries around line 6250-6260), add:

```yaml
    QandaConfig:
      $ref: "../schemas/qanda_config.schema.json"
```

- [ ] **Step 5: Validate the spec**

Run: `cd server && npm run validate`
Expected: zero errors.

- [ ] **Step 6: Run the Kotlin quality gate**

Run: `cd server && ./gradlew check --no-daemon`
Expected: BUILD SUCCESSFUL (all tests + lint pass).

- [ ] **Step 7: Bundle the documentation**

Run: `cd server && npm run bundle`
Expected: `documentation.yaml` regenerated with no errors.

- [ ] **Step 8: Commit**

```bash
git add server/application/src/main/resources/schemas/create_event.schema.json server/application/src/main/resources/schemas/qanda_config.schema.json server/application/src/main/resources/schemas/event_display.schema.json server/application/src/main/resources/openapi/openapi.yaml server/application/src/main/resources/openapi/documentation.yaml
git commit -m "feat(server): document Q&A submission deadline and qanda_config in OpenAPI"
```

---

## Task 6: Regenerate the front API client

**Files:**
- Modify: `front/utils/api.ts`

- [ ] **Step 1: Regenerate from the local bundled spec**

Invoke the `regenerate-front-api` skill (it runs Orval against
`server/application/src/main/resources/openapi/documentation.yaml`, not production).

- [ ] **Step 2: Verify the generated types**

Run: `cd front && grep -n "QandaConfigSchema\|qanda_config\|qanda_submission_deadline\|submission_deadline" utils/api.ts`
Expected: `CreateEventSchema` now has `qanda_submission_deadline?: string | null`, and `EventDisplaySchema` now has a `qanda_config` property carrying `max_questions`, `max_answers`, and `submission_deadline?: string | null` — either as a named `QandaConfigSchema` interface (if Orval emits one from the registered component) or as an inline object type. Both are acceptable as long as `submission_deadline` is reachable via `event.qanda_config`.

- [ ] **Step 3: Type-check the front**

Run: `cd front && npm run lint`
Expected: passes (no new errors introduced by the regenerated types).

- [ ] **Step 4: Commit**

```bash
git add front/utils/api.ts
git commit -m "chore(front): regenerate API client for Q&A submission deadline"
```

---

## Task 7: Add the deadline input to the organiser form

**Files:**
- Modify: `front/components/EventForm.vue`

- [ ] **Step 1: Add the deadline to `FormState` and initialise it**

In the `<script setup>` block, extend the `FormState` type and the `form` ref:

```ts
type FormState = Omit<EventDisplay, 'slug' | 'qanda_config'> & {
  qanda_enabled: boolean
  qanda_max_questions: number | null
  qanda_max_answers: number | null
  qanda_submission_deadline: string | null
}

const form = ref<FormState>({
  ...data,
  qanda_enabled: !!data.qanda_config,
  qanda_max_questions: data.qanda_config?.max_questions ?? null,
  qanda_max_answers: data.qanda_config?.max_answers ?? null,
  qanda_submission_deadline: data.qanda_config?.submission_deadline ?? null,
})
```

- [ ] **Step 2: Include the deadline in the emitted `qanda_config`**

Update `onSave`:

```ts
function onSave() {
  const {
    qanda_enabled,
    qanda_max_questions,
    qanda_max_answers,
    qanda_submission_deadline,
    ...rest
  } = form.value
  emit('save', {
    ...rest,
    qanda_config: qanda_enabled
      ? {
          max_questions: qanda_max_questions,
          max_answers: qanda_max_answers,
          submission_deadline: qanda_submission_deadline,
        }
      : null,
  })
}
```

- [ ] **Step 3: Add the datetime-local input in the Q&A section**

Inside the `<div v-if="form.qanda_enabled" class="grid grid-cols-1 md:grid-cols-2 gap-6">` block, after the `qanda_max_answers` field's closing `</div>`, add a new field:

```vue
        <div>
          <label for="qanda_submission_deadline" class="block text-sm font-medium text-gray-700 mb-2">
            Date limite de soumission (optionnel)
          </label>
          <UInput
            id="qanda_submission_deadline"
            :model-value="form.qanda_submission_deadline ?? ''"
            type="datetime-local"
            class="w-full"
            @update:model-value="val => form.qanda_submission_deadline = val === '' ? null : String(val)"
          />
        </div>
```

- [ ] **Step 4: Lint**

Run: `cd front && npm run lint`
Expected: passes.

- [ ] **Step 5: Commit**

```bash
git add front/components/EventForm.vue
git commit -m "feat(front): add Q&A submission deadline field to event form"
```

---

## Task 8: Map the deadline into the create/edit payloads

**Files:**
- Modify: `front/pages/orgs/[slug]/events/create.vue`
- Modify: `front/pages/orgs/[slug]/events/[eventSlug]/information.vue`

- [ ] **Step 1: Add the deadline to `create.vue`'s payload**

In `create.vue` `onSave`, add the field to `eventPayload` after `qanda_max_answers`:

```ts
      qanda_enabled: !!eventData.qanda_config,
      qanda_max_questions: eventData.qanda_config?.max_questions ?? null,
      qanda_max_answers: eventData.qanda_config?.max_answers ?? null,
      qanda_submission_deadline: eventData.qanda_config?.submission_deadline ?? null,
```

(`initialData.qanda_config` is already `null`; no change needed there.)

- [ ] **Step 2: Add the deadline to `information.vue`'s payload**

In `information.vue` `onSave`, add the field to `eventPayload` after `qanda_max_answers`:

```ts
      qanda_enabled: !!updatedEvent.qanda_config,
      qanda_max_questions: updatedEvent.qanda_config?.max_questions ?? null,
      qanda_max_answers: updatedEvent.qanda_config?.max_answers ?? null,
      qanda_submission_deadline: updatedEvent.qanda_config?.submission_deadline ?? null,
```

- [ ] **Step 3: Lint**

Run: `cd front && npm run lint`
Expected: passes.

- [ ] **Step 4: Commit**

```bash
git add front/pages/orgs/[slug]/events/create.vue front/pages/orgs/[slug]/events/[eventSlug]/information.vue
git commit -m "feat(front): send Q&A submission deadline when saving events"
```

---

## Task 9: Disable partner submissions and show the deadline message

**Files:**
- Modify: `front/composables/usePublicPartnership.ts`
- Modify: `front/pages/[eventSlug]/[partnershipId]/scanzee.vue`

- [ ] **Step 1: Expose the deadline from the composable**

In `usePublicPartnership.ts`, add the state next to the other `qanda*` states (after line 26):

```ts
  const qandaSubmissionDeadline = useState<string | null>("qanda-submission-deadline", () => null);
```

In `loadPartnership`, after the existing `qandaMaxAnswers.value = ...` line (line 47), add:

```ts
      qandaSubmissionDeadline.value = event.qanda_config?.submission_deadline ?? null;
```

In the returned object (after `qandaMaxAnswers,`), add:

```ts
    qandaSubmissionDeadline,
```

- [ ] **Step 2: Read the deadline and compute the closed state in `scanzee.vue`**

In the `<script setup>` block, add `qandaSubmissionDeadline` to the destructured composable result:

```ts
const {
  eventSlug,
  partnershipId,
  partnership,
  qandaMaxQuestions,
  qandaMaxAnswers,
  qandaSubmissionDeadline,
  loadPartnership,
} = usePublicPartnership();
```

Add the computed flag and a formatted-deadline helper after the refs are declared:

```ts
const isDeadlinePassed = computed(() => {
  const d = qandaSubmissionDeadline.value;
  return d ? new Date() > new Date(d) : false;
});

const formattedDeadline = computed(() => {
  const d = qandaSubmissionDeadline.value;
  return d
    ? new Date(d).toLocaleString('fr-FR', {
        dateStyle: 'long',
        timeStyle: 'short',
      })
    : '';
});
```

- [ ] **Step 3: Show the message banner**

In the `<template>`, inside the `<template v-else>` block, immediately before the intro `<UCard class="mb-6">`, add:

```vue
          <div
            v-if="isDeadlinePassed"
            role="alert"
            class="mb-6 bg-amber-50 border border-amber-200 text-amber-800 px-4 py-3 rounded"
          >
            La période de soumission est terminée (depuis le {{ formattedDeadline }}). Vous ne pouvez
            plus ajouter, modifier ou supprimer de questions.
          </div>
```

- [ ] **Step 4: Disable every mutating control when closed**

Combine `isDeadlinePassed` into the existing `:disabled` bindings (use `||` so the existing rules still apply):

- "Ajouter une question" button:
  ```vue
  :disabled="isDeadlinePassed || (qandaMaxQuestions !== null && questions.length >= qandaMaxQuestions)"
  ```
- Question text input (`UInput` with `:id="`question-${q._key}`"`): add `:disabled="isDeadlinePassed"`.
- Delete-question button: change `:loading="deleting.has(q._key)"` to also disable — add `:disabled="isDeadlinePassed"`.
- Per-answer "Ajouter" button:
  ```vue
  :disabled="isDeadlinePassed || (qandaMaxAnswers !== null && q.answers.length >= qandaMaxAnswers)"
  ```
- Answer `UCheckbox`: add `:disabled="isDeadlinePassed"`.
- Answer text `UInput`: add `:disabled="isDeadlinePassed"`.
- Remove-answer button (`v-if="q.answers.length > 2"`): add `:disabled="isDeadlinePassed"`.
- "Sauvegarder" button:
  ```vue
  :disabled="isDeadlinePassed || !q.question.trim() || q.answers.length < 2 || q.answers.some(a => !a.answer.trim())"
  ```

- [ ] **Step 5: Lint**

Run: `cd front && npm run lint`
Expected: passes.

- [ ] **Step 6: Commit**

```bash
git add front/composables/usePublicPartnership.ts front/pages/[eventSlug]/[partnershipId]/scanzee.vue
git commit -m "feat(front): disable Scanzee submissions after Q&A deadline with a message"
```

---

## Final verification

- [ ] **Server:** `cd server && ./gradlew check --no-daemon && npm run validate` — all green.
- [ ] **Front:** `cd front && npm run lint && npm test` — all green.
- [ ] **Manual smoke (optional):** As organiser, set a past deadline on an event with Q&A enabled; as partner, open `…/{partnershipId}/scanzee` and confirm the amber banner shows the formatted deadline and all add/edit/delete controls are disabled. Set a future deadline (or clear it) and confirm controls are enabled again.
