package fr.devlille.partners.connect.ecosystempartners.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class EcosystemPartnerCategoryEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EcosystemPartnerCategoryEntity>(EcosystemPartnerCategoriesTable) {
        fun listByEvent(eventId: UUID): List<EcosystemPartnerCategoryEntity> =
            find { EcosystemPartnerCategoriesTable.eventId eq eventId }.toList()

        fun singleByEventAndName(eventId: UUID, name: String): EcosystemPartnerCategoryEntity? = find {
            (EcosystemPartnerCategoriesTable.eventId eq eventId) and
                (EcosystemPartnerCategoriesTable.name eq name)
        }.singleOrNull()
    }

    var event by EventEntity referencedOn EcosystemPartnerCategoriesTable.eventId
    var name by EcosystemPartnerCategoriesTable.name
    var displayOrder by EcosystemPartnerCategoriesTable.displayOrder
    var createdAt by EcosystemPartnerCategoriesTable.createdAt
}
