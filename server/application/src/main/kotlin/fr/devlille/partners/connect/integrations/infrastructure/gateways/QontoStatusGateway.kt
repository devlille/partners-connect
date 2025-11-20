package fr.devlille.partners.connect.integrations.infrastructure.gateways

import fr.devlille.partners.connect.billing.infrastructure.providers.QontoProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class QontoStatusGateway(
    private val qontoProvider: QontoProvider,
) : StatusGateway {
    override val provider: IntegrationProvider
        get() = IntegrationProvider.QONTO

    override suspend fun status(integrationId: UUID): Boolean {
        val config = transaction { QontoIntegrationsTable[integrationId] }
        return try {
            qontoProvider.listClients(null, config)
            true
        } catch (_: UnauthorizedException) {
            false
        }
    }
}
