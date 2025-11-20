package fr.devlille.partners.connect.agenda.infrastructure.providers

import fr.devlille.partners.connect.integrations.infrastructure.db.OpenPlannerConfig
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

class OpenPlannerProvider(
    private val httpClient: HttpClient,
) {
    suspend fun status(config: OpenPlannerConfig): Boolean {
        val response = httpClient.get("https://api.openplanner.fr/v1/${config.eventId}/event") {
            headers[HttpHeaders.ContentType] = "application/json"
        }
        return response.status.isSuccess()
    }

    suspend fun eventInfo(config: OpenPlannerConfig): OpenPlannerEventInfo {
        val response = httpClient.get("https://api.openplanner.fr/v1/${config.eventId}/event") {
            headers[HttpHeaders.ContentType] = "application/json"
        }
        if (response.status.isSuccess().not()) {
            throw ForbiddenException("Can't load openplanner event information")
        }
        return response.body<OpenPlannerEventInfo>()
    }

    suspend fun planning(url: String): OpenPlannerAgendaEvent {
        val dataResponse = httpClient.get(url)
        if (dataResponse.status.isSuccess().not()) {
            throw ForbiddenException("Can't load OpenPlanner event agenda")
        }
        return dataResponse.body<OpenPlannerAgendaEvent>()
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
