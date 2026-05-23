# Implementation Plan: 026-llm-chat-integration

This plan is the technical companion to [`spec.md`](./spec.md). Read the spec first.

It is written so a Claude instance opened in `partners-connect/server/` (and in the sibling `cortex/` workspace for the deployment-side) can execute the work without needing access to the original brainstorming conversation.

---

## 1. Architecture summary

```
┌───────────────────────────────────────────────────────────┐
│  partners-connect/server  (Ktor app, this workspace)       │
│                                                            │
│   App.kt                                                   │
│    └── routing { ... aiRoutes() }                          │
│                                                            │
│   fr.devlille.partners.connect.ai/                         │
│    ├── domain/        ChatRequest, ChatResponse, ...       │
│    ├── infrastructure/                                     │
│    │   ├── api/       AiRoutes.kt  (POST /chat, GET /models)│
│    │   ├── gateways/  OllamaLlmGateway.kt  (Koog wrapper)  │
│    │   └── bindings/  AiModule.kt  (Koin)                  │
│    └── (no db/ — feature is stateless in v1)               │
│                                                            │
│   talks HTTP to ──────────┐                                │
└───────────────────────────┼────────────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────────┐
│  cortex  (sibling workspace, owned separately)      │
│                                                            │
│   ollama/Dockerfile  ← FROM ollama/ollama + pre-pull       │
│                        gemma3:1b at build time             │
│   Dockerfile.deploy  ← pulls from GHCR                     │
│   .github/workflows/build-push.yaml  (→ ghcr.io)           │
│   .github/workflows/deploy.yaml  (clever-tools)            │
└───────────────────────────────────────────────────────────┘
```

Two completely separate deploys, joined at runtime by the env var `OLLAMA_BASE_URL` on the partners-connect side.

---

## 2. Koog 0.5.1 API gotchas

A prototype was built against Koog 0.5.1 and validated end-to-end against `gemma3:1b` and `llama3.2:3b`. Two documented APIs do NOT exist (or are private) in the published 0.5.1 artifact — work around them as follows. **Do not waste time trying the documented form; the docs lag the artifact.**

### 2.1 `OllamaClient.listModels()` is private

Compile error: `Cannot access 'suspend fun listModels(): OllamaModelsListResponseDTO': it is private in 'ai/koog/prompt/executor/ollama/client/OllamaClient'.`

**Workaround**: call Ollama's REST API directly. Ollama exposes `GET /api/tags` which is stable and well-documented at the Ollama level.

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class OllamaTagsResponse(val models: List<OllamaTag> = emptyList())

@Serializable
private data class OllamaTag(val name: String)

private val json = Json { ignoreUnknownKeys = true }

suspend fun listOllamaModels(http: HttpClient, baseUrl: String): List<String> {
    val body = http.get("$baseUrl/api/tags").bodyAsText()
    return json.decodeFromString<OllamaTagsResponse>(body).models.map { it.name }
}
```

Reuse the existing `ktor-client-core` + `ktor-client-java` from the `ktor-client` bundle (already in the version catalog) — no new client dependency needed.

### 2.2 `OllamaClient.createDynamicModel(name)` does not exist

Compile error: `Unresolved reference 'createDynamicModel'`.

**Workaround**: construct `LLModel` manually. `OllamaModels.Meta.LLAMA_3_2` and friends are just pre-built `LLModel` instances — building one ourselves for any model name Ollama knows about is the supported path.

```kotlin
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

fun ollamaModel(name: String): LLModel = LLModel(
    provider = LLMProvider.Ollama,
    id = name,
    capabilities = listOf(LLMCapability.Temperature),
    contextLength = DEFAULT_CONTEXT_LENGTH,
)

private const val DEFAULT_CONTEXT_LENGTH = 8192L
```

`contextLength` is required in 0.5.1 (another undocumented change vs the README examples). 8192 is a safe default that all small Ollama models support.

### 2.3 Local development

You have two options for running Ollama locally during development. Pick **option A** unless you need a fully Docker-isolated stack — the partners-connect server defaults `OLLAMA_BASE_URL` to `http://localhost:11434`, so a native daemon needs zero extra config.

