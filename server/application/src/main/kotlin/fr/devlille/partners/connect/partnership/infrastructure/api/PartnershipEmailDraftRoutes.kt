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

private const val MAIL_DRAFT_MODEL = "gemini-2.5-flash"

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
        |
        |Follow the organiser's instructions in the user prompt. Output only the email body —
        |no subject line, no greeting/signature placeholders, no surrounding commentary.
        |
        |Formatting rules:
        |- Plain text only. The output is sent as an email body, so do NOT use Markdown:
        |  no **bold**, no *italics*, no `code`, no # headers, no Markdown bullet characters.
        |- Use real line breaks. Separate paragraphs with a blank line. When listing items,
        |  put each item on its own line (a plain "1." / "2." prefix is fine; no "- ").
        |
        |The following partners may be recipients of this email. Treat the list as background
        |context only — do not enumerate them or address each one by name in the output unless
        |the user prompt explicitly asks for it. Use the listed languages to pick the right
        |output language when the user prompt does not specify one:
        |$partnersBlock
        """.trimMargin()
}
