package fr.devlille.partners.connect.integrations.infrastructure.bindings

import fr.devlille.partners.connect.integrations.application.IntegrationRepositoryExposed
import fr.devlille.partners.connect.integrations.domain.IntegrationRepository
import fr.devlille.partners.connect.integrations.infrastructure.api.DefaultIntegrationDeserializerRegistry
import fr.devlille.partners.connect.integrations.infrastructure.api.IntegrationDeserializerRegistry
import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebRegistrar
import fr.devlille.partners.connect.integrations.infrastructure.db.MailjetRegistrar
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoRegistrar
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackRegistrar
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
            ),
        )
    }
}
