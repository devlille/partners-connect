package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipAgreementRepository {
    fun generateAgreement(eventId: UUID, partnershipId: UUID): ByteArray

    fun updateAgreementUrl(eventId: UUID, partnershipId: UUID, agreementUrl: String): UUID

    fun updateAgreementSignedUrl(eventId: UUID, partnershipId: UUID, agreementSignedUrl: String): UUID
}
