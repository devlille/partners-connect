package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.EventStorageRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class EventStorageRepositoryGoogleStorage(
    private val storage: Storage,
) : EventStorageRepository {
    override fun uploadBoothPlanImage(
        eventSlug: String,
        content: ByteArray,
        mimeType: String,
    ): String = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value

        // Determine MimeType enum from MIME type string
        val mimeTypeEnum = when (mimeType.lowercase()) {
            "image/png" -> MimeType.PNG
            "image/jpeg", "image/jpg" -> MimeType.JPEG
            "image/gif" -> MimeType.GIF
            "image/webp" -> MimeType.WEBP
            else -> MimeType.JPEG // default fallback
        }

        val uploaded = storage.upload(
            filename = "events/$eventId/booth-plan.${mimeTypeEnum.extension}",
            content = content,
            mimeType = mimeTypeEnum,
        )
        uploaded.url
    }
}
