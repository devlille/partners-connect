package fr.devlille.partners.connect.legaentity.infrastructure.bindings

import fr.devlille.partners.connect.legaentity.application.LegalEntityRepositoryExposed
import fr.devlille.partners.connect.legaentity.domain.LegalEntityRepository
import org.koin.dsl.module

val legalEntityModule = module {
    single<LegalEntityRepository> {
        LegalEntityRepositoryExposed()
    }
}
