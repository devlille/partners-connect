# API Contracts: Partnership Q&A Game

**Feature**: 023-partnership-qanda  
**Date**: 2026-04-02

## Endpoints Summary

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/events/{eventSlug}/qanda/questions` | Public | List all event questions grouped by partnership |
| GET | `/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions` | Public | List questions for a partnership |
| POST | `/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions` | Public | Create a question |
| PUT | `/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions/{questionId}` | Public | Update a question |
| DELETE | `/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions/{questionId}` | Public | Delete a question |

> **Note**: Event update (PUT `/orgs/{orgSlug}/events/{eventSlug}`) is modified to accept Q&A config fields — no new endpoint needed.

---

## 1. GET /events/{eventSlug}/qanda/questions

**Purpose**: Retrieve all Q&A questions for an event, grouped by partnership (FR-011).

**Request**:
```
GET /events/{eventSlug}/qanda/questions
```

**Response 200**:
```json
[
  {
    "partnership_id": "550e8400-e29b-41d4-a716-446655440001",
    "company_name": "Acme Corp",
    "questions": [
      {
        "id": "650e8400-e29b-41d4-a716-446655440001",
        "partnership_id": "550e8400-e29b-41d4-a716-446655440001",
        "question": "What year was Acme Corp founded?",
        "answers": [
          { "id": "750e8400-0001", "answer": "2010", "is_correct": false },
          { "id": "750e8400-0002", "answer": "2015", "is_correct": true },
          { "id": "750e8400-0003", "answer": "2020", "is_correct": false }
        ],
        "created_at": "2026-04-01T10:00:00"
      }
    ]
  }
]
```

**Response 403** (Q&A disabled):
```json
{ "message": "Q&A is not enabled for this event" }
```

**Response 404** (event not found):
```json
{ "message": "Event not found" }
```

---

## 2. GET /events/{eventSlug}/partnerships/{partnershipId}/qanda/questions

**Purpose**: List all questions for a specific partnership.

**Request**:
```
GET /events/{eventSlug}/partnerships/{partnershipId}/qanda/questions
```

**Response 200**:
```json
[
  {
    "id": "650e8400-e29b-41d4-a716-446655440001",
    "partnership_id": "550e8400-e29b-41d4-a716-446655440001",
    "question": "What year was Acme Corp founded?",
    "answers": [
      { "id": "750e8400-0001", "answer": "2010", "is_correct": false },
      { "id": "750e8400-0002", "answer": "2015", "is_correct": true },
      { "id": "750e8400-0003", "answer": "2020", "is_correct": false }
    ],
    "created_at": "2026-04-01T10:00:00"
  }
]
```

---

## 3. POST /events/{eventSlug}/partnerships/{partnershipId}/qanda/questions

**Purpose**: Create a new question with answers (FR-004).

**Request**:
```
POST /events/{eventSlug}/partnerships/{partnershipId}/qanda/questions
Content-Type: application/json

{
  "question": "What year was Acme Corp founded?",
  "answers": [
    { "answer": "2010", "is_correct": false },
    { "answer": "2015", "is_correct": true },
    { "answer": "2020", "is_correct": false }
  ]
}
```

**Response 201**:
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440001",
  "partnership_id": "550e8400-e29b-41d4-a716-446655440001",
  "question": "What year was Acme Corp founded?",
  "answers": [
    { "id": "750e8400-0001", "answer": "2010", "is_correct": false },
    { "id": "750e8400-0002", "answer": "2015", "is_correct": true },
    { "id": "750e8400-0003", "answer": "2020", "is_correct": false }
  ],
  "created_at": "2026-04-01T10:00:00"
}
```

**Response 400** (validation errors):
```json
{ "message": "Exactly one answer must be marked as correct" }
{ "message": "At least 2 answers are required" }
{ "message": "Number of answers exceeds the maximum of 4" }
```

**Response 403** (Q&A disabled):
```json
{ "message": "Q&A is not enabled for this event" }
```

**Response 409** (limit reached):
```json
{ "message": "Maximum number of questions (3) reached for this partnership" }
```

---

## 4. PUT /events/{eventSlug}/partnerships/{partnershipId}/qanda/questions/{questionId}

**Purpose**: Update an existing question and its answers (FR-009).

**Request**:
```
PUT /events/{eventSlug}/partnerships/{partnershipId}/qanda/questions/{questionId}
Content-Type: application/json

{
  "question": "In which year was Acme Corp founded?",
  "answers": [
    { "answer": "2010", "is_correct": false },
    { "answer": "2015", "is_correct": true },
    { "answer": "2018", "is_correct": false },
    { "answer": "2020", "is_correct": false }
  ]
}
```

