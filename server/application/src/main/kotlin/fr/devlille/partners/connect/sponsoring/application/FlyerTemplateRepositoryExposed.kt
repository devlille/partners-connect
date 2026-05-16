package fr.devlille.partners.connect.sponsoring.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplate
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplateRepository
import fr.devlille.partners.connect.sponsoring.domain.FlyerZone
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.hasFlyerTemplate
import fr.devlille.partners.connect.sponsoring.infrastructure.db.singlePackById
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.UUID
import javax.imageio.ImageIO

class FlyerTemplateRepositoryExposed(
    private val storage: Storage,
) : FlyerTemplateRepository {
    override fun get(eventSlug: String, packId: UUID): FlyerTemplate? = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
        if (!pack.hasFlyerTemplate()) {
            null
        } else {
            FlyerTemplate(
                templateUrl = pack.flyerTemplateUrl!!,
                zone = FlyerZone(
                    x = pack.flyerZoneX!!,
                    y = pack.flyerZoneY!!,
                    width = pack.flyerZoneWidth!!,
                    height = pack.flyerZoneHeight!!,
                ),
            )
        }
    }

    override fun save(
        eventSlug: String,
        packId: UUID,
        pngBytes: ByteArray,
        zone: FlyerZone,
    ): FlyerTemplate {
        val image = readPng(pngBytes)
        validateZoneFitsInside(zone, image)
        return transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException("Event with slug $eventSlug not found")
            val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
            val filename = "events/${event.id.value}/packs/$packId/flyer-template.png"
            val upload = storage.upload(filename, pngBytes, MimeType.PNG)
            pack.flyerTemplateUrl = upload.url
            pack.flyerZoneX = zone.x
            pack.flyerZoneY = zone.y
            pack.flyerZoneWidth = zone.width
            pack.flyerZoneHeight = zone.height
            FlyerTemplate(templateUrl = upload.url, zone = zone)
        }
    }

    private fun readPng(pngBytes: ByteArray): BufferedImage =
        ImageIO.read(ByteArrayInputStream(pngBytes))
            ?: throw BadRequestException("Uploaded file is not a readable PNG image")

    private fun validateZoneFitsInside(zone: FlyerZone, image: BufferedImage) {
        if (!zone.hasValidShape()) {
            throw BadRequestException("Zone coordinates must be non-negative with positive width/height")
        }
        if (zone.x + zone.width > image.width || zone.y + zone.height > image.height) {
            throw BadRequestException(
                "Zone ($zone) does not fit inside template (${image.width}x${image.height})",
            )
        }
    }

    private fun FlyerZone.hasValidShape(): Boolean =
        x >= 0 && y >= 0 && width > 0 && height > 0

    override fun clear(eventSlug: String, packId: UUID) {
        transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException("Event with slug $eventSlug not found")
            val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
            if (pack.hasFlyerTemplate()) {
                storage.delete("events/${event.id.value}/packs/$packId/flyer-template.png")
                pack.flyerTemplateUrl = null
                pack.flyerZoneX = null
                pack.flyerZoneY = null
                pack.flyerZoneWidth = null
                pack.flyerZoneHeight = null
            }
        }
    }
}
