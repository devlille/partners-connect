package fr.devlille.partners.connect.webhooks.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebhookAnswer(
    val id: String,
    val answer: String,
    @SerialName("is_correct")
    val isCorrect: Boolean,
    val order: Int,
)
