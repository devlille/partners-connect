package fr.devlille.partners.connect.events.domain

import fr.devlille.partners.connect.provider.domain.Provider
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventDisplay(
    val slug: String,
    val name: String,
    @SerialName("start_time")
    val startTime: LocalDateTime,
    @SerialName("end_time")
    val endTime: LocalDateTime,
    @SerialName("submission_start_time")
    val submissionStartTime: LocalDateTime,
    @SerialName("submission_end_time")
    val submissionEndTime: LocalDateTime,
    val address: String,
    val contact: Contact,
    @SerialName("external_links")
    val externalLinks: List<EventExternalLink>,
    val providers: List<Provider>,
)
