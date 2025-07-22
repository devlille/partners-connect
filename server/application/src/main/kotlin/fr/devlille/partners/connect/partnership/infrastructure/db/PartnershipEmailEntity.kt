package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class PartnershipEmailEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEmailEntity>(PartnershipEmailsTable)

    var partnership by PartnershipEntity referencedOn PartnershipEmailsTable.partnershipId
    var email by PartnershipEmailsTable.email
}
