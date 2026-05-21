package fr.devlille.partners.connect.internal.infrastructure.ktor

import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerLifecycleEvent
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerNotificationRepository
import fr.devlille.partners.connect.ecosystempartners.infrastructure.api.ecosystemPartnerId
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.util.AttributeKey
import io.ktor.util.Attributes
import org.koin.ktor.ext.inject

/**
 * Triggers an ecosystem partner lifecycle notification after the route
 * responds. The route handler must set the lifecycle event before
 * `call.respond(...)` — for example:
 *
 * ```
 * call.attributes.ecosystemPartnerLifecycle = EcosystemPartnerLifecycleEvent.VALIDATED
 * call.respond(HttpStatusCode.OK, ...)
 * ```
 *
 * Routes whose execution removes the partner (DELETE) or that don't have
 * an `ecosystemPartnerId` in the path yet (POST create) call the
 * repository directly instead of relying on this plugin.
 */
val NotificationEcosystemPartnerPlugin = createRouteScopedPlugin(name = "NotificationEcosystemPartnerPlugin") {
    val repository by application.inject<EcosystemPartnerNotificationRepository>()

    onCallRespond { call ->
        val lifecycle = call.attributes.ecosystemPartnerLifecycleOrNull ?: return@onCallRespond
        val eventSlug = call.parameters.eventSlug
        val ecosystemPartnerId = call.parameters.ecosystemPartnerId
        repository.notify(eventSlug, ecosystemPartnerId, lifecycle)
    }
}

private object NotificationEcosystemPartnerPluginKeys {
    val LifecycleKey = AttributeKey<EcosystemPartnerLifecycleEvent>(
        "EcosystemPartnerNotificationLifecycle",
    )
}

var Attributes.ecosystemPartnerLifecycle: EcosystemPartnerLifecycleEvent
    get() = this[NotificationEcosystemPartnerPluginKeys.LifecycleKey]
    set(value) {
        this.put(NotificationEcosystemPartnerPluginKeys.LifecycleKey, value)
    }

private val Attributes.ecosystemPartnerLifecycleOrNull: EcosystemPartnerLifecycleEvent?
    get() = this.getOrNull(NotificationEcosystemPartnerPluginKeys.LifecycleKey)
