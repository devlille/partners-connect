package fr.devlille.partners.connect.integrations.infrastructure.api

import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
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
        IntegrationProvider.WEBHOOK to CreateIntegration.CreateWebhookIntegration.serializer(),
    )

    override fun serializerFor(provider: IntegrationProvider): KSerializer<out CreateIntegration> {
        return serializers[provider]
            ?: throw NotFoundException(
                code = ErrorCode.PROVIDER_NOT_FOUND,
                message = "No serializer found for provider: $provider",
                meta = mapOf(MetaKeys.PROVIDER to provider.toString()),
            )
    }
}
