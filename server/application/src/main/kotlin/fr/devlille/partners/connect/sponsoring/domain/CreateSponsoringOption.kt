package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.Serializable

@Serializable
class CreateSponsoringOption(
    val translations: List<TranslatedLabel>,
    val price: Int? = null,
)

@Serializable
data class TranslatedLabel(
    val language: String,
    val name: String,
    val description: String? = null,
)
