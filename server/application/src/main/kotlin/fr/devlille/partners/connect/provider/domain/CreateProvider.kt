package fr.devlille.partners.connect.provider.domain

import kotlinx.serialization.Serializable

@Serializable
data class CreateProvider(
    val name: String,
    val type: String,
    val website: String? = null,
    val phone: String? = null,
    val email: String? = null,
)
