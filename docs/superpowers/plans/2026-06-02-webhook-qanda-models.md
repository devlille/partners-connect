# Webhook Q&A Models Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the outbound webhook its own Q&A contract (`WebhookQuestion` / `WebhookAnswer` with an explicit `order`), decoupled from the internal `QandaQuestion` domain model.

> **Serialization decision:** `order` and `isCorrect` are **required** (no Kotlin defaults). kotlinx.serialization always emits non-default properties, so the webhook always carries `order` and `is_correct` (even when 0 / false) without changing the gateway's `Json` instance or any other payload field.

**Architecture:** Add two `@Serializable` domain models in the `webhooks` module, a webhook-specific entity→domain mapper that derives `order` from list position, swap the `WebhookPayload.questions` type, and update `HttpWebhookGateway` to fetch questions ordered by `created_at ASC` and index them. No DB migration, no API change, no OpenAPI change.

**Tech Stack:** Kotlin, Ktor, Exposed ORM (v1.3), kotlinx.serialization, kotlin.test, JUnit via Gradle.

**Working directory for all commands:** `server/` (the Gradle root). Paths below are relative to the repo root.

**Reference skills (consult while implementing):** `clean-architecture` (file placement), `exposed-entities` (query/order DSL), `test-factories` (factory conventions), `test-driven-development`.

---

### Task 1: `WebhookAnswer` and `WebhookQuestion` domain models

These are the new external webhook contract. Pure data classes — tested via a serialization unit test (no DB needed) that locks the wire key names (`is_correct`, `order`) and the defaults.

**Files:**
- Create: `server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookAnswer.kt`
- Create: `server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookQuestion.kt`
- Test: `server/application/src/test/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookQuestionTest.kt`

- [ ] **Step 1: Write the failing test**

Create `server/application/src/test/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookQuestionTest.kt`:

```kotlin
package fr.devlille.partners.connect.webhooks.domain

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebhookQuestionTest {
    @Test
    fun `serializes order and is_correct even at zero-or-false values`() {
        val question = WebhookQuestion(
            id = "q1",
            question = "What is Kotlin?",
            order = 0,
            answers = listOf(
                WebhookAnswer(id = "a1", answer = "A language", isCorrect = true, order = 0),
                WebhookAnswer(id = "a2", answer = "A drink", isCorrect = false, order = 1),
            ),
        )

        val json = Json.encodeToString(WebhookQuestion.serializer(), question)

        // order and is_correct are required (no defaults) -> never dropped, even at 0 / false
        assertTrue(json.contains("\"is_correct\":true"), json)
        assertTrue(json.contains("\"is_correct\":false"), json)
        assertTrue(json.contains("\"order\":0"), json)
        assertTrue(json.contains("\"order\":1"), json)
    }

    @Test
    fun `question defaults answers to empty list`() {
        val question = WebhookQuestion(id = "q1", question = "x", order = 0)

        assertEquals(emptyList(), question.answers)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd server && ./gradlew test --tests "fr.devlille.partners.connect.webhooks.domain.WebhookQuestionTest" --no-daemon`
Expected: FAIL — compilation error, `WebhookQuestion` / `WebhookAnswer` unresolved.

- [ ] **Step 3: Create `WebhookAnswer`**

Create `server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookAnswer.kt`:

```kotlin
package fr.devlille.partners.connect.webhooks.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebhookAnswer(
    val id: String,
    val answer: String,
    @SerialName("is_correct")
    val isCorrect: Boolean,
    val order: Int,
)
```

- [ ] **Step 4: Create `WebhookQuestion`**

Create `server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookQuestion.kt`:

```kotlin
package fr.devlille.partners.connect.webhooks.domain

import kotlinx.serialization.Serializable

@Serializable
data class WebhookQuestion(
    val id: String,
    val question: String,
    val order: Int,
    val answers: List<WebhookAnswer> = emptyList(),
)
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd server && ./gradlew test --tests "fr.devlille.partners.connect.webhooks.domain.WebhookQuestionTest" --no-daemon`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookAnswer.kt \
        server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookQuestion.kt \
        server/application/src/test/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookQuestionTest.kt
