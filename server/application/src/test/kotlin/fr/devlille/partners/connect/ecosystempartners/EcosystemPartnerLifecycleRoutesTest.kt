package fr.devlille.partners.connect.ecosystempartners

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.ecosystempartners.domain.RegisterEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.domain.RegisterEcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEntity
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.partnership.domain.TextSelection
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("MaxLineLength")
class EcosystemPartnerLifecycleRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Suppress("LongMethod") // Integration test requires comprehensive workflow validation
    @Test
    fun `full lifecycle - create category, submit, validate, list public, decline, listing drops it`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId, name = "ACME Media")
            }
        }

        // 1. Create category
        val createCategoryResponse = client.post("/orgs/$orgId/events/$eventId/ecosystem-partner-categories") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    RegisterEcosystemPartnerCategory.serializer(),
                    RegisterEcosystemPartnerCategory(name = "Media", displayOrder = 1),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, createCategoryResponse.status)
        val categoryId = json.parseToJsonElement(createCategoryResponse.bodyAsText())
            .jsonObject["id"]!!
            .jsonPrimitive
            .content

        // 2. Public submission
        val submitResponse = client.post("/events/$eventId/ecosystem-partners") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterEcosystemPartner.serializer(),
                    RegisterEcosystemPartner(
                        companyId = companyId.toString(),
                        categoryId = categoryId,
                        emails = listOf("contact@acme.test"),
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, submitResponse.status)
        val partnerId = json.parseToJsonElement(submitResponse.bodyAsText())
            .jsonObject["id"]!!
            .jsonPrimitive
            .content

        // 3. Public listing empty (not validated yet)
        val publicBeforeValidate = client.get("/events/$eventId/ecosystem-partners")
        assertEquals(HttpStatusCode.OK, publicBeforeValidate.status)
        assertFalse(publicBeforeValidate.bodyAsText().contains("ACME Media"))

        // 4. Validate
        val validateResponse = client.post(
            "/orgs/$orgId/events/$eventId/ecosystem-partners/$partnerId/validate",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, validateResponse.status)

        // 5. Public listing shows it
        val publicAfterValidate = client.get("/events/$eventId/ecosystem-partners")
        assertEquals(HttpStatusCode.OK, publicAfterValidate.status)
        val publicAfterValidateBody = publicAfterValidate.bodyAsText()
        assertTrue(publicAfterValidateBody.contains("ACME Media"))
        assertTrue(publicAfterValidateBody.contains("Media"))

        // 6. Decline
        val declineResponse = client.post(
            "/orgs/$orgId/events/$eventId/ecosystem-partners/$partnerId/decline",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, declineResponse.status)

        // 7. Public listing drops it
        val publicAfterDecline = client.get("/events/$eventId/ecosystem-partners")
        assertEquals(HttpStatusCode.OK, publicAfterDecline.status)
        assertFalse(publicAfterDecline.bodyAsText().contains("ACME Media"))

        // 8. Category in-use cannot be deleted
        val deleteCategoryInUseResponse = client.delete(
            "/orgs/$orgId/events/$eventId/ecosystem-partner-categories/$categoryId",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Conflict, deleteCategoryInUseResponse.status)

        // 9. Delete partner
        val deletePartnerResponse = client.delete(
            "/orgs/$orgId/events/$eventId/ecosystem-partners/$partnerId",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, deletePartnerResponse.status)

        // 10. Now category can be deleted
        val deleteCategoryResponse = client.delete(
            "/orgs/$orgId/events/$eventId/ecosystem-partner-categories/$categoryId",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, deleteCategoryResponse.status)
    }

    @Test
    fun `same company can have multiple categories on the same event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val mediaId = UUID.randomUUID()
        val communityId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId, name = "ACME Media")
                insertMockedEcosystemPartnerCategory(mediaId, eventId = eventId, name = "Media")
                insertMockedEcosystemPartnerCategory(communityId, eventId = eventId, name = "Community")
            }
        }

        val firstSubmission = client.post("/events/$eventId/ecosystem-partners") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterEcosystemPartner.serializer(),
                    RegisterEcosystemPartner(
                        companyId = companyId.toString(),
                        categoryId = mediaId.toString(),
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, firstSubmission.status)

        val secondSubmission = client.post("/events/$eventId/ecosystem-partners") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterEcosystemPartner.serializer(),
                    RegisterEcosystemPartner(
                        companyId = companyId.toString(),
                        categoryId = communityId.toString(),
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, secondSubmission.status)

        val listResponse = client.get("/orgs/$orgId/events/$eventId/ecosystem-partners") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val list = json.parseToJsonElement(listResponse.bodyAsText()).jsonArray
        assertEquals(2, list.size)
    }

    @Suppress("LongMethod") // Integration test requires comprehensive workflow validation
    @Test
    fun `same company can have classic partnership and ecosystem partner on the same event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId, name = "ACME Media")
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId, required = false)
                insertMockedEcosystemPartnerCategory(categoryId, eventId = eventId, name = "Media")
            }
        }

        // 1. Classic partnership
        val classicResponse = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterPartnership.serializer(),
                    RegisterPartnership(
                        packId = packId.toString(),
                        companyId = companyId.toString(),
                        contactName = "John Doe",
                        contactRole = "Marketing Manager",
                        language = "en",
                        phone = "+33600000000",
                        emails = listOf("partner@example.com"),
                        optionSelections = listOf(TextSelection(optionId = optionId.toString())),
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, classicResponse.status)
        val partnershipId = UUID.fromString(
            json.parseToJsonElement(classicResponse.bodyAsText())
                .jsonObject["id"]!!
                .jsonPrimitive
                .content,
        )

        // 2. Ecosystem partner (light)
        val ecosystemResponse = client.post("/events/$eventId/ecosystem-partners") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterEcosystemPartner.serializer(),
                    RegisterEcosystemPartner(
                        companyId = companyId.toString(),
                        categoryId = categoryId.toString(),
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, ecosystemResponse.status)
        val ecosystemPartnerId = UUID.fromString(
            json.parseToJsonElement(ecosystemResponse.bodyAsText())
                .jsonObject["id"]!!
                .jsonPrimitive
                .content,
        )

        // 3. Both rows exist and reference the same company
        transaction {
            val partnership = PartnershipEntity.findById(partnershipId)
            assertNotNull(partnership)
            val ecosystemPartner = EcosystemPartnerEntity.findById(ecosystemPartnerId)
            assertNotNull(ecosystemPartner)
            assertEquals(companyId, partnership.company.id.value)
            assertEquals(companyId, ecosystemPartner.company.id.value)
        }
    }
}
