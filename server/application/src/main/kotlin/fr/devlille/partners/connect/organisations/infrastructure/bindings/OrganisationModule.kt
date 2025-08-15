package fr.devlille.partners.connect.organisations.infrastructure.bindings

import fr.devlille.partners.connect.organisations.application.OrganisationRepositoryExposed
import fr.devlille.partners.connect.organisations.domain.OrganisationRepository
import org.koin.dsl.module

val organisationModule = module {
    single<OrganisationRepository> {
        OrganisationRepositoryExposed()
    }
}