#### Option A — Native Ollama (recommended)

Ollama runs as a host-level service and partners-connect (started via `./gradlew run` or Compose) reaches it on the loopback interface.

1. **Install Ollama once** (skip if already installed — check with `which ollama`):

   ```sh
   brew install ollama
   # or, on Linux:  curl -fsSL https://ollama.com/install.sh | sh
   ```

2. **Start the daemon.** Homebrew installs a launchd agent:

   ```sh
   brew services start ollama
   ```

   On Linux, run `ollama serve` in a separate terminal (or wire systemd).

3. **Pull the default model** (~815 MB, one-time):

   ```sh
   ollama pull gemma3:1b
   ```

4. **Verify Ollama itself responds:**

   ```sh
   curl -s http://localhost:11434/api/tags | jq '.models[].name'
   ```

5. **Boot partners-connect** without setting `OLLAMA_BASE_URL`. The default in `SystemVarEnv.Llm` matches the running daemon.

#### Option B — Docker Compose Ollama

Use this only if you need every dependency containerised. Add this service to `server/docker-compose.yml`:

```yaml
  ollama:
    image: ollama/ollama:latest
    container_name: partners-connect-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_models:/root/.ollama
    healthcheck:
      test: ["CMD", "ollama", "list"]
      interval: 10s
      timeout: 5s
      retries: 5
```

Append `ollama_models:` under the `volumes:` block at the bottom so pulled models survive `docker compose down`. Then:

```sh
docker compose up -d ollama
docker compose exec ollama ollama pull gemma3:1b
```

When the partners-connect app **also runs inside Compose**, override the env var so it resolves the container DNS name instead of `localhost`:

```yaml
  app:
    environment:
      OLLAMA_BASE_URL: http://ollama:11434
```

When the partners-connect app runs on the host (`./gradlew run`), keep the default — it will reach the published `11434` port on the loopback.

#### Smoke test — both layers

Run these in order. If layer 1 works but layer 2 returns `503`, the partners-connect server can't reach Ollama — check `OLLAMA_BASE_URL` resolution (host vs container DNS).

```sh
# Layer 1 — Ollama itself (bypass partners-connect)
curl -s http://localhost:11434/api/tags

# Layer 2 — partners-connect → Ollama through the new gateway
curl -s -X POST http://localhost:8080/orgs/<orgSlug>/ai/chat \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <token>" \
  -d '{"prompt":"Reply with: hello"}'
```

#### Security note

The gateway talks to Ollama over plain HTTP with no authentication. This is safe in exactly two scenarios:

- **Locally**, where Ollama listens on the loopback interface (`127.0.0.1`) and is not exposed.
- **In production**, where Clever Cloud's network group keeps Ollama on a private network with no public domain (see § 6.8 step 3).

**Never set `OLLAMA_HOST=0.0.0.0:11434` on a public host** — every Ollama API would be exposed to the internet without auth.

---

## 3. Version catalog additions

File: `gradle/libs.versions.toml`

```toml
[versions]
# ... existing entries ...
koog = "0.5.1"

[libraries]
# ... existing entries ...

# Koog — LLM client framework. Use the umbrella ktor artifact for transitive coverage
# of OllamaClient, SingleLLMPromptExecutor, and the prompt DSL.
# We do NOT use the install(Koog) Ktor plugin (we wire Koog manually via Koin),
# but pulling koog-ktor is the simplest way to get all the executor + client classes.
koog-ktor = { module = "ai.koog:koog-ktor", version.ref = "koog" }
```

Then add the dependency in `application/build.gradle.kts`:

```kotlin
dependencies {
    // ... existing ...
    implementation(libs.koog.ktor)
}
```

> **Why not the targeted artifacts (`ai.koog:prompt-executor-ollama-client`, etc.)?**
> Their exact Maven coordinates are not 100 % confirmed in the published 0.5.1 BOM, and the umbrella `koog-ktor` brings everything we need transitively. Bytecode bloat is small. If a follow-up wants to slim deps, that's the right time to reverse-engineer the targeted artifacts.

