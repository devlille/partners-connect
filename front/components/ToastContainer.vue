<template>
  <div
    aria-live="polite"
    aria-atomic="true"
    class="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-md"
  >
    <TransitionGroup name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        :class="[
          'flex items-start gap-3 p-4 rounded-lg shadow-lg border',
          toastClasses(toast.type)
        ]"
        role="alert"
      >
        <div class="flex-shrink-0">
          <!-- Success Icon -->
          <svg v-if="toast.type === ToastType.SUCCESS" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          <!-- Error Icon -->
          <svg v-else-if="toast.type === ToastType.ERROR" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
          <!-- Warning Icon -->
          <svg v-else-if="toast.type === ToastType.WARNING" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <!-- Info Icon -->
          <svg v-else class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div class="flex-1 text-sm">
          {{ toast.message }}
        </div>
        <button
          type="button"
          class="flex-shrink-0 text-gray-400 hover:text-gray-600 transition-colors"
          @click="removeToast(toast.id)"
          aria-label="Fermer la notification"
        >
          <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { ToastType, useCustomToast } from '~/composables/useCustomToast';

const { toasts, removeToast } = useCustomToast();

/**
 * Classes CSS en fonction du type de toast
 */
const toastClasses = (type: ToastType): string => {
  switch (type) {
    case ToastType.SUCCESS:
      return 'bg-green-50 border-green-200 text-green-800';
    case ToastType.ERROR:
      return 'bg-red-50 border-red-200 text-red-800';
    case ToastType.WARNING:
      return 'bg-yellow-50 border-yellow-200 text-yellow-800';
    case ToastType.INFO:
      return 'bg-blue-50 border-blue-200 text-blue-800';
    default:
      return 'bg-gray-50 border-gray-200 text-gray-800';
  }
};
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>
