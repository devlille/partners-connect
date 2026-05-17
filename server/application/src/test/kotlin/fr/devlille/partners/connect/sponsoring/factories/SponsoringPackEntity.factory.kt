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
    flyerTemplateUrl: String? = null,
    flyerZoneX: Int? = null,
    flyerZoneY: Int? = null,
    flyerZoneWidth: Int? = null,
    flyerZoneHeight: Int? = null,
): SponsoringPackEntity =
    SponsoringPackEntity.new(id) {
        this.event = EventEntity[eventId]
        this.name = name
        this.basePrice = basePrice
        this.maxQuantity = maxQuantity
        this.flyerTemplateUrl = flyerTemplateUrl
        this.flyerZoneX = flyerZoneX
        this.flyerZoneY = flyerZoneY
        this.flyerZoneWidth = flyerZoneWidth
        this.flyerZoneHeight = flyerZoneHeight
    }

@Suppress("LongParameterList")
fun insertMockedFlyerEnabledPack(
    packId: UUID = UUID.randomUUID(),
    eventId: UUID = UUID.randomUUID(),
    templateUrl: String =
        "https://storage.googleapis.com/test-bucket/events/$eventId/packs/$packId/flyer-template.png",
    zoneX: Int = 100,
    zoneY: Int = 200,
    zoneWidth: Int = 800,
    zoneHeight: Int = 500,
): SponsoringPackEntity = insertMockedSponsoringPack(
    id = packId,
    eventId = eventId,
    flyerTemplateUrl = templateUrl,
    flyerZoneX = zoneX,
    flyerZoneY = zoneY,
    flyerZoneWidth = zoneWidth,
    flyerZoneHeight = zoneHeight,
)
