package fr.devlille.partners.connect.notifications.infrastructure.bindings

import com.slack.api.Slack
import com.slack.api.SlackConfig
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkClientModule
import fr.devlille.partners.connect.notifications.application.NotificationRepositoryExposed
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.infrastructure.gateways.MailjetNotificationGateway
import fr.devlille.partners.connect.notifications.infrastructure.gateways.SlackNotificationGateway
import org.koin.dsl.module

val notificationModule = module {
    includes(networkClientModule)
    single<NotificationRepository> {
        NotificationRepositoryExposed(
            notificationGateways = listOf(
                SlackNotificationGateway(Slack.getInstance(SlackConfig())),
                MailjetNotificationGateway(httpClient = get()),
            ),
        )
    }
}
