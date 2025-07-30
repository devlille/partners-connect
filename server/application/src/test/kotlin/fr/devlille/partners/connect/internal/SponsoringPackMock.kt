package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockSponsoringPack(
    id: UUID = UUID.randomUUID(),
    eventId: UUID = UUID.randomUUID(),
    name: String = "Mock Sponsoring Pack",
    basePrice: Int = 1000,
    withBooth: Boolean = false,
    maxQuantity: Int = 100,
): SponsoringPackEntity = transaction {
    SponsoringPackEntity.new(id) {
        this.eventId = eventId
        this.name = name
        this.basePrice = basePrice
        this.withBooth = withBooth
        this.maxQuantity = maxQuantity
    }
}
