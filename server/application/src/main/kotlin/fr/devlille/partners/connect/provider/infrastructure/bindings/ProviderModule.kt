package fr.devlille.partners.connect.provider.infrastructure.bindings

import fr.devlille.partners.connect.provider.application.ProviderRepositoryExposed
import fr.devlille.partners.connect.provider.domain.ProviderRepository
import org.koin.dsl.module

val providerModule = module {
    single<ProviderRepository> { ProviderRepositoryExposed() }
}
