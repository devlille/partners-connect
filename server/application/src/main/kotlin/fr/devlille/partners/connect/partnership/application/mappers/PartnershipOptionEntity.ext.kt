package fr.devlille.partners.connect.partnership.application.mappers

import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.domain.NumberPartnershipOption
import fr.devlille.partners.connect.partnership.domain.PartnershipOption
import fr.devlille.partners.connect.partnership.domain.QuantitativePartnershipOption
import fr.devlille.partners.connect.partnership.domain.SelectablePartnershipOption
import fr.devlille.partners.connect.partnership.domain.SelectedValue
import fr.devlille.partners.connect.partnership.domain.TextPartnershipOption
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.sponsoring.domain.OptionType

/**
 * Maps PartnershipOptionEntity to PartnershipOption domain model.
 * Generates complete description by merging original description with selected value.
 *
 * @param language Partnership language for translation lookup
 * @return PartnershipOption with complete description and pricing
 * @throws ForbiddenException if translation not found for language
 */
internal fun PartnershipOptionEntity.toDomain(
    language: String,
): PartnershipOption {
    val translation = option.translations.firstOrNull { it.language == language }
        ?: throw ForbiddenException(
            "Option ${option.id} does not have a translation for language $language",
        )

    val originalDescription = translation.description ?: ""

    return when (option.optionType) {
        OptionType.TEXT -> toTextPartnershipOption(translation.name, originalDescription)
        OptionType.TYPED_QUANTITATIVE -> toQuantitativePartnershipOption(translation.name, originalDescription)
        OptionType.TYPED_NUMBER -> toNumberPartnershipOption(translation.name, originalDescription)
        OptionType.TYPED_SELECTABLE -> toSelectablePartnershipOption(translation.name, originalDescription)
    }
}

private fun PartnershipOptionEntity.toTextPartnershipOption(
    name: String,
    description: String,
): TextPartnershipOption = TextPartnershipOption(
    id = option.id.value.toString(),
    name = name,
    description = description,
    // No value to append
    labelWithValue = name,
    price = option.price ?: 0,
    quantity = 1,
    totalPrice = option.price ?: 0,
)

private fun PartnershipOptionEntity.toQuantitativePartnershipOption(
    name: String,
    description: String,
): QuantitativePartnershipOption {
    val quantity = selectedQuantity ?: 0
    val price = option.price ?: 0
    val labelWithValue = if (quantity > 0) "$name ($quantity)" else name

    return QuantitativePartnershipOption(
        id = option.id.value.toString(),
        name = name,
        description = description,
        labelWithValue = labelWithValue,
        price = price,
        quantity = quantity,
        totalPrice = price * quantity,
        typeDescriptor = option.quantitativeDescriptor!!,
    )
}

private fun PartnershipOptionEntity.toNumberPartnershipOption(
    name: String,
    description: String,
): NumberPartnershipOption {
    val fixedQty = option.fixedQuantity ?: 0
    val price = option.price ?: 0
    val labelWithValue = if (fixedQty > 0) "$name ($fixedQty)" else name

    return NumberPartnershipOption(
        id = option.id.value.toString(),
        name = name,
        description = description,
        labelWithValue = labelWithValue,
        price = price,
        quantity = fixedQty,
        totalPrice = price * fixedQty,
        typeDescriptor = option.numberDescriptor!!,
    )
}

private fun PartnershipOptionEntity.toSelectablePartnershipOption(
    name: String,
    description: String,
): SelectablePartnershipOption {
    val selectedVal = selectedValue
        ?: throw ForbiddenException("Selected value not found for selectable option ${option.id}")
    val labelWithValue = "$name (${selectedVal.value})"

    return SelectablePartnershipOption(
        id = option.id.value.toString(),
        name = name,
        description = description,
        labelWithValue = labelWithValue,
        price = selectedVal.price,
        quantity = 1,
        totalPrice = selectedVal.price,
        typeDescriptor = option.selectableDescriptor!!,
        selectedValue = SelectedValue(
            id = selectedVal.id.value.toString(),
            value = selectedVal.value,
            price = selectedVal.price,
        ),
    )
}
