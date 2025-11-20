<template>
  <Teleport to="body">
    <div
      v-if="isOpen && integration"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-modal-title"
      @click.self="handleClose"
      @keydown.esc="handleClose"
    >
      <div
        class="w-full max-w-md bg-white rounded-lg shadow-xl"
        @click.stop
      >
        <!-- Header -->
        <div class="px-6 py-4 border-b border-gray-200">
          <h3 id="delete-modal-title" class="text-lg font-semibold text-gray-900">Supprimer l'intégration</h3>
        </div>

        <!-- Body -->
        <div class="px-6 py-4">
          <p class="text-sm text-gray-700 mb-4">
            Voulez-vous vraiment supprimer l'intégration
            <span class="font-semibold">{{ getProviderName(integration.provider) }}</span>
            ({{ getUsageName(integration.usage) }}) ?
          </p>
          <p class="text-sm text-red-600 font-medium">
            Cette action est irréversible
          </p>
        </div>

        <!-- Footer -->
        <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
          <UButton
            color="neutral"
            variant="ghost"
            :disabled="deleting"
            @click="handleClose"
          >
            Annuler
          </UButton>
          <UButton
            color="error"
            :loading="deleting"
            :disabled="deleting"
            @click="handleConfirm"
          >
            Supprimer
          </UButton>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import type { IntegrationSchema } from '~/utils/api';
import { PROVIDERS } from '~/utils/integrationProviders';
import { USAGES } from '~/utils/integrationUsages';

interface Props {
  /** Whether modal is open */
  isOpen: boolean;

  /** Integration to delete */
  integration: IntegrationSchema | null;

  /** Whether deletion is in progress */
  deleting: boolean;
}

interface Emits {
  /** Emitted when modal should close */
  (event: 'close'): void;

  /** Emitted when user confirms deletion */
  (event: 'confirm'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

/**
 * Get provider display name
 */
function getProviderName(provider: string): string {
  return PROVIDERS[provider as keyof typeof PROVIDERS]?.name || provider;
}

/**
 * Get usage display name
 */
function getUsageName(usage: string): string {
  return USAGES[usage as keyof typeof USAGES]?.name || usage;
}

/**
 * Handle modal close
 * Prevent closing during deletion
 */
function handleClose() {
  if (props.deleting) return;
  emit('close');
}

/**
 * Handle confirm deletion
 */
function handleConfirm() {
  emit('confirm');
}
</script>
