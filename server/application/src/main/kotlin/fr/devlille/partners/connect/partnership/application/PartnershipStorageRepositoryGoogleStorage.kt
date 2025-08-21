package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipStorageRepositoryGoogleStorage(
    private val storage: Storage,
) : PartnershipStorageRepository {
    override fun uploadAgreement(
        eventSlug: String,
        partnershipId: UUID,
        content: ByteArray,
    ): String = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val uploaded = storage.upload(
            filename = "events/$eventId/partnerships/$partnershipId/agreement.pdf",
            content = content,
            mimeType = MimeType.PDF,
        )
        uploaded.url
    }

    override fun uploadSignedAgreement(
        eventSlug: String,
        partnershipId: UUID,
        content: ByteArray,
    ): String = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value
        val uploaded = storage.upload(
            filename = "events/$eventId/partnerships/$partnershipId/signed-agreement.pdf",
            content = content,
            mimeType = MimeType.PDF,
        )
        uploaded.url
    }

    override fun uploadCommunicationSupport(
        eventSlug: String,
        partnershipId: UUID,
        content: ByteArray,
        mimeType: String,
    ): String = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value

        // Check for empty content
        if (content.isEmpty()) {
            throw BadRequestException("Empty file content")
        }

        // Convert mimeType string to MimeType enum and validate
        val supportMimeType = when (mimeType) {
            "image/png" -> MimeType.PNG
            "image/jpeg" -> MimeType.JPEG
            "image/jpg" -> MimeType.JPG
            "image/gif" -> MimeType.GIF
            "image/svg+xml" -> MimeType.SVG
            "image/webp" -> MimeType.WEBP
            else -> throw UnsupportedMediaTypeException("Unsupported image type: $mimeType")
        }

        val uploaded = storage.upload(
            filename = "events/$eventId/partnerships/$partnershipId/communication-support.${supportMimeType.extension}",
            content = content,
            mimeType = supportMimeType,
        )
        uploaded.url
    }
}
