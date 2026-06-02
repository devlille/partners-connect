package fr.devlille.partners.connect.webhooks.application.mappers

import fr.devlille.partners.connect.partnership.infrastructure.db.QandaAnswerEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionEntity
import fr.devlille.partners.connect.webhooks.domain.WebhookAnswer
import fr.devlille.partners.connect.webhooks.domain.WebhookQuestion

fun QandaQuestionEntity.toWebhookQuestion(order: Int): WebhookQuestion = WebhookQuestion(
    id = id.value.toString(),
    question = question,
    order = order,
    answers = answers.mapIndexed { index, answer -> answer.toWebhookAnswer(index) },
)

fun QandaAnswerEntity.toWebhookAnswer(order: Int): WebhookAnswer = WebhookAnswer(
    id = id.value.toString(),
    answer = answer,
    isCorrect = isCorrect,
    order = order,
)
