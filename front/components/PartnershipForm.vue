<template>
  <form @submit.prevent="onSubmit" class="space-y-6">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Nom du contact<span class="text-red-500 ml-1">*</span>
        </label>
        <UInput v-model="form.contact_name" placeholder="Nom du contact" required class="w-full" />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Rôle du contact<span class="text-red-500 ml-1">*</span>
        </label>
        <UInput v-model="form.contact_role" placeholder="Rôle" required class="w-full" />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2"> Pack </label>
        <UInput
          v-model="selectedPackName"
          placeholder="Pack de sponsoring"
          disabled
          class="w-full"
        />
      </div>

      <div class="md:col-span-2">
        <label class="block text-sm font-medium text-gray-700 mb-2"> Options </label>

        <!-- Options incluses dans le pack -->
        <div
          v-if="requiredOptions.length > 0"
          class="bg-blue-50 rounded-lg p-3 mb-3"
          role="region"
          aria-labelledby="required-options-heading"
        >
          <h4
            id="required-options-heading"
            class="text-sm font-semibold text-blue-900 uppercase mb-2"
          >
            <i class="i-heroicons-check-circle mr-1" aria-hidden="true"></i>
            Options incluses dans le pack
          </h4>
          <div class="space-y-3" role="list">
            <div
              v-for="option in requiredOptions"
              :key="option.id"
              class="flex items-start gap-3"
              role="listitem"
            >
              <input
                type="checkbox"
                :id="`form-required-option-${option.id}`"
                checked
                disabled
                tabindex="-1"
                :aria-label="`Option incluse : ${option.name}`"
                class="mt-1 h-4 w-4 rounded border-blue-300 text-blue-600 cursor-not-allowed opacity-80"
              />
              <label :for="`form-required-option-${option.id}`" class="flex-1 cursor-not-allowed">
                <span class="block text-sm font-medium text-gray-900">
                  {{ option.name }}
                  <span
                    v-if="option.price !== null && option.price !== undefined"
                    class="text-gray-600"
                  >
                    ({{ option.price }} €)
                  </span>
                </span>
                <span
                  v-if="option.description"
                  class="block text-sm text-gray-500 mt-1"
                  :class="{ 'line-clamp-2': isDescriptionLong(option.description) }"
                  :title="isDescriptionLong(option.description) ? option.description : undefined"
                >
                  {{ option.description }}
                </span>
              </label>
            </div>
          </div>
        </div>

        <!-- Options optionnelles sélectionnées -->
        <div
          v-if="selectedOptions.length > 0"
          class="bg-gray-50 rounded-lg p-3"
          role="region"
          aria-labelledby="optional-options-heading"
        >
          <h4
            id="optional-options-heading"
            class="text-sm font-semibold text-gray-700 uppercase mb-2"
          >
            <i class="i-heroicons-plus-circle mr-1" aria-hidden="true"></i>
            Options optionnelles sélectionnées
          </h4>
          <div class="space-y-3" role="list">
            <div
              v-for="option in selectedOptions"
              :key="option.id"
              class="flex items-start gap-3"
              role="listitem"
            >
              <input
                type="checkbox"
                :id="`form-option-${option.id}`"
                checked
                disabled
                tabindex="-1"
                :aria-label="`Option optionnelle : ${option.name}`"
                class="mt-1 h-4 w-4 rounded border-gray-300 text-primary-600 cursor-not-allowed opacity-60"
              />
              <label :for="`form-option-${option.id}`" class="flex-1 cursor-not-allowed">
                <span class="block text-sm font-medium text-gray-900">
                  {{ option.name }}
                  <!-- Pour les options quantitatives: afficher "Quantité x Montant Unitaire" -->
                  <span
                    v-if="option.type === 'typed_quantitative' && option.quantity && option.price !== null && option.price !== undefined"
                    class="text-gray-600"
                  >
                    ({{ option.quantity }} x {{ option.price }} €)
                  </span>
                  <!-- Pour les options sélectables: afficher le texte et le prix du choix fait -->
                  <span
                    v-else-if="option.type === 'typed_selectable' && option.selected_value"
                    class="text-gray-600"
                  >
                    ({{ option.selected_value.value }} - {{ option.selected_value.price }} €)
                  </span>
                  <!-- Pour les autres options: afficher juste le prix -->
                  <span
                    v-else-if="option.price !== null && option.price !== undefined"
                    class="text-gray-600"
                  >
                    ({{ option.price }} €)
                  </span>
                </span>
                <span
                  v-if="option.description"
                  class="block text-sm text-gray-500 mt-1"
                  :class="{ 'line-clamp-2': isDescriptionLong(option.description) }"
                  :title="isDescriptionLong(option.description) ? option.description : undefined"
                >
                  {{ option.description }}
                </span>
              </label>
            </div>
          </div>
        </div>

        <p
          v-if="requiredOptions.length === 0 && selectedOptions.length === 0"
          class="text-sm text-gray-500 italic"
          role="status"
          aria-live="polite"
        >
          Aucune option
        </p>
      </div>

      <div>
        <LanguageSelect v-model="form.language" label="Langue" :required="true" />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Email(s)<span class="text-red-500 ml-1">*</span>
        </label>
        <UInput v-model="form.emails" placeholder="email@example.com" type="email" class="w-full" />
        <p class="text-xs text-gray-500 mt-1">Séparer plusieurs emails par des virgules</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Téléphone<span class="text-red-500 ml-1">*</span>
        </label>
        <UInput v-model="form.phone" placeholder="+33 6 12 34 56 78" type="tel" class="w-full" />
      </div>
    </div>

    <div class="flex justify-end gap-3 pt-4 ">
      <UButton
        type="button"
        color="neutral"
        variant="ghost"
        label="Annuler"
        @click="$emit('cancel')"
      />
      <UButton type="submit" color="primary" label="Enregistrer" :loading="loading" />
    </div>
  </form>
