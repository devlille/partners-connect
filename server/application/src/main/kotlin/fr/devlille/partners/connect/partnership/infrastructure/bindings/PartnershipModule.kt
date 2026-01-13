package fr.devlille.partners.connect.partnership.infrastructure.bindings

import fr.devlille.partners.connect.notifications.infrastructure.gateways.MailjetNotificationGateway
import fr.devlille.partners.connect.partnership.application.PartnershipAgreementRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipBillingRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipBoothRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipCommunicationRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipDecisionRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipEmailHistoryRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipEmailRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipJobOfferRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipSpeakerRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipStorageRepositoryGoogleStorage
import fr.devlille.partners.connect.partnership.application.PartnershipSuggestionRepositoryExposed
import fr.devlille.partners.connect.partnership.application.PartnershipTicketRepositoryExposed
import fr.devlille.partners.connect.partnership.domain.PartnershipAgreementRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipBillingRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipBoothRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipCommunicationRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipDecisionRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailHistoryRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipJobOfferRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSpeakerRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipTicketRepository
import org.koin.dsl.module

val partnershipModule = module {
    single<PartnershipRepository> {
        PartnershipRepositoryExposed()
    }
    single<PartnershipSuggestionRepository> {
        PartnershipSuggestionRepositoryExposed()
    }
    single<PartnershipDecisionRepository> {
        PartnershipDecisionRepositoryExposed()
    }
    single<PartnershipBillingRepository> {
        PartnershipBillingRepositoryExposed()
    }
    single<PartnershipAgreementRepository> {
        PartnershipAgreementRepositoryExposed()
    }
    single<PartnershipTicketRepository> {
        PartnershipTicketRepositoryExposed()
    }
    single<PartnershipStorageRepository> {
        PartnershipStorageRepositoryGoogleStorage(get())
    }
    single<PartnershipJobOfferRepository> {
        PartnershipJobOfferRepositoryExposed()
    }
    single<PartnershipCommunicationRepository> {
        PartnershipCommunicationRepositoryExposed()
    }
    single<PartnershipBoothRepository> {
        PartnershipBoothRepositoryExposed()
    }
    single<PartnershipSpeakerRepository> {
        PartnershipSpeakerRepositoryExposed()
    }
    single<PartnershipEmailRepository> {
        PartnershipEmailRepositoryExposed(
            notificationGateway = MailjetNotificationGateway(mailjetProvider = get()),
        )
    }
    single<PartnershipEmailHistoryRepository> {
        PartnershipEmailHistoryRepositoryExposed()
    }
}
