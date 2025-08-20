package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipStorageRepository {
    fun uploadAgreement(
        eventSlug: String,
        partnershipId: UUID,
        content: ByteArray,
    ): String

    fun uploadSignedAgreement(
        eventSlug: String,
        partnershipId: UUID,
        content: ByteArray,
    ): String
}
