package fr.devlille.partners.connect.ecosystempartners.application

import fr.devlille.partners.connect.ecosystempartners.application.mappers.toDomain
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerCategoryRepository
import fr.devlille.partners.connect.ecosystempartners.domain.RegisterEcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.domain.UpdateEcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerCategoriesTable
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerCategoryEntity
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class EcosystemPartnerCategoryRepositoryExposed : EcosystemPartnerCategoryRepository {
    override fun create(eventSlug: String, request: RegisterEcosystemPartnerCategory): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        if (EcosystemPartnerCategoryEntity.singleByEventAndName(event.id.value, request.name) != null) {
            throw ConflictException("Category '${request.name}' already exists for this event")
        }
        val entity = EcosystemPartnerCategoryEntity.new {
            this.event = event
            this.name = request.name
            this.displayOrder = request.displayOrder
        }
        entity.id.value
    }

    override fun listByEvent(eventSlug: String): List<EcosystemPartnerCategory> = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        EcosystemPartnerCategoryEntity
            .find { EcosystemPartnerCategoriesTable.eventId eq event.id.value }
            .orderBy(
                EcosystemPartnerCategoriesTable.displayOrder to SortOrder.ASC_NULLS_LAST,
                EcosystemPartnerCategoriesTable.name to SortOrder.ASC,
            )
            .map { it.toDomain() }
    }

    override fun update(
        eventSlug: String,
        categoryId: UUID,
        request: UpdateEcosystemPartnerCategory,
    ): EcosystemPartnerCategory = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val category = EcosystemPartnerCategoryEntity.findById(categoryId)
            ?: throw NotFoundException("Category $categoryId not found")
        if (category.event.id.value != event.id.value) {
            throw NotFoundException("Category $categoryId does not belong to event $eventSlug")
        }
        request.name?.let { newName ->
            val existing = EcosystemPartnerCategoryEntity.singleByEventAndName(event.id.value, newName)
            if (existing != null && existing.id.value != categoryId) {
                throw ConflictException("Category '$newName' already exists for this event")
            }
            category.name = newName
        }
        request.displayOrder?.let { category.displayOrder = it }
        category.toDomain()
    }

    override fun delete(eventSlug: String, categoryId: UUID) = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val category = EcosystemPartnerCategoryEntity.findById(categoryId)
            ?: throw NotFoundException("Category $categoryId not found")
        if (category.event.id.value != event.id.value) {
            throw NotFoundException("Category $categoryId does not belong to event $eventSlug")
        }
        if (EcosystemPartnerEntity.countByCategory(categoryId) > 0) {
            throw ConflictException("Category $categoryId is in use by one or more ecosystem partners")
        }
        category.delete()
    }
}
