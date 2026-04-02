# Feature Specification: Partnership Q&A Game

**Feature Branch**: `023-partnership-qanda`  
**Created**: 2026-04-02  
**Status**: Draft  
**Input**: User description: "I would like to add an option for events that can be enabled, if it is enabled, it'll allow partnership to provide questions and answers related to their company or funny story. The idea is to allow external services based on qanda to fetch these questions and animate a game during the event. Organiser should be able to set the number of max questions and the max number of answers by question at the event level and partnership should be able to encode these questions and answers on their partnership space. Then, all questions of an event can be return and attached to partnership and they are added in the webhook payload for external connection."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Organiser enables Q&A game for an event (Priority: P1)

As an organiser, I want to enable a Q&A game feature on my event and configure its constraints (maximum number of questions per partnership, maximum number of answers per question) so that partners know the rules before submitting their questions.

**Why this priority**: Without event-level configuration, no partner can submit questions. This is the foundational prerequisite for the entire feature.

**Independent Test**: Can be fully tested by toggling the Q&A feature on an event and verifying the configuration is persisted and visible. Delivers value by letting organisers prepare the event for the game.

**Acceptance Scenarios**:

1. **Given** an event with Q&A disabled, **When** the organiser enables Q&A and sets max questions to 3 and max answers to 4, **Then** the event configuration is saved and the Q&A settings are visible in the event details.
2. **Given** an event with Q&A enabled and partners with submitted questions, **When** the organiser disables the Q&A feature, **Then** the Q&A settings are removed from the event details, partners can no longer submit questions, the public questions endpoint returns an error, but existing questions are preserved in the database and become available again if Q&A is re-enabled.
3. **Given** an event with Q&A enabled, **When** the organiser updates the max questions or max answers values, **Then** the new limits are persisted and reflected in the event configuration.

---

### User Story 2 - Partner submits questions and answers (Priority: P2)

As a partner, I want to create questions with multiple answer choices (including marking the correct one) from my partnership space so that my company can participate in the Q&A game during the event.

**Why this priority**: This is the core data entry flow. Without partner-submitted questions, there is no content for the game.

**Independent Test**: Can be fully tested by a partner navigating to their partnership space, creating a question with answers, and verifying it is saved. Delivers value by allowing partners to contribute game content.

**Acceptance Scenarios**:

1. **Given** an event with Q&A enabled and a partner with an active partnership, **When** the partner creates a question with the allowed number of answers and marks one as correct, **Then** the question and its answers are saved and visible on the partnership space.
2. **Given** a partner who has already submitted the maximum number of questions, **When** the partner tries to add another question, **Then** the system rejects the submission and informs the partner that the limit has been reached.
3. **Given** a partner who is creating a question, **When** the partner tries to add more answers than the configured maximum, **Then** the system rejects the submission and informs the partner of the answer limit.
4. **Given** a partner with existing questions, **When** the partner edits a question or its answers, **Then** the changes are persisted and reflected on the partnership space.
5. **Given** a partner with existing questions, **When** the partner deletes a question, **Then** the question and all its answers are removed.

---

### User Story 3 - Retrieve all questions for an event (Priority: P3)

As an external service, I want to fetch all Q&A questions for a given event, grouped by partnership, so that I can animate the Q&A game during the event.

**Why this priority**: This is the read/consumption side of the feature. It unlocks the external game experience but depends on questions existing first.

**Independent Test**: Can be fully tested by calling the event questions endpoint and verifying the returned data includes questions grouped by partnership with their answers. Delivers value by providing game data to external services.

**Acceptance Scenarios**:

1. **Given** an event with Q&A enabled and multiple partnerships with submitted questions, **When** an external service fetches all event questions, **Then** all questions are returned grouped by partnership, including question text, answers, and correct answer indicator.
2. **Given** an event with Q&A disabled, **When** an external service attempts to fetch questions, **Then** the system returns an appropriate error indicating Q&A is not enabled for this event.
3. **Given** an event with Q&A enabled but no questions submitted, **When** an external service fetches event questions, **Then** the system returns an empty list.

---

### User Story 4 - Q&A data included in webhook payload (Priority: P4)

As an integration consumer, I want partnership Q&A data to be included in the webhook payload so that I receive question data automatically when partnerships are created or updated.

**Why this priority**: This extends existing webhook infrastructure with Q&A data. It provides convenience for integration consumers but is not required for the core game flow.

**Independent Test**: Can be fully tested by triggering a webhook for a partnership that has submitted questions and verifying the payload includes the Q&A data. Delivers value by automating data delivery to integrated systems.

**Acceptance Scenarios**:

