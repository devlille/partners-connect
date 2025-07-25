package fr.devlille.partners.connect.auth.infrastructure.bindings

import fr.devlille.partners.connect.auth.application.AuthRepositoryGoogle
import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.auth.infrastructure.providers.GoogleProvider
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkClientModule
import org.koin.dsl.module

val authModule = module {
    includes(networkClientModule)
    single<AuthRepository> {
        AuthRepositoryGoogle(GoogleProvider(httpClient = get()))
    }
}
