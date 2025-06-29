package fr.devlille.partners.connect.events.domain

import kotlinx.datetime.LocalDateTime
import java.util.UUID

class EventSummaryEntity(
    val id: UUID,
    val name: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val submissionStartTime: LocalDateTime,
    val submissionEndTime: LocalDateTime
)
