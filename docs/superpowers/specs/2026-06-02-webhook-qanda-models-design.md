# Design: Webhook Q&A models (`WebhookQuestion` / `WebhookAnswer`)

**Date**: 2026-06-02
**Area**: `server` — `webhooks` domain
**Status**: Approved (pending spec review)

## Goal

Give the outbound webhook its own Q&A contract so the wire format no longer
reuses the internal `QandaQuestion` domain model. The webhook should carry the
questions and answers entered ("encoded") by the partner using two dedicated,
leaner models that add an explicit `order`:

```kotlin
@Serializable
data class WebhookQuestion(
    val id: String,
    val question: String,
    val order: Int = 0,
    val answers: List<WebhookAnswer> = emptyList(),
)

@Serializable
data class WebhookAnswer(
    val id: String,
    val answer: String,
    @SerialName("is_correct")
    val isCorrect: Boolean = false,
    val order: Int = 0,
)
```

## Why a dedicated model

Today `WebhookPayload.questions` is typed `List<QandaQuestion>`. `QandaQuestion`
is the internal partnership-domain model and leaks `partnership_id` and
`created_at` into the webhook wire format, and lacks `order`. Decoupling the
external contract from the internal model lets each evolve independently and
makes the rendering order explicit for the receiving system.

## Components

### 1. New domain models — `webhooks/domain/`
- `WebhookQuestion.kt`
- `WebhookAnswer.kt`

Placed alongside `WebhookPayload` because they are part of the webhook contract.

### 2. New mapper — webhooks module (`application/mappers`)
- `QandaQuestionEntity.toWebhookQuestion(order: Int): WebhookQuestion`
  - maps `id`, `question`, the passed `order`, and
    `answers.mapIndexed { i, a -> a.toWebhookAnswer(i) }`
- `QandaAnswerEntity.toWebhookAnswer(order: Int): WebhookAnswer`
  - maps `id`, `answer`, `isCorrect`, and the passed `order`

The existing `partnership/application/mappers/QandaEntity.ext.kt` (`toDomain()`)
is left untouched — the partnership Q&A REST API keeps using `QandaQuestion`.

### 3. `WebhookPayload` change
- `questions: List<QandaQuestion>` → `questions: List<WebhookQuestion>`
  (replace, not duplicate — the payload already carries `partnership`).

### 4. `HttpWebhookGateway` change
The questions fetch becomes ordered and indexed:

```kotlin
val questions = QandaQuestionEntity
    .find { QandaQuestionsTable.partnershipId eq partnershipId }
    .orderBy(QandaQuestionsTable.createdAt to SortOrder.ASC) // deterministic, app-consistent
    .mapIndexed { index, entity -> entity.toWebhookQuestion(index) }
```

This also fixes an inconsistency: the gateway previously fetched questions
unordered, whereas `QandaRepositoryExposed` always orders by `createdAt ASC`.

## Data flow

`sendWebhook` (per partnership) → fetch `QandaQuestionEntity` rows for the
partnership, ordered by `createdAt ASC` → `mapIndexed` to `WebhookQuestion`
(order = list index) → each question maps its `answers` to `WebhookAnswer`
(order = collection index) → serialized into `WebhookPayload` → POSTed to the
configured webhook URL.

## Order derivation (the key decision)

`order` is **derived from list position**, not persisted:
- **Questions**: ordered by `createdAt ASC`, `order` = index (0, 1, 2, …).
- **Answers**: `order` = position in the entity's `answers` collection.

No DB migration, no API/request changes — contained to the webhook module.

### Known limitation
Answer order cannot reflect the partner's original input order: answers have no
timestamp/order column and `QandaRepositoryExposed.update()` deletes and
recreates them with random UUID primary keys. `order` for answers therefore
reflects the collection read order, not authored order. Persisting true order
was the rejected "Persist in DB" alternative.

## Testing

Add a focused test using the existing `insertMockedQandaQuestion` /
`insertMockedQandaAnswer` factories within a DB transaction, asserting:
- questions receive `order` 0, 1, 2 … in `createdAt` order;
- answers receive `order` by position;
- JSON encoding of `WebhookQuestion` produces wire keys `is_correct` and
  `order` (serialization contract).

## Out of scope / no change
- OpenAPI spec — the webhook is an outbound POST, not a documented endpoint.
- DB migrations.
- The partnership Q&A REST API (`QandaRepositoryExposed`, `QandaQuestion`).
- The front end.
