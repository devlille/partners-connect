package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class PartnershipOptionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipOptionEntity>(PartnershipOptionsTable)

    var partnership by PartnershipEntity referencedOn PartnershipOptionsTable.partnershipId
    var optionId by PartnershipOptionsTable.optionId
}

fun UUIDEntityClass<PartnershipOptionEntity>.deleteAllByPartnershipId(partnershipId: UUID): Unit = this
    .find { PartnershipOptionsTable.partnershipId eq partnershipId }
    .forEach { it.delete() }
