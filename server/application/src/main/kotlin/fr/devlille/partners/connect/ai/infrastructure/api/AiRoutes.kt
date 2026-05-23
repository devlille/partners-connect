package fr.devlille.partners.connect.ai.infrastructure.api

import fr.devlille.partners.connect.ai.domain.ChatRequest
import fr.devlille.partners.connect.ai.domain.LlmRepository
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.aiRoutes() {
    val repository by inject<LlmRepository>()

    route("/orgs/{orgSlug}/ai") {
        install(AuthorizedOrganisationPlugin)

        get("/models") {
            call.respond(HttpStatusCode.OK, repository.listModels())
        }

        post("/chat") {
            val request = call.receive<ChatRequest>(schema = "ai_chat_request.schema.json")
            call.respond(HttpStatusCode.OK, repository.chat(request))
        }
    }
}
