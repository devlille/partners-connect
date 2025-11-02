package fr.devlille.partners.connect.partnership.domain

import kotlinx.datetime.LocalDateTime
import java.util.UUID

interface PartnershipCommunicationRepository {
    fun updateCommunicationPublicationDate(eventSlug: String, partnershipId: UUID, publicationDate: LocalDateTime): UUID

    fun updateCommunicationSupportUrl(eventSlug: String, partnershipId: UUID, supportUrl: String): UUID

    fun listCommunicationPlan(eventSlug: String): CommunicationPlan
}
