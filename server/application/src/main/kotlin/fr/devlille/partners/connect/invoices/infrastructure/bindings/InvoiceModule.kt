package fr.devlille.partners.connect.invoices.infrastructure.bindings

import fr.devlille.partners.connect.internal.infrastructure.bindings.networkClientModule
import fr.devlille.partners.connect.invoices.application.InvoiceRepositoryExposed
import fr.devlille.partners.connect.invoices.domain.InvoiceGateway
import fr.devlille.partners.connect.invoices.domain.InvoiceRepository
import fr.devlille.partners.connect.invoices.infrastructure.gateways.QontoInvoiceGateway
import org.koin.dsl.module

val invoiceIntegrationsModule = module {
    includes(networkClientModule)

    single<List<InvoiceGateway>> {
        listOf(
            QontoInvoiceGateway(httpClient = get()),
        )
    }
}

val invoicesModule = module {
    includes(invoiceIntegrationsModule)

    single<InvoiceRepository> {
        InvoiceRepositoryExposed(invoiceGateways = get())
    }
}
