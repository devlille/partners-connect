package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.events.domain.EventWithOrganisation
import kotlinx.serialization.Serializable

@Serializable
data class DetailedPartnershipResponse(
    val partnership: PartnershipDetail,
    val company: Company,
    val event: EventWithOrganisation,
)
