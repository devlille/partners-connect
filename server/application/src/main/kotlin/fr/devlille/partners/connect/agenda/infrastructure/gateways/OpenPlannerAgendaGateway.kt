package fr.devlille.partners.connect.agenda.infrastructure.gateways

import fr.devlille.partners.connect.agenda.domain.AgendaGateway
import fr.devlille.partners.connect.agenda.infrastructure.db.SessionEntity
import fr.devlille.partners.connect.agenda.infrastructure.db.SessionsTable
import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakerEntity
import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakersTable
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.OpenPlannerIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class OpenPlannerAgendaGateway(
    private val httpClient: HttpClient,
) : AgendaGateway {
    override val provider: IntegrationProvider = IntegrationProvider.OPENPLANNER

    @Suppress("LongMethod")
    override fun fetchAndStore(integrationId: UUID, eventId: UUID) = runBlocking {
        val config = OpenPlannerIntegrationsTable[integrationId]
        val response = httpClient.get("https://api.openplanner.fr/v1/${config.eventId}/event") {
            headers[HttpHeaders.ContentType] = "application/json"
        }
        if (response.status.isSuccess().not()) {
            throw ForbiddenException("Can't load openplanner event information")
        }
        val eventInfo = response.body<OpenPlannerEventInfo>()
        val dataResponse = httpClient.get(eventInfo.dataUrl)
        if (dataResponse.status.isSuccess().not()) {
            throw ForbiddenException("Can't load openplanner event agenda")
        }
        val agenda = dataResponse.body<OpenPlannerAgendaEvent>()
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

@Serializable
data class OpenPlannerEventInfo(
    val eventName: String,
    val dataUrl: String,
)

@Serializable
data class OpenPlannerAgendaEvent(
    val event: OpenPlannerEvent,
    val speakers: List<OpenPlannerSpeaker>,
    val sessions: List<OpenPlannerSession>,
)

@Serializable
data class OpenPlannerEvent(
    val tracks: List<OpenPlannerTrack> = emptyList(),
)

@Serializable
data class OpenPlannerTrack(
    val name: String,
    val id: String,
)

@Serializable
data class OpenPlannerSpeaker(
    val id: String,
    val name: String,
    val pronouns: String? = null,
    val jobTitle: String? = null,
    val bio: String? = null,
    val company: String? = null,
    val photoUrl: String? = null,
)

@Serializable
data class OpenPlannerSession(
    val id: String,
    val title: String,
    val abstract: String? = null,
    val dateStart: Instant? = null,
    val dateEnd: Instant? = null,
    val speakerIds: List<String> = emptyList(),
    val trackId: String? = null,
    val language: String? = null,
)
