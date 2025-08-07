package fr.devlille.partners.connect.integrations.infrastructure.api

import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.KSerializer

interface IntegrationDeserializerRegistry {
    fun serializerFor(provider: IntegrationProvider): KSerializer<out CreateIntegration>
}

class DefaultIntegrationDeserializerRegistry : IntegrationDeserializerRegistry {
    private val serializers: Map<IntegrationProvider, KSerializer<out CreateIntegration>> = mapOf(
        IntegrationProvider.SLACK to CreateIntegration.CreateSlackIntegration.serializer(),
        IntegrationProvider.MAILJET to CreateIntegration.CreateMailjetIntegration.serializer(),
        IntegrationProvider.QONTO to CreateIntegration.CreateQontoIntegration.serializer(),
        IntegrationProvider.BILLETWEB to CreateIntegration.CreateBilletWebIntegration.serializer(),
        // Add other providers here
    )

    override fun serializerFor(provider: IntegrationProvider): KSerializer<out CreateIntegration> {
        return serializers[provider]
            ?: throw NotFoundException("No serializer found for provider: $provider")
    }
}
