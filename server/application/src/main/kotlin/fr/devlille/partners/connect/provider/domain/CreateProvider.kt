package fr.devlille.partners.connect.provider.domain

import kotlinx.serialization.Serializable

@Serializable
data class CreateProvider(
    val name: String,
    val type: String,
    val website: String?,
    val phone: String?,
    val email: String?,
)
