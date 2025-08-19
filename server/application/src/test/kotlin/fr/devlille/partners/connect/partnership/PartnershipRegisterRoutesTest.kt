package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedOptionTranslation
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
            insertMockedOptionTranslation(optionId)
            insertMockedPackOptions(packId, optionId, required = false)
        }

        val body = RegisterPartnership(
            packId = packId.toString(),
            companyId = companyId.toString(),
            optionIds = listOf(optionId.toString()),
            language = "en",
            phone = "+33600000000",
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            emails = listOf("partner@example.com"),
        )

        val response = client.post("/events/$eventSlug/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val bodyText = response.bodyAsText()
        assertTrue(bodyText.contains("id"))
    }

    @Test
    fun `POST returns 404 when event not found`() = testApplication {
        val body = RegisterPartnership(
            companyId = UUID.randomUUID().toString(),
            packId = "pack",
            optionIds = listOf(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
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
            packId = "pack",
            optionIds = listOf(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
        )
        val response = client.post("/events/$eventSlug/partnership") {
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
            optionIds = listOf(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
        )
        val response = client.post("/events/$eventSlug/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 400 when partnership already exists`() = testApplication {
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
            optionIds = listOf(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
        )
        val response = client.post("/events/$eventSlug/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST returns 400 when option not optional`() = testApplication {
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
            optionIds = listOf(optionId.toString()),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
        )
        val response = client.post("/events/$eventSlug/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("not optional"))
    }

    @Test
    fun `POST returns 400 when option has no translation`() = testApplication {
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
            optionIds = listOf(optionId.toString()),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "fr",
        )
        val response = client.post("/events/$eventSlug/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("does not have a translation"))
    }
}
