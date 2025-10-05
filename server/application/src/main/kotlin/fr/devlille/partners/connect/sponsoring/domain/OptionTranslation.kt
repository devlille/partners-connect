package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.Serializable

@Serializable
data class OptionTranslation(
    val language: String,
    val name: String,
    val description: String?,
)
