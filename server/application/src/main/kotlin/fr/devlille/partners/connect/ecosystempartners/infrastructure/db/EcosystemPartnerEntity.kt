package fr.devlille.partners.connect.ecosystempartners.infrastructure.db

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

class EcosystemPartnerEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EcosystemPartnerEntity>(EcosystemPartnersTable) {
        fun filters(
            eventId: UUID,
            categoryId: UUID?,
            validated: Boolean?,
            declined: Boolean = false,
        ): SizedIterable<EcosystemPartnerEntity> {
            var op = EcosystemPartnersTable.eventId eq eventId
            if (categoryId != null) {
                op = op and (EcosystemPartnersTable.categoryId eq categoryId)
            }
            if (validated != null) {
                op = if (validated) {
                    op and EcosystemPartnersTable.validatedAt.isNotNull()
                } else {
                    op and EcosystemPartnersTable.validatedAt.isNull()
                }
            }
            op = if (declined) {
                op and EcosystemPartnersTable.declinedAt.isNotNull()
            } else {
                op and EcosystemPartnersTable.declinedAt.isNull()
            }
            return find { op }
        }

        fun listPublic(eventId: UUID): List<EcosystemPartnerEntity> = find {
            (EcosystemPartnersTable.eventId eq eventId) and
                EcosystemPartnersTable.validatedAt.isNotNull() and
                EcosystemPartnersTable.declinedAt.isNull()
        }.toList()

        fun countByCategory(categoryId: UUID): Long =
            find { EcosystemPartnersTable.categoryId eq categoryId }.count()
    }

    var event by EventEntity referencedOn EcosystemPartnersTable.eventId
    var company by CompanyEntity referencedOn EcosystemPartnersTable.companyId
    var category by EcosystemPartnerCategoryEntity referencedOn EcosystemPartnersTable.categoryId
    var displayOrder by EcosystemPartnersTable.displayOrder
    var language by EcosystemPartnersTable.language
    var validatedAt by EcosystemPartnersTable.validatedAt
    var declinedAt by EcosystemPartnersTable.declinedAt
    var createdAt by EcosystemPartnersTable.createdAt
}
