package fr.devlille.partners.connect.agenda.infrastructure.gateways

import fr.devlille.partners.connect.agenda.domain.AgendaGateway
import fr.devlille.partners.connect.agenda.infrastructure.db.SessionEntity
import fr.devlille.partners.connect.agenda.infrastructure.db.SessionsTable
import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakerEntity
import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakersTable
import fr.devlille.partners.connect.agenda.infrastructure.providers.OpenPlannerProvider
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.OpenPlannerIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class OpenPlannerAgendaGateway(
    private val openPlannerProvider: OpenPlannerProvider,
) : AgendaGateway {
    override val provider: IntegrationProvider = IntegrationProvider.OPENPLANNER

    @Suppress("LongMethod")
    override suspend fun fetchAndStore(integrationId: UUID, eventId: UUID) {
        val config = transaction { OpenPlannerIntegrationsTable[integrationId] }
        val eventInfo = openPlannerProvider.eventInfo(config)
        val agenda = openPlannerProvider.planning(eventInfo.dataUrl)
        transaction {
            val event = EventEntity.findById(eventId)
                ?: throw NotFoundException("Event with id $eventId not found")
            agenda.speakers.forEach { speaker ->
                val existing = SpeakerEntity.find { SpeakersTable.externalId eq speaker.id }.firstOrNull()
                if (existing != null) {
                    existing.apply {
                        this.name = speaker.name
                        this.biography = speaker.bio
                        this.photoUrl = speaker.photoUrl
                        this.jobTitle = speaker.jobTitle
                        this.pronouns = speaker.pronouns
                    }
                } else {
                    SpeakerEntity.new {
                        this.externalId = speaker.id
                        this.name = speaker.name
                        this.biography = speaker.bio
                        this.photoUrl = speaker.photoUrl
                        this.jobTitle = speaker.jobTitle
                        this.pronouns = speaker.pronouns
                        this.event = event
                    }
                }
            }
            agenda.sessions.forEach { session ->
                val trackName = if (session.trackId != null) {
                    agenda.event.tracks.find { session.trackId == it.id }?.name
                } else {
                    null
                }
                val existing = SessionEntity.find { SessionsTable.externalId eq session.id }.firstOrNull()
                if (existing != null) {
                    existing.apply {
                        this.name = session.title
                        this.abstract = session.abstract
                        this.startTime = session.dateStart?.toLocalDateTime(TimeZone.UTC)
                        this.endTime = session.dateEnd?.toLocalDateTime(TimeZone.UTC)
                        this.trackName = trackName
                        this.language = session.language
                    }
                } else {
                    SessionEntity.new {
                        this.externalId = session.id
                        this.name = session.title
                        this.abstract = session.abstract
                        this.startTime = session.dateStart?.toLocalDateTime(TimeZone.UTC)
                        this.endTime = session.dateEnd?.toLocalDateTime(TimeZone.UTC)
                        this.trackName = trackName
                        this.language = session.language
                        this.event = event
                    }
                }
            }
        }
    }
}
