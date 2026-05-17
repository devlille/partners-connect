package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.partnership.domain.FlyerGenerationRepository
import fr.devlille.partners.connect.partnership.domain.GeneratedFlyer
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.domain.FlyerZone
import fr.devlille.partners.connect.sponsoring.infrastructure.db.hasFlyerTemplate
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class FlyerGenerationRepositoryImpl(
    private val httpClient: HttpClient,
    private val storage: Storage,
) : FlyerGenerationRepository {
    override suspend fun generate(eventSlug: String, partnershipId: UUID): GeneratedFlyer {
        val context = readGenerationContext(eventSlug, partnershipId)

        val templateBytes = httpClient.get(context.templateUrl).readRawBytes()
        val logoBytes = httpClient.get(context.logoUrl).readRawBytes()
        val jpgBytes = FlyerComposer.compose(templateBytes, logoBytes, context.zone)

        val filename = "events/${context.eventId}/partnerships/$partnershipId/communication-support.jpg"
        val upload = storage.upload(filename, jpgBytes, MimeType.JPG)

        transaction {
            PartnershipEntity[partnershipId].communicationSupportUrl = upload.url
        }

        return GeneratedFlyer(url = upload.url)
    }

    @Suppress("ThrowsCount")
    private fun readGenerationContext(eventSlug: String, partnershipId: UUID): GenerationContext = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")
        if (partnership.event.id != event.id) {
            throw NotFoundException("Partnership $partnershipId not found in event $eventSlug")
        }
        val pack = partnership.validatedPack()
            ?: throw ConflictException("Partnership must be validated before generating a flyer")
        if (!pack.hasFlyerTemplate()) {
            throw ConflictException("Pack ${pack.id.value} is not flyer-enabled")
        }
        val logoUrl = partnership.company.logoUrl1000 ?: partnership.company.logoUrlOriginal
            ?: throw ConflictException("Company has no logo")
        GenerationContext(
            eventId = event.id.value,
            templateUrl = pack.flyerTemplateUrl!!,
            zone = FlyerZone(
                x = pack.flyerZoneX!!,
                y = pack.flyerZoneY!!,
                width = pack.flyerZoneWidth!!,
                height = pack.flyerZoneHeight!!,
            ),
            logoUrl = logoUrl,
        )
    }

    private data class GenerationContext(
        val eventId: UUID,
        val templateUrl: String,
        val zone: FlyerZone,
        val logoUrl: String,
    )
}
