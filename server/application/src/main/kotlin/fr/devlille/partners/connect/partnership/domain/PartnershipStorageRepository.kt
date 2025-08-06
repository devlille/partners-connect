package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipStorageRepository {
    fun uploadAssignment(
        eventId: UUID,
        partnershipId: UUID,
        content: ByteArray,
    ): String
}
