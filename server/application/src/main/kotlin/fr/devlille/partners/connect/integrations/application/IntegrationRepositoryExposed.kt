package fr.devlille.partners.connect.integrations.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.IntegrationRegistrar
import fr.devlille.partners.connect.integrations.domain.IntegrationRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class IntegrationRepositoryExposed(
    private val registrars: List<IntegrationRegistrar<*>>,
) : IntegrationRepository {
    override fun register(eventSlug: String, usage: IntegrationUsage, input: CreateIntegration): UUID {
        val eventId = transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException(
                    code = ErrorCode.EVENT_NOT_FOUND,
                    message = "Event with slug $eventSlug not found",
                    meta = mapOf(MetaKeys.EVENT to eventSlug)
                )
            event.id.value
        }
        val registrar = registrars.find { it.supports(input) && usage in it.supportedUsages }
            ?: throw NotFoundException(
                code = ErrorCode.NOT_FOUND,
                message = "No registrar found for input ${input::class.simpleName} and usage $usage",
                meta = mapOf(
                    MetaKeys.RESOURCE to "registrar",
                    MetaKeys.OPERATION to usage.toString()
                )
            )
        @Suppress("UNCHECKED_CAST")
        return (registrar as IntegrationRegistrar<CreateIntegration>)
            .register(eventId, usage, input)
    }
}