---

## 4. File-by-file implementation (partners-connect side)

All paths are relative to `application/src/main/kotlin/fr/devlille/partners/connect/`.

### 4.1 `ai/domain/ChatRequest.kt`

```kotlin
package fr.devlille.partners.connect.ai.domain

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val prompt: String,
    val model: String? = null,
    val system: String? = null,
)
```

### 4.2 `ai/domain/ChatResponse.kt`

```kotlin
package fr.devlille.partners.connect.ai.domain

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val model: String,
    val response: String,
)
```

### 4.4 `ai/domain/LlmGateway.kt`

> (Section 4.3 intentionally omitted — the numbering is kept stable so downstream § 4.x references don't shift.)

```kotlin
package fr.devlille.partners.connect.ai.domain

interface LlmGateway {
    suspend fun chat(
        userPrompt: String,
        system: String?,
        modelName: String,
    ): String

    suspend fun listOllamaModels(): List<String>
}
```

### 4.5 `ai/infrastructure/gateways/OllamaLlmGateway.kt`

This is where Koog lives. Patterns to follow:

- Construct `MultiLLMPromptExecutor` once at startup (in the Koin module), inject it here.
- Inject a `HttpClient` for the `/api/tags` call.
- All public methods are `suspend` — they call into Koog's coroutine-based API.

> **Naming note** — the chat parameter is `userPrompt` (not `prompt`) to avoid shadowing the Koog DSL builder `ai.koog.prompt.dsl.prompt(...)`. Match this in the interface (§ 4.4).

```kotlin
package fr.devlille.partners.connect.ai.infrastructure.gateways

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import fr.devlille.partners.connect.ai.domain.LlmGateway
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val DEFAULT_CONTEXT_LENGTH = 8192L

class OllamaLlmGateway(
    private val executor: SingleLLMPromptExecutor,
    private val http: HttpClient,
    private val ollamaBaseUrl: String,
) : LlmGateway {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun chat(
        userPrompt: String,
        system: String?,
        modelName: String,
    ): String {
        val model = LLModel(
            provider = LLMProvider.Ollama,
            id = modelName,
            capabilities = listOf(LLMCapability.Temperature),
            contextLength = DEFAULT_CONTEXT_LENGTH,
        )
        val chatPrompt = prompt("ai-chat") {
            system?.let { system(it) }
            user(userPrompt)
        }
        val messages = executor.execute(chatPrompt, model)
        return messages.joinToString("\n") { it.content }
    }

    override suspend fun listOllamaModels(): List<String> {
        val body = http.get("$ollamaBaseUrl/api/tags").bodyAsText()
        return json.decodeFromString<OllamaTagsResponse>(body).models.map { it.name }
    }

    @Serializable
    private data class OllamaTagsResponse(val models: List<OllamaTag> = emptyList())

    @Serializable
    private data class OllamaTag(val name: String)
}
```

**Error mapping** — wrap the gateway calls in `AiRoutes.kt` (not here). Catch Koog/Ktor client connectivity errors and re-throw as `ServiceUnavailableException` (see § 4.7 for where to add it).

### 4.6 `ai/infrastructure/bindings/AiModule.kt`

> **Type note** — `SingleLLMPromptExecutor` is declared in Koog 0.5.1 as
> `SingleLLMPromptExecutor(llmClient: LLMClient)`. Since the feature only targets Ollama, this single-client executor is the right fit.

```kotlin
package fr.devlille.partners.connect.ai.infrastructure.bindings

import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import fr.devlille.partners.connect.ai.domain.LlmGateway
import fr.devlille.partners.connect.ai.infrastructure.gateways.OllamaLlmGateway
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import io.ktor.client.HttpClient
import org.koin.dsl.module

val aiModule = module {
    single<LlmGateway> {
        val ollamaBaseUrl = SystemVarEnv.Llm.ollamaBaseUrl
        OllamaLlmGateway(
            executor = SingleLLMPromptExecutor(OllamaClient(baseUrl = ollamaBaseUrl)),
            http = get<HttpClient>(),
            ollamaBaseUrl = ollamaBaseUrl,
        )
    }
}
```

> **Note on the `HttpClient`**: it's already provided by `networkClientModule` in `internal/infrastructure/bindings/`. Don't create a new one.

> **Note on `SystemVarEnv.Llm`**: you'll need to add a nested object to `internal/infrastructure/system/SystemVarEnv.kt`. See § 4.8.

### 4.7 `ai/infrastructure/api/AiRoutes.kt`

> **Import notes**
> - `BadRequestException` is `io.ktor.server.plugins.BadRequestException` (Ktor framework type — there is no project-local one). It is already wired to 400 in `App.kt`'s `configureStatusPage()`.
> - There is no `EmptyStringValidationException` in this codebase. For an empty/blank `prompt`, throw `BadRequestException("prompt must not be blank")` directly — matches the existing pattern in `FlyerTemplateRepositoryExposed.kt`. The JSON schema's `"minLength": 1` will also reject `""` at the schema layer, so this branch only catches whitespace-only strings that slip past `minLength`.
> - `ServiceUnavailableException` does **not** exist yet — § 4.7b adds it.

```kotlin
package fr.devlille.partners.connect.ai.infrastructure.api

import fr.devlille.partners.connect.ai.domain.ChatRequest
import fr.devlille.partners.connect.ai.domain.ChatResponse
import fr.devlille.partners.connect.ai.domain.LlmGateway
import fr.devlille.partners.connect.internal.infrastructure.api.ServiceUnavailableException
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.net.ConnectException
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private const val DEFAULT_MODEL = "gemma3:1b"
private val logger = LoggerFactory.getLogger("ai.AiRoutes")

fun Route.aiRoutes() {
    val gateway by inject<LlmGateway>()

    route("/orgs/{orgSlug}/ai") {
        install(AuthorizedOrganisationPlugin)

        get("/models") {
            val models = withOllamaErrorHandling { gateway.listOllamaModels() }
            call.respond(HttpStatusCode.OK, models)
        }

        post("/chat") {
            val req = call.receive<ChatRequest>(schema = "ai_chat_request.schema.json")
            if (req.prompt.isBlank()) throw BadRequestException("prompt must not be blank")

            val modelName = req.model ?: DEFAULT_MODEL
            val started = System.currentTimeMillis()
            val response = withOllamaErrorHandling {
                gateway.chat(req.prompt, req.system, modelName)
            }
            val latency = System.currentTimeMillis() - started

            logger.info(
                "ai.chat ok model={} prompt_chars={} response_chars={} latency_ms={}",
                modelName, req.prompt.length, response.length, latency,
            )

            call.respond(HttpStatusCode.OK, ChatResponse(model = modelName, response = response))
        }
    }
}

// The shared `HttpClient` from `networkClientModule` is configured with
// `expectSuccess = true` and a `HttpResponseValidator` that rethrows 401 as
// `UnauthorizedException`. The gateway's `/api/tags` call goes through that
// client, so any non-2xx from Ollama would otherwise surface as the wrong
// status to the caller (401 or 500). Map all of those to 503 — Ollama's
// HTTP status is a property of the upstream, not of the partners-connect API.
//
// (The chat path uses Koog's own `OllamaClient`, which has its own HTTP stack;
// these catches don't apply to it, but the `ConnectException` /
// `ConnectTimeoutException` cases still do.)
private suspend fun <T> withOllamaErrorHandling(block: suspend () -> T): T =
    try {
        block()
    } catch (e: ConnectException) {
        throw ServiceUnavailableException("LLM backend is unreachable", e)
    } catch (e: ConnectTimeoutException) {
        throw ServiceUnavailableException("LLM backend timed out", e)
    } catch (e: ServerResponseException) {
        throw ServiceUnavailableException("LLM backend returned ${e.response.status}", e)
    } catch (e: ClientRequestException) {
        throw ServiceUnavailableException("LLM backend returned ${e.response.status}", e)
    }
```

> **Route convention** — declared as `Route.aiRoutes()` (matches `sponsoringRoutes()`, `providersRoutes()`, etc., which are called inside the `routing { }` block in `App.kt`). The newer `Application.digestRoutes()` style opens its own `routing { }` block; we deliberately do **not** follow that style to avoid nested `routing` blocks.

### 4.7b `internal/infrastructure/api/ServiceUnavailableException.kt`

New file — mirrors `ConflictException.kt` exactly. Two-arg constructor because the gateway re-throws with a wrapped cause.

```kotlin
package fr.devlille.partners.connect.internal.infrastructure.api

class ServiceUnavailableException(
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause)
```

And add the 503 mapping inside `Application.configureStatusPage()` in `App.kt`, alongside the existing `exception<ConflictException> { ... }` block:

```kotlin
exception<ServiceUnavailableException> { call, cause ->
    call.respond(
        status = HttpStatusCode.ServiceUnavailable,
        message = ResponseException(
            message = cause.message ?: "503 Service Unavailable",
            stack = cause.cause?.stackTraceToString(),
        ),
    )
}
```

Add the import at the top of `App.kt`:

```kotlin
import fr.devlille.partners.connect.internal.infrastructure.api.ServiceUnavailableException
```

### 4.8 `internal/infrastructure/system/SystemVarEnv.kt`

Add a nested `Llm` object alongside the existing `Exposed`, `Crypto`, `GoogleProvider`, `QontoProvider`. The actual convention in the file is `val xxx: String = System.getenv("KEY") ?: "default"` (eagerly evaluated `val`, no helper indirection — see lines 10-32 of `SystemVarEnv.kt`).

```kotlin
object Llm {
    val ollamaBaseUrl: String = System.getenv("OLLAMA_BASE_URL") ?: "http://localhost:11434"
}
```

### 4.9 JSON schema

`application/src/main/resources/schemas/ai_chat_request.schema.json`:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["prompt"],
  "properties": {
    "prompt": { "type": "string", "minLength": 1 },
    "model":  { "type": ["string", "null"] },
    "system": { "type": ["string", "null"] }
  },
  "additionalProperties": false
}
```

Then register the schema in `internal/infrastructure/ktor/ApplicationCall.ext.kt` (add a new `.register(readResourceFile("/schemas/ai_chat_request.schema.json"), SchemaType.DRAFT_7)` line inside the `schemas` lazy initializer).

### 4.10 OpenAPI updates

The OpenAPI source lives at `application/src/main/resources/openapi/openapi.yaml` and is **bundled** into `application/src/main/resources/openapi/documentation.yaml` (the latter is what Ktor serves at `/openapi`, see `App.kt:135`). **Edit `openapi.yaml`, never `documentation.yaml` by hand.**

Follow the project's `openapi-schemas` skill (`.claude/skills/openapi-schemas/`) for the full authoring + validation workflow. The end-to-end shape is:

- Add components `ChatRequest`, `ChatResponse`, `LlmModel` (typically as separate files under `openapi/components/` referenced via `$ref`, per existing convention — check how `ChatRequest` or `EventBudget` are organised).
- Add paths:
  - `POST /orgs/{orgSlug}/ai/chat`
  - `GET /orgs/{orgSlug}/ai/models`
- Tag both as `ai`.
- Mark them as requiring the existing org auth scheme.
- Run `cd server && npm run validate` (lints `openapi.yaml`) followed by `npm run bundle` (regenerates `documentation.yaml`) — both must pass and the bundled file must be checked in.

### 4.11 `App.kt` changes

Two edits:

1. Import `aiModule` and add it to `ApplicationConfig.modules`:

   ```kotlin
   import fr.devlille.partners.connect.ai.infrastructure.bindings.aiModule
   // ...
   modules = listOf(
       // ... existing modules ...
       aiModule,
   )
   ```

2. Import `aiRoutes` and mount it in the `routing { }` block:

   ```kotlin
   import fr.devlille.partners.connect.ai.infrastructure.api.aiRoutes
   // ...
   routing {
       // ... existing ...
       aiRoutes()
   }
   ```

---

## 5. Tests (partners-connect side)

Follow `.claude/skills/integration-tests/` and `contract-tests/` skills exactly. Test root: `application/src/test/kotlin/fr/devlille/partners/connect/ai/`.

### 5.1 Factories

`ai/factories/Chat.factory.kt`:

```kotlin
fun createChatRequest(
    prompt: String = "Hello",
    model: String? = null,
    system: String? = null,
) = ChatRequest(prompt = prompt, model = model, system = system)
```

### 5.2 Gateway under test — fake or mock?

Koog's `OllamaClient` talks to a real Ollama server. For tests, mock at the **`LlmGateway` interface level** (not the Koog level). This isolates tests from Koog and Ollama entirely.

Create `ai/FakeLlmGateway.kt` in the test source root:

```kotlin
class FakeLlmGateway(
    private val response: String = "fake response",
    private val models: List<String> = listOf("gemma3:1b"),
    private val throws: Throwable? = null,
) : LlmGateway {
    override suspend fun chat(userPrompt: String, system: String?, modelName: String) =
        throws?.let { throw it } ?: response
    override suspend fun listOllamaModels() =
        throws?.let { throw it } ?: models
}
```

Then in tests, override the Koin binding:

```kotlin
application {
    moduleSharedDb(userId = userId)
    GlobalContext.get().loadModules(listOf(module {
        single<LlmGateway> { FakeLlmGateway(response = "hi from test") }
    }))
}
```

### 5.3 Contract tests (HTTP shape)

`ai/infrastructure/api/AiChatRoutePostTest.kt`:
- 400 when prompt is empty
- 400 when JSON schema fails (extra field, wrong type)
- 401 when no auth header
- 200 with `{model, response}` shape on success

`ai/infrastructure/api/AiModelsRouteGetTest.kt`:
- 401 when no auth header
- 200 returning a JSON array of strings

### 5.4 Integration tests

`ai/AiRoutesTest.kt`:
- End-to-end: authenticated org calls `/ai/chat`, gets the fake response back.
- 503 path — connect failure: fake gateway throws `ConnectException` → expect `503`.
- 503 path — Ollama 5xx: fake gateway throws `ServerResponseException` → expect `503` (covers the `expectSuccess = true` translation from the shared `HttpClient`).
- 503 path — Ollama 4xx (incl. 401): fake gateway throws `ClientRequestException` → expect `503` (Ollama's auth state must not leak through as partners-connect's 401).

---

## 6. Cortex workspace changes

This section is for the Claude instance working in `~/Documents/workspace/cortex/`. Goal: trim the existing Ktor-scaffold workspace down to just an Ollama deployment, mirroring the partners-connect deploy pattern (GHCR + Clever Cloud + `clever-tools`).

### 6.1 Files to delete from cortex

- The entire `ktor-app/` directory (it was a prototype; the API now lives in partners-connect).
- The root `docker-compose.yml` (replaced — see § 6.2).
- `.env.example` keep (just trim to `DEFAULT_MODEL`).
- The existing `clevercloud/README.md` keep but rewrite (see § 6.5).

### 6.2 New layout

```
cortex/
├── ollama/
│   └── Dockerfile           # ← build-time pre-pull
├── Dockerfile.deploy        # ← pulls from GHCR, used by Clever Cloud
├── docker-compose.yml       # local smoke test only
├── .env.example
├── .github/workflows/
│   ├── build-push-ollama.yaml
│   └── deploy-ollama.yaml
├── clevercloud/README.md    # deployment runbook
└── README.md                # rewritten as: "this repo owns the Ollama deploy"
```

### 6.3 `ollama/Dockerfile`

Pre-pull `gemma3:1b` at build time so Clever Cloud cold-starts don't have to download 800 MB.

```dockerfile
FROM ollama/ollama:latest

