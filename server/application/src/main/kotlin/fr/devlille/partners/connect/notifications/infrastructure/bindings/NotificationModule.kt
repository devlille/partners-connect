package fr.devlille.partners.connect.notifications.infrastructure.bindings

import com.slack.api.Slack
import com.slack.api.SlackConfig
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackConfigDao
import fr.devlille.partners.connect.notifications.application.NotificationRepositoryExposed
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.infrastructure.gateways.SlackNotificationGateway
import fr.devlille.partners.connect.notifications.infrastructure.gateways.SlackTemplateGateway
import org.koin.dsl.module

val notificationModule = module {
    single<NotificationRepository> {
        NotificationRepositoryExposed(
            notificationGateways = listOf(
                SlackNotificationGateway(Slack.getInstance(SlackConfig()), SlackConfigDao()),
            ),
            templateGateways = listOf(
                SlackTemplateGateway(),
            ),
        )
    }
}
