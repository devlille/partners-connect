package fr.devlille.partners.connect.billing.infrastructure.bindings

import fr.devlille.partners.connect.billing.application.BillingRepositoryExposed
import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.domain.BillingRepository
import fr.devlille.partners.connect.billing.infrastructure.gateways.QontoBillingGateway
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkClientModule
import org.koin.dsl.module

val billingIntegrationsModule = module {
    includes(networkClientModule)

    single<List<BillingGateway>> {
        listOf(
            QontoBillingGateway(httpClient = get()),
        )
    }
}

val billingModule = module {
    includes(billingIntegrationsModule)

    single<BillingRepository> {
        BillingRepositoryExposed(billingGateways = get())
    }
}
