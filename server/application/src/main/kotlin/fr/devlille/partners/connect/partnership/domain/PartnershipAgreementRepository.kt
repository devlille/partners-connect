package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipAgreementRepository {
    fun generateAgreement(eventSlug: String, partnershipId: UUID): ByteArray

    fun updateAgreementUrl(eventSlug: String, partnershipId: UUID, agreementUrl: String): UUID

    fun updateAgreementSignedUrl(eventSlug: String, partnershipId: UUID, agreementSignedUrl: String): UUID
}
