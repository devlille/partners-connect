package fr.devlille.partners.connect.sponsoring.application.mappers

import fr.devlille.partners.connect.sponsoring.domain.OptionTranslation
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.domain.SelectableValue
import fr.devlille.partners.connect.sponsoring.domain.SponsoringOptionWithTranslations
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity

internal fun SponsoringOptionEntity.toDomainWithAllTranslations(): SponsoringOptionWithTranslations {
    val translationsMap = translations.associate { translation ->
        translation.language to OptionTranslation(
            language = translation.language,
            name = translation.name,
            description = translation.description,
        )
    }

    return when (optionType) {
        OptionType.TEXT -> SponsoringOptionWithTranslations.Text(
            id = id.value.toString(),
            translations = translationsMap,
            price = price,
        )

        OptionType.TYPED_QUANTITATIVE -> SponsoringOptionWithTranslations.TypedQuantitative(
            id = id.value.toString(),
            translations = translationsMap,
            price = price,
            typeDescriptor = quantitativeDescriptor!!,
        )

        OptionType.TYPED_NUMBER -> SponsoringOptionWithTranslations.TypedNumber(
            id = id.value.toString(),
            translations = translationsMap,
            price = price,
            typeDescriptor = numberDescriptor!!,
            fixedQuantity = fixedQuantity!!,
        )

        OptionType.TYPED_SELECTABLE -> SponsoringOptionWithTranslations.TypedSelectable(
            id = id.value.toString(),
            translations = translationsMap,
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
