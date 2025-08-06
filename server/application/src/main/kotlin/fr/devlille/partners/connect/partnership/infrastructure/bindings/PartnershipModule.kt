package fr.devlille.partners.connect.partnership.infrastructure.bindings

import fr.devlille.partners.connect.partnership.application.PartnershipAgreementRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipInvoiceRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipStorageRepositoryGoogleStorage
import fr.devlille.partners.connect.partnership.application.PartnershipSuggestionRepositoryExposed
import fr.devlille.partners.connect.partnership.domain.PartnershipAgreementRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipInvoiceRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import org.koin.dsl.module

val partnershipModule = module {
    single<PartnershipRepository> {
        PartnershipRepositoryExposed()
    }
    single<PartnershipSuggestionRepository> {
        PartnershipSuggestionRepositoryExposed()
    }
    single<PartnershipInvoiceRepository> {
        PartnershipInvoiceRepositoryExposed()
    }
    single<PartnershipAgreementRepository> {
        PartnershipAgreementRepositoryExposed()
    }
    single<PartnershipStorageRepository> {
        PartnershipStorageRepositoryGoogleStorage(get())
    }
}
