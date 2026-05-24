package fr.devlille.partners.connect.events.application.mappers

import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity

internal fun EventEntity.toEventSummary(): EventSummary = EventSummary(
    slug = slug,
    name = name,
    startTime = startTime,
    endTime = endTime,
    submissionStartTime = submissionStartTime,
    submissionEndTime = submissionEndTime,
    orgSlug = organisation.slug,
    orgName = organisation.name,
)
