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
        :aria-describedby="messageId"
        aria-modal="true"
        class="w-full max-w-lg bg-white dark:bg-gray-800 rounded-lg shadow-xl"
        @click.stop
      >
        <!-- Header -->
        <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <div class="flex items-center gap-3">
            <!-- Icon -->
            <div
              v-if="icon"
              class="flex-shrink-0 flex items-center justify-center w-10 h-10 rounded-full"
              :class="iconBgClass"
            >
              <i :class="[icon, iconColorClass]" class="text-xl" />
            </div>

            <h3
              :id="titleId"
              class="text-lg font-semibold text-gray-900 dark:text-gray-100"
            >
              {{ title }}
            </h3>
          </div>
        </div>

        <!-- Content -->
        <div class="px-6 py-4">
          <p :id="messageId" class="text-gray-700 dark:text-gray-300" v-html="message" />
          <p v-if="showIrreversibleWarning" class="text-sm text-gray-600 dark:text-gray-400 mt-3">
            Cette action est irréversible.
          </p>
        </div>

        <!-- Footer -->
        <div class="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
          <UButton
            color="neutral"
            variant="ghost"
            :disabled="loading"
            @click="handleCancel"
          >
            {{ cancelText }}
          </UButton>
          <UButton
            :color="variant === 'danger' ? 'error' : 'primary'"
            :loading="loading"
            :disabled="confirmDisabled"
            @click="handleConfirm"
          >
            {{ confirmText }}
          </UButton>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import type { ConfirmModalProps } from '~/types/modal';

const props = withDefaults(defineProps<ConfirmModalProps>(), {
  confirmText: 'Confirmer',
  cancelText: 'Annuler',
  variant: 'info',
  loading: false,
  confirmDisabled: false,
  icon: '',
});

const emit = defineEmits<{
  confirm: [];
  cancel: [];
  close: [];
  'update:isOpen': [value: boolean];
}>();

// IDs uniques pour l'accessibilité
const titleId = `confirm-modal-title-${Math.random().toString(36).substr(2, 9)}`;
const messageId = `confirm-modal-message-${Math.random().toString(36).substr(2, 9)}`;

// Afficher l'avertissement pour les actions irréversibles
const showIrreversibleWarning = computed(() =>
  props.variant === 'danger' || props.variant === 'warning'
);

// Classes pour l'icône selon la variante
const iconBgClass = computed(() => {
  switch (props.variant) {
    case 'danger':
      return 'bg-red-100 dark:bg-red-900/20';
    case 'warning':
      return 'bg-amber-100 dark:bg-amber-900/20';
    case 'info':
      return 'bg-blue-100 dark:bg-blue-900/20';
    default:
      return 'bg-gray-100 dark:bg-gray-700';
  }
});

const iconColorClass = computed(() => {
  switch (props.variant) {
    case 'danger':
      return 'text-red-600 dark:text-red-400';
    case 'warning':
      return 'text-amber-600 dark:text-amber-400';
    case 'info':
      return 'text-blue-600 dark:text-blue-400';
    default:
      return 'text-gray-600 dark:text-gray-400';
  }
});

function handleConfirm() {
  emit('confirm');
}

function handleCancel() {
  emit('cancel');
  emit('update:isOpen', false);
}

function handleBackdropClick() {
  if (!props.loading) {
    handleCancel();
  }
}

// Écouter les changements de isOpen pour émettre close
watch(() => props.isOpen, (newValue, oldValue) => {
  if (oldValue && !newValue) {
    emit('close');
  }
});
</script>