ENV OLLAMA_HOST=0.0.0.0:11434

# Pre-pull the default model so the image ships ready-to-serve.
# This adds ~815 MB to the image but makes cold-start instant.
RUN (ollama serve &) && \
    sleep 5 && \
    ollama pull gemma3:1b && \
    pkill -f "ollama serve" && \
    sleep 2

EXPOSE 11434
```

> The `ollama serve &` + `sleep 5` + `pkill` dance is needed because `ollama pull` requires a running server. There is no `--build-pull` flag.

### 6.4 `Dockerfile.deploy`

Same pattern as `partners-connect/server/Dockerfile.deploy`:

```dockerfile
ARG TAG_VERSION=latest
FROM ghcr.io/<owner>/<repo>-ollama:${TAG_VERSION}
```

Replace `<owner>/<repo>` to match your GHCR layout (likely mirrors what partners-connect uses: e.g. `ghcr.io/devlille/devlille/cortex-ollama`).

### 6.5 `docker-compose.yml` (local smoke test only)

```yaml
services:
  ollama:
    build:
      context: ./ollama
    container_name: ollama
    ports:
      - "11434:11434"
    healthcheck:
      test: ["CMD", "ollama", "list"]
      interval: 10s
      timeout: 5s
      retries: 5
```

Local test: `docker compose up --build` then `curl http://127.0.0.1:11435/api/tags` (host port 11435 to avoid colliding with a native Ollama on 11434).

