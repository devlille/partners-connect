package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun insertMockedSponsoringOption(
    optionId: UUID = UUID.randomUUID(),
    eventId: UUID = UUID.randomUUID(),
    price: Int = 150,
): SponsoringOptionEntity = transaction {
    SponsoringOptionEntity.new(optionId) {
        this.event = EventEntity[eventId]
        this.price = price
    }
}
