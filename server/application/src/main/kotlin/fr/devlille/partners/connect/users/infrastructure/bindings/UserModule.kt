package fr.devlille.partners.connect.users.infrastructure.bindings

import fr.devlille.partners.connect.users.application.UserRepositoryExposed
import fr.devlille.partners.connect.users.domain.UserRepository
import org.koin.dsl.module

val userModule = module {
    single<UserRepository> { UserRepositoryExposed() }
}
