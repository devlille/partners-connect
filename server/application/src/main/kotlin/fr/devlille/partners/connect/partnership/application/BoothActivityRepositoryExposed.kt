package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.BoothActivity
import fr.devlille.partners.connect.partnership.domain.BoothActivityRepository
import fr.devlille.partners.connect.partnership.domain.BoothActivityRequest
import fr.devlille.partners.connect.partnership.infrastructure.db.BoothActivitiesTable
import fr.devlille.partners.connect.partnership.infrastructure.db.BoothActivityEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class BoothActivityRepositoryExposed : BoothActivityRepository {
    override fun list(partnershipId: UUID): List<BoothActivity> = transaction {
        PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")

        BoothActivityEntity
            .find { BoothActivitiesTable.partnershipId eq partnershipId }
            .orderBy(BoothActivitiesTable.createdAt to SortOrder.ASC)
            .toList()
            .sortedWith(
                compareBy<BoothActivityEntity, LocalDateTime?>(
                    nullsLast(naturalOrder()),
                ) { it.startTime }
                    .thenBy { it.createdAt },
            )
            .map { it.toDomain() }
    }

    override fun create(partnershipId: UUID, request: BoothActivityRequest): BoothActivity = transaction {
        PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")

        if (!PartnershipOptionEntity.hasBoothOption(partnershipId)) {
            throw ForbiddenException("Partnership does not have a booth option")
        }

        BoothActivityEntity.new {
            this.partnership = PartnershipEntity[partnershipId]
            this.title = request.title
            this.description = request.description
            this.startTime = request.startTime
            this.endTime = request.endTime
        }.toDomain()
    }

    override fun update(partnershipId: UUID, activityId: UUID, request: BoothActivityRequest): BoothActivity =
        transaction {
            PartnershipEntity.findById(partnershipId)
                ?: throw NotFoundException("Partnership $partnershipId not found")

            if (!PartnershipOptionEntity.hasBoothOption(partnershipId)) {
                throw ForbiddenException("Partnership does not have a booth option")
            }

            val entity = BoothActivityEntity.findById(activityId)
                ?.takeIf { it.partnership.id.value == partnershipId }
                ?: throw NotFoundException("Activity $activityId not found")

            entity.title = request.title
            entity.description = request.description
            entity.startTime = request.startTime
            entity.endTime = request.endTime
            entity.toDomain()
        }

    override fun delete(partnershipId: UUID, activityId: UUID): Unit = transaction {
        PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")

        if (!PartnershipOptionEntity.hasBoothOption(partnershipId)) {
            throw ForbiddenException("Partnership does not have a booth option")
        }

        val entity = BoothActivityEntity.findById(activityId)
            ?.takeIf { it.partnership.id.value == partnershipId }
            ?: throw NotFoundException("Activity $activityId not found")

        entity.delete()
    }
}
