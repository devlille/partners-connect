package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

class CommunicationPlanEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CommunicationPlanEntity>(CommunicationPlansTable) {
        fun countByPartnerships(partnershipIds: Set<UUID>): Map<UUID, Int> = CommunicationPlansTable
            .selectAll()
            .where { CommunicationPlansTable.partnershipId inList partnershipIds }
            .mapNotNull { it[CommunicationPlansTable.partnershipId]?.value }
            .groupingBy { it }
            .eachCount()
    }

    var event by EventEntity referencedOn CommunicationPlansTable.eventId
    var partnership by PartnershipEntity optionalReferencedOn CommunicationPlansTable.partnershipId
    var title by CommunicationPlansTable.title
    var scheduledDate by CommunicationPlansTable.scheduledDate
    var description by CommunicationPlansTable.description
    var supportUrl by CommunicationPlansTable.supportUrl
    var createdAt by CommunicationPlansTable.createdAt
    var updatedAt: LocalDateTime by CommunicationPlansTable.updatedAt
}
