package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventSummaryEntity

fun EventSummaryEntity.toApi(): EventSummary = EventSummary(
    id = id.toString(),
    name = name,
    startTime = startTime,
    endTime = endTime,
    submissionStartTime = submissionStartTime,
    submissionEndTime = submissionEndTime
)
