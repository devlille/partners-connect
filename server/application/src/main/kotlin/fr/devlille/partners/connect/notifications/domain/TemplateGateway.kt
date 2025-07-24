package fr.devlille.partners.connect.notifications.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider

interface TemplateGateway {
    val provider: IntegrationProvider

    fun render(variables: NotificationVariables): String
}
