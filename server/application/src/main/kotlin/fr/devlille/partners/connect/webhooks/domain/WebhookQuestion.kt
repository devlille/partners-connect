package fr.devlille.partners.connect.webhooks.domain

import kotlinx.serialization.Serializable

@Serializable
data class WebhookQuestion(
    val id: String,
    val question: String,
    val order: Int,
    val answers: List<WebhookAnswer> = emptyList(),
)
