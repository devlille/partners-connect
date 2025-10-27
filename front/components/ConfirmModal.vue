<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="isOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="`confirm-modal-title-${id}`"
        :aria-describedby="`confirm-modal-description-${id}`"
      >
        <!-- Overlay -->
        <div
          class="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
          @click="handleCancel"
          aria-hidden="true"
        />

        <!-- Modal -->
        <div class="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6 transform transition-all">
          <!-- Icon -->
          <div v-if="type === 'danger'" class="flex items-center justify-center w-12 h-12 mx-auto mb-4 bg-red-100 rounded-full">
            <i class="i-heroicons-exclamation-triangle text-2xl text-red-600" aria-hidden="true" />
          </div>
          <div v-else-if="type === 'warning'" class="flex items-center justify-center w-12 h-12 mx-auto mb-4 bg-orange-100 rounded-full">
            <i class="i-heroicons-exclamation-circle text-2xl text-orange-600" aria-hidden="true" />
          </div>
          <div v-else class="flex items-center justify-center w-12 h-12 mx-auto mb-4 bg-blue-100 rounded-full">
            <i class="i-heroicons-information-circle text-2xl text-blue-600" aria-hidden="true" />
          </div>

          <!-- Title -->
          <h3
            :id="`confirm-modal-title-${id}`"
            class="text-lg font-semibold text-gray-900 text-center mb-2"
          >
            {{ title }}
          </h3>

          <!-- Message -->
          <p
            :id="`confirm-modal-description-${id}`"
            class="text-sm text-gray-600 text-center mb-6"
          >
            {{ message }}
          </p>

          <!-- Actions -->
          <div class="flex gap-3 justify-end">
            <UButton
              color="neutral"
              variant="ghost"
              @click="handleCancel"
              :aria-label="cancelLabel"
            >
              {{ cancelLabel }}
            </UButton>
            <UButton
              :color="type === 'danger' ? 'red' : 'primary'"
              @click="handleConfirm"
              :loading="confirming"
              :aria-label="confirmLabel"
            >
              {{ confirmLabel }}
            </UButton>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  /** Titre de la modale */
  title: string;
  /** Message de confirmation */
  message: string;
  /** Label du bouton de confirmation */
  confirmLabel?: string;
  /** Label du bouton d'annulation */
  cancelLabel?: string;
  /** Type de confirmation (danger, warning, info) */
  type?: 'danger' | 'warning' | 'info';
  /** État de chargement pendant la confirmation */
  confirming?: boolean;
}>(), {
  confirmLabel: 'Confirmer',
  cancelLabel: 'Annuler',
  type: 'info',
  confirming: false
});

const emit = defineEmits<{
  confirm: [];
  cancel: [];
}>();

const isOpen = defineModel<boolean>('modelValue', { default: false });

// ID unique pour l'accessibilité
const id = ref(`modal-${Math.random().toString(36).substr(2, 9)}`);

function handleConfirm() {
  emit('confirm');
}

function handleCancel() {
  if (props.confirming) return; // Ne pas fermer pendant le chargement
  isOpen.value = false;
  emit('cancel');
}

// Gérer la touche Escape
onMounted(() => {
  const handleEscape = (e: KeyboardEvent) => {
    if (e.key === 'Escape' && isOpen.value && !props.confirming) {
      handleCancel();
    }
  };

  window.addEventListener('keydown', handleEscape);

  onUnmounted(() => {
    window.removeEventListener('keydown', handleEscape);
  });
});
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .relative,
.modal-leave-active .relative {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
  opacity: 0;
}
</style>
