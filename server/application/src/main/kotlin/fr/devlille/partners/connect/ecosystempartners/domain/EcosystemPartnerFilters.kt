package fr.devlille.partners.connect.ecosystempartners.domain

import kotlinx.serialization.Serializable

@Serializable
data class EcosystemPartnerFilters(
    val categoryId: String? = null,
    val validated: Boolean? = null,
    val declined: Boolean = false,
)
