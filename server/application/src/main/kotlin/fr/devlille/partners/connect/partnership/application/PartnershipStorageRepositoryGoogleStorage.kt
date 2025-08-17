package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import io.ktor.server.plugins.NotFoundException
import java.util.UUID

class PartnershipStorageRepositoryGoogleStorage(
    private val storage: Storage,
) : PartnershipStorageRepository {
    override fun uploadAgreement(
        eventSlug: String,
        partnershipId: UUID,
        content: ByteArray,
    ): String {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val uploaded = storage.upload(
            filename = "events/$eventId/partnerships/$partnershipId/agreement.pdf",
            content = content,
            mimeType = MimeType.PDF,
        )
        return uploaded.url
    }

    override fun uploadSignedAgreement(
        eventSlug: String,
        partnershipId: UUID,
        content: ByteArray,
    ): String {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val uploaded = storage.upload(
            filename = "events/$eventId/partnerships/$partnershipId/signed-agreement.pdf",
            content = content,
            mimeType = MimeType.PDF,
        )
        return uploaded.url
    }
}
