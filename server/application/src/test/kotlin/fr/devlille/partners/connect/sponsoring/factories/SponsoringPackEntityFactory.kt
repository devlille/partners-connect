package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedSponsoringPack(
    id: UUID = UUID.randomUUID(),
    event: UUID = UUID.randomUUID(),
    name: String = "Mock Sponsoring Pack",
    basePrice: Int = 1000,
    boothSize: String? = null,
    nbTickets: Int = 10,
    maxQuantity: Int? = 100,
): SponsoringPackEntity = transaction {
    SponsoringPackEntity.new(id) {
        this.event = EventEntity[event]
        this.name = name
        this.basePrice = basePrice
        this.boothSize = boothSize
        this.nbTickets = nbTickets
        this.maxQuantity = maxQuantity
    }
}