### 6.6 `.github/workflows/build-push-ollama.yaml`

Mirror `partners-connect/.github/workflows/ci-server.yaml`. Triggers on push to main; builds `ollama/Dockerfile` and pushes to GHCR as `ghcr.io/<owner>/<repo>-ollama:<sha>` and `:latest`.

Key differences from the server CI:
- `working-directory: ollama` instead of `server`
- No `npm install` / OpenAPI step
- Build time is dominated by the model pre-pull, not Gradle — expect 2-3 minutes

### 6.7 `.github/workflows/deploy-ollama.yaml`

**Manual trigger only** (`workflow_dispatch`) — Cortex has one Clever Cloud instance, deployed when someone clicks "Run workflow" or runs `gh workflow run`. There is no auto-deploy on tag push, no preprod auto-deploy on `workflow_run` after CI.

Required GitHub Secrets (new, separate from the server ones):
- `CLEVERCLOUD_OLLAMA_APP_ID`
- Reuse from partners-connect: `CLEVERCLOUD_TOKEN`, `CLEVERCLOUD_SECRET`, `CC_DOCKER_LOGIN_PASSWORD`, `CC_DOCKER_LOGIN_USERNAME`

Required Clever Cloud env vars set via `clever env set`:
- `CC_DOCKERFILE=Dockerfile.deploy`
- `CC_DOCKER_LOGIN_PASSWORD`, `CC_DOCKER_LOGIN_USERNAME`, `CC_DOCKER_LOGIN_SERVER=ghcr.io`
- `TAG_VERSION=<sha or latest>`
- `OLLAMA_NUM_PARALLEL=1` to cap memory use under concurrent requests

