package fr.devlille.partners.connect.ai.factories

import fr.devlille.partners.connect.ai.domain.ChatRequest

fun createChatRequest(
    prompt: String = "Hello",
    model: String? = null,
    system: String? = null,
): ChatRequest = ChatRequest(prompt = prompt, model = model, system = system)
