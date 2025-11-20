package fr.devlille.partners.connect.integrations.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.tickets.infrastructure.providers.BilletWebProvider
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class BilletWebStatusGateway(
    private val billetWebProvider: BilletWebProvider,
) : StatusGateway {
    override val provider: IntegrationProvider
        get() = IntegrationProvider.BILLETWEB

    override suspend fun status(integrationId: UUID): Boolean {
        val config = transaction { BilletWebIntegrationsTable[integrationId] }
        return try {
            billetWebProvider.listTariffs(config)
            true
        } catch (_: UnauthorizedException) {
            false
        }
    }
}
