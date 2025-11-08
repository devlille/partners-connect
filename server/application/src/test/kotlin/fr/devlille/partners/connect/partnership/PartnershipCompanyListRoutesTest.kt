package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.domain.Contact
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipCompanyListRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Suppress("LongMethod")
    @Test
    fun `GET returns partnerships for an existing company`() = testApplication {
        val companyId = UUID.randomUUID()
        val eventId1 = UUID.randomUUID()
        val eventId2 = UUID.randomUUID()
        val packId1 = UUID.randomUUID()
        val packId2 = UUID.randomUUID()
        val partnerId1 = UUID.randomUUID()
        val partnerId2 = UUID.randomUUID()
        val partnerId3 = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId1)
            insertMockedEventWithOrga(eventId2)
            insertMockedCompany(companyId, name = "TestCompany")
            insertMockedSponsoringPack(packId1, eventId1, name = "Gold Pack")
            insertMockedSponsoringPack(packId2, eventId2, name = "Silver Pack")

            // Insert three partnerships for the same company
            insertMockedPartnership(
                id = partnerId1,
                eventId = eventId1,
                companyId = companyId,
                contactName = "John Doe",
                contactRole = "Director",
                selectedPackId = packId1,
                phone = "0612788709",
                language = "en",
            )
            insertMockedPartnership(
                id = partnerId2,
                eventId = eventId2,
                companyId = companyId,
                contactName = "Jane Smith",
                contactRole = "Manager",
                suggestionPackId = packId2,
                phone = null,
                language = "fr",
            )
            insertMockedPartnership(
                id = partnerId3,
                eventId = eventId1,
                companyId = companyId,
                contactName = "Bob Wilson",
                contactRole = "CEO",
                language = "en",
            )

            // Add emails to partnerships
            transaction {
                PartnershipEmailEntity.new {
                    partnership = PartnershipEntity[partnerId1]
                    email = "john@example.com"
                }
                PartnershipEmailEntity.new {
                    partnership = PartnershipEntity[partnerId1]
                    email = "contact@example.com"
                }
                PartnershipEmailEntity.new {
                    partnership = PartnershipEntity[partnerId2]
                    email = "jane@example.com"
                }
            }
        }

        val response = client.get("/companies/$companyId/partnerships")
        assertEquals(HttpStatusCode.OK, response.status)

        val partnerships = json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(3, partnerships.size)

        // Find specific partnerships
        val johnPartnership = partnerships.find { it.contact.displayName == "John Doe" }!!
        val janePartnership = partnerships.find { it.contact.displayName == "Jane Smith" }!!
        val bobPartnership = partnerships.find { it.contact.displayName == "Bob Wilson" }!!

        // Verify John's partnership
        assertEquals(partnerId1.toString(), johnPartnership.id)
        assertEquals(Contact("John Doe", "Director"), johnPartnership.contact)
        assertEquals("TestCompany", johnPartnership.companyName)
        assertEquals("Gold Pack", johnPartnership.selectedPackName)
        assertEquals(null, johnPartnership.suggestedPackName)
        assertEquals("en", johnPartnership.language)
        assertEquals("0612788709", johnPartnership.phone)
        assertEquals(listOf("john@example.com", "contact@example.com"), johnPartnership.emails)

        // Verify Jane's partnership
        assertEquals(partnerId2.toString(), janePartnership.id)
        assertEquals(Contact("Jane Smith", "Manager"), janePartnership.contact)
        assertEquals("TestCompany", janePartnership.companyName)
        assertEquals(null, janePartnership.selectedPackName)
        assertEquals("Silver Pack", janePartnership.suggestedPackName)
        assertEquals("fr", janePartnership.language)
        assertEquals(null, janePartnership.phone)
        assertEquals(listOf("jane@example.com"), janePartnership.emails)

        // Verify Bob's partnership
        assertEquals(partnerId3.toString(), bobPartnership.id)
        assertEquals(Contact("Bob Wilson", "CEO"), bobPartnership.contact)
        assertEquals("TestCompany", bobPartnership.companyName)
        assertEquals(null, bobPartnership.selectedPackName)
        assertEquals(null, bobPartnership.suggestedPackName)
        assertEquals("en", bobPartnership.language)
        assertEquals(null, bobPartnership.phone)
        assertEquals(emptyList(), bobPartnership.emails)
    }

    @Test
    fun `GET returns empty list when company has no partnerships`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId, name = "TestCompany")
        }

        val response = client.get("/companies/$companyId/partnerships")
        assertEquals(HttpStatusCode.OK, response.status)

        val partnerships = json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(0, partnerships.size)
    }

    @Test
    fun `GET partnerships are sorted by created_at desc`() = testApplication {
        val companyId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnerId1 = UUID.randomUUID()
        val partnerId2 = UUID.randomUUID()
        val partnerId3 = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId)
            insertMockedCompany(companyId, name = "TestCompany")

            // Insert partnerships - they'll get different creation times by default
            Thread.sleep(1) // Ensure different timestamps
            insertMockedPartnership(
                id = partnerId1,
                eventId = eventId,
                companyId = companyId,
                contactName = "First",
            )
            Thread.sleep(1)
            insertMockedPartnership(
                id = partnerId2,
                eventId = eventId,
                companyId = companyId,
                contactName = "Second",
            )
            Thread.sleep(1)
            insertMockedPartnership(
                id = partnerId3,
                eventId = eventId,
                companyId = companyId,
                contactName = "Third",
            )
        }

        val response = client.get("/companies/$companyId/partnerships")
        assertEquals(HttpStatusCode.OK, response.status)

        val partnerships = json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(3, partnerships.size)

        // Should be sorted by created_at desc (most recent first)
        // Since Third was created last, it should be first
        assertEquals("Third", partnerships[0].contact.displayName)
    }

    @Test
    fun `GET returns 404 when company does not exist`() = testApplication {
        val randomCompanyId = UUID.randomUUID()

        application { moduleMocked() }

        val response = client.get("/companies/$randomCompanyId/partnerships")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET returns 400 when company ID is invalid UUID`() = testApplication {
        application { moduleMocked() }

        val response = client.get("/companies/not-a-uuid/partnerships")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
