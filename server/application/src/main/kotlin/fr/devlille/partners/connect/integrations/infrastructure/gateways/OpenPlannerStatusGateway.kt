package fr.devlille.partners.connect.integrations.infrastructure.gateways

import fr.devlille.partners.connect.agenda.infrastructure.providers.OpenPlannerProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.OpenPlannerIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class OpenPlannerStatusGateway(
    private val openPlannerProvider: OpenPlannerProvider,
) : StatusGateway {
    override val provider: IntegrationProvider
        get() = IntegrationProvider.OPENPLANNER

    override suspend fun status(integrationId: UUID): Boolean {
        val config = transaction { OpenPlannerIntegrationsTable[integrationId] }
        return try {
            openPlannerProvider.status(config)
            true
        } catch (_: UnauthorizedException) {
            false
        }
    }
}
