package fr.devlille.partners.connect.ecosystempartners.factories

import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerCategoryEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import java.util.UUID

fun insertMockedEcosystemPartnerCategory(
    id: UUID = UUID.randomUUID(),
    eventId: UUID,
    name: String = id.toString(),
    displayOrder: Int? = null,
): EcosystemPartnerCategoryEntity {
    val event = EventEntity.findById(eventId)
        ?: error("Event $eventId must exist before creating an ecosystem partner category")
    return EcosystemPartnerCategoryEntity.new(id) {
        this.event = event
        this.name = name
        this.displayOrder = displayOrder
    }
}
