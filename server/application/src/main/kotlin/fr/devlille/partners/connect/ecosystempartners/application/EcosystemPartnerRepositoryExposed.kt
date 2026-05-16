package fr.devlille.partners.connect.ecosystempartners.application

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.ecosystempartners.application.mappers.toDomain
import fr.devlille.partners.connect.ecosystempartners.application.mappers.toItem
import fr.devlille.partners.connect.ecosystempartners.application.mappers.toPublic
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerFilters
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerItem
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerRepository
import fr.devlille.partners.connect.ecosystempartners.domain.PublicEcosystemPartnerGroup
import fr.devlille.partners.connect.ecosystempartners.domain.RegisterEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.domain.UpdateEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerCategoryEntity
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEmailEntity
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEntity
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnersTable
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class EcosystemPartnerRepositoryExposed : EcosystemPartnerRepository {
    override fun register(eventSlug: String, request: RegisterEcosystemPartner): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val company = CompanyEntity.findById(UUID.fromString(request.companyId))
            ?: throw NotFoundException("Company ${request.companyId} not found")
        val category = EcosystemPartnerCategoryEntity.findById(UUID.fromString(request.categoryId))
            ?: throw NotFoundException("Category ${request.categoryId} not found")
        if (category.event.id.value != event.id.value) {
            throw NotFoundException("Category ${request.categoryId} does not belong to event $eventSlug")
        }
        val duplicate = EcosystemPartnerEntity.find {
            (EcosystemPartnersTable.eventId eq event.id.value) and
                (EcosystemPartnersTable.companyId eq company.id.value) and
                (EcosystemPartnersTable.categoryId eq category.id.value)
        }.singleOrNull()
        if (duplicate != null) {
            throw ConflictException("Company already registered for this category on this event")
        }
        val entity = EcosystemPartnerEntity.new {
            this.event = event
            this.company = company
            this.category = category
            this.displayOrder = request.displayOrder
            this.language = request.language
        }
        request.emails.forEach { addr ->
            EcosystemPartnerEmailEntity.new {
                this.ecosystemPartner = entity
                this.email = addr
            }
        }
        entity.id.value
    }

    override fun getById(eventSlug: String, ecosystemPartnerId: UUID): EcosystemPartner = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val entity = EcosystemPartnerEntity.findById(ecosystemPartnerId)
            ?: throw NotFoundException("Ecosystem partner $ecosystemPartnerId not found")
        if (entity.event.id.value != event.id.value) {
            throw NotFoundException("Ecosystem partner $ecosystemPartnerId does not belong to event $eventSlug")
        }
        val emails = EcosystemPartnerEmailEntity
            .listByEcosystemPartner(ecosystemPartnerId)
            .map { it.email }
        entity.toDomain(emails)
    }

    override fun update(
        eventSlug: String,
        ecosystemPartnerId: UUID,
        request: UpdateEcosystemPartner,
    ): EcosystemPartner = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val entity = EcosystemPartnerEntity.findById(ecosystemPartnerId)
            ?: throw NotFoundException("Ecosystem partner $ecosystemPartnerId not found")
        if (entity.event.id.value != event.id.value) {
            throw NotFoundException("Ecosystem partner $ecosystemPartnerId does not belong to event $eventSlug")
        }
        if (entity.validatedAt != null) {
            throw ConflictException("Ecosystem partner is validated; decline first to edit")
        }
        request.categoryId?.let { newCatId ->
            val newCategory = EcosystemPartnerCategoryEntity.findById(UUID.fromString(newCatId))
                ?: throw NotFoundException("Category $newCatId not found")
            if (newCategory.event.id.value != event.id.value) {
                throw NotFoundException("Category $newCatId does not belong to event $eventSlug")
            }
            entity.category = newCategory
        }
        request.displayOrder?.let { entity.displayOrder = it }
        request.language?.let { entity.language = it }
        request.emails?.let { newEmails ->
            EcosystemPartnerEmailEntity.deleteByEcosystemPartner(ecosystemPartnerId)
            newEmails.forEach { addr ->
                EcosystemPartnerEmailEntity.new {
                    this.ecosystemPartner = entity
                    this.email = addr
                }
            }
        }
        val emails = EcosystemPartnerEmailEntity
            .listByEcosystemPartner(ecosystemPartnerId)
            .map { it.email }
        entity.toDomain(emails)
    }

    override fun delete(eventSlug: String, ecosystemPartnerId: UUID) = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val entity = EcosystemPartnerEntity.findById(ecosystemPartnerId)
            ?: throw NotFoundException("Ecosystem partner $ecosystemPartnerId not found")
        if (entity.event.id.value != event.id.value) {
            throw NotFoundException("Ecosystem partner $ecosystemPartnerId does not belong to event $eventSlug")
        }
        entity.delete()
    }

    override fun listByEvent(eventSlug: String, filters: EcosystemPartnerFilters): List<EcosystemPartnerItem> =
        transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException("Event with slug $eventSlug not found")
            EcosystemPartnerEntity
                .filters(
                    eventId = event.id.value,
                    categoryId = filters.categoryId?.let(UUID::fromString),
                    validated = filters.validated,
                    declined = filters.declined,
                )
                .orderBy(EcosystemPartnersTable.createdAt to SortOrder.ASC)
                .map { it.toItem() }
        }

    override fun listPublic(eventSlug: String): List<PublicEcosystemPartnerGroup> = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partners = EcosystemPartnerEntity.listPublic(event.id.value)
        partners
            .groupBy { it.category }
            .toList()
            .sortedWith(
                compareBy(
                    { it.first.displayOrder ?: Int.MAX_VALUE },
                    { it.first.name },
                ),
            )
            .map { (category, entities) ->
                val sorted = entities.sortedWith(
                    compareBy(
                        { it.displayOrder ?: Int.MAX_VALUE },
                        { it.company.name },
                    ),
                )
                PublicEcosystemPartnerGroup(
                    category = category.name,
                    partners = sorted.map { it.toPublic() },
                )
            }
    }
}
