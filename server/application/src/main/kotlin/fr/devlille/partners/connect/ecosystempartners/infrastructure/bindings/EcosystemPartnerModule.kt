package fr.devlille.partners.connect.ecosystempartners.infrastructure.bindings

import fr.devlille.partners.connect.ecosystempartners.application.EcosystemPartnerCategoryRepositoryExposed
import fr.devlille.partners.connect.ecosystempartners.application.EcosystemPartnerDecisionRepositoryExposed
import fr.devlille.partners.connect.ecosystempartners.application.EcosystemPartnerRepositoryExposed
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerCategoryRepository
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerDecisionRepository
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerRepository
import org.koin.dsl.module

val ecosystemPartnerModule = module {
    single<EcosystemPartnerCategoryRepository> { EcosystemPartnerCategoryRepositoryExposed() }
    single<EcosystemPartnerRepository> { EcosystemPartnerRepositoryExposed() }
    single<EcosystemPartnerDecisionRepository> { EcosystemPartnerDecisionRepositoryExposed() }
}
