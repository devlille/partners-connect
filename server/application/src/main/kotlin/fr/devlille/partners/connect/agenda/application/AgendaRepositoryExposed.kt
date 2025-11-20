package fr.devlille.partners.connect.agenda.application

import fr.devlille.partners.connect.agenda.domain.AgendaGateway
import fr.devlille.partners.connect.agenda.domain.AgendaRepository
import fr.devlille.partners.connect.agenda.domain.AgendaResponse
import fr.devlille.partners.connect.agenda.domain.Session
import fr.devlille.partners.connect.agenda.domain.Speaker
import fr.devlille.partners.connect.agenda.infrastructure.db.SessionEntity
import fr.devlille.partners.connect.agenda.infrastructure.db.SessionsTable
import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakerEntity
import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakersTable
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class AgendaRepositoryExposed(
    private val gateways: List<AgendaGateway>,
) : AgendaRepository {
    override suspend fun fetchAndStore(eventSlug: String) {
        val event = transaction {
            EventEntity.findBySlug(eventSlug) ?: throw NotFoundException("Event $eventSlug not found")
        }
        val integration = transaction {
            IntegrationEntity.singleIntegration(event.id.value, IntegrationUsage.AGENDA)
        }
        val gateway = gateways.find { it.provider == integration.provider }
            ?: throw NotFoundException("No gateway for provider ${integration.provider}")
        gateway.fetchAndStore(integration.id.value, event.id.value)
    }

    override fun getAgendaByEventSlug(eventSlug: String): AgendaResponse = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event $eventSlug not found")
        val sessions = SessionEntity
            .find { SessionsTable.eventId eq event.id.value }
            .map { sessionEntity ->
                Session(
                    id = sessionEntity.id.value.toString(),
                    name = sessionEntity.name,
                    abstract = sessionEntity.abstract,
                    startTime = sessionEntity.startTime,
                    endTime = sessionEntity.endTime,
                    trackName = sessionEntity.trackName,
                    language = sessionEntity.language,
                )
            }
            .sortedBy { it.startTime }
        val speakers = SpeakerEntity
            .find { SpeakersTable.eventId eq event.id.value }
            .map { speakerEntity ->
                Speaker(
                    id = speakerEntity.id.value.toString(),
                    name = speakerEntity.name,
                    biography = speakerEntity.biography,
                    jobTitle = speakerEntity.jobTitle,
                    photoUrl = speakerEntity.photoUrl,
                    pronouns = speakerEntity.pronouns,
                )
            }
            .sortedBy { it.name }

        AgendaResponse(
            sessions = sessions,
            speakers = speakers,
        )
    }
}
