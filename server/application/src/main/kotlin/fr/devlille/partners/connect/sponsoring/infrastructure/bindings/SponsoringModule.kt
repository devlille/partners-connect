package fr.devlille.partners.connect.sponsoring.infrastructure.bindings

import fr.devlille.partners.connect.sponsoring.application.OptionRepositoryExposed
import fr.devlille.partners.connect.sponsoring.application.PackRepositoryExposed
import fr.devlille.partners.connect.sponsoring.domain.OptionRepository
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import org.koin.dsl.module

val sponsoringModule = module {
    single<PackRepository> { PackRepositoryExposed() }
    single<OptionRepository> { OptionRepositoryExposed() }
}
