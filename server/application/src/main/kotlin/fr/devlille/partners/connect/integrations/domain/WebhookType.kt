package fr.devlille.partners.connect.integrations.domain

import kotlinx.serialization.Serializable

@Serializable
enum class WebhookType {
    ALL,
    PARTNERSHIP,
}