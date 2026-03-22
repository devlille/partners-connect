package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.partnership.infrastructure.db.BoothActivityEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import kotlinx.datetime.LocalDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedBoothActivity(
    id: UUID = UUID.randomUUID(),
    partnershipId: UUID,
    title: String = id.toString(),
    description: String = id.toString(),
    startTime: LocalDateTime? = null,
    endTime: LocalDateTime? = null,
): BoothActivityEntity = BoothActivityEntity.new(id) {
    this.partnership = PartnershipEntity[partnershipId]
    this.title = title
    this.description = description
    this.startTime = startTime
    this.endTime = endTime
}

/**
 * Inserts a BOOTH-type sponsoring option and links it to the given partnership.
 * This makes the partnership booth-eligible, enabling write access to booth activities.
 *
 * Requires: partnership, pack, and event rows to exist before calling this factory.
 */
fun insertMockedBoothOption(
    partnershipId: UUID,
    packId: UUID,
    eventId: UUID,
    optionId: UUID = UUID.randomUUID(),
): SponsoringOptionEntity {
    val option = insertMockedSponsoringOption(
        optionId = optionId,
        eventId = eventId,
        optionType = OptionType.TYPED_SELECTABLE,
    )
    PartnershipOptionEntity.new {
        this.partnership = PartnershipEntity[partnershipId]
        this.pack = SponsoringPackEntity[packId]
        this.option = option
    }
    return option
}
