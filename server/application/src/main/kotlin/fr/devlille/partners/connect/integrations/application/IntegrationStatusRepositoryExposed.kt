package fr.devlille.partners.connect.integrations.application

import fr.devlille.partners.connect.integrations.domain.IntegrationStatusRepository
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import fr.devlille.partners.connect.integrations.infrastructure.gateways.StatusGateway
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class IntegrationStatusRepositoryExposed(
    private val gateways: List<StatusGateway>,
) : IntegrationStatusRepository {
    override suspend fun status(integrationId: UUID): Boolean {
        val integration = transaction { IntegrationEntity.findById(integrationId) }
            ?: throw NotFoundException("Not found integration with id: $integrationId")
        return gateways.find { it.provider == integration.provider }?.status(integrationId) ?: false
    }
}
