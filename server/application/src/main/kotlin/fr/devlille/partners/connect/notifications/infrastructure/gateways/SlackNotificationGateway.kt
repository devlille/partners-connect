package fr.devlille.partners.connect.notifications.infrastructure.gateways

import com.slack.api.Slack
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.resources.readResourceFile
import fr.devlille.partners.connect.notifications.domain.Destination
import fr.devlille.partners.connect.notifications.domain.NotificationGateway
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import java.util.UUID

class SlackNotificationGateway(
    private val slack: Slack,
) : NotificationGateway {
    override val provider = IntegrationProvider.SLACK

    override fun getDestination(
        eventId: UUID,
        partnership: PartnershipItem,
    ): Destination {
        TODO("Not yet implemented")
    }

    override suspend fun send(integrationId: UUID, variables: NotificationVariables): Boolean {
        val path = "/notifications/${provider.name.lowercase()}/${variables.usageName}/${variables.language}.md"
        val message = try {
            variables.populate(readResourceFile(path))
        } catch (_: IllegalArgumentException) {
            return false
        }
        val config = SlackIntegrationsTable[integrationId]
        val response = slack.methods(config.token).chatPostMessage {
            it.channel(config.channel).text(message)
        }
        return response.isOk
    }

    override suspend fun send(
        integrationId: UUID,
        header: String,
        body: String,
        destination: Destination,
    ): Boolean {
        TODO("Not yet implemented")
    }
}
