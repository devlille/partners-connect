package fr.devlille.partners.connect.integrations.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface CreateIntegration {
    @Serializable
    class CreateMailjetIntegration(
        @SerialName("api_key")
        val apiKey: String,
        val secret: String,
    ) : CreateIntegration

    @Serializable
    data class CreateSlackIntegration(val token: String, val channel: String) : CreateIntegration

    @Serializable
    class CreateQontoIntegration(
        @SerialName("api_key")
        val apiKey: String,
        val secret: String,
        @SerialName("sandbox_token")
        val sandboxToken: String,
    ) : CreateIntegration

    @Serializable
    class CreateBilletWebIntegration(
        val basic: String,
        @SerialName("event_id")
        val eventId: String,
        @SerialName("rate_id")
        val rateId: String,
    ) : CreateIntegration
}
