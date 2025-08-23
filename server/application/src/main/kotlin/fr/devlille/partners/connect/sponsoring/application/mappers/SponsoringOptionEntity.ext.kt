package fr.devlille.partners.connect.sponsoring.application.mappers

import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import fr.devlille.partners.connect.sponsoring.domain.SponsoringOption
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity

internal fun SponsoringOptionEntity.toDomain(language: String): SponsoringOption {
    val translation = translations.firstOrNull { it.language == language }
        ?: throw NotFoundException(
            code = ErrorCode.INTEGRATION_NOT_FOUND,
            message = "Translation not found for option $id in language $language",
            meta = mapOf(
                MetaKeys.ID to id.value.toString(),
                MetaKeys.RESOURCE to language,
            ),
        )
    return SponsoringOption(
        id = id.value.toString(),
        name = translation.name,
        description = translation.description,
        price = price,
    )
}
