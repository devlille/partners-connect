package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipStorageRepository {
    fun uploadAgreement(
        eventId: UUID,
        partnershipId: UUID,
        content: ByteArray,
    ): String

    fun uploadSignedAgreement(
        eventId: UUID,
        partnershipId: UUID,
        content: ByteArray,
    ): String
}