### 6.8 Clever Cloud setup checklist (one-time, manual)

1. Create **one** Docker app on Clever Cloud (no prod/preprod split — both partners-connect environments share this single Cortex backend).
2. Instance size: **M or L** (need ≥ 4 GB RAM for gemma3:1b headroom; S OOMs under load).
3. **Do NOT** attach a public domain — keep it on the internal network.
4. Add an **FS Bucket** add-on if you plan to pull more models at runtime (so they persist across restarts). For the pre-baked model only, skip this.
5. Create a **network group** including Cortex and every partners-connect app that needs to call it (production + preprod if present). Note the Cortex internal hostname.
6. In every partners-connect Clever Cloud app's env, set `OLLAMA_BASE_URL=http://<cortex-internal-hostname>:8080`. Port 8080 (not Ollama's default 11434) because Clever Cloud's healthcheck polls 8080; the Cortex deploy workflow sets `OLLAMA_HOST=0.0.0.0:8080` on the Cortex app to match. Add this to `cd-server.yaml` (§ 6.9) so future deploys don't lose it.
7. Note the new Cortex app ID and add it to GitHub Secrets per § 6.7.

### 6.9 `cd-server.yaml` update on the partners-connect side

Cortex has a single URL that's the same regardless of which partners-connect environment is deploying. Add **one** secret (`OLLAMA_BASE_URL`) to the partners-connect repo and pass it through unchanged.

