<template>
  <Teleport to="body">
    <div
      v-if="isOpen"
      class="fixed inset-0 flex items-center justify-center p-4 bg-black/50"
      style="z-index: 9999;"
      role="dialog"
      aria-modal="true"
      aria-labelledby="create-modal-title"
      @click.self="handleClose"
      @keydown.esc="handleClose"
    >
      <div
        class="w-full max-w-2xl bg-white rounded-lg shadow-xl max-h-[90vh] overflow-y-auto"
        @click.stop
      >
        <!-- Header -->
        <div class="px-6 py-4 border-b border-gray-200">
          <h3 id="create-modal-title" class="text-lg font-semibold text-gray-900">
            Ajouter une intégration
          </h3>
        </div>

        <!-- Body -->
        <div class="px-6 py-4">
          <!-- All Providers Configured Message -->
          <div v-if="availableProviders.length === 0" class="text-center py-8">
            <svg
              class="mx-auto h-12 w-12 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            <h3 class="mt-2 text-sm font-medium text-gray-900">
              Tous les types d'intégration sont déjà configurés
            </h3>
            <p class="mt-1 text-sm text-gray-500">
              Supprimez une intégration existante pour en ajouter une nouvelle.
            </p>
          </div>

          <!-- Form -->
          <form v-else @submit.prevent="handleSubmit" class="space-y-4">
            <!-- Provider Selection -->
            <div>
              <label for="provider" class="block text-sm font-medium text-gray-700 mb-2">
                Fournisseur <span class="text-red-500">*</span>
              </label>
              <select
                id="provider"
                v-model="selectedProvider"
                class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                :disabled="isSubmitting"
              >
                <option value="">Sélectionnez un fournisseur</option>
                <option v-for="option in providerOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>

            <!-- Usage Selection -->
            <div v-if="selectedProvider">
              <label for="usage" class="block text-sm font-medium text-gray-700 mb-2">
                Utilisation <span class="text-red-500">*</span>
              </label>
              <select
                id="usage"
                v-model="selectedUsage"
                class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                :disabled="isSubmitting || !selectedProvider"
              >
                <option value="">Sélectionnez une utilisation</option>
                <option v-for="option in usageOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>

            <!-- Provider-Specific Form -->
            <div v-if="selectedProvider && selectedUsage" class="pt-4 border-t border-gray-200">
              <h4 class="text-sm font-medium text-gray-900 mb-4">
                Configuration {{ getProviderName(selectedProvider) }}
              </h4>

              <component
                :is="providerFormComponent"
                v-model="configuration"
                :disabled="isSubmitting"
                @update:valid="(valid) => formValid = valid"
              />
            </div>

            <!-- Error Message -->
            <AlertMessage v-if="error" :message="error" type="error" class="text-sm" />
          </form>
        </div>

        <!-- Footer -->
        <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
          <UButton color="neutral" variant="ghost" :disabled="isSubmitting" @click="handleClose">
            Annuler
          </UButton>
          <UButton
            v-if="availableProviders.length > 0"
            color="primary"
            :loading="isSubmitting"
            :disabled="!canSubmit"
            @click="handleSubmit"
          >
            Créer
          </UButton>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { IntegrationSchemaProvider, IntegrationSchemaUsage } from '~/utils/api';
import { PROVIDERS, ALL_PROVIDERS } from '~/utils/integrationProviders';
import { USAGES } from '~/utils/integrationUsages';
import QontoForm from './forms/QontoForm.vue';
import MailjetForm from './forms/MailjetForm.vue';
import BilletwebForm from './forms/BilletwebForm.vue';
import OpenPlannerForm from './forms/OpenPlannerForm.vue';
import SlackForm from './forms/SlackForm.vue';
import WebhookForm from './forms/WebhookForm.vue';

interface Props {
  /** Whether modal is open */
  isOpen: boolean;

