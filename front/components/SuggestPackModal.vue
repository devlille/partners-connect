<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="isOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
      >
        <!-- Overlay -->
        <div
          class="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
          @click="handleClose"
          aria-hidden="true"
        />

        <!-- Modal -->
        <div class="relative bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto transform transition-all">
          <!-- Header -->
          <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <h3 class="text-lg font-semibold text-gray-900">
              {{ title }}
            </h3>
            <button
              type="button"
              aria-label="Fermer"
              class="text-gray-400 hover:text-gray-600 transition-colors"
              @click="handleClose"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Content -->
          <div class="px-6 py-4">
            <form @submit.prevent="handleSubmit" class="space-y-6">
              <!-- Sélection du pack -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                  Pack de sponsoring *
                </label>
                <select
                  v-model="formData.packId"
                  required
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                >
                  <option value="">Sélectionner un pack</option>
                  <option v-for="pack in packs" :key="pack.id" :value="pack.id">
                    {{ pack.name }} - {{ pack.base_price }} €
                  </option>
                </select>
              </div>

              <!-- Options de sponsoring -->
              <div v-if="selectedPackOptions.length > 0">
                <label class="block text-sm font-medium text-gray-700 mb-3">
                  Options de sponsoring
                </label>
                <div class="space-y-3">
                  <div
                    v-for="option in selectedPackOptions"
                    :key="option.id"
                    class="flex items-start"
                  >
                    <input
                      :id="`option-${option.id}`"
                      v-model="formData.optionIds"
                      type="checkbox"
                      :value="option.id"
                      class="mt-1 h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                    />
                    <label
                      :for="`option-${option.id}`"
                      class="ml-3 flex-1"
                    >
                      <span class="block text-sm font-medium text-gray-900">
                        {{ option.name }}
                      </span>
                      <span v-if="option.price" class="block text-sm text-gray-500">
                        {{ option.price }} €
                      </span>
                    </label>
                  </div>
                </div>
              </div>

              <!-- Langue -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                  Langue de communication *
                </label>
                <select
                  v-model="formData.language"
                  required
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                >
                  <option value="fr">Français</option>
                  <option value="en">English</option>
                </select>
              </div>

              <!-- Message informatif -->
              <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div class="flex">
                  <i class="i-heroicons-information-circle text-blue-600 mr-3 mt-0.5 shrink-0" />
                  <div class="text-sm text-blue-800">
                    <p class="font-medium mb-1">Proposition de pack alternatif</p>
                    <p>
                      Une suggestion de pack sera envoyée au sponsor. Il devra approuver cette suggestion avant que le partenariat puisse être validé.
                    </p>
                  </div>
                </div>
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
              :disabled="loading"
              @click="handleClose"
            />
            <UButton
              type="button"
              color="primary"
              label="Proposer ce pack"
              :loading="loading"
              @click="handleSubmit"
            />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { getEventsSponsoringPacks, type SponsoringPack } from "~/utils/api";

interface Props {
  eventSlug: string;
  currentPackId?: string;
  currentLanguage?: string;
  title?: string;
}

const props = withDefaults(defineProps<Props>(), {
  title: 'Proposer un autre pack',
  currentLanguage: 'fr'
});

const emit = defineEmits<{
  close: [];
  submit: [data: { packId: string; language: string; optionIds: string[] }];
}>();

// Utiliser defineModel comme ConfirmModal
const isOpen = defineModel<boolean>({ default: false });

const packs = ref<SponsoringPack[]>([]);
const loading = ref(false);
const formData = ref({
  packId: '',
  language: props.currentLanguage,
  optionIds: [] as string[],
});

// Options du pack sélectionné
const selectedPackOptions = computed(() => {
  if (!formData.value.packId) return [];
  const selectedPack = packs.value.find(pack => pack.id === formData.value.packId);
  return selectedPack?.optional_options || [];
});

// Charger les packs quand la modale s'ouvre
watch(isOpen, async (open) => {
  if (open) {
    await loadPacks();
  } else {
    // Réinitialiser le formulaire quand on ferme
    formData.value = {
      packId: '',
      language: props.currentLanguage,
      optionIds: [],
    };
  }
});

// Réinitialiser les options quand on change de pack
watch(() => formData.value.packId, () => {
  formData.value.optionIds = [];
});

async function loadPacks() {
  try {
    loading.value = true;
    const response = await getEventsSponsoringPacks(props.eventSlug);
    packs.value = response.data;
  } catch (error) {
    console.error('Failed to load sponsoring packs:', error);
  } finally {
    loading.value = false;
  }
}

function handleClose() {
  if (loading.value) return;
  isOpen.value = false;
  emit('close');
}

async function handleSubmit() {
  if (!formData.value.packId) {
    return;
  }

  emit('submit', {
    packId: formData.value.packId,
    language: formData.value.language,
    optionIds: formData.value.optionIds,
  });
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
