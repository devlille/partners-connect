package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

class BoothActivityEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<BoothActivityEntity>(BoothActivitiesTable) {
        fun countByPartnerships(partnershipIds: Set<UUID>): Map<UUID, Int> = BoothActivitiesTable
            .selectAll()
            .where { BoothActivitiesTable.partnershipId inList partnershipIds }
            .groupingBy { it[BoothActivitiesTable.partnershipId].value }
            .eachCount()
    }

    var partnership by PartnershipEntity referencedOn BoothActivitiesTable.partnershipId
    var title by BoothActivitiesTable.title
    var description by BoothActivitiesTable.description
    var startTime by BoothActivitiesTable.startTime
    var endTime by BoothActivitiesTable.endTime
    var createdAt by BoothActivitiesTable.createdAt
}
