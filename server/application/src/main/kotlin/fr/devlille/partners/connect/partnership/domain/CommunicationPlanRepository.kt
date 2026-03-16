package fr.devlille.partners.connect.partnership.domain

import kotlinx.datetime.LocalDateTime
import java.util.UUID

interface CommunicationPlanRepository {
    fun create(
        eventSlug: String,
        title: String,
        scheduledDate: LocalDateTime?,
        description: String?,
        supportUrl: String?,
    ): CommunicationPlanEntry

    fun findById(eventSlug: String, id: UUID): CommunicationPlanEntry

    @Suppress("LongParameterList")
    fun update(
        eventSlug: String,
        id: UUID,
        title: String,
        scheduledDate: LocalDateTime?,
        description: String?,
        supportUrl: String?,
    ): CommunicationPlanEntry

    fun delete(eventSlug: String, id: UUID)

    fun upsertForPartnership(
        eventSlug: String,
        partnershipId: UUID,
        scheduledDate: LocalDateTime?,
        supportUrl: String?,
    ): CommunicationPlanEntry
}
