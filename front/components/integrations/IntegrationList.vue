<template>
  <div class="bg-white rounded-lg shadow">
    <!-- Loading State -->
    <div v-if="loading" class="px-6 py-12">
      <div class="space-y-4">
        <div v-for="i in 3" :key="i" class="animate-pulse">
          <div class="flex items-start gap-4">
            <div class="w-12 h-12 bg-gray-200 rounded-lg" />
            <div class="flex-1 space-y-2">
              <div class="h-5 bg-gray-200 rounded w-1/4" />
              <div class="h-4 bg-gray-200 rounded w-1/3" />
              <div class="h-4 bg-gray-200 rounded w-1/5" />
            </div>
            <div class="w-20 h-9 bg-gray-200 rounded" />
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="integrations.length === 0" class="px-6 py-12 text-center">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">Aucune intégration configurée</h3>
      <p class="mt-1 text-sm text-gray-500">Commencez par ajouter une intégration.</p>
    </div>

    <!-- Integration List -->
    <div v-else>
      <div class="px-6 py-4 border-b border-gray-200">
        <h2 class="text-lg font-semibold text-gray-900">
          Intégrations ({{ integrations.length }})
        </h2>
      </div>

      <ul class="divide-y divide-gray-200">
        <li
          v-for="integration in integrations"
          :key="integration.id"
          class="px-6 py-4 hover:bg-gray-50 transition-colors"
        >
          <div class="flex items-start justify-between">
            <div class="flex items-start gap-4 flex-1">
              <!-- Provider Icon -->
              <div
                class="w-12 h-12 rounded-lg flex items-center justify-center flex-shrink-0"
                :class="getProviderBgClass(integration.provider)"
              >
                <i
                  :class="[getProviderIcon(integration.provider), 'text-2xl']"
                  :style="{ color: getProviderIconColor(integration.provider) }"
                />
              </div>

              <!-- Integration Details -->
              <div class="flex-1 min-w-0">
                <h3 class="text-base font-semibold text-gray-900">
                  {{ getProviderName(integration.provider) }}
                </h3>
                <div class="mt-1 space-y-1">
                  <p class="text-sm text-gray-600">
                    <span class="font-medium">Utilisation:</span> {{ getUsageName(integration.usage) }}
                  </p>
                  <p class="text-sm text-gray-600">
                    <span class="font-medium">Créé le:</span> {{ formatDate(integration.created_at) }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Delete Button -->
            <UButton
              color="error"
              variant="ghost"
              size="sm"
              icon="i-heroicons-trash"
              :loading="deletingId === integration.id"
              :disabled="deletingId !== null"
              @click="$emit('delete', integration)"
            >
              Supprimer
            </UButton>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { IntegrationSchema } from '~/utils/api';
import { PROVIDERS } from '~/utils/integrationProviders';
import { USAGES } from '~/utils/integrationUsages';

/**
 * Props for IntegrationList component
 */
interface Props {
  /** Array of integrations to display */
  integrations: IntegrationSchema[];

  /** Loading state for the list */
  loading: boolean;

  /** ID of integration currently being deleted (for loading state) */
  deletingId: string | null;
}

/**
 * Events emitted by IntegrationList component
 */
interface Emits {
  /** Emitted when user clicks delete button on an integration */
  (event: 'delete', integration: IntegrationSchema): void;
}

const props = defineProps<Props>();
defineEmits<Emits>();

/**
 * Get provider display name from metadata
 */
function getProviderName(provider: string): string {
  return PROVIDERS[provider as keyof typeof PROVIDERS]?.name || provider;
}

/**
 * Get provider icon class from metadata
 */
function getProviderIcon(provider: string): string {
  return PROVIDERS[provider as keyof typeof PROVIDERS]?.icon || 'i-heroicons-cube';
}

/**
 * Get usage display name from metadata
 */
function getUsageName(usage: string): string {
  return USAGES[usage as keyof typeof USAGES]?.name || usage;
}

/**
 * Get background color class for provider icon container
 */
function getProviderBgClass(provider: string): string {
  const bgColors: Record<string, string> = {
    qonto: 'bg-purple-100',
    mailjet: 'bg-blue-100',
    billetweb: 'bg-green-100',
    openplanner: 'bg-orange-100',
    slack: 'bg-pink-100',
    webhook: 'bg-gray-100'
  };
  return bgColors[provider] || 'bg-gray-100';
}

/**
 * Get icon color for provider
 */
function getProviderIconColor(provider: string): string {
  const iconColors: Record<string, string> = {
    qonto: '#9333ea', // purple-600
    mailjet: '#2563eb', // blue-600
    billetweb: '#16a34a', // green-600
    openplanner: '#ea580c', // orange-600
    slack: '#db2777', // pink-600
    webhook: '#4b5563' // gray-600
  };
  return iconColors[provider] || '#4b5563';
}

/**
 * Format ISO date string to French date format
 */
function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
}
</script>
