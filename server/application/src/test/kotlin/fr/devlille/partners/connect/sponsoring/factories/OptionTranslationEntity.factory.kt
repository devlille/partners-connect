package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import java.util.UUID

fun insertMockedOptionTranslation(
    optionId: UUID = UUID.randomUUID(),
    language: String = "en",
    name: String = "Option Name",
    description: String = "Option Description",
): OptionTranslationEntity =
    OptionTranslationEntity.new {
        this.option = SponsoringOptionEntity[optionId]
        this.language = language
        this.name = name
        this.description = description
    }
