package fr.devlille.partners.connect.sponsoring.application.mappers

import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.domain.SelectableValue
import fr.devlille.partners.connect.sponsoring.domain.SponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.Text
import fr.devlille.partners.connect.sponsoring.domain.TypedNumber
import fr.devlille.partners.connect.sponsoring.domain.TypedQuantitative
import fr.devlille.partners.connect.sponsoring.domain.TypedSelectable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import io.ktor.server.plugins.NotFoundException

internal fun SponsoringOptionEntity.toDomain(language: String): SponsoringOption {
    val translation = translations.firstOrNull { it.language == language }
        ?: throw NotFoundException("Translation not found for option $id in language $language")

    return mapToPolymorphicSponsoringOption(
        id = id.value.toString(),
        name = translation.name,
        description = translation.description,
        price = price,
    )
}

/**
 * Converts SponsoringOptionEntity to domain model with required/optional context.
 * Required options have null price (included in base pack), optional options show actual price.
 */
internal fun SponsoringOptionEntity.toDomain(language: String, required: Boolean): SponsoringOption {
    val translation = translations.firstOrNull { it.language == language }
        ?: throw NotFoundException("Translation not found for option $id in language $language")

    return mapToPolymorphicSponsoringOption(
        id = id.value.toString(),
        name = translation.name,
        description = translation.description,
        price = if (required) null else price,
    )
}

/**
 * Maps database entity to appropriate polymorphic SponsoringOption sealed class based on optionType.
 */
private fun SponsoringOptionEntity.mapToPolymorphicSponsoringOption(
    id: String,
    name: String,
    description: String?,
    price: Int?,
): SponsoringOption {
    return when (optionType) {
        OptionType.TEXT -> Text(
            id = id,
            name = name,
            description = description,
            price = price,
        )

        OptionType.TYPED_QUANTITATIVE -> TypedQuantitative(
            id = id,
            name = name,
            description = description,
            price = price,
            typeDescriptor = quantitativeDescriptor!!,
        )

        OptionType.TYPED_NUMBER -> TypedNumber(
            id = id,
            name = name,
            description = description,
            price = price,
            typeDescriptor = numberDescriptor!!,
            fixedQuantity = fixedQuantity!!,
        )

        OptionType.TYPED_SELECTABLE -> TypedSelectable(
            id = id,
            name = name,
            description = description,
            price = price,
            typeDescriptor = selectableDescriptor!!,
            selectableValues = selectableValues.map {
                SelectableValue(
                    id = it.id.value.toString(),
                    value = it.value,
                    price = it.price,
                )
            },
        )
    }
}
