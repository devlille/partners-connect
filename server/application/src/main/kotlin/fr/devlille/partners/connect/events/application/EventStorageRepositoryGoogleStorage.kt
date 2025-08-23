package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.EventStorageRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
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
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )
        val eventId = event.id.value

        // Determine MimeType enum from MIME type string
        val mimeTypeEnum = when (mimeType.lowercase()) {
            "image/png" -> MimeType.PNG
            "image/jpeg", "image/jpg" -> MimeType.JPEG
            "image/gif" -> MimeType.GIF
            "image/webp" -> MimeType.WEBP
            else -> throw UnsupportedMediaTypeException(
                code = ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                message = "Unsupported media type: $mimeType",
                meta = mapOf(
                    MetaKeys.MEDIA_TYPE to mimeType,
                    MetaKeys.SUPPORTED_TYPES to "image/png, image/jpeg, image/jpg, image/gif, image/webp",
                    MetaKeys.EVENT to event.slug,
                ),
            )
        }

        val uploaded = storage.upload(
            filename = "events/$eventId/booth-plan.${mimeTypeEnum.extension}",
            content = content,
            mimeType = mimeTypeEnum,
        )
        uploaded.url
    }
}
