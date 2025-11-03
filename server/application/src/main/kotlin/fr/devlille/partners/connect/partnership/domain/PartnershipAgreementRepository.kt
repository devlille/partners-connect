package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipAgreementRepository {
    fun agreement(eventSlug: String, partnershipId: UUID): PartnershipAgreement

    fun generatePDF(agreement: PartnershipAgreement, pricing: PartnershipPricing): ByteArray

    fun updateAgreementUrl(eventSlug: String, partnershipId: UUID, agreementUrl: String): UUID

    fun updateAgreementSignedUrl(eventSlug: String, partnershipId: UUID, agreementSignedUrl: String): UUID
}
