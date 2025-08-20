package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
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

    override fun uploadBoothPlanImage(
        eventSlug: String,
        content: ByteArray,
        mimeType: String,
    ): String = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value

        // Determine file extension from MIME type
        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/jpeg", "image/jpg" -> "jpg"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "jpg" // default fallback
        }

        // Determine MimeType enum from MIME type string
        val mimeTypeEnum = when (mimeType.lowercase()) {
            "image/png" -> MimeType.PNG
            "image/jpeg", "image/jpg" -> MimeType.JPEG
            "image/gif" -> MimeType.GIF
            "image/webp" -> MimeType.WEBP
            else -> MimeType.JPEG // default fallback
        }

        val uploaded = storage.upload(
            filename = "events/$eventId/booth-plan.$extension",
            content = content,
            mimeType = mimeTypeEnum,
        )
        uploaded.url
    }
}
