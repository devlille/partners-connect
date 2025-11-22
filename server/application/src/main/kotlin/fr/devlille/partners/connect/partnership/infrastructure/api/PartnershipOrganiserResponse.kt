package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.users.domain.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response containing partnership ID and organiser information.
 *
 * @property partnershipId UUID of the partnership
 * @property organiser Organiser user information or null if no organiser assigned
 */
@Serializable
data class PartnershipOrganiserResponse(
    @SerialName("partnership_id")
    val partnershipId: String,
    val organiser: User?,
)
