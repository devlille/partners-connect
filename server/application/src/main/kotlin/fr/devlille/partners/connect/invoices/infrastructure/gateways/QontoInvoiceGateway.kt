package fr.devlille.partners.connect.invoices.infrastructure.gateways

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoConfig
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.invoices.domain.InvoiceGateway
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoicesTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndCompany
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionalOptionsByPack
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import java.util.UUID

class QontoInvoiceGateway(
    private val httpClient: HttpClient,
) : InvoiceGateway {
    override val provider: IntegrationProvider = IntegrationProvider.QONTO

    override fun createInvoice(integrationId: UUID, eventId: UUID, companyId: UUID): String = runBlocking {
        val config = QontoIntegrationsTable[integrationId]
        val event = EventEntity.findById(eventId)
            ?: throw NotFoundException("Event $eventId not found")
        val company = CompanyEntity.findById(companyId)
            ?: throw NotFoundException("Company $companyId not found")
        val invoice = InvoiceEntity
            .find { InvoicesTable.companyId eq company.id }
            .singleOrNull()
            ?: throw NotFoundException("No invoice found for company $companyId")
        val partnership = PartnershipEntity
            .singleByEventAndCompany(eventId, companyId)
            ?: throw NotFoundException("No partnership found for event $eventId and company $companyId")
        val selectedPackId = partnership.selectedPackId
        val suggestionPackId = partnership.suggestionPackId
        val pack = if (selectedPackId != null) {
            SponsoringPackEntity.findById(selectedPackId)
                ?: throw NotFoundException("Selected pack $selectedPackId not found")
        } else if (suggestionPackId != null) {
            SponsoringPackEntity.findById(suggestionPackId)
                ?: throw NotFoundException("Suggested pack $suggestionPackId not found")
        } else {
            throw NotFoundException("No sponsoring pack found for partnership ${partnership.id}")
        }
        val optionIds = PackOptionsTable.listOptionalOptionsByPack(pack.id.value)
            .map { it[PackOptionsTable.option].value }
        val optionalOptions = SponsoringOptionEntity
            .find { (SponsoringOptionsTable.eventId eq eventId) and (SponsoringOptionsTable.id inList optionIds) }
            .toList()

        val clients = listClients(taxId = invoice.siret, config = config)
        val client = if (clients.clients.isEmpty()) {
            createClient(invoice.toQontoClientRequest(), config).client
        } else {
            clients.clients.first()
        }
        if (event.iban == null) {
            throw NotFoundException("Event $eventId does not have an IBAN set")
        }
        val request = event.toQontoInvoiceRequest(
            clientId = client.id,
            invoicePo = invoice.po,
            invoiceItems = invoiceItem(partnership.language, pack, optionalOptions),
        )
        createInvoice(request, config).clientInvoice.invoiceUrl
    }

    private suspend fun listClients(taxId: String, config: QontoConfig): QontoClientsResponse {
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/clients?filter[tax_identification_number]=$taxId"
        val response = httpClient.get(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            headers["X-Qonto-Staging-Token"] = config.sandboxToken
            headers[HttpHeaders.ContentType] = "application/json"
        }
        return response.body<QontoClientsResponse>()
    }

    private suspend fun createClient(request: QontoClientRequest, config: QontoConfig): QontoClientResponse {
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/clients"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            headers["X-Qonto-Staging-Token"] = config.sandboxToken
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(QontoClientRequest.serializer(), request))
        }
        return response.body<QontoClientResponse>()
    }

    private suspend fun createInvoice(request: QontoInvoiceRequest, config: QontoConfig): QontoClientInvoiceResponse {
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/client_invoices"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            headers["X-Qonto-Staging-Token"] = config.sandboxToken
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(QontoInvoiceRequest.serializer(), request))
        }
        return response.body<QontoClientInvoiceResponse>()
    }

    private fun InvoiceEntity.toQontoClientRequest(): QontoClientRequest = QontoClientRequest(
        name = this.name ?: company.name,
        firstName = this.contactFirstName,
        lastName = this.contactLastName,
        type = "company",
        email = this.contactEmail,
        extraEmails = listOf(),
        vatNumber = this.vat,
        taxId = this.siret,
        billingAddress = QontoBillingAddress(
            streetAddress = this.address,
            city = this.city,
            zipCode = this.zipCode,
            countryCode = this.country,
        ),
        currency = "EUR",
        locale = "FR",
    )

    private fun EventEntity.toQontoInvoiceRequest(
        clientId: String,
        invoicePo: String?,
        invoiceItems: List<QontoInvoiceItem>,
    ): QontoInvoiceRequest {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val eventMonth = "%02d".format(startTime.monthNumber)
        val eventDay = "%02d".format(startTime.dayOfMonth)
        return QontoInvoiceRequest(
            settings = QontoInvoiceSettings(legalCapitalShare = QontoLegalCapitalShare(currency = "EUR")),
            clientId = clientId,
            dueDate = "${now.year}-${"%02d".format(now.monthNumber)}-${"%02d".format(now.dayOfMonth)}",
            issueDate = "${startTime.year}-$eventMonth-$eventDay",
            currency = "EUR",
            paymentMethods = QontoPaymentMethods(
                iban = iban!!,
            ),
            purchaseOrder = invoicePo,
            items = invoiceItems,
        )
    }

    @Suppress("SpreadOperator")
    private fun invoiceItem(
        language: String,
        pack: SponsoringPackEntity,
        options: List<SponsoringOptionEntity>,
    ): List<QontoInvoiceItem> = listOf(
        QontoInvoiceItem(
            title = "Sponsoring ${pack.name}",
            quantity = "1",
            unitPrice = QontoMoneyAmount(value = "${pack.basePrice}", currency = "EUR"),
            vatRate = "0",
        ),
        *options.map { option ->
            val translation = option.translations.firstOrNull { it.language == language }
                ?: throw NotFoundException("Translation not found for option ${option.id} in language $language")
            QontoInvoiceItem(
                title = translation.name,
                quantity = "1",
                unitPrice = QontoMoneyAmount(value = "${option.price}", currency = "EUR"),
                vatRate = "0",
            )
        }.toTypedArray(),
    )
}
