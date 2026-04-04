package fr.devlille.partners.connect.agenda.infrastructure.providers

import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpenPlannerSessionDeserializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize session with valid dateStart and dateEnd`() {
        val raw = """
            {
                "id": "abc123",
                "title": "My Talk",
                "dateStart": "2026-06-11T08:00:00.000+00:00",
                "dateEnd": "2026-06-11T08:45:00.000+00:00"
            }
        """.trimIndent()

        val session = json.decodeFromString<OpenPlannerSession>(raw)

        assertEquals("abc123", session.id)
        assertEquals(Instant.parse("2026-06-11T08:00:00.000+00:00"), session.dateStart)
        assertEquals(Instant.parse("2026-06-11T08:45:00.000+00:00"), session.dateEnd)
    }

    @Test
    fun `deserialize session with empty dateStart and dateEnd returns null`() {
        val raw = """
            {
                "id": "xyz789",
                "title": "Unscheduled Talk",
                "dateStart": "",
                "dateEnd": ""
            }
        """.trimIndent()

        val session = json.decodeFromString<OpenPlannerSession>(raw)

        assertEquals("xyz789", session.id)
        assertNull(session.dateStart)
        assertNull(session.dateEnd)
    }

    @Test
    fun `deserialize session with null dateStart and dateEnd returns null`() {
        val raw = """
            {
                "id": "null123",
                "title": "Talk Without Dates"
            }
        """.trimIndent()

        val session = json.decodeFromString<OpenPlannerSession>(raw)

        assertEquals("null123", session.id)
        assertNull(session.dateStart)
        assertNull(session.dateEnd)
    }
}
