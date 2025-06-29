package fr.devlille.partners.connect.events.infrastructure.api.mappers

import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.infrastructure.api.EventSummaryResponse

fun EventSummary.toResponse(): EventSummaryResponse = EventSummaryResponse(
    id = id.toString(),
    name = name,
    startTime = startTime,
    endTime = endTime,
    submissionStartTime = submissionStartTime,
    submissionEndTime = submissionEndTime
)
