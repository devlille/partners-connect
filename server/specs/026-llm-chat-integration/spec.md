# Feature Specification: Self-hosted LLM chat integration

**Feature Branch**: `026-llm-chat-integration`
**Created**: 2026-05-23
**Status**: Draft
**Input**: User description: "Integrate a self-hosted open-source LLM (Ollama, running as a separate Clever Cloud Docker app) into the partners-connect server via Koog (JetBrains' Kotlin AI framework), exposing chat endpoints under `/orgs/{orgSlug}/ai/...`. Default to `gemma3:1b` for cost/CPU-friendliness; allow swapping models per request."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Send a prompt and get a generated response (Priority: P1)

As an organiser authenticated on a given organisation, I can `POST /orgs/{orgSlug}/ai/chat` with a prompt and get back a generated response from the self-hosted LLM. This is the minimum viable feature: a single round-trip chat call backed by Ollama.

**Why this priority**: The whole feature exists to enable LLM-backed UX (drafting partner emails, summarising partnerships, etc.). Without this endpoint nothing else matters — every other story builds on it.

**Independent Test**: With `OLLAMA_BASE_URL` pointing at a running Ollama instance that has `gemma3:1b` pulled, a `POST /orgs/{orgSlug}/ai/chat` with body `{"prompt":"Reply with: hello"}` returns 200 and `{"model":"gemma3:1b","response":"..."}` where the response is non-empty.

**Acceptance Scenarios**:

1. **Given** an authenticated organiser and Ollama reachable with `gemma3:1b` pulled, **When** they `POST /orgs/{orgSlug}/ai/chat` with `{"prompt":"Hello"}`, **Then** the server returns 200 with `{"model":"gemma3:1b","response":"<non-empty text>"}`.
2. **Given** the same setup, **When** the body includes `"system":"You are terse. Answer in 1 sentence."`, **Then** the response reflects that system instruction.
3. **Given** the same setup, **When** the body includes `"model":"llama3.2:3b"` and that model is pulled in Ollama, **Then** the response field `"model"` equals `"llama3.2:3b"`.
4. **Given** an unauthenticated request to the same route, **When** sent, **Then** the server returns 401 (via `AuthorizedOrganisationPlugin`).

---

### User Story 2 - Discover available models (Priority: P2)

As an organiser, I can `GET /orgs/{orgSlug}/ai/models` to know which models are currently pulled into the Ollama instance, so that I can pick a valid `"model"` value when calling `/chat`.

**Why this priority**: Without this, callers have to guess model names. It's not blocking the core chat use case but it makes the API usable from a UI or Bruno collection without out-of-band knowledge.

**Independent Test**: With Ollama running and at least `gemma3:1b` pulled, `GET /orgs/{orgSlug}/ai/models` returns 200 with a JSON array of model name strings that includes `"gemma3:1b"`.

**Acceptance Scenarios**:

1. **Given** Ollama with `gemma3:1b` and `llama3.2:3b` pulled, **When** an authenticated organiser calls `GET /orgs/{orgSlug}/ai/models`, **Then** the response is `["gemma3:1b","llama3.2:3b"]` (order not guaranteed).
2. **Given** an empty Ollama (no models pulled), **When** the same call is made, **Then** the response is `[]`.

---

### Edge Cases

