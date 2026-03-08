package fr.devlille.partners.connect.digest.infrastructure.bindings

import fr.devlille.partners.connect.digest.application.DigestRepositoryExposed
import fr.devlille.partners.connect.digest.domain.DigestRepository
import org.koin.dsl.module

val digestModule = module {
    single<DigestRepository> { DigestRepositoryExposed() }
}