**Response 200**:
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440001",
  "partnership_id": "550e8400-e29b-41d4-a716-446655440001",
  "question": "In which year was Acme Corp founded?",
  "answers": [
    { "id": "750e8400-0004", "answer": "2010", "is_correct": false },
    { "id": "750e8400-0005", "answer": "2015", "is_correct": true },
    { "id": "750e8400-0006", "answer": "2018", "is_correct": false },
    { "id": "750e8400-0007", "answer": "2020", "is_correct": false }
  ],
  "created_at": "2026-04-01T10:00:00"
}
```

**Note**: On update, existing answers are deleted and replaced with the new set (full replace of answers within a question).

**Response 400**: Same validation rules as POST.

**Response 404** (question not found or doesn't belong to partnership):
```json
{ "message": "Question not found" }
```

---

## 5. DELETE /events/{eventSlug}/partnerships/{partnershipId}/qanda/questions/{questionId}

**Purpose**: Delete a question and all its answers (FR-010).

**Request**:
```
DELETE /events/{eventSlug}/partnerships/{partnershipId}/qanda/questions/{questionId}
```

**Response 204**: No content.

**Response 404**:
```json
{ "message": "Question not found" }
```

---

## 6. Event Update (modified existing endpoint)

**Endpoint**: `PUT /orgs/{orgSlug}/events/{eventSlug}`

**Modified request body** (new fields are optional):
```json
{
  "name": "DevLille 2026",
  "start_time": "2026-06-15T09:00:00",
  "end_time": "2026-06-15T18:00:00",
  "submission_start_time": "2026-01-01T00:00:00",
  "submission_end_time": "2026-05-01T23:59:59",
  "address": "Lille Grand Palais",
  "contact": { "email": "contact@devlille.fr", "phone": "+33123456789" },
  "qanda_enabled": true,
  "qanda_max_questions": 3,
  "qanda_max_answers": 4
}
```

**Validation rules**:
- When `qanda_enabled` is true, `qanda_max_questions` must be ≥ 1 and `qanda_max_answers` must be ≥ 2.
- When `qanda_enabled` is false or omitted, `qanda_max_questions` and `qanda_max_answers` are ignored/set to null.

---

## 7. Event Display (modified response)

**Modified response** for `GET /events/{eventSlug}` and `GET /orgs/{orgSlug}/events/{eventSlug}`:

```json
{
  "id": "...",
  "slug": "devlille-2026",
  "name": "DevLille 2026",
  "qanda_config": {
    "max_questions": 3,
    "max_answers": 4
  }
}
```

When Q&A is disabled, `qanda_config` is `null` (omitted from response).

---

## 8. Webhook Payload (modified)

**Modified `WebhookPayload`** — new `questions` field:

```json
{
  "eventType": "UPDATED",
  "partnership": { "..." },
  "company": { "..." },
  "event": { "..." },
  "jobs": [],
  "activities": [],
  "questions": [
    {
      "id": "650e8400-e29b-41d4-a716-446655440001",
      "partnership_id": "550e8400-e29b-41d4-a716-446655440001",
      "question": "What year was Acme Corp founded?",
      "answers": [
        { "id": "750e8400-0001", "answer": "2010", "is_correct": false },
        { "id": "750e8400-0002", "answer": "2015", "is_correct": true }
      ],
      "created_at": "2026-04-01T10:00:00"
    }
  ],
  "timestamp": "2026-04-02T14:00:00Z"
}
```

When no questions exist, `questions` is an empty array `[]`.

---

## JSON Schemas (to create)

### qanda_question_request.schema.json

```json
{
  "type": "object",
  "required": ["question", "answers"],
  "properties": {
    "question": {
      "type": "string",
      "minLength": 1
    },
    "answers": {
      "type": "array",
      "minItems": 2,
      "items": {
        "type": "object",
        "required": ["answer", "is_correct"],
        "properties": {
          "answer": {
            "type": "string",
            "minLength": 1
          },
          "is_correct": {
            "type": "boolean"
          }
        },
        "additionalProperties": false
      }
    }
  },
  "additionalProperties": false
}
```

### create_event.schema.json (modified)

Add optional Q&A fields to the existing schema:
```json
{
  "qanda_enabled": { "type": "boolean" },
  "qanda_max_questions": { "type": ["integer", "null"], "minimum": 1 },
  "qanda_max_answers": { "type": ["integer", "null"], "minimum": 2 }
}
```

These fields are NOT added to the `required` array (backward compatible).