In the `Configure Clever Cloud for monorepo` step, add to the env block:

```yaml
OLLAMA_BASE_URL: ${{ secrets.OLLAMA_BASE_URL }}
```

…and in the same step's `run:` block, add:

```bash
clever env set OLLAMA_BASE_URL "$OLLAMA_BASE_URL" --app $CLEVER_APP_ID
```

No per-environment branching needed — same value for partners-connect prod and preprod, since they share the Cortex backend.

---

## 7. Order of operations

Recommended sequence to minimise broken-state windows:

1. **Partners-connect side, behind a flag:**
   1. Add `OLLAMA_BASE_URL` to local `.env.example` (default `http://localhost:11434`).
   2. Implement § 4 (domain, gateway, routes, Koin, schemas, OpenAPI, App.kt wiring).
   3. Tests (§ 5).
   4. Local validation: `./gradlew check --no-daemon` + `npm run validate` must pass.
   5. Local smoke: with the existing `cortex` stack running (still has the prototype), point `OLLAMA_BASE_URL=http://localhost:11434` and hit `/orgs/.../ai/chat` from Bruno.
2. **Cortex side:**
   1. Implement § 6 (Ollama Dockerfile with pre-pull, GHCR build workflow, manual deploy workflow).
   2. Test locally: `docker compose up --build` and `curl http://127.0.0.1:11435/api/tags`.
   3. Merge to main → GHCR image builds and lands as `:latest` and `:<sha>`.
   4. Set up the single Clever Cloud app (§ 6.8).
   5. Manually trigger the deploy workflow with `image_tag=latest`.