- **Ollama unreachable** (connection refused, DNS resolution failure, timeout): return **503 Service Unavailable** with a message naming Ollama. Do not leak the internal hostname to the response body.
- **Model not pulled in Ollama** (404 from Ollama's `/api/generate`): return **400 Bad Request** with message `"model '<name>' is not available; call GET /ai/models to list pulled models"`.
- **Empty prompt** (`""` or whitespace-only): return **400 Bad Request**. `""` is rejected by the JSON schema (`"minLength": 1`) via `RequestBodyValidationException`; whitespace-only is rejected by an explicit `BadRequestException("prompt must not be blank")` in the route handler.
- **Prompt exceeds context length** (Ollama returns truncation or error): for v1, do not pre-validate — let Ollama's error surface as a 503 with the Ollama-reported message. Pre-validation can come later when we have token counting.
- **Concurrent requests from the same org**: Koog's `OllamaClient` is thread-safe; no special handling required. Ollama itself queues requests serially per model — concurrent callers will see latency stack up, which is acceptable for v1.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose `POST /orgs/{orgSlug}/ai/chat` accepting JSON body `{prompt: string, model?: string, system?: string}` and returning `200` with JSON `{model: string, response: string}`.
- **FR-002**: System MUST expose `GET /orgs/{orgSlug}/ai/models` returning a JSON array of model name strings pulled into the Ollama instance.
- **FR-003**: Both endpoints MUST install `AuthorizedOrganisationPlugin` so that only members of `{orgSlug}` with `canEdit` may call them.
- **FR-004**: System MUST default the model to `gemma3:1b` when `model` is absent from the request body.
- **FR-005**: System MUST read the Ollama base URL from env var `OLLAMA_BASE_URL`. Default for local dev is `http://localhost:11434`; in deployed environments this is the Clever Cloud internal hostname of the Ollama app.
- **FR-007**: System MUST validate that `prompt` is non-empty after trimming. The JSON schema (FR-010) enforces `"minLength": 1` at the schema layer; whitespace-only prompts that slip past the schema MUST throw `io.ktor.server.plugins.BadRequestException("prompt must not be blank")` (→ 400 via the existing `BadRequestException` handler in `App.kt`'s `configureStatusPage()`).
- **FR-008**: System MUST handle Ollama connectivity failures (connect timeout, refused, DNS) by throwing a new `ServiceUnavailableException` (added under `internal/infrastructure/api/`, mirroring `ConflictException`) mapped to **503 Service Unavailable** via a new handler in `App.kt`'s `configureStatusPage()`.
- **FR-009**: System MUST log each chat call's `orgSlug`, model name, prompt length, response length, and latency in milliseconds at INFO level. The prompt and response content MUST NOT be logged.
- **FR-010**: Request body validation MUST go through `call.receive<T>(schema = "...")` with a JSON schema in `application/src/main/resources/schemas/ai_chat_request.schema.json` (per project convention), and the schema MUST be registered in the `schemas` lazy initializer in `internal/infrastructure/ktor/ApplicationCall.ext.kt`.
- **FR-011**: The OpenAPI source spec at `application/src/main/resources/openapi/openapi.yaml` MUST be updated to document the two new endpoints, and the bundled `application/src/main/resources/openapi/documentation.yaml` MUST be regenerated. Both `npm run validate` and `npm run bundle` MUST pass; see the `openapi-schemas` skill for the authoring workflow.
- **FR-012**: A new Koin module `aiModule` MUST be registered in `App.kt`'s `ApplicationConfig.modules` list.
- **FR-013**: The new top-level `Route.aiRoutes()` function MUST be mounted in `App.kt`'s `routing {}` block alongside the other `*Routes()` calls.
- **FR-014**: All new code MUST pass `./gradlew ktlintCheck detekt test --no-daemon`.

### Key Entities

- **ChatRequest** (`domain/ChatRequest.kt`): represents the inbound payload. Fields: `prompt: String`, `model: String? = null`, `system: String? = null`. `@Serializable`.
- **ChatResponse** (`domain/ChatResponse.kt`): represents the outbound payload. Fields: `model: String`, `response: String`. `@Serializable`.
- **LlmModel** (`domain/LlmModel.kt`): wrapper for the `/ai/models` response item. Field: `name: String`. `@Serializable`.
- **LlmGateway** (`domain/LlmGateway.kt`): the single business contract. Methods:
  - `suspend fun chat(userPrompt: String, system: String?, modelName: String): String` — parameter is named `userPrompt` to avoid shadowing the Koog DSL builder `ai.koog.prompt.dsl.prompt(...)`.
  - `suspend fun listOllamaModels(): List<String>`

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For `gemma3:1b` on the Clever Cloud Ollama instance, the p50 latency of `POST /ai/chat` for a 50-token prompt and a ≤200-token response is under **5 seconds** end-to-end (measured server-side, excluding network).
- **SC-002**: All quality gates pass in CI: `./gradlew ktlintCheck detekt test --no-daemon` and `npm run validate`.
- **SC-003**: Test coverage for the new `ai/` module is **≥ 80 %** (contract tests + integration tests + gateway unit tests).
- **SC-004**: After deploy, an authenticated organiser can hit `https://<preprod-base-url>/orgs/<orgSlug>/ai/chat` with a sample prompt and receive a non-empty response within 10 seconds (cold-start budget included).
- **SC-005**: The Ollama Clever Cloud app cold-starts in under **30 seconds** when the `gemma3:1b` model is pre-baked into the image (FR side: see `infrastructure.md`).

## Out of scope for v1

- Streaming responses (Koog supports `executeStreaming`; can be added in a follow-up).
- Token counting / pre-validation of prompt length.
- Agent loops (Koog's `aiAgent(strategy = reActStrategy(), ...)`) — this v1 is a single round-trip.
- Tools / function calling.
- Embeddings / RAG (Koog supports them; out of scope here).
- Persistent chat history.
- Rate limiting per org (relies on existing infrastructure; if needed, a follow-up).
- Public/unauthenticated access — explicitly excluded; the endpoint is always `/orgs/...` scoped.

## Cross-references

- **Implementation plan**: see `plan.md` in this folder — contains file-by-file code structure, Koog API gotchas, dependency additions, and the deployment plan for the companion Ollama Clever Cloud app.
- **Companion repository**: `~/Documents/workspace/cortex/` (separate workspace) — owns the Ollama Docker image, the GHCR build workflow, and the Clever Cloud deploy workflow. See `plan.md` § "Cortex workspace changes" for the exact files to create/modify.
- **Reference prototype**: a working Koog + Ktor + Ollama prototype was built in `~/Documents/workspace/cortex/ktor-app/` (will be deleted as part of this work). It validated the Koog 0.5.1 API; see `plan.md` § "Koog 0.5.1 API gotchas" for what we learned.
