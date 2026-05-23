package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.Serializable

@Serializable
data class DraftPartnershipEmailResponse(
    val draft: String,
)
