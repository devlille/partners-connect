package fr.devlille.partners.connect.webhooks.domain

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebhookQuestionTest {
    @Test
    fun `serializes order and is_correct even at zero-or-false values`() {
        val question = WebhookQuestion(
            id = "q1",
            question = "What is Kotlin?",
            order = 0,
            answers = listOf(
                WebhookAnswer(id = "a1", answer = "A language", isCorrect = true, order = 0),
                WebhookAnswer(id = "a2", answer = "A drink", isCorrect = false, order = 1),
            ),
        )

        val json = Json.encodeToString(WebhookQuestion.serializer(), question)

        // order and is_correct are required (no defaults) -> never dropped, even at 0 / false
        assertTrue(json.contains("\"is_correct\":true"), json)
        assertTrue(json.contains("\"is_correct\":false"), json)
        assertTrue(json.contains("\"order\":0"), json)
        assertTrue(json.contains("\"order\":1"), json)
    }

    @Test
    fun `question defaults answers to empty list`() {
        val question = WebhookQuestion(id = "q1", question = "x", order = 0)

        assertEquals(emptyList(), question.answers)
    }
}
