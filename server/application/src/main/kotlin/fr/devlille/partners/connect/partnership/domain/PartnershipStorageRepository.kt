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

    fun uploadCommunicationSupport(
        eventSlug: String,
        partnershipId: UUID,
        content: ByteArray,
        mimeType: String,
    ): String

    /**
     * Uploads a partner's support video to Google Cloud Storage and returns its public URL.
     * Throws [UnsupportedMediaTypeException] if [mimeType] is not in the allow-list
     * ("video/mp4", "video/webm").
     */
    fun uploadSupportVideo(
        eventSlug: String,
        partnershipId: UUID,
        content: ByteArray,
        mimeType: String,
    ): String
}
