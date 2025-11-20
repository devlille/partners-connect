package fr.devlille.partners.connect.integrations.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.MailjetIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.notifications.infrastructure.providers.MailjetProvider
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.io.encoding.ExperimentalEncodingApi

class MailjetStatusGateway(
    private val mailjetProvider: MailjetProvider,
) : StatusGateway {
    override val provider: IntegrationProvider
        get() = IntegrationProvider.MAILJET

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun status(integrationId: UUID): Boolean {
        val config = transaction { MailjetIntegrationsTable[integrationId] }
        return try {
            mailjetProvider.status(config)
            true
        } catch (_: UnauthorizedException) {
            false
        }
    }
}
