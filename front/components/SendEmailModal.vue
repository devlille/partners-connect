<template>
  <Teleport to="body">
    <Transition name="modal" @after-enter="onModalOpened" @after-leave="onModalClosed">
      <div
        v-if="isOpen"
        ref="modalContainerRef"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="send-email-modal-title"
        aria-describedby="send-email-modal-description"
        @keydown="handleKeydown"
      >
        <!-- Overlay -->
        <div
          class="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
          @click="handleClose"
          aria-hidden="true"
        />

        <!-- Modal -->
        <div
          ref="modalContentRef"
          class="relative bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto transform transition-all"
          tabindex="-1"
        >
          <!-- Header -->
          <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <h3 id="send-email-modal-title" class="text-lg font-semibold text-gray-900">
              {{ $t('email.modal.title') }}
            </h3>
            <button
              ref="closeButtonRef"
              type="button"
              :aria-label="$t('common.close')"
              class="text-gray-400 hover:text-gray-600 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 rounded-md p-1 transition-colors"
              @click="handleClose"
            >
              <svg
                class="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>

          <!-- Description pour les lecteurs d'écran -->
          <p id="send-email-modal-description" class="sr-only">
            {{ $t('email.modal.recipientsAll') }}
          </p>

          <!-- Content -->
          <div class="px-6 py-4">
            <form ref="formRef" @submit.prevent="handleSubmit" class="space-y-6">
              <!-- Destinataires info -->
              <div
                class="bg-blue-50 border border-blue-200 rounded-lg p-4"
                role="region"
                aria-label="Informations destinataires"
              >
                <div class="flex">
                  <UIcon
                    name="i-heroicons-information-circle"
                    class="text-blue-600 mr-3 mt-0.5 shrink-0 w-5 h-5"
                  />
                  <div class="text-sm text-blue-800 flex-1">
                    <p class="font-medium mb-1">
                      {{ $t('email.modal.recipients') }}
                      <span v-if="recipientEmails?.length" class="text-blue-600">
                        ({{ recipientEmails.length }})
                      </span>
                    </p>
                    <p v-if="hasFilters" class="mb-2">
                      {{ $t('email.modal.recipientsWithFilters') }}
                    </p>
                    <p v-else class="mb-2">{{ $t('email.modal.recipientsAll') }}</p>

                    <!-- Liste des emails -->
                    <div v-if="recipientEmails?.length" class="mt-2">
                      <div class="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto">
                        <span
                          v-for="email in recipientEmails"
                          :key="email"
                          class="inline-flex items-center px-2 py-0.5 rounded-full text-xs bg-blue-100 text-blue-700"
                        >
                          {{ email }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Sujet -->
              <div>
                <label for="email-subject" class="block text-sm font-medium text-gray-700 mb-2">
                  {{ $t('email.modal.subject') }}
                  <span class="text-red-500">{{ $t('email.modal.required') }}</span>
                </label>
                <UInput
                  id="email-subject"
                  v-model="formData.subject"
                  :placeholder="$t('email.modal.subjectPlaceholder')"
                  required
                  :disabled="sending"
                  class="w-full"
                />
                <p class="mt-1 text-xs text-gray-500">
                  {{ $t('email.modal.subjectHint') }}
                </p>
              </div>

              <!-- Corps du message -->
              <div>
                <label for="email-body" class="block text-sm font-medium text-gray-700 mb-2">
                  {{ $t('email.modal.body') }}
                  <span class="text-red-500">{{ $t('email.modal.required') }}</span>
                </label>
                <Suspense>
                  <LazyWysiwygEditor
                    v-model="formData.body"
                    :placeholder="$t('email.modal.bodyPlaceholder')"
                    :disabled="sending"
                  />
                  <template #fallback>
                    <div class="wysiwyg-skeleton">
                      <div class="wysiwyg-skeleton-toolbar" />
                      <div class="wysiwyg-skeleton-content" />
                    </div>
                  </template>
                </Suspense>
              </div>

              <!-- Erreur -->
              <div
                v-if="error"
                role="alert"
                aria-live="assertive"
                class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded"
              >
                <span class="sr-only">{{ $t('common.error') }}: </span>
                {{ error }}
              </div>

              <!-- Succès -->
              <div
                v-if="successMessage"
                role="status"
                aria-live="polite"
                class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded"
              >
                <span class="sr-only">{{ $t('common.success') }}: </span>
                {{ successMessage }}
              </div>
            </form>
          </div>

          <!-- Footer -->
          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              type="button"
              color="neutral"
              variant="ghost"
              :label="$t('common.cancel')"
              :disabled="sending"
              @click="handleClose"
            />
            <UButton
              type="button"
              color="primary"
              :label="$t('email.modal.send')"
              icon="i-heroicons-paper-airplane"
              :loading="sending"
              :disabled="!isFormValid"
              @click="handleSubmit"
            />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import type { PostPartnershipEmailParams } from "~/utils/api";
import { useSendEmail } from "~/composables/useSendEmail";

// Lazy load du WYSIWYG editor pour réduire la taille du bundle initial
const LazyWysiwygEditor = defineAsyncComponent({
  loader: () => import('~/components/WysiwygEditor.vue'),
  delay: 200,
});

interface Props {
  orgSlug: string;
  eventSlug: string;
  filterParams?: PostPartnershipEmailParams;
  /** Liste des emails des destinataires filtrés */
  recipientEmails?: string[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  close: [];
  sent: [recipientCount: number];
}>();

const isOpen = defineModel<boolean>({ default: false });

// Refs pour la gestion du focus
const modalContainerRef = ref<HTMLElement | null>(null);
const modalContentRef = ref<HTMLElement | null>(null);
const closeButtonRef = ref<HTMLButtonElement | null>(null);
const formRef = ref<HTMLFormElement | null>(null);
const previouslyFocusedElement = ref<HTMLElement | null>(null);

const {
  formData,
  sending,
  error,
  successMessage,
  isFormValid,
  hasFilters,
  resetForm,
  sendEmail,
} = useSendEmail({
  orgSlug: props.orgSlug,
  eventSlug: props.eventSlug,
  filterParams: props.filterParams,
  onSuccess: (recipientCount) => {
    emit('sent', recipientCount);
    // Fermer la modale après 2 secondes
    setTimeout(() => {
      handleClose();
    }, 2000);
  },
});

// Sélecteur pour les éléments focusables
const FOCUSABLE_SELECTOR = 'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"]):not([disabled])';

/**
 * Récupère tous les éléments focusables dans la modale
 */
function getFocusableElements(): HTMLElement[] {
  if (!modalContentRef.value) return [];
  return Array.from(modalContentRef.value.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR));
}

/**
 * Gère le focus trap - garde le focus dans la modale
 */
function handleKeydown(event: KeyboardEvent) {
  // Fermer avec Escape
  if (event.key === 'Escape' && !sending.value) {
    event.preventDefault();
    handleClose();
    return;
  }

  // Focus trap avec Tab
  if (event.key === 'Tab') {
    const focusableElements = getFocusableElements();
    if (focusableElements.length === 0) return;

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    // Shift + Tab sur le premier élément -> aller au dernier
    if (event.shiftKey && document.activeElement === firstElement) {
      event.preventDefault();
      lastElement.focus();
    }
    // Tab sur le dernier élément -> aller au premier
    else if (!event.shiftKey && document.activeElement === lastElement) {
      event.preventDefault();
      firstElement.focus();
    }
  }
}

/**
 * Appelé après l'ouverture de la modale
 */
function onModalOpened() {
  // Sauvegarder l'élément qui avait le focus
  previouslyFocusedElement.value = document.activeElement as HTMLElement;

  // Focus sur le premier champ du formulaire après un court délai
  // pour laisser le temps au composant lazy de se charger
  nextTick(() => {
    const focusableElements = getFocusableElements();
    // Chercher le premier input ou le bouton fermer
    const firstInput = focusableElements.find(el => el.tagName === 'INPUT');
    if (firstInput) {
      firstInput.focus();
    } else if (closeButtonRef.value) {
      closeButtonRef.value.focus();
    }
  });

  // Empêcher le scroll du body
  document.body.style.overflow = 'hidden';
}

/**
 * Appelé après la fermeture de la modale
 */
function onModalClosed() {
  // Restaurer le focus sur l'élément précédent
  if (previouslyFocusedElement.value && previouslyFocusedElement.value.focus) {
    previouslyFocusedElement.value.focus();
  }
  previouslyFocusedElement.value = null;

  // Restaurer le scroll du body
  document.body.style.overflow = '';
}

watch(isOpen, (open) => {
  if (!open) {
    resetForm();
  }
});

function handleClose() {
  if (sending.value) return;
  isOpen.value = false;
  emit('close');
}

async function handleSubmit() {
  await sendEmail();
}

// Nettoyage au démontage du composant
onUnmounted(() => {
  document.body.style.overflow = '';
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

/* Skeleton loader pour le WYSIWYG editor */
.wysiwyg-skeleton {
  border: 1px solid #d1d5db;
  border-radius: 0.5rem;
  overflow: hidden;
}

.wysiwyg-skeleton-toolbar {
  height: 3rem;
  background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

.wysiwyg-skeleton-content {
  height: 12rem;
  background: linear-gradient(90deg, #f9fafb 25%, #f3f4f6 50%, #f9fafb 75%);
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

@keyframes skeleton-pulse {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