git commit -m "feat(server): add WebhookQuestion/WebhookAnswer webhook Q&A models"
```

---

### Task 2: Webhook Q&A mapper (`toWebhookQuestion` / `toWebhookAnswer`)

Maps `QandaQuestionEntity`/`QandaAnswerEntity` (partnership infra) to the new webhook models. `order` for a question is passed by the caller (the gateway, by list index); `order` for answers is derived inside the mapper from the answer's position in the entity's `answers` collection.

**Files:**
- Create: `server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/application/mappers/QandaWebhookEntity.ext.kt`
- Test: `server/application/src/test/kotlin/fr/devlille/partners/connect/webhooks/application/QandaWebhookMapperTest.kt`

- [ ] **Step 1: Write the failing test**

Create `server/application/src/test/kotlin/fr/devlille/partners/connect/webhooks/application/QandaWebhookMapperTest.kt`. It mirrors the DB setup pattern from `QandaEventQuestionsRouteGetTest` (org → event → company → pack → partnership → question → answers), triggers app/DB init with `client.get("/")`, then exercises the mapper inside a `transaction {}`:

```kotlin
package fr.devlille.partners.connect.webhooks.application

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedQandaAnswer
import fr.devlille.partners.connect.partnership.factories.insertMockedQandaQuestion
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.webhooks.application.mappers.toWebhookQuestion
import io.ktor.client.request.get
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class QandaWebhookMapperTest {
    @Test
    fun `maps question fields and indexes answers, preserving is_correct`() = testApplication {
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
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId, name = "Acme Corp")
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedQandaQuestion(id = questionId, partnershipId = partnershipId, question = "What is Kotlin?")
                insertMockedQandaAnswer(questionId = questionId, answer = "A language", isCorrect = true)
                insertMockedQandaAnswer(questionId = questionId, answer = "A drink", isCorrect = false)
                insertMockedQandaAnswer(questionId = questionId, answer = "An island", isCorrect = false)
            }
        }
        client.get("/")

        transaction {
            val result = QandaQuestionEntity[questionId].toWebhookQuestion(order = 5)

            assertEquals(questionId.toString(), result.id)
            assertEquals("What is Kotlin?", result.question)
            assertEquals(5, result.order)
            assertEquals(3, result.answers.size)
            // order is derived from collection position, always 0,1,2 for three answers
            assertEquals(listOf(0, 1, 2), result.answers.map { it.order })
            assertEquals(1, result.answers.count { it.isCorrect })
            assertEquals("A language", result.answers.single { it.isCorrect }.answer)
            assertEquals(setOf("A language", "A drink", "An island"), result.answers.map { it.answer }.toSet())
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd server && ./gradlew test --tests "fr.devlille.partners.connect.webhooks.application.QandaWebhookMapperTest" --no-daemon`
Expected: FAIL — compilation error, `toWebhookQuestion` unresolved.

- [ ] **Step 3: Create the mapper**

Create `server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/application/mappers/QandaWebhookEntity.ext.kt`:

```kotlin
package fr.devlille.partners.connect.webhooks.application.mappers

import fr.devlille.partners.connect.partnership.infrastructure.db.QandaAnswerEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionEntity
import fr.devlille.partners.connect.webhooks.domain.WebhookAnswer
import fr.devlille.partners.connect.webhooks.domain.WebhookQuestion

fun QandaQuestionEntity.toWebhookQuestion(order: Int): WebhookQuestion = WebhookQuestion(
    id = id.value.toString(),
    question = question,
    order = order,
    answers = answers.mapIndexed { index, answer -> answer.toWebhookAnswer(index) },
)

fun QandaAnswerEntity.toWebhookAnswer(order: Int): WebhookAnswer = WebhookAnswer(
    id = id.value.toString(),
    answer = answer,
    isCorrect = isCorrect,
    order = order,
)
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd server && ./gradlew test --tests "fr.devlille.partners.connect.webhooks.application.QandaWebhookMapperTest" --no-daemon`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/application/mappers/QandaWebhookEntity.ext.kt \
        server/application/src/test/kotlin/fr/devlille/partners/connect/webhooks/application/QandaWebhookMapperTest.kt
git commit -m "feat(server): add QandaQuestionEntity->WebhookQuestion mapper"
```

---

### Task 3: Wire the new models into `WebhookPayload` and `HttpWebhookGateway`

Swap the payload's `questions` type to the new model and update the gateway to fetch questions ordered by `created_at ASC` (consistent with `QandaRepositoryExposed`) and assign `order` by index.

**Files:**
- Modify: `server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookPayload.kt`
- Modify: `server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/infrastructure/gateways/HttpWebhookGateway.kt`

- [ ] **Step 1: Change the `WebhookPayload.questions` type**

In `WebhookPayload.kt`:
- Remove the import line: `import fr.devlille.partners.connect.partnership.domain.QandaQuestion`
- Change the field `val questions: List<QandaQuestion>,` to `val questions: List<WebhookQuestion>,`

`WebhookQuestion` is in the same package (`webhooks.domain`), so no new import is required. Resulting file:

```kotlin
package fr.devlille.partners.connect.webhooks.domain

import fr.devlille.partners.connect.agenda.domain.Speaker
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.JobOffer
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.partnership.domain.BoothActivity
import fr.devlille.partners.connect.partnership.domain.PartnershipDetail
import kotlinx.serialization.Serializable

@Serializable
data class WebhookPayload(
    val eventType: WebhookEventType,
    val partnership: PartnershipDetail,
    val company: Company,
    val event: EventSummary,
    val jobs: List<JobOffer>,
    val activities: List<BoothActivity>,
    val questions: List<WebhookQuestion>,
    val speakers: List<Speaker>,
    val supportVideoUrl: String? = null,
    val timestamp: String,
)
```

- [ ] **Step 2: Update the gateway's questions fetch**

In `HttpWebhookGateway.kt`, add two imports (keep alphabetical ktlint order):
- `import fr.devlille.partners.connect.webhooks.application.mappers.toWebhookQuestion`
- `import org.jetbrains.exposed.v1.core.SortOrder`

Then replace the questions block (currently):

```kotlin
            val questions = QandaQuestionEntity
                .find { QandaQuestionsTable.partnershipId eq partnershipId }
                .map { it.toDomain() }
```

with:

```kotlin
            val questions = QandaQuestionEntity
                .find { QandaQuestionsTable.partnershipId eq partnershipId }
                .orderBy(QandaQuestionsTable.createdAt to SortOrder.ASC)
                .mapIndexed { index, entity -> entity.toWebhookQuestion(index) }
```

Do NOT remove the existing `import fr.devlille.partners.connect.partnership.application.mappers.toDomain` — it is still used by `BoothActivityEntity.toDomain()` (and pack mapping) in the same file.

- [ ] **Step 3: Format and verify the gateway/payload compile**

Run: `cd server && ./gradlew ktlintFormat --no-daemon && ./gradlew compileKotlin --no-daemon`
Expected: BUILD SUCCESSFUL. If ktlint reordered the new imports, that is fine.

- [ ] **Step 4: Commit**

```bash
git add server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookPayload.kt \
        server/application/src/main/kotlin/fr/devlille/partners/connect/webhooks/infrastructure/gateways/HttpWebhookGateway.kt
git commit -m "feat(server): send webhook Q&A via WebhookQuestion ordered by created_at"
```

---

### Task 4: Full verification

Run the complete server gate (lint + detekt + tests + build) to confirm nothing else referenced the old `WebhookPayload.questions` shape and everything is green.

**Files:** none (verification only).

- [ ] **Step 1: Run the full check**

Run: `cd server && ./gradlew ktlintCheck detekt test build --no-daemon`
Expected: BUILD SUCCESSFUL. All tests pass (including the two new tests from Tasks 1–2).

- [ ] **Step 2: If green, no commit needed**

If any step fails, fix inline (most likely a ktlint import-order nit or an unused import) and re-run. Do not claim completion until this command is green — see `superpowers:verification-before-completion`.

---

## Notes / accepted limitations

- **Answer order is collection-position, not authored order.** Answers have no timestamp/order column and `QandaRepositoryExposed.update()` recreates them with random UUID PKs. The mapper assigns `order` by the entity collection's read position. Persisting true order was the rejected "Persist in DB" option.
- **Gateway-level ordering is not unit-tested in isolation** (no webhook-integration factory exists; building one is out of scope). It reuses the proven `orderBy(QandaQuestionsTable.createdAt to SortOrder.ASC)` pattern plus `mapIndexed`, and is covered by compilation + the mapper test. This is an accepted, low-risk gap.
- **No OpenAPI / migration / front-end / partnership-API changes** — confirmed out of scope in the design.
