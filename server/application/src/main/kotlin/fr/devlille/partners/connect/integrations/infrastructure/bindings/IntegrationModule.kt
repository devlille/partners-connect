package fr.devlille.partners.connect.integrations.infrastructure.bindings

import com.slack.api.Slack
import com.slack.api.SlackConfig
import fr.devlille.partners.connect.integrations.application.IntegrationRepositoryExposed
import fr.devlille.partners.connect.integrations.application.NotificationRepositoryExposed
import fr.devlille.partners.connect.integrations.domain.IntegrationRepository
import fr.devlille.partners.connect.integrations.domain.NotificationRepository
import fr.devlille.partners.connect.integrations.infrastructure.api.DefaultIntegrationDeserializerRegistry
import fr.devlille.partners.connect.integrations.infrastructure.api.IntegrationDeserializerRegistry
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackConfigDao
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackRegistrar
import fr.devlille.partners.connect.integrations.infrastructure.gateways.SlackNotificationGateway
import org.koin.dsl.module

val integrationModule = module {
    single<IntegrationDeserializerRegistry> { DefaultIntegrationDeserializerRegistry() }

    single<NotificationRepository> {
        NotificationRepositoryExposed(
            gateways = listOf(
                SlackNotificationGateway(Slack.getInstance(SlackConfig()), SlackConfigDao()),
            ),
        )
    }
    single<IntegrationRepository> {
        IntegrationRepositoryExposed(
            registrars = listOf(
                SlackRegistrar(),
            ),
        )
    }
}
