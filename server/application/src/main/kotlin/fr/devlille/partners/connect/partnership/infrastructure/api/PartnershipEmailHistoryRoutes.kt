package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.DEFAULT_PAGE_SIZE
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailHistoryRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * Routes for viewing partnership email history.
 *
 * Provides GET endpoint for organisers to view all emails sent to a partnership,
 * with pagination support and per-recipient delivery status.
 */
fun Route.partnershipEmailHistoryRoutes() {
    val partnershipEmailHistoryRepository by inject<PartnershipEmailHistoryRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/email-history") {
        install(AuthorizedOrganisationPlugin)

        /**
         * GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/email-history
         *
         * Returns paginated list of all emails sent to a partnership, ordered by most recent first.
         * Includes full email content, delivery status, and per-recipient delivery details.
         *
         * Query Parameters:
         * - page: Page number (default: 1)
         * - pageSize: Number of items per page (default: 20)
         *
         * Response: PaginatedResponse<PartnershipEmailHistoryResponse>
         * Status Codes:
         * - 200 OK: Email history retrieved successfully
         * - 401 Unauthorized: User not authorized for this organisation
         * - 404 Not Found: Partnership not found
         */
        get {
            val partnershipId = call.parameters.partnershipId
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            val response = partnershipEmailHistoryRepository.findByPartnershipId(
                partnershipId = partnershipId,
                page = page,
                pageSize = pageSize,
            )
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
