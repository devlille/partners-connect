package fr.devlille.partners.connect.ai.domain

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val model: String,
    val response: String,
)
