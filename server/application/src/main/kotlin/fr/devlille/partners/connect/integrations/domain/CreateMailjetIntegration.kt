package fr.devlille.partners.connect.integrations.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CreateMailjetIntegration(
    @SerialName("api_key")
    val apiKey: String,
    val secret: String,
) : CreateIntegration
