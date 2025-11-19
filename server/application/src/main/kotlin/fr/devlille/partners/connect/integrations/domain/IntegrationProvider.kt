package fr.devlille.partners.connect.integrations.domain

import kotlinx.serialization.SerialName

enum class IntegrationProvider {
    @SerialName("slack")
    SLACK,

    @SerialName("mailjet")
    MAILJET,

    @SerialName("qonto")
    QONTO,

    @SerialName("billetweb")
    BILLETWEB,

    @SerialName("webhook")
    WEBHOOK,

    @SerialName("openplanner")
    OPENPLANNER,
}
