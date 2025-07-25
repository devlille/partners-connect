package fr.devlille.partners.connect.integrations.application

import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.IntegrationRegistrar
import fr.devlille.partners.connect.integrations.domain.IntegrationRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import io.ktor.server.plugins.NotFoundException
import java.util.UUID

class IntegrationRepositoryExposed(
    private val registrars: List<IntegrationRegistrar<*>>,
) : IntegrationRepository {
    override fun register(eventId: UUID, usage: IntegrationUsage, input: CreateIntegration): UUID {
        val registrar = registrars.find { it.supports(input) && usage in it.supportedUsages }
            ?: throw NotFoundException("No registrar found for input ${input::class.simpleName} and usage $usage")
        @Suppress("UNCHECKED_CAST")
        return (registrar as IntegrationRegistrar<CreateIntegration>)
            .register(eventId, usage, input)
    }
}
