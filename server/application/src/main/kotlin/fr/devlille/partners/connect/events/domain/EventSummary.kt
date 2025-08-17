package fr.devlille.partners.connect.events.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class EventSummary(
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
)
