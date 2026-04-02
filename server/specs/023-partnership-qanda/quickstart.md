# Quickstart: Partnership Q&A Game

**Feature**: 023-partnership-qanda  
**Date**: 2026-04-02

## Prerequisites

- Java 21 (Amazon Corretto)
- Docker (for PostgreSQL via `docker-compose`)
- Node.js (for OpenAPI validation)

## Setup

```bash
cd server
docker-compose up -d   # Start PostgreSQL
npm install            # For OpenAPI validation
```

## Build & Test

```bash
# Full build + all quality gates
./gradlew check --no-daemon

# Tests only (~45s)
./gradlew test --no-daemon

# Lint + format
./gradlew ktlintFormat --no-daemon
./gradlew detekt --no-daemon

# OpenAPI validation
npm run validate
```

## Functional Validation

### Step 1: Enable Q&A on an event

```bash
curl -X PUT http://localhost:8080/orgs/{orgSlug}/events/{eventSlug} \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "DevLille 2026",
    "start_time": "2026-06-15T09:00:00",
    "end_time": "2026-06-15T18:00:00",
    "submission_start_time": "2026-01-01T00:00:00",
    "submission_end_time": "2026-05-01T23:59:59",
    "address": "Lille Grand Palais",
    "contact": { "email": "contact@devlille.fr" },
    "qanda_enabled": true,
    "qanda_max_questions": 3,
    "qanda_max_answers": 4
  }'
# Expected: 200 OK with { "slug": "..." }
```

### Step 2: Verify Q&A config in event display

```bash
curl http://localhost:8080/events/{eventSlug}
# Expected: response includes "qanda_config": { "max_questions": 3, "max_answers": 4 }
```

### Step 3: Create a question

```bash
curl -X POST http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What year was our company founded?",
    "answers": [
      { "answer": "2010", "is_correct": false },
      { "answer": "2015", "is_correct": true },
      { "answer": "2020", "is_correct": false }
    ]
  }'
# Expected: 201 Created with full question object including IDs
```

### Step 4: List partnership questions

```bash
curl http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions
# Expected: 200 OK with array of questions
```

### Step 5: Update a question

```bash
curl -X PUT http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions/{questionId} \
  -H "Content-Type: application/json" \
  -d '{
    "question": "In which year was our company founded?",
    "answers": [
      { "answer": "2010", "is_correct": false },
      { "answer": "2015", "is_correct": true },
      { "answer": "2018", "is_correct": false },
      { "answer": "2020", "is_correct": false }
    ]
  }'
# Expected: 200 OK with updated question
```

### Step 6: Get all event questions (public endpoint)

```bash
curl http://localhost:8080/events/{eventSlug}/qanda/questions
# Expected: 200 OK with questions grouped by partnership
```

### Step 7: Delete a question

```bash
curl -X DELETE http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions/{questionId}
# Expected: 204 No Content
```

### Step 8: Verify validation rules

```bash
# Too many answers (assuming max 4)
curl -X POST http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Test?",
    "answers": [
      { "answer": "A", "is_correct": false },
      { "answer": "B", "is_correct": false },
      { "answer": "C", "is_correct": false },
      { "answer": "D", "is_correct": false },
      { "answer": "E", "is_correct": true }
    ]
  }'
# Expected: 400 Bad Request

# No correct answer
curl -X POST http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Test?",
    "answers": [
      { "answer": "A", "is_correct": false },
      { "answer": "B", "is_correct": false }
    ]
  }'
# Expected: 400 Bad Request

# Only 1 answer
curl -X POST http://localhost:8080/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Test?",
    "answers": [
      { "answer": "A", "is_correct": true }
    ]
  }'
# Expected: 400 Bad Request
```

## Key Files to Verify

| File | What to check |
|------|---------------|
| `events/domain/Event.kt` | Q&A fields added |
| `events/domain/EventDisplay.kt` | `qandaConfig` field |
| `events/infrastructure/db/EventsTable.kt` | 3 new columns |
| `partnership/domain/QandaRepository.kt` | Interface defined |
| `partnership/infrastructure/db/QandaQuestionsTable.kt` | Table created |
| `partnership/infrastructure/db/QandaAnswersTable.kt` | Table created |
| `partnership/infrastructure/api/QandaRoutes.kt` | Routes registered |
| `webhooks/domain/WebhookPayload.kt` | `questions` field added |
| `internal/infrastructure/migrations/MigrationRegistry.kt` | Migration registered |
