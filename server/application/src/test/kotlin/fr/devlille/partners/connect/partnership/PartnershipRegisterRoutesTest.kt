package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
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
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartnershipRegisterRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST registers a valid partnership`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-registers-a-val-982"
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId, eventId)
            insertMockedPackOptions(packId, optionId, required = false)
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

        val response = client.post("/events/$eventSlug/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val bodyText = response.bodyAsText()
        assertTrue(bodyText.contains("id"))
    }

    @Test
    fun `POST creates partnership with option selections - schema validation`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-partnership-with-selections"
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val quantitativeOptionId = UUID.randomUUID()
        val selectableOptionId = UUID.randomUUID()
        val selectableValueId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(quantitativeOptionId, eventId, optionType = OptionType.TYPED_QUANTITATIVE)
            insertMockedSponsoringOption(
                selectableOptionId,
                eventId,
                optionType = OptionType.TYPED_SELECTABLE,
                selectableValues = listOf(SelectableValue(selectableValueId.toString(), "3x6m", 100)),
            )
            // Associate options with pack
            insertMockedPackOptions(packId, quantitativeOptionId, required = false)
            insertMockedPackOptions(packId, selectableOptionId, required = false)
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

        val response = client.post("/events/$eventSlug/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterPartnership.serializer(), request))
        }

        // Contract test: Should create partnership with selections
        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("id"))
        assertTrue(responseBody.contains("\"id\":"))
    }

    @Test
    fun `POST creates partnership with quantity zero exclusion - schema validation`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-partnership-zero-quantity"
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val quantitativeOptionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(quantitativeOptionId, eventId, optionType = OptionType.TYPED_QUANTITATIVE)
            // Associate option with pack
            insertMockedPackOptions(packId, quantitativeOptionId, required = false)
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

        val response = client.post("/events/$eventSlug/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterPartnership.serializer(), request))
        }

        // Contract test: Should create partnership but exclude zero-quantity option
        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("id"))
    }

    @Test
    fun `POST creates partnership without selections - backward compatibility`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-partnership-no-selections"
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val textOptionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(textOptionId, eventId)
        }

        // Traditional partnership request without option_selections field
        val request = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            // Backward compatibility - no selections
            optionSelections = emptyList(),
            contactName = "Legacy User",
            contactRole = "Manager",
            language = "en",
        )

        val response = client.post("/events/$eventSlug/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterPartnership.serializer(), request))
        }

        // Contract test: Should maintain backward compatibility
        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("id"))
    }

    @Test
    fun `POST return 400 to validate option selections schema - contract validation`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-partnership-selection-validation"
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val selectableOptionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(selectableOptionId, eventId)
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

        val response = client.post("/events/$eventSlug/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(invalidRequestJson)
        }

        // Contract test: Should validate selection schema appropriately
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST returns 404 when event not found`() = testApplication {
        val body = RegisterPartnership(
            companyId = UUID.randomUUID().toString(),
            packId = "pack",
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            optionSelections = listOf(),
        )
        val response = client.post("/events/${UUID.randomUUID()}/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when company not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-returns-404-whe-711"
        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
        }

        val body = RegisterPartnership(
            companyId = UUID.randomUUID().toString(),
            packId = UUID.randomUUID().toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            optionSelections = listOf(),
        )
        val response = client.post("/events/$eventSlug/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when pack not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-returns-404-whe-169"
        val companyId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = UUID.randomUUID().toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            optionSelections = listOf(),
        )
        val response = client.post("/events/$eventSlug/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 409 when partnership already exists`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-returns-400-whe-141"
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            val selectedPack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(eventId = eventId, companyId = companyId, selectedPackId = selectedPack.id.value)
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
            optionSelections = listOf(),
        )
        val response = client.post("/events/$eventSlug/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST returns 403 when option not optional`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-returns-400-whe-15"
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId, eventId)
            insertMockedPackOptions(packId, optionId)
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
        val response = client.post("/events/$eventSlug/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("not optional"))
    }

    @Test
    fun `POST returns 403 when option has no translation`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-returns-400-whe-604"
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId, eventId)
            insertMockedPackOptions(packId, optionId, required = false)
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
        val response = client.post("/events/$eventSlug/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("does not have a translation"))
    }
}
