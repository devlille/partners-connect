package fr.devlille.partners.connect.internal

import com.slack.api.Slack
import fr.devlille.partners.connect.ApplicationConfig
import fr.devlille.partners.connect.agenda.infrastructure.bindings.agendaModule
import fr.devlille.partners.connect.auth.infrastructure.bindings.authModule
import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.infrastructure.bindings.billingModule
import fr.devlille.partners.connect.companies.infrastructure.bindings.companyModule
import fr.devlille.partners.connect.events.infrastructure.bindings.eventModule
import fr.devlille.partners.connect.integrations.infrastructure.bindings.integrationModule
import fr.devlille.partners.connect.internal.infrastructure.bindings.mapsModule
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkClientModule
import fr.devlille.partners.connect.internal.infrastructure.bindings.storageModule
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.geocode.Geocode
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
import io.mockk.every
import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.UUID

/**
 * Sets up the application module with a shared in-memory database for testing.
 * @param userId The ID of the user to be used in the mock network engine.
 * @param nbProductsForTickets The number of products to be returned by the mock network engine for tickets.
 * @param storage The storage instance to be used in the module. Defaults to a mock instance.
 */
fun Application.moduleSharedDb(
    userId: UUID,
    nbProductsForTickets: Int = 0,
    storage: Storage = mockk(),
) {
    moduleMocked(
        databaseUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        mockNetwork = module {
            single<HttpClientEngine> { mockEngine(userId, nbProductsForTickets) }
        },
        mockStorage = module {
            single { storage }
        },
    )
}

/**
 * Sets up the application module with mocked dependencies for testing.
 * @param databaseUrl The JDBC URL for the in-memory database.
 * @param mockNetwork The Koin module providing a mocked network client.
 * @param mockSlack The Koin module providing a mocked Slack client.
 * @param mockStorage The Koin module providing a mocked storage client.
 * @param mockGeocode The Koin module providing a mocked geocode client.
 * @param mockBillingIntegration The Koin module providing mocked billing gateways.
 * @param mockWebhook The Koin module providing a mocked webhook repository.
 */
@Suppress("LongParameterList")
fun Application.moduleMocked(
    databaseUrl: String = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
    mockNetwork: Module = module {
        single<HttpClientEngine> { mockEngine }
    },
    mockSlack: Module = module {
        single<Slack> { mockk() }
    },
    mockStorage: Module = module {
        single<Storage> { mockk() }
    },
    mockGeocode: Module = module {
        single<Geocode> {
            val geocodeApi = mockk<Geocode>()
            every { geocodeApi.countryCode(any()) } returns "FR"
            geocodeApi
        }
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
            databaseUrl = databaseUrl,
            enableOpenAPI = false,
            modules = listOf(
                networkClientModule,
                storageModule,
                mapsModule,
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
                mockSlack,
                mockStorage,
                mockGeocode,
                mockBillingIntegration,
                mockWebhook,
            ),
        ),
    )
}
