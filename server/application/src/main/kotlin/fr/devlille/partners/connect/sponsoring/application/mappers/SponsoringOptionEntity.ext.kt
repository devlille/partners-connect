package fr.devlille.partners.connect.sponsoring.application.mappers

import fr.devlille.partners.connect.sponsoring.domain.SponsoringOption
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import io.ktor.server.plugins.NotFoundException

internal fun SponsoringOptionEntity.toDomain(language: String): SponsoringOption {
    val translation = translations.firstOrNull { it.language == language }
        ?: throw NotFoundException("Translation not found for option $id in language $language")
    return SponsoringOption(
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
    return SponsoringOption(
        id = id.value.toString(),
        name = translation.name,
        description = translation.description,
        price = if (required) null else price,
    )
}
