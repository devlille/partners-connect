package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.ai.domain.ChatRequest
import fr.devlille.partners.connect.ai.domain.LlmRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.DraftPartnershipEmailRequest
import fr.devlille.partners.connect.partnership.domain.DraftPartnershipEmailResponse
import fr.devlille.partners.connect.partnership.domain.PartnershipDraftContext
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

private const val MAIL_DRAFT_MODEL = "gemini-2.0-flash"

fun Route.partnershipEmailDraftRoutes() {
    val partnershipRepository by inject<PartnershipRepository>()
    val llmRepository by inject<LlmRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/email/draft") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val request = call.receive<DraftPartnershipEmailRequest>(
                schema = "draft_partnership_email_request.schema.json",
            )
            val partnershipIds = request.partnershipIds.map { it.toUUID() }
            val contexts = partnershipRepository.getDraftContexts(eventSlug, partnershipIds)
            val draft = llmRepository.chat(
                ChatRequest(
                    prompt = request.prompt,
                    model = MAIL_DRAFT_MODEL,
                    system = buildSystemPrompt(contexts),
                ),
            )
            call.respond(HttpStatusCode.OK, DraftPartnershipEmailResponse(draft = draft.response))
        }
    }
}

internal fun buildSystemPrompt(contexts: List<PartnershipDraftContext>): String {
    val partnersBlock = contexts.joinToString("\n") { ctx ->
        val pack = ctx.packName ?: "no pack"
        "- ${ctx.companyName} ($pack, status: ${ctx.status}, language: ${ctx.language})"
    }
    return """
        |You are an email-drafting assistant for an event organiser.
        |The organiser is composing one email body that will be sent to the following partners:
        |$partnersBlock
        |
        |Follow the organiser's instructions in the user prompt. Output only the email body —
        |no subject line, no greeting/signature placeholders, no surrounding commentary.
        """.trimMargin()
}
