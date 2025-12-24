package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyListRouteGetTest {
    @Test
    fun `GET returns empty list if no companies exist`() = testApplication {
        application { moduleMocked() }

        val response = client.get("/companies")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val paginated = Json.parseToJsonElement(responseBody).jsonObject
        val items = paginated["items"]!!.jsonArray
        assertEquals(0, items.size)
        assertEquals(1, paginated["page"]!!.toString().toInt())
        assertEquals(20, paginated["page_size"]!!.toString().toInt())
        assertEquals(0, paginated["total"]!!.toString().toInt())
    }

    @Test
    fun `GET returns companies sorted by name`() = testApplication {
        application {
            moduleMocked()
            transaction {
                listOf("Zeta", "Alpha", "Beta").forEach {
                    insertMockedCompany(name = it, description = it)
                }
            }
        }

        val response = client.get("/companies")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val paginated = Json.parseToJsonElement(responseBody).jsonObject
        val items = paginated["items"]!!.jsonArray
        val names = items.map { it.jsonObject["name"]!!.toString() }
        val alphaIdx = names.indexOf("\"Alpha\"")
        val betaIdx = names.indexOf("\"Beta\"")
        val zetaIdx = names.indexOf("\"Zeta\"")
        assertTrue(alphaIdx < betaIdx)
        assertTrue(betaIdx < zetaIdx)
    }
}
