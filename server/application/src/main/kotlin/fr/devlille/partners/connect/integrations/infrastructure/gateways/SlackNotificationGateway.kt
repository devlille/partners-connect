package fr.devlille.partners.connect.integrations.infrastructure.gateways

import com.slack.api.Slack
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.NotificationGateway
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackConfigDao
import java.util.UUID

class SlackNotificationGateway(
    private val slack: Slack,
    private val configDao: SlackConfigDao,
) : NotificationGateway {
    override val provider = IntegrationProvider.SLACK

    override fun send(integrationId: UUID, message: String): Boolean {
        val config = configDao.get(integrationId)
        val response = slack.methods(config.token).chatPostMessage {
            it.channel(config.channel).text(message)
        }
        return response.isOk
    }
}
