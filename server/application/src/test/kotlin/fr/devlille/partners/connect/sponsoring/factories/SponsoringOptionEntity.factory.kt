package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.sponsoring.domain.NumberDescriptor
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.domain.QuantitativeDescriptor
import fr.devlille.partners.connect.sponsoring.domain.SelectableDescriptor
import fr.devlille.partners.connect.sponsoring.domain.SelectableValue
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SelectableValueEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedSponsoringOption(
    optionId: UUID = UUID.randomUUID(),
    eventId: UUID = UUID.randomUUID(),
    optionType: OptionType = OptionType.TEXT,
    name: String = optionId.toString(),
    price: Int = 150,
    description: String = "Test Description",
    selectableValues: List<SelectableValue> = emptyList(),
): SponsoringOptionEntity {
    val option = SponsoringOptionEntity.new(optionId) {
        this.event = EventEntity[eventId]
        this.price = price
        this.optionType = optionType

        // Set type-specific fields based on optionType
        when (optionType) {
            OptionType.TYPED_QUANTITATIVE -> {
                this.quantitativeDescriptor = QuantitativeDescriptor.JOB_OFFER
            }
            OptionType.TYPED_NUMBER -> {
                this.numberDescriptor = NumberDescriptor.NB_TICKET
                this.fixedQuantity = 1
            }
            OptionType.TYPED_SELECTABLE -> {
                this.selectableDescriptor = SelectableDescriptor.BOOTH
            }
            OptionType.TEXT -> {
                // No additional fields needed for TEXT type
            }
        }
    }

    selectableValues.forEach {
        SelectableValueEntity.new(it.id.toUUID()) {
            this.option = option
            this.value = it.value
            this.price = it.price
        }
    }

    // Create translation
    insertMockedOptionTranslation(
        optionId = optionId,
        language = "en",
        name = name,
        description = description,
    )

    return option
}
