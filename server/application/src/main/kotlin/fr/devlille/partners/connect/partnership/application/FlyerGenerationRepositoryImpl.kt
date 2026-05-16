package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.partnership.domain.FlyerGenerationRepository
import fr.devlille.partners.connect.partnership.domain.GeneratedFlyer
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplate
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplateRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class FlyerGenerationRepositoryImpl(
    private val httpClient: HttpClient,
    private val flyerTemplateRepository: FlyerTemplateRepository,
    private val partnershipStorageRepository: PartnershipStorageRepository,
) : FlyerGenerationRepository {

    override suspend fun generate(eventSlug: String, partnershipId: UUID): GeneratedFlyer {
        val context = transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException("Event with slug $eventSlug not found")
            val partnership = PartnershipEntity.findById(partnershipId)
                ?: throw NotFoundException("Partnership $partnershipId not found")
            if (partnership.event.id != event.id) {
                throw NotFoundException("Partnership $partnershipId not found in event $eventSlug")
            }
            if (partnership.validatedAt == null) {
                throw ConflictException("Partnership must be validated before generating a flyer")
            }
            val pack = partnership.selectedPack
                ?: throw ConflictException("Partnership has no selected pack")
            val template = flyerTemplateRepository.get(eventSlug, pack.id.value)
                ?: throw ConflictException("Pack ${pack.id.value} is not flyer-enabled")
            val logoUrl = partnership.company.logoUrl1000 ?: partnership.company.logoUrlOriginal
                ?: throw ConflictException("Company has no logo")
            GenerationContext(template = template, logoUrl = logoUrl)
        }

        val templateBytes = httpClient.get(context.template.templateUrl).readRawBytes()
        val logoBytes = httpClient.get(context.logoUrl).readRawBytes()
        val jpgBytes = FlyerComposer.compose(templateBytes, logoBytes, context.template.zone)

        val storedUrl = partnershipStorageRepository.uploadCommunicationSupport(
            eventSlug = eventSlug,
            partnershipId = partnershipId,
            content = jpgBytes,
            mimeType = "image/jpeg",
        )

        transaction {
            val partnership = PartnershipEntity[partnershipId]
            partnership.communicationSupportUrl = storedUrl
        }

        return GeneratedFlyer(url = storedUrl)
    }

    private data class GenerationContext(val template: FlyerTemplate, val logoUrl: String)
}
