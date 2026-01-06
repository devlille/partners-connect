package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.Destination
import fr.devlille.partners.connect.notifications.domain.NotificationGateway
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipFilters
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed ORM implementation of PartnershipEmailRepository.
 *
 * Fetches partnerships with pre-structured EmailContact objects for both
 * organizers and company contacts. This repository is responsible for
 * data fetching only - email sending is handled by NotificationRepository.
 */
class PartnershipEmailRepositoryExposed(
    private val notificationGateway: NotificationGateway,
) : PartnershipEmailRepository {
    override suspend fun getPartnershipDestination(
        eventSlug: String,
        filters: PartnershipFilters,
    ): List<Destination> = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value

        // Resolve organiser email to user ID if provided
        val organiserUserId = filters.organiser?.let { email ->
            UserEntity.singleUserByEmail(email)?.id?.value
        }

        // Fetch partnerships matching filters
        val partnerships = PartnershipEntity
            .filters(
                eventId = eventId,
                packId = filters.packId?.toUUID(),
                validated = filters.validated,
                suggestion = filters.suggestion,
                agreementGenerated = filters.agreementGenerated,
                agreementSigned = filters.agreementSigned,
                organiserUserId = organiserUserId,
            )

        // Apply additional filters that require entity-level checks
        val filteredPartnerships = if (filters.paid != null) {
            partnerships.filter {
                val billing = BillingEntity.singleByEventAndPartnership(eventId, it.id.value)
                if (filters.paid) billing?.status == InvoiceStatus.PAID else billing?.status != InvoiceStatus.PAID
            }
        } else {
            partnerships
        }

        // Convert to domain objects with pre-structured EmailContact objects
        filteredPartnerships.map { partnership ->
            notificationGateway.getDestination(
                eventId = event.id.value,
                partnership = partnership.toDomain(
                    emails = PartnershipEmailEntity.emails(partnership.id.value).toList(),
                ),
            )
        }
    }
}
