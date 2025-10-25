<template>
  <Teleport to="body">
    <div
      v-if="isOpen"
      class="fixed inset-0 z-[1040] flex items-center justify-center p-4 bg-black/50"
      @click.self="handleBackdropClick"
    >
      <div
        role="dialog"
        :aria-labelledby="titleId"
        aria-modal="true"
        class="w-full bg-white dark:bg-gray-800 rounded-lg shadow-xl transform transition-all"
        :class="[
          modalSizeClass,
          isMobile ? 'h-full max-h-screen rounded-none' : 'max-h-[90vh]'
        ]"
        @click.stop
      >
        <!-- Header -->
        <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <h2
            :id="titleId"
            class="text-lg font-semibold text-gray-900 dark:text-gray-100"
          >
            {{ title }}
          </h2>

          <button
            type="button"
            aria-label="Fermer"
            :disabled="loading && disableWhileLoading"
            class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors disabled:opacity-50"
            @click="handleClose"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- Content -->
        <div class="overflow-y-auto relative" :class="isMobile ? 'h-[calc(100vh-130px)]' : 'max-h-[calc(90vh-130px)]'">
          <!-- Loading overlay -->
          <div
            v-if="loading"
            class="absolute inset-0 bg-white/80 dark:bg-gray-800/80 flex items-center justify-center z-10"
            aria-label="Chargement"
            role="status"
          >
            <div class="flex flex-col items-center gap-2">
              <svg class="animate-spin h-8 w-8 text-primary-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span class="text-sm text-gray-600 dark:text-gray-400">Chargement...</span>
            </div>
          </div>

          <!-- Slot pour le contenu du formulaire -->
          <slot />
        </div>

        <!-- Footer (slot optionnel) -->
        <div
          v-if="$slots.footer"
          class="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3"
        >
          <slot name="footer" :loading="loading" :close="handleClose" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import type { FormModalProps } from '~/types/modal';
import { MODAL_SIZES, UI_CONSTANTS } from '~/constants/ui';

const props = withDefaults(defineProps<FormModalProps>(), {
  size: 'md',
  loading: false,
  disableWhileLoading: true,
  closeOnBackdropClick: true,
  closeOnEsc: true,
});

const emit = defineEmits<{
  close: [];
  'update:isOpen': [value: boolean];
}>();

// ID unique pour l'accessibilité
const titleId = `form-modal-title-${Math.random().toString(36).substr(2, 9)}`;

// Détection mobile
const isMobile = ref(false);

if (import.meta.client) {
  const updateMobile = () => {
    isMobile.value = window.innerWidth < UI_CONSTANTS.BREAKPOINTS.SM;
  };

  onMounted(() => {
    updateMobile();
    window.addEventListener('resize', updateMobile);
  });

  onUnmounted(() => {
    window.removeEventListener('resize', updateMobile);
  });
}

// Classes de taille du modal
const modalSizeClass = computed(() => MODAL_SIZES[props.size.toUpperCase() as keyof typeof MODAL_SIZES]);

function handleClose() {
  if (props.loading && props.disableWhileLoading) {
    return;
  }
  emit('close');
  emit('update:isOpen', false);
}

function handleBackdropClick() {
  if (props.closeOnBackdropClick && !props.loading) {
    handleClose();
  }
}

// Écouter la touche ESC
if (import.meta.client) {
  const handleEscapeKey = (event: KeyboardEvent) => {
    if (event.key === 'Escape' && props.isOpen && props.closeOnEsc && !props.loading) {
      handleClose();
    }
  };

  onMounted(() => {
    document.addEventListener('keydown', handleEscapeKey);
  });

  onUnmounted(() => {
    document.removeEventListener('keydown', handleEscapeKey);
  });
}
</script>