  /** List of already-configured provider types */
  configuredProviders: IntegrationSchemaProvider[];
}

interface Emits {
  /** Emitted when modal should close */
  (event: 'close'): void;

  /** Emitted when user submits valid integration data */
  (event: 'create', data: {
    provider: IntegrationSchemaProvider;
    usage: IntegrationSchemaUsage;
    configuration: Record<string, unknown>;
  }): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

// Form state
const selectedProvider = ref<IntegrationSchemaProvider | null>(null);
const selectedUsage = ref<IntegrationSchemaUsage | null>(null);
const configuration = ref<Record<string, unknown>>({});
const formValid = ref(false);
const isSubmitting = ref(false);
const error = ref<string | null>(null);

/**
 * Available providers (excluding configured ones)
 */
const availableProviders = computed(() => {
  return ALL_PROVIDERS.filter(
    provider => !props.configuredProviders.includes(provider)
  );
});

/**
 * Provider options for dropdown
 */
const providerOptions = computed(() => {
  return availableProviders.value.map(provider => ({
    value: provider,
    label: PROVIDERS[provider].name
  }));
});

/**
 * Available usages for selected provider
 */
const availableUsages = computed<IntegrationSchemaUsage[]>(() => {
  if (!selectedProvider.value) return [];
  return PROVIDERS[selectedProvider.value].supportedUsages;
});

/**
 * Usage options for dropdown
 */
const usageOptions = computed(() => {
  return availableUsages.value.map(usage => ({
    value: usage,
    label: USAGES[usage].name
  }));
});

/**
 * Dynamic form component based on selected provider
 */
const providerFormComponent = computed(() => {
  if (!selectedProvider.value) return null;

  const componentMap: Record<IntegrationSchemaProvider, any> = {
    qonto: QontoForm,
    mailjet: MailjetForm,
    billetweb: BilletwebForm,
    openplanner: OpenPlannerForm,
    slack: SlackForm,
    webhook: WebhookForm
  };

  return componentMap[selectedProvider.value];
});

/**
 * Whether form can be submitted
 */
const canSubmit = computed(() => {
  return selectedProvider.value &&
         selectedUsage.value &&
         formValid.value &&
         !isSubmitting.value;
});

/**
 * Get provider display name
 */
function getProviderName(provider: IntegrationSchemaProvider): string {
  return PROVIDERS[provider].name;
}

/**
 * Handle provider selection change
 */
watch(selectedProvider, (newProvider, oldProvider) => {
  if (newProvider !== oldProvider) {
    // Reset usage and configuration when provider changes
    selectedUsage.value = null;
    configuration.value = {};
    error.value = null;

    // Auto-select usage if provider has only one supported usage
    if (newProvider && availableUsages.value.length === 1) {
      selectedUsage.value = availableUsages.value[0];
    }
  }
});

/**
 * Handle form submission
 */
async function handleSubmit() {
  error.value = null;

  // Validation
  if (!selectedProvider.value) {
    error.value = 'Veuillez sélectionner un fournisseur';
    return;
  }

  if (!selectedUsage.value) {
    error.value = 'Veuillez sélectionner une utilisation';
    return;
  }

  if (!formValid.value) {
    error.value = 'Veuillez remplir tous les champs obligatoires';
    return;
  }

  // Emit create event
  emit('create', {
    provider: selectedProvider.value,
    usage: selectedUsage.value,
    configuration: { ...configuration.value }
  });
}

/**
 * Handle modal close
 */
function handleClose() {
  if (isSubmitting.value) return;
  emit('close');
}

/**
 * Reset form when modal opens
 */
watch(() => props.isOpen, (isOpen) => {
  if (isOpen) {
    selectedProvider.value = null;
    selectedUsage.value = null;
    configuration.value = {};
    formValid.value = false;
    error.value = null;
  }
});

/**
 * Expose isSubmitting for parent component
 */
defineExpose({ isSubmitting });
</script>
