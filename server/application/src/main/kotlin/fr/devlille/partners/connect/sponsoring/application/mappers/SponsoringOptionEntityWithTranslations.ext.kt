package fr.devlille.partners.connect.sponsoring.application.mappers

import fr.devlille.partners.connect.sponsoring.domain.OptionTranslation
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

    return SponsoringOptionWithTranslations(
        id = id.value.toString(),
        translations = translationsMap,
        price = price,
    )
}