3. **Wire up partners-connect:**
   1. Add the `OLLAMA_BASE_URL` secret and update `cd-server.yaml` (§ 6.9).
   2. Deploy partners-connect to its preprod environment.
   3. Smoke test the `/orgs/<slug>/ai/chat` route against partners-connect preprod (it talks to the same Cortex instance).
4. **Deploy partners-connect production** after a few days of preprod stability.

---

## 8. Pre-merge checklist

- [ ] `./gradlew ktlintCheck detekt --no-daemon` clean
- [ ] `./gradlew test --no-daemon` clean, ≥80 % coverage on `ai/` package
- [ ] `cd server && npm run validate` clean (OpenAPI valid)
- [ ] `./gradlew build --no-daemon` clean
- [ ] Local Bruno collection has working `/orgs/<slug>/ai/chat` and `/orgs/<slug>/ai/models` requests
- [ ] `App.kt` registers `aiModule` and `aiRoutes()`
- [ ] `SystemVarEnv.Llm.ollamaBaseUrl` reads `OLLAMA_BASE_URL`
- [ ] `cd-server.yaml` sets `OLLAMA_BASE_URL` in Clever Cloud
- [ ] Cortex repo: `Dockerfile`, `Dockerfile.deploy`, both workflows, README
- [ ] New GitHub secrets exist: `CLEVERCLOUD_OLLAMA_APP_ID` (in cortex repo), `OLLAMA_BASE_URL` (in partners-connect repo)
- [ ] (manual) Single Clever Cloud Ollama app exists, network group set up, app ID noted

---

## 9. Open questions / NEEDS CLARIFICATION

- **Q1**: Should the chat route also accept an `Accept-Language` header and pass it to a system prompt automatically (mirroring the `sponsoring` and `partnership` routes that use it)? — Default to no for v1; the caller supplies their own `system` if they want language steering.
- **Q2**: Should the prompt + response be persisted for audit / debugging? — Out of scope for v1 (see spec § "Out of scope"). If yes, add an `ai_chat_log/` Exposed table.
