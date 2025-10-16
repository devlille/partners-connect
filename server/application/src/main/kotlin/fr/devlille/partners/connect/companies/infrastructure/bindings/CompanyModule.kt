package fr.devlille.partners.connect.companies.infrastructure.bindings

import fr.devlille.partners.connect.companies.application.CompanyImageProcessingRepositoryDefault
import fr.devlille.partners.connect.companies.application.CompanyJobOfferRepositoryExposed
import fr.devlille.partners.connect.companies.application.CompanyMediaRepositoryGoogleCloud
import fr.devlille.partners.connect.companies.application.CompanyRepositoryExposed
import fr.devlille.partners.connect.companies.domain.CompanyImageProcessingRepository
import fr.devlille.partners.connect.companies.domain.CompanyJobOfferRepository
import fr.devlille.partners.connect.companies.domain.CompanyMediaRepository
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.internal.infrastructure.bindings.storageModule
import org.koin.dsl.module

val companyModule = module {
    includes(storageModule)
    single<CompanyRepository> {
        CompanyRepositoryExposed()
    }
    single<CompanyJobOfferRepository> {
        CompanyJobOfferRepositoryExposed()
    }
    single<CompanyMediaRepository> {
        CompanyMediaRepositoryGoogleCloud(get())
    }
    single<CompanyImageProcessingRepository> {
        CompanyImageProcessingRepositoryDefault()
    }
}
