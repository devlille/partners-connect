package fr.devlille.partners.connect.provider.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Provider(
    val id: String,
    val name: String,
    val type: String,
    val website: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
)
