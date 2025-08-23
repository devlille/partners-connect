package fr.devlille.partners.connect.webhooks.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class EventWebhookEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EventWebhookEntity>(EventWebhooksTable)

    var event by EventEntity referencedOn EventWebhooksTable.eventId
    var url by EventWebhooksTable.url
    var type by EventWebhooksTable.type
    var partnership by PartnershipEntity optionalReferencedOn EventWebhooksTable.partnershipId
    var headerAuth by EventWebhooksTable.headerAuth
    var createdAt by EventWebhooksTable.createdAt
    var updatedAt by EventWebhooksTable.updatedAt

    fun updateTimestamp() {
        updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
