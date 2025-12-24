package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.QuantitativeSelection
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.partnership.domain.SelectableSelection
import fr.devlille.partners.connect.partnership.domain.TextSelection
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.domain.SelectableValue
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class PartnershipRegisterRoutePostTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST registers a valid partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId, required = false)
            }
        }

        val body = RegisterPartnership(
            packId = packId.toString(),
            companyId = companyId.toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            phone = "+33600000000",
            emails = listOf("partner@example.com"),
            optionSelections = listOf(
                TextSelection(
                    optionId = optionId.toString(),
                ),
            ),
        )

        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST creates partnership with option selections - schema validation`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val quantitativeOptionId = UUID.randomUUID()
        val selectableOptionId = UUID.randomUUID()
        val selectableValueId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(quantitativeOptionId, eventId, optionType = OptionType.TYPED_QUANTITATIVE)
                insertMockedSponsoringOption(
                    optionId = selectableOptionId,
                    eventId = eventId,
                    optionType = OptionType.TYPED_SELECTABLE,
                    selectableValues = listOf(SelectableValue(selectableValueId.toString(), "3x6m", 100)),
                )
                insertMockedPackOptions(packId, quantitativeOptionId, required = false)
                insertMockedPackOptions(packId, selectableOptionId, required = false)
            }
        }

        val request = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            optionSelections = listOf(
                QuantitativeSelection(
                    optionId = quantitativeOptionId.toString(),
                    selectedQuantity = 3,
                ),
                SelectableSelection(
                    optionId = selectableOptionId.toString(),
                    selectedValueId = selectableValueId.toString(),
                ),
            ),
            contactName = "John Doe",
            contactRole = "Developer",
            language = "en",
        )

        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterPartnership.serializer(), request))
        }

        // Contract test: Should create partnership with selections
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST creates partnership with quantity zero exclusion - schema validation`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val quantitativeOptionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(quantitativeOptionId, eventId, optionType = OptionType.TYPED_QUANTITATIVE)
                insertMockedPackOptions(packId, quantitativeOptionId, required = false)
            }
        }

        val request = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            optionSelections = listOf(
                QuantitativeSelection(
                    optionId = quantitativeOptionId.toString(),
                    // Zero quantity should exclude option
                    selectedQuantity = 0,
                ),
            ),
            contactName = "Jane Smith",
            contactRole = "CEO",
            language = "en",
        )

        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterPartnership.serializer(), request))
        }

        // Contract test: Should create partnership but exclude zero-quantity option
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST creates partnership without selections - backward compatibility`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val textOptionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(textOptionId, eventId)
            }
        }

        // Traditional partnership request without option_selections field
        val request = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            optionSelections = emptyList(),
            contactName = "Legacy User",
            contactRole = "Manager",
            language = "en",
        )

        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterPartnership.serializer(), request))
        }

        // Contract test: Should maintain backward compatibility
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST return 400 to validate option selections schema - contract validation`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val selectableOptionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(selectableOptionId, eventId)
            }
        }

        // Invalid selection - wrong type for selectable option
        val invalidRequestJson = """
            {
                "company_id": "$companyId",
                "pack_id": "$packId",
                "option_selections": [
                    {
                        "type": "selectable_selection",
                        "option_id": "$selectableOptionId",
                        "selected_quantity": 1
                    }
                ],
                "contact_name": "Invalid User",
                "contact_role": "Tester",
                "language": "en"
            }
        """.trimIndent()

        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(invalidRequestJson)
        }

        // Contract test: Should validate selection schema appropriately
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST returns 404 when event not found`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
            }
        }

        val body = RegisterPartnership(
            companyId = UUID.randomUUID().toString(),
            packId = UUID.randomUUID().toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            optionSelections = listOf(),
        )
        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when company not found`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val body = RegisterPartnership(
            companyId = UUID.randomUUID().toString(),
            packId = UUID.randomUUID().toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            optionSelections = listOf(),
        )
        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when pack not found`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
            }
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = UUID.randomUUID().toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            optionSelections = listOf(),
        )
        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 403 when event submission period is not started`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val now = Clock.System.now()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(
                    eventId,
                    orgId = orgId,
                    submissionStartTime = now.plus(1.days).toLocalDateTime(TimeZone.UTC),
                    submissionEndTime = now.plus(3.days).toLocalDateTime(TimeZone.UTC),
                )
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
            }
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            optionSelections = listOf(),
        )
        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST returns 403 when option not optional`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId)
            }
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            optionSelections = listOf(
                TextSelection(
                    optionId = optionId.toString(),
                ),
            ),
        )
        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("not optional"))
    }

    @Test
    fun `POST returns 403 when option has no translation`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId, required = false)
            }
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "fr",
            optionSelections = listOf(
                TextSelection(
                    optionId = optionId.toString(),
                ),
            ),
        )
        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("does not have a translation"))
    }

    @Test
    fun `POST returns 409 when partnership already exists`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(eventId = eventId, companyId = companyId, selectedPackId = packId)
            }
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            optionSelections = listOf(),
        )
        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }
}
