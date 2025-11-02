package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.domain.PartnershipBoothRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipBoothRepositoryExposed : PartnershipBoothRepository {
    override fun updateBoothLocation(
        eventSlug: String,
        partnershipId: UUID,
        location: String,
    ): Unit = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        // Check if location is already taken by another partnership for this event
        val existingPartnership = PartnershipEntity.find {
            (PartnershipsTable.eventId eq event.id) and
                (PartnershipsTable.id neq partnershipId) and
                (PartnershipsTable.boothLocation eq location)
        }.firstOrNull()

        if (existingPartnership != null) {
            val companyName = existingPartnership.company.name
            throw ForbiddenException(
                "Location '$location' is already assigned to another partnership " +
                    "for this event by company '$companyName'",
            )
        }

        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        partnership.boothLocation = location
    }
}
