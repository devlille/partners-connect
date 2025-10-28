package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to validate a partnership with customizable package details.
 *
 * Organizers can override pack defaults for ticket count, job offers, and booth size.
 * Missing fields will use the selected sponsoring pack's default values.
 *
 * Validation is enforced via JSON schema (validate_partnership_request.schema.json):
 * - nbTickets: Optional integer >= 0
 * - nbJobOffers: Required integer >= 0
 * - boothSize: Optional non-empty string
 *
 * @property nbTickets Optional number of tickets. Must be >= 0. Uses pack default if null.
 * @property nbJobOffers Required number of job offers. Must be >= 0.
 * @property boothSize Optional booth size (e.g., "3x3m", "6x2m").
 *                     Must exist in any event pack if provided. Uses pack default if null.
 */
@Serializable
data class ValidatePartnershipRequest(
    @SerialName("nb_tickets")
    val nbTickets: Int? = null,
    @SerialName("nb_job_offers")
    val nbJobOffers: Int,
    @SerialName("booth_size")
    val boothSize: String? = null,
)
