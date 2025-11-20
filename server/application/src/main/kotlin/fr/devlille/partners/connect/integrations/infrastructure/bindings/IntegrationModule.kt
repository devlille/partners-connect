package fr.devlille.partners.connect.integrations.infrastructure.bindings

import fr.devlille.partners.connect.integrations.application.IntegrationRepositoryExposed
import fr.devlille.partners.connect.integrations.application.IntegrationStatusRepositoryExposed
import fr.devlille.partners.connect.integrations.domain.IntegrationRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationStatusRepository
import fr.devlille.partners.connect.integrations.infrastructure.api.DefaultIntegrationDeserializerRegistry
import fr.devlille.partners.connect.integrations.infrastructure.api.IntegrationDeserializerRegistry
import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebRegistrar
import fr.devlille.partners.connect.integrations.infrastructure.db.MailjetRegistrar
import fr.devlille.partners.connect.integrations.infrastructure.db.OpenPlannerRegistrar
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoRegistrar
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackRegistrar
import fr.devlille.partners.connect.integrations.infrastructure.db.WebhookRegistrar
import fr.devlille.partners.connect.integrations.infrastructure.gateways.BilletWebStatusGateway
import fr.devlille.partners.connect.integrations.infrastructure.gateways.MailjetStatusGateway
import fr.devlille.partners.connect.integrations.infrastructure.gateways.OpenPlannerStatusGateway
import fr.devlille.partners.connect.integrations.infrastructure.gateways.QontoStatusGateway
import fr.devlille.partners.connect.integrations.infrastructure.gateways.SlackStatusGateway
import fr.devlille.partners.connect.integrations.infrastructure.gateways.WebhookStatusGateway
import org.koin.dsl.module

val integrationModule = module {
    single<IntegrationDeserializerRegistry> { DefaultIntegrationDeserializerRegistry() }
    single<IntegrationRepository> {
        IntegrationRepositoryExposed(
            registrars = listOf(
                SlackRegistrar(),
                MailjetRegistrar(),
                QontoRegistrar(),
                BilletWebRegistrar(),
                WebhookRegistrar(),
                OpenPlannerRegistrar(),
            ),
        )
    }
    single<IntegrationStatusRepository> {
        IntegrationStatusRepositoryExposed(
            gateways = listOf(
                BilletWebStatusGateway(billetWebProvider = get()),
                MailjetStatusGateway(mailjetProvider = get()),
                OpenPlannerStatusGateway(openPlannerProvider = get()),
                QontoStatusGateway(qontoProvider = get()),
                SlackStatusGateway(slack = get()),
                WebhookStatusGateway(httpClient = get()),
            ),
        )
    }
}
