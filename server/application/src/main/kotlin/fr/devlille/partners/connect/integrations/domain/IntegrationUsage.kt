package fr.devlille.partners.connect.integrations.domain

import kotlinx.serialization.SerialName

enum class IntegrationUsage {
    @SerialName("notification")
    NOTIFICATION,

    @SerialName("billing")
    BILLING,

    @SerialName("ticketing")
    TICKETING,

    @SerialName("webhook")
    WEBHOOK,

    @SerialName("agenda")
    AGENDA,
}
