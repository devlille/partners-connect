package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class PartnershipOptionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipOptionEntity>(PartnershipOptionsTable)

    var partnership by PartnershipEntity referencedOn PartnershipOptionsTable.partnershipId
    var packId by PartnershipOptionsTable.packId
    var option by SponsoringOptionEntity referencedOn PartnershipOptionsTable.optionId
}

fun UUIDEntityClass<PartnershipOptionEntity>.listByPartnershipAndPack(
    partnershipId: UUID,
    packId: UUID,
): List<PartnershipOptionEntity> = this
    .find { (PartnershipOptionsTable.partnershipId eq partnershipId) and (PartnershipOptionsTable.packId eq packId) }
    .toList()

fun UUIDEntityClass<PartnershipOptionEntity>.deleteAllByPartnershipId(partnershipId: UUID, packId: UUID): Unit = this
    .find { (PartnershipOptionsTable.partnershipId eq partnershipId) and (PartnershipOptionsTable.packId eq packId) }
    .forEach { it.delete() }
