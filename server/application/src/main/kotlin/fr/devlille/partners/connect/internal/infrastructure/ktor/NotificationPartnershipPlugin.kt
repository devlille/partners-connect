package fr.devlille.partners.connect.internal.infrastructure.ktor

import fr.devlille.partners.connect.internal.infrastructure.api.user
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.notifications.infrastructure.gateways.EmailDeliveryResult
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailHistoryRepository
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipId
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.util.AttributeKey
import io.ktor.util.Attributes
import org.koin.ktor.ext.inject

val NotificationPartnershipPlugin = createRouteScopedPlugin(name = "NotificationPartnershipPlugin") {
    val notificationRepository by application.inject<NotificationRepository>()
    val partnershipEmailHistoryRepository by application.inject<PartnershipEmailHistoryRepository>()

    onCallRespond { call ->
        val deliveryResult = notificationRepository.sendMessage(call.attributes.variables)
            .filterIsInstance<EmailDeliveryResult>()
            .firstOrNull()
        deliveryResult?.let { deliveryResult ->
            partnershipEmailHistoryRepository.create(
                partnershipId = call.parameters.partnershipId,
                senderEmail = deliveryResult.senderEmail,
                subject = deliveryResult.subject,
                bodyPlainText = deliveryResult.body,
                deliveryResult = deliveryResult,
                triggeredBy = call.attributes.user.userId.toUUID(),
            )
        }
    }
}

private object NotificationPartnershipPluginKeys {
    val VariablesKey = AttributeKey<NotificationVariables>("NotificationPartnershipVariables")
}

var Attributes.variables: NotificationVariables
    get() = this[NotificationPartnershipPluginKeys.VariablesKey]
    set(value) {
        this.put(NotificationPartnershipPluginKeys.VariablesKey, value)
    }
