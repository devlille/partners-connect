package fr.devlille.partners.connect.partnership.application.mappers

import fr.devlille.partners.connect.partnership.domain.BoothActivity
import fr.devlille.partners.connect.partnership.infrastructure.db.BoothActivityEntity

fun BoothActivityEntity.toDomain() = BoothActivity(
    id = id.value.toString(),
    partnershipId = partnership.id.value.toString(),
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime,
    createdAt = createdAt,
)
