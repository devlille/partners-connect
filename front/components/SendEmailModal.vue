<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="isOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="send-email-modal-title"
      >
        <!-- Overlay -->
        <div
          class="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
          @click="handleClose"
          aria-hidden="true"
        />

        <!-- Modal -->
        <div
          class="relative bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto transform transition-all"
        >
          <!-- Header -->
          <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <h3 id="send-email-modal-title" class="text-lg font-semibold text-gray-900">
              Envoyer un email aux sponsors
            </h3>
            <button
              type="button"
              aria-label="Fermer"
              class="text-gray-400 hover:text-gray-600 transition-colors"
              @click="handleClose"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>

          <!-- Content -->
          <div class="px-6 py-4">
            <form @submit.prevent="handleSubmit" class="space-y-6">
              <!-- Destinataires info -->
              <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div class="flex">
                  <i class="i-heroicons-information-circle text-blue-600 mr-3 mt-0.5 shrink-0" />
                  <div class="text-sm text-blue-800">
                    <p class="font-medium mb-1">Destinataires</p>
                    <p v-if="hasFilters">
                      L'email sera envoyé aux sponsors correspondant aux filtres actifs.
                    </p>
                    <p v-else>L'email sera envoyé à tous les sponsors de l'événement.</p>
                  </div>
                </div>
              </div>

              <!-- Sujet -->
              <div>
                <label for="email-subject" class="block text-sm font-medium text-gray-700 mb-2">
                  Sujet <span class="text-red-500">*</span>
                </label>
                <UInput
                  id="email-subject"
                  v-model="formData.subject"
                  placeholder="Sujet de l'email"
                  required
                  :disabled="sending"
                  class="w-full"
                />
                <p class="mt-1 text-xs text-gray-500">
                  Le sujet sera préfixé par le nom de l'événement.
                </p>
              </div>

              <!-- Corps du message -->
              <div>
                <label for="email-body" class="block text-sm font-medium text-gray-700 mb-2">
                  Message <span class="text-red-500">*</span>
                </label>
                <UTextarea
                  id="email-body"
                  v-model="formData.body"
                  placeholder="Contenu de l'email (HTML supporté)"
                  required
                  :disabled="sending"
                  :rows="8"
                  class="w-full"
                />
              </div>

              <!-- Erreur -->
              <div
                v-if="error"
                class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded"
              >
                {{ error }}
              </div>

              <!-- Succès -->
              <div
                v-if="successMessage"
                class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded"
              >
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
              label="Annuler"
              :disabled="sending"
              @click="handleClose"
            />
            <UButton
              type="button"
              color="primary"
              label="Envoyer"
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
import { postPartnershipEmail, type PostPartnershipEmailParams } from "~/utils/api";

interface Props {
  orgSlug: string;
  eventSlug: string;
  filterParams?: PostPartnershipEmailParams;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  close: [];
  sent: [recipientCount: number];
}>();

const isOpen = defineModel<boolean>({ default: false });

const formData = ref({
  subject: '',
  body: '',
});

const sending = ref(false);
const error = ref<string | null>(null);
const successMessage = ref<string | null>(null);

const isFormValid = computed(() => {
  return formData.value.subject.trim().length > 0 && formData.value.body.trim().length > 0;
});

const hasFilters = computed(() => {
  if (!props.filterParams) return false;
  return Object.values(props.filterParams).some(v => v !== undefined && v !== null);
});

watch(isOpen, (open) => {
  if (!open) {
    // Réinitialiser le formulaire quand on ferme
    formData.value = {
      subject: '',
      body: '',
    };
    error.value = null;
    successMessage.value = null;
  }
});

function handleClose() {
  if (sending.value) return;
  isOpen.value = false;
  emit('close');
}

async function handleSubmit() {
  if (!isFormValid.value || sending.value) return;

  sending.value = true;
  error.value = null;
  successMessage.value = null;

  try {
    const response = await postPartnershipEmail(
      props.orgSlug,
      props.eventSlug,
      {
        subject: formData.value.subject.trim(),
        body: formData.value.body.trim(),
      },
      props.filterParams
    );

    const recipientCount = response.data.recipient_count;
    successMessage.value = `Email envoyé avec succès à ${recipientCount} destinataire(s).`;
    emit('sent', recipientCount);

    // Fermer la modale après 2 secondes
    setTimeout(() => {
      handleClose();
    }, 2000);
  } catch (err: any) {
    console.error('Failed to send email:', err);
    error.value = err.response?.data?.message || 'Impossible d\'envoyer l\'email. Veuillez réessayer.';
  } finally {
    sending.value = false;
  }
}
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
