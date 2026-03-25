package fr.devlille.partners.connect.sponsoring.domain

import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import kotlinx.serialization.Serializable

@Serializable
data class SponsoringOptionDetailWithPartners(
    val option: SponsoringOptionWithTranslations,
    val partnerships: List<PartnershipItem>,
)
