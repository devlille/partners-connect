package fr.devlille.partners.connect.ecosystempartners.infrastructure.db

import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class EcosystemPartnerEmailEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EcosystemPartnerEmailEntity>(EcosystemPartnerEmailsTable) {
        fun listByEcosystemPartner(ecosystemPartnerId: UUID): List<EcosystemPartnerEmailEntity> =
            find { EcosystemPartnerEmailsTable.ecosystemPartnerId eq ecosystemPartnerId }.toList()

        fun deleteByEcosystemPartner(ecosystemPartnerId: UUID) {
            find { EcosystemPartnerEmailsTable.ecosystemPartnerId eq ecosystemPartnerId }
                .forEach { it.delete() }
        }
    }

    var ecosystemPartner by EcosystemPartnerEntity referencedOn EcosystemPartnerEmailsTable.ecosystemPartnerId
    var email by EcosystemPartnerEmailsTable.email
}
