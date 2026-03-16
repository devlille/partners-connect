package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.CommunicationPlanEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import kotlinx.datetime.LocalDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedCommunicationPlan(
    id: UUID = UUID.randomUUID(),
    eventId: UUID = UUID.randomUUID(),
    partnershipId: UUID? = null,
    title: String = id.toString(),
    scheduledDate: LocalDateTime? = null,
    description: String? = null,
    supportUrl: String? = null,
): CommunicationPlanEntity = CommunicationPlanEntity.new(id) {
    this.event = EventEntity[eventId]
    this.partnership = partnershipId?.let { PartnershipEntity[it] }
    this.title = title
    this.scheduledDate = scheduledDate
    this.description = description
    this.supportUrl = supportUrl
}
