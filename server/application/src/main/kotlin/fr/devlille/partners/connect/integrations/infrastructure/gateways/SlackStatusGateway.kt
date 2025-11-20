package fr.devlille.partners.connect.integrations.infrastructure.gateways

import com.slack.api.Slack
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class SlackStatusGateway(
    private val slack: Slack,
) : StatusGateway {
    override val provider: IntegrationProvider
        get() = IntegrationProvider.SLACK

    override suspend fun status(integrationId: UUID): Boolean {
        val config = transaction { SlackIntegrationsTable[integrationId] }
        val response = slack.methods(config.token).apiTest {
            it.foo("bar")
        }
        return response.isOk
    }
}