</template>

<script setup lang="ts">
import type { ExtendedPartnershipItem } from "~/types/partnership";
import { getEventsSponsoringPacks, type SponsoringPack } from "~/utils/api";

interface Props {
  partnership?: ExtendedPartnershipItem | null;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
});

const emit = defineEmits<{
  save: [data: any];
  cancel: [];
}>();

const route = useRoute();
const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  return Array.isArray(params) ? params[1] as string : params as string;
});

const packs = ref<SponsoringPack[]>([]);
const selectedPackName = ref('');

// Options incluses dans le pack (required_options)
const requiredOptions = ref<Array<{
  id: string;
  name: string;
  description?: string | null;
  price?: number | null;
}>>([]);

// Options optionnelles sélectionnées (optional_options)
const selectedOptions = ref<Array<{
  id: string;
  name: string;
  description?: string | null;
  price?: number | null;
  type?: string;
  quantity?: number | null;
  total_price?: number | null;
  selected_value?: { id: string; value: string; price: number } | null;
}>>([]);

// Charger les packs disponibles
async function loadPacks() {
  try {
    const response = await getEventsSponsoringPacks(eventSlug.value);
    packs.value = response.data;
    updateSelectedPackName();
    updateRequiredOptions();
    updateSelectedOptions();
  } catch (error) {
    console.error('Failed to load packs:', error);
  }
}

// Mettre à jour le nom du pack sélectionné
function updateSelectedPackName() {
  if (props.partnership?.selected_pack_id) {
    const pack = packs.value.find(p => p.id === props.partnership?.selected_pack_id);
    selectedPackName.value = pack?.name || '';
  } else if (props.partnership?.suggested_pack_name) {
    selectedPackName.value = `${props.partnership.suggested_pack_name} (suggéré)`;
  } else {
    selectedPackName.value = '';
  }
}

// Vérifier si une description est longue (> 100 caractères)
// Pour afficher un tooltip et tronquer le texte
function isDescriptionLong(description: string | null | undefined): boolean {
  if (!description) return false;
  return description.length > 100;
}

// Mettre à jour les options incluses dans le pack (required_options)
function updateRequiredOptions() {
  requiredOptions.value = [];

  // Trouver le pack pour accéder à ses options incluses
  const packId = props.partnership?.selected_pack_id || props.partnership?.suggested_pack_id;
  if (!packId) return;

  const pack = packs.value.find(p => p.id === packId);
  if (!pack || !pack.required_options || pack.required_options.length === 0) return;

  // Mapper les options incluses
  requiredOptions.value = pack.required_options.map((opt) => ({
    id: opt.id,
    name: opt.name,
    description: opt.description || null,
    price: opt.price ?? null
  }));
}

// Mettre à jour les options sélectionnées
function updateSelectedOptions() {
  selectedOptions.value = [];

  // Si pack_options est fourni directement (nouveau format depuis getEventsPartnershipDetailed),
  // on l'utilise directement avec toutes les informations
  if (props.partnership?.pack_options && props.partnership.pack_options.length > 0) {
    selectedOptions.value = props.partnership.pack_options.map((opt: any) => ({
      id: opt.id,
      name: opt.name,
      description: opt.description || null,
      price: opt.price ?? null,
      type: opt.type,
      quantity: opt.quantity ?? null,
      total_price: opt.total_price ?? null,
      selected_value: opt.selected_value ?? null
    }));
    return;
  }

  // Sinon, on garde l'ancienne méthode de chargement via l'API getEventsSponsoringPacks
  if (!props.partnership?.option_ids || props.partnership.option_ids.length === 0) {
    return;
  }

  // Trouver le pack pour accéder à ses options
  const packId = props.partnership.selected_pack_id || props.partnership.suggested_pack_id;
  if (!packId) return;

  const pack = packs.value.find(p => p.id === packId);
  if (!pack) return;

  // Gérer les deux formats possibles: "optional_options" (schéma) ou "options" (API)
  const packOptions = (pack as any).options || pack.optional_options || [];
  if (packOptions.length === 0) return;

  // Filtrer les options qui sont dans option_ids
  selectedOptions.value = packOptions
    .filter((opt: any) => props.partnership?.option_ids?.includes(opt.id))
    .map((opt: any) => ({
      id: opt.id,
      name: opt.name,
      description: opt.description || null,
      price: opt.price ?? null,
      type: opt.type,
      quantity: opt.quantity ?? null,
      total_price: opt.total_price ?? null,
      selected_value: opt.selected_value ?? null
    }));
}

const form = ref({
  contact_name: props.partnership?.contact?.display_name || '',
  contact_role: props.partnership?.contact?.role || '',
  language: props.partnership?.language || 'fr',
  emails: props.partnership?.emails || '',
  phone: props.partnership?.phone || ''
});

// Mettre à jour le formulaire si les props changent
watch(() => props.partnership, (newPartnership) => {
  if (newPartnership) {
    form.value = {
      contact_name: newPartnership.contact?.display_name || '',
      contact_role: newPartnership.contact?.role || '',
      language: newPartnership.language || 'fr',
      emails: newPartnership.emails || '',
      phone: newPartnership.phone || ''
    };
    updateSelectedPackName();
    updateRequiredOptions();
    updateSelectedOptions();
  }
}, { deep: true });

onMounted(() => {
  loadPacks();
});

function onSubmit() {
  emit('save', {
    contact_name: form.value.contact_name,
    contact_role: form.value.contact_role,
    language: form.value.language,
    emails: form.value.emails ? form.value.emails.split(',').map(e => e.trim()) : [],
    phone: form.value.phone || null
  });
}
</script>
