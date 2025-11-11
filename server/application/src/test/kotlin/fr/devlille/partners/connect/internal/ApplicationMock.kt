package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.ApplicationConfig
import fr.devlille.partners.connect.agenda.infrastructure.bindings.agendaModule
import fr.devlille.partners.connect.auth.infrastructure.bindings.authModule
import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.infrastructure.bindings.billingModule
import fr.devlille.partners.connect.companies.infrastructure.bindings.companyModule
import fr.devlille.partners.connect.events.infrastructure.bindings.eventModule
import fr.devlille.partners.connect.integrations.infrastructure.bindings.integrationModule
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkClientModule
import fr.devlille.partners.connect.internal.infrastructure.bindings.storageModule
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.module
import fr.devlille.partners.connect.notifications.infrastructure.bindings.notificationModule
import fr.devlille.partners.connect.organisations.infrastructure.bindings.organisationModule
import fr.devlille.partners.connect.partnership.infrastructure.bindings.partnershipModule
import fr.devlille.partners.connect.provider.infrastructure.bindings.providerModule
import fr.devlille.partners.connect.sponsoring.infrastructure.bindings.sponsoringModule
import fr.devlille.partners.connect.tickets.infrastructure.bindings.ticketingModule
import fr.devlille.partners.connect.users.infrastructure.bindings.userModule
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import io.ktor.client.engine.HttpClientEngine
import io.ktor.server.application.Application
import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.UUID

fun Application.moduleMocked(
    mockNetwork: Module = module {
        single<HttpClientEngine> { mockEngine }
    },
    mockStorage: Module = module {
        single<Storage> { mockk() }
    },
    mockBillingIntegration: Module = module {
        single<List<BillingGateway>> {
            listOf(
                FakeBillingGateway(),
            )
        }
    },
    mockWebhook: Module = module {
        single<WebhookRepository> {
            object : WebhookRepository {
                override suspend fun sendWebhooks(eventSlug: String, partnershipId: UUID, eventType: WebhookEventType) {
                    // Mock implementation - do nothing
                }
            }
        }
    },
) {
    module(
        ApplicationConfig(
            databaseUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            enableOpenAPI = false,
            modules = listOf(
                networkClientModule,
                storageModule,
                authModule,
                organisationModule,
                eventModule,
                userModule,
                sponsoringModule,
                companyModule,
                partnershipModule,
                providerModule,
                notificationModule,
                billingModule,
                ticketingModule,
                agendaModule,
                integrationModule,
                mockNetwork,
                mockStorage,
                mockBillingIntegration,
                mockWebhook,
            ),
        ),
    )
}
