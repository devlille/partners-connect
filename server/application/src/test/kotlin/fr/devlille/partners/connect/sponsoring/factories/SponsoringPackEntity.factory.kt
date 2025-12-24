package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedSponsoringPack(
    id: UUID = UUID.randomUUID(),
    eventId: UUID = UUID.randomUUID(),
    name: String = id.toString(),
    basePrice: Int = 1000,
    maxQuantity: Int? = 100,
): SponsoringPackEntity =
    SponsoringPackEntity.new(id) {
        this.event = EventEntity[eventId]
        this.name = name
        this.basePrice = basePrice
        this.maxQuantity = maxQuantity
    }
