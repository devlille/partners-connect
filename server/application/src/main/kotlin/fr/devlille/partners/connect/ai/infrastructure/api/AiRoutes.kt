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
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.net.ConnectException

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
                modelName,
                req.prompt.length,
                response.length,
                latency,
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
