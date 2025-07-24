package fr.devlille.partners.connect.notifications.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.notifications.domain.TemplateGateway

class SlackTemplateGateway : TemplateGateway {
    override val provider: IntegrationProvider
        get() = IntegrationProvider.SLACK

    override fun render(variables: NotificationVariables): String {
        val path = "/notifications/${provider.name.lowercase()}/${variables.usageName}/${variables.language}.md"
        val resource = object {}.javaClass.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Missing resource for path $path")
        return variables.populate(
            resource.bufferedReader().use {
                it.readLines().joinToString("\n")
            },
        )
    }
}
