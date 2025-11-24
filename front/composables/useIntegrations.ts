import { ref, computed } from 'vue';
import type { Ref, ComputedRef } from 'vue';
import {
  getOrgsEventsIntegrations,
  postOrgsEventsIntegrations,
  deleteOrgsEventsIntegrations,
  getStatusIntegration,
  type IntegrationSchema,
  type IntegrationSchemaProvider,
  type IntegrationSchemaUsage
} from '~/utils/api';

/**
 * Extended integration schema with status
 */
export interface IntegrationWithStatus extends IntegrationSchema {
  status?: 'success' | 'error' | 'loading' | null;
}

/**
 * Integration create data structure
 */
export interface IntegrationCreateData {
  provider: IntegrationSchemaProvider;
  usage: IntegrationSchemaUsage;
  configuration: Record<string, unknown>;
}

/**
 * Options for useIntegrations composable
 */
export interface UseIntegrationsOptions {
  orgSlug: string;
  eventSlug: string;
}

/**
 * Return type for useIntegrations composable
 */
export interface UseIntegrationsReturn {
  integrations: Ref<IntegrationWithStatus[]>;
  loading: Ref<boolean>;
  error: Ref<string | null>;
  loadIntegrations: () => Promise<void>;
  createIntegration: (data: IntegrationCreateData) => Promise<void>;
  deleteIntegration: (integrationId: string) => Promise<void>;
  configuredProviders: ComputedRef<IntegrationSchemaProvider[]>;
  loadIntegrationStatus: (integrationId: string) => Promise<void>;
}

/**
 * Composable for integration management
 *
 * Provides reactive state and methods for managing event integrations:
 * - Load integrations from API
 * - Create new integrations
 * - Delete existing integrations
 * - Track configured providers
 *
 * @param options - Organization and event slugs
 * @returns Integration management state and methods
 *
 * @example
 * ```ts
 * const { integrations, loading, loadIntegrations } = useIntegrations({
 *   orgSlug: 'devlille',
 *   eventSlug: 'devlille-2025'
 * });
 *
 * onMounted(() => loadIntegrations());
 * ```
 */
export function useIntegrations(options: UseIntegrationsOptions): UseIntegrationsReturn {
  const { orgSlug, eventSlug } = options;

  // Reactive state
  const integrations = ref<IntegrationWithStatus[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  /**
   * Computed property: List of configured provider types
   * Used to filter provider dropdown in create modal
   */
  const configuredProviders = computed(() => {
    return integrations.value.map(integration => integration.provider);
  });

  /**
   * Load status for a specific integration
   * Updates the status field in the integrations array
   *
   * @param integrationId - Integration UUID
   */
  async function loadIntegrationStatus(integrationId: string): Promise<void> {
    const integration = integrations.value.find(i => i.id === integrationId);
    if (!integration) return;

    try {
      integration.status = 'loading';
      const response = await getStatusIntegration(orgSlug, eventSlug, integrationId);

      // L'API retourne { status: boolean }
      // true = success, false = error
      const statusData = response.data as unknown as { status: boolean };
      integration.status = statusData.status ? 'success' : 'error';
    } catch (err: any) {
      console.error(`Failed to load status for integration ${integrationId}:`, err);
      // Si l'appel échoue (erreur réseau, 404, etc.), le statut est error
      integration.status = 'error';
    }
  }

  /**
   * Load all integrations for the event
   * Sets loading state and handles errors gracefully
   */
  async function loadIntegrations(): Promise<void> {
    try {
      loading.value = true;
      error.value = null;

      const response = await getOrgsEventsIntegrations(orgSlug, eventSlug);
      integrations.value = response.data.map(integration => ({
        ...integration,
        status: null
      }));

      // Charger le statut de chaque intégration en parallèle
      await Promise.all(
        integrations.value.map(integration => loadIntegrationStatus(integration.id))
      );
    } catch (err) {
      console.error('Failed to load integrations:', err);
      error.value = 'Impossible de charger les intégrations. Veuillez rafraîchir la page.';
      integrations.value = [];
    } finally {
      loading.value = false;
    }
  }

  /**
   * Create a new integration
   * Automatically refreshes list on success
   *
   * @param data - Integration creation data (provider, usage, configuration)
   * @throws Error with user-friendly message if creation fails
   */
  async function createIntegration(data: IntegrationCreateData): Promise<void> {
    try {
      await postOrgsEventsIntegrations(
        orgSlug,
        eventSlug,
        data.provider,
        data.usage,
        data.configuration
      );

      // Refresh integration list to show new integration
      await loadIntegrations();
    } catch (err) {
      console.error('Failed to create integration:', err);
      throw new Error('Échec de la création de l\'intégration. Vérifiez vos identifiants.');
    }
  }

  /**
   * Delete an integration
   * Automatically refreshes list on success
   *
   * @param integrationId - Integration UUID
   * @throws Error with user-friendly message if deletion fails
   */
  async function deleteIntegration(
    integrationId: string
  ): Promise<void> {
    try {
      await deleteOrgsEventsIntegrations(
        orgSlug,
        eventSlug,
        integrationId
      );

      // Refresh integration list to remove deleted integration
      await loadIntegrations();
    } catch (err) {
      console.error('Failed to delete integration:', err);
      throw new Error('Impossible de supprimer l\'intégration. Veuillez réessayer.');
    }
  }

  return {
    integrations,
    loading,
    error,
    loadIntegrations,
    createIntegration,
    deleteIntegration,
    configuredProviders,
    loadIntegrationStatus
  };
}
