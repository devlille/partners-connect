# Data Model: Partnership Q&A Game

**Feature**: 023-partnership-qanda  
**Date**: 2026-04-02

## Entity Relationship Diagram

```
EventsTable (MODIFIED)
├── qanda_enabled: Boolean (default false)
├── qanda_max_questions: Int? (nullable, null when disabled)
└── qanda_max_answers: Int? (nullable, null when disabled)
    │
    │ 1:N (via partnership → event)
    ▼
QandaQuestionsTable (NEW)
├── id: UUID (PK)
├── partnership_id: UUID (FK → PartnershipsTable, CASCADE delete)
├── question: Text
└── created_at: DateTime
    │
    │ 1:N
    ▼
QandaAnswersTable (NEW)
├── id: UUID (PK)
├── question_id: UUID (FK → QandaQuestionsTable, CASCADE delete)
├── answer: Text
└── is_correct: Boolean
```

## Modified Entities

### EventsTable (3 new columns)

```kotlin
// Added to existing EventsTable object
val qandaEnabled = bool("qanda_enabled").default(false)
val qandaMaxQuestions = integer("qanda_max_questions").nullable()
val qandaMaxAnswers = integer("qanda_max_answers").nullable()
```

**Rules**:
- `qanda_max_questions` and `qanda_max_answers` MUST be null when `qanda_enabled` is false.
- When `qanda_enabled` is true, both limits MUST be positive integers (≥1).
- `qanda_max_answers` MUST be ≥2 (minimum 2 answers per question per FR-008).

### Event DTO (modified)

```kotlin
@Serializable
data class Event(
    val name: String,
    @SerialName("start_time") val startTime: LocalDateTime,
    @SerialName("end_time") val endTime: LocalDateTime,
    @SerialName("submission_start_time") val submissionStartTime: LocalDateTime,
    @SerialName("submission_end_time") val submissionEndTime: LocalDateTime,
    val address: String,
    val contact: Contact,
    // NEW fields (optional for backward compatibility)
    @SerialName("qanda_enabled") val qandaEnabled: Boolean = false,
    @SerialName("qanda_max_questions") val qandaMaxQuestions: Int? = null,
    @SerialName("qanda_max_answers") val qandaMaxAnswers: Int? = null,
)
```

### EventDisplay DTO (modified)

```kotlin
@Serializable
data class EventDisplay(
    // ... existing fields ...
    // NEW field (nullable, only present when enabled)
    @SerialName("qanda_config") val qandaConfig: QandaConfig? = null,
)
```

### QandaConfig (new value object)

```kotlin
@Serializable
data class QandaConfig(
    @SerialName("max_questions") val maxQuestions: Int,
    @SerialName("max_answers") val maxAnswers: Int,
)
```

**Usage**: Included in `EventDisplay` only when `qandaEnabled` is true. When disabled, `qandaConfig` is null.

## New Entities

### QandaQuestionsTable

```kotlin
object QandaQuestionsTable : UUIDTable("qanda_questions") {
    val partnershipId = reference("partnership_id", PartnershipsTable, onDelete = ReferenceOption.CASCADE)
    val question = text("question")
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
```

### QandaQuestionEntity

```kotlin
class QandaQuestionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<QandaQuestionEntity>(QandaQuestionsTable)
    var partnership by PartnershipEntity referencedOn QandaQuestionsTable.partnershipId
    var question by QandaQuestionsTable.question
    var createdAt by QandaQuestionsTable.createdAt
    val answers by QandaAnswerEntity referrersOn QandaAnswersTable.questionId
}
```

### QandaAnswersTable

```kotlin
object QandaAnswersTable : UUIDTable("qanda_answers") {
    val questionId = reference("question_id", QandaQuestionsTable, onDelete = ReferenceOption.CASCADE)
    val answer = text("answer")
    val isCorrect = bool("is_correct").default(false)
}
```

### QandaAnswerEntity

```kotlin
class QandaAnswerEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<QandaAnswerEntity>(QandaAnswersTable)
    var questionEntity by QandaQuestionEntity referencedOn QandaAnswersTable.questionId
    var answer by QandaAnswersTable.answer
    var isCorrect by QandaAnswersTable.isCorrect
}
```

## Domain Models

### QandaQuestion (response)

```kotlin
@Serializable
data class QandaQuestion(
    val id: String,
    @SerialName("partnership_id") val partnershipId: String,
    val question: String,
    val answers: List<QandaAnswer>,
    @SerialName("created_at") val createdAt: LocalDateTime,
)
```

### QandaAnswer (nested in question response)

```kotlin
@Serializable
data class QandaAnswer(
    val id: String,
    val answer: String,
    @SerialName("is_correct") val isCorrect: Boolean,
)
```

### QandaQuestionRequest (create/update input)

```kotlin
@Serializable
data class QandaQuestionRequest(
    val question: String,
    val answers: List<QandaAnswerInput>,
)

@Serializable
data class QandaAnswerInput(
    val answer: String,
    @SerialName("is_correct") val isCorrect: Boolean,
)
```

### PartnershipQandaSummary (for event-level listing, grouped by partnership)

```kotlin
@Serializable
data class PartnershipQandaSummary(
    @SerialName("partnership_id") val partnershipId: String,
    @SerialName("company_name") val companyName: String,
    val questions: List<QandaQuestion>,
)
```

## Repository Interface

```kotlin
interface QandaRepository {
    fun listByPartnership(partnershipId: UUID): List<QandaQuestion>
    fun listByEvent(eventSlug: String): List<PartnershipQandaSummary>
    fun create(partnershipId: UUID, eventSlug: String, request: QandaQuestionRequest): QandaQuestion
    fun update(partnershipId: UUID, questionId: UUID, eventSlug: String, request: QandaQuestionRequest): QandaQuestion
    fun delete(partnershipId: UUID, questionId: UUID)
}
```

## Validation Rules

| Rule | Where Enforced | Error |
|------|---------------|-------|
| `qanda_max_answers` ≥ 2 when enabled | Event update (repository) | `BadRequestException` |
| `qanda_max_questions` ≥ 1 when enabled | Event update (repository) | `BadRequestException` |
| Exactly 1 answer marked `is_correct` | Q&A create/update (repository) | `BadRequestException` |
| Answers count ≥ 2 | Q&A create/update (repository) | `BadRequestException` |
| Answers count ≤ `qanda_max_answers` | Q&A create/update (repository) | `BadRequestException` |
| Questions count < `qanda_max_questions` | Q&A create (repository) | `ConflictException` |
| Q&A must be enabled on event | Q&A create (repository) | `ForbiddenException` |
| Question must belong to partnership | Q&A update/delete (repository) | `NotFoundException` |

## WebhookPayload (modified)

```kotlin
@Serializable
data class WebhookPayload(
    val eventType: WebhookEventType,
    val partnership: PartnershipDetail,
    val company: Company,
    val event: EventSummary,
    val jobs: List<JobOffer>,
    val activities: List<BoothActivity>,
    val questions: List<QandaQuestion>,  // NEW
    val timestamp: String,
)
```

## Migration

New migration `CreateQandaTablesMigration`:
1. Add `qanda_enabled`, `qanda_max_questions`, `qanda_max_answers` columns to `events` table
2. Create `qanda_questions` table
3. Create `qanda_answers` table
