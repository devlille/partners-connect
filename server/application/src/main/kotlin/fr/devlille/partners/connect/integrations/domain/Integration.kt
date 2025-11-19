package fr.devlille.partners.connect.integrations.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Integration(
    val id: String,
    val provider: IntegrationProvider,
    val usage: IntegrationUsage,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
)
