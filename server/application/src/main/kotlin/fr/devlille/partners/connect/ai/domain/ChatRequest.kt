package fr.devlille.partners.connect.ai.domain

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val prompt: String,
    val model: String? = null,
    val system: String? = null,
)
