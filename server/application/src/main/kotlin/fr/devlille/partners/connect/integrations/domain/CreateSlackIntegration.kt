package fr.devlille.partners.connect.integrations.domain

import kotlinx.serialization.Serializable

@Serializable
data class CreateSlackIntegration(val token: String, val channel: String) : CreateIntegration