1. **Given** a partnership with submitted questions on a Q&A-enabled event, **When** a webhook is triggered for that partnership, **Then** the webhook payload includes the partnership's questions and answers.
2. **Given** a partnership with no submitted questions on a Q&A-enabled event, **When** a webhook is triggered, **Then** the webhook payload includes an empty questions list.
3. **Given** a partnership on an event with Q&A disabled, **When** a webhook is triggered, **Then** the webhook payload does not include a questions field (or includes an empty list).

---

### Edge Cases

- What happens when an organiser reduces the max questions limit after partners have already submitted more questions than the new limit? Existing questions should be preserved, but no new questions can be added until the count is within the new limit.
- What happens when an organiser reduces the max answers limit after questions with more answers exist? Existing answers should be preserved, but new questions must respect the new limit.
- What happens when a question is submitted without marking any answer as correct? The system should reject the submission — exactly one answer must be marked as correct.
- What happens when a partner submits a question with fewer than 2 answers? The system should reject the submission — a minimum of 2 answers is required for a meaningful question.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow organisers to enable or disable a Q&A game feature per event via the existing event update endpoint.
- **FR-002**: System MUST allow organisers to configure the maximum number of questions per partnership as part of the event update payload.
- **FR-003**: System MUST allow organisers to configure the maximum number of answers per question as part of the event update payload.
- **FR-004**: System MUST allow partners to create a single question with answer choices via a POST endpoint on their partnership space when Q&A is enabled for the event.
- **FR-005**: System MUST enforce that exactly one answer per question is marked as correct.
- **FR-006**: System MUST enforce the maximum number of questions per partnership as configured by the organiser.
- **FR-007**: System MUST enforce the maximum number of answers per question as configured by the organiser.
- **FR-008**: System MUST enforce a minimum of 2 answers per question.
- **FR-009**: System MUST allow partners to update a single existing question and its answers via a PUT endpoint.
- **FR-010**: System MUST allow partners to delete a single question (cascading to its answers) via a DELETE endpoint.
- **FR-011**: System MUST provide a public unauthenticated endpoint to retrieve all questions for an event, grouped by partnership.
- **FR-012**: System MUST include partnership Q&A data in the webhook payload when questions exist.
- **FR-013**: System MUST return the Q&A configuration (enabled status, max questions, max answers) as part of the event details.

### Key Entities

- **Q&A Configuration**: Event-level settings including enabled/disabled status, maximum questions per partnership, and maximum answers per question. Belongs to one event.
- **Question**: A question submitted by a partner, containing the question text. Belongs to one partnership. An event can have many questions across different partnerships.
- **Answer**: A possible answer to a question, containing the answer text and a flag indicating whether it is the correct answer. Belongs to one question. Each question has between 2 and the configured maximum number of answers.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Organisers can enable Q&A and configure limits for an event in under 1 minute.
- **SC-002**: Partners can create a question with answers in under 2 minutes.
- **SC-003**: External services can retrieve all event questions in a single request.
- **SC-004**: Webhook payloads include Q&A data within the same delivery latency as existing payloads.
- **SC-005**: 100% of question submissions that violate configured limits are rejected with a clear error message.

## Clarifications

### Session 2026-04-02

- Q: How should the event questions endpoint (US3) be authenticated? → A: Public unauthenticated endpoint (anyone with event slug can fetch).
- Q: Should Q&A configuration be managed via the existing event update endpoint or a dedicated new endpoint? → A: Part of the existing event update endpoint (add fields to event payload).
- Q: Should partner question management use individual CRUD operations or bulk replace? → A: Individual CRUD (POST to create, PUT to update, DELETE to remove a single question).
- Q: What happens to existing questions when an organiser disables Q&A? → A: Preserve questions (hidden from API responses, retained in database).

## Assumptions

- The Q&A configuration (enabled, max questions, max answers) is managed through the existing event update endpoint — no dedicated endpoint is needed.
- Partners interact with questions via their existing partnership space — no separate authentication or portal is needed.
- The "correct answer" flag is included in the API response for external services (the game application decides whether to expose it to players).
- Question and answer text is plain text (no rich formatting or media).
- There is no ordering or categorisation of questions beyond grouping by partnership.
- The event questions endpoint is public and unauthenticated — the data (company trivia) is non-sensitive and intended for consumption by external game services.
- Partner question management uses individual CRUD operations (one question at a time), consistent with how other partnership resources are managed.
- Partner Q&A CRUD endpoints are public (no authentication required), consistent with the booth activities pattern. Any client with the partnership ID can create/edit/delete questions. This is by design — partners access their partnership space via a unique link.
- When Q&A is disabled, existing questions are preserved in the database (not deleted). They are hidden from API responses and become available again if Q&A is re-enabled.
- On PUT (update), the new set of answers must respect the current `max_answers` limit, even if the existing question had more answers before a limit reduction.
