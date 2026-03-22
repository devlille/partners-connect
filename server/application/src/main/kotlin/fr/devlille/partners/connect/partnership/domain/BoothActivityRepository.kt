package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface BoothActivityRepository {
    fun list(partnershipId: UUID): List<BoothActivity>

    fun create(partnershipId: UUID, request: BoothActivityRequest): BoothActivity

    fun update(partnershipId: UUID, activityId: UUID, request: BoothActivityRequest): BoothActivity

    fun delete(partnershipId: UUID, activityId: UUID)
}
