<template>
  <form @submit.prevent="onSubmit" class="space-y-6">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Nom du contact
        </label>
        <UInput
          v-model="form.contact_name"
          placeholder="Nom du contact"
          required
          :disabled="readonly"
          class="w-full"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Rôle du contact
        </label>
        <UInput
          v-model="form.contact_role"
          placeholder="Rôle"
          required
          :disabled="readonly"
          class="w-full"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Pack
        </label>
        <UInput
          v-model="selectedPackDisplay"
          placeholder="Pack de sponsoring"
          disabled
          class="w-full"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Options
        </label>
        <div v-if="selectedOptions.length > 0" class="bg-gray-50 rounded-lg p-3">
          <div class="space-y-3">
            <div
              v-for="option in selectedOptions"
              :key="option.id"
              class="flex items-start gap-3"
            >
              <input
                type="checkbox"
                :id="`form-option-${option.id}`"
                checked
                disabled
                class="mt-1 h-4 w-4 rounded border-gray-300 text-primary-600 cursor-not-allowed opacity-60"
              >
              <label
                :for="`form-option-${option.id}`"
                class="flex-1 cursor-not-allowed"
              >
                <span class="block text-sm font-medium text-gray-900">
                  {{ option.name }}
                  <!-- Pour les options quantitatives: afficher "Quantité x Montant Unitaire" -->
                  <span v-if="option.type === 'typed_quantitative' && option.quantity && option.price !== null && option.price !== undefined" class="text-gray-600">
                    ({{ option.quantity }} x {{ option.price }} €)
                  </span>
                  <!-- Pour les options sélectables: afficher le texte et le prix du choix fait -->
                  <span v-else-if="option.type === 'typed_selectable' && option.selected_value" class="text-gray-600">
                    ({{ option.selected_value.value }} - {{ option.selected_value.price }} €)
                  </span>
                  <!-- Pour les autres options: afficher juste le prix -->
                  <span v-else-if="option.price !== null && option.price !== undefined" class="text-gray-600">
                    ({{ option.price }} €)
                  </span>
                </span>
                <span v-if="option.description" class="block text-sm text-gray-500 mt-1">
                  {{ option.description }}
                </span>
              </label>
            </div>
          </div>
        </div>
        <p v-else class="text-sm text-gray-500 italic">Aucune option sélectionnée</p>
      </div>

      <div>
        <LanguageSelect
          v-model="form.language"
          label="Langue"
          :disabled="readonly"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Email(s)
        </label>
        <UInput
          v-model="form.emails"
          placeholder="email@example.com"
          type="email"
          :disabled="readonly"
          class="w-full"
        />
        <p class="text-xs text-gray-500 mt-1">Séparer plusieurs emails par des virgules</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Téléphone
        </label>
        <UInput
          v-model="form.phone"
          placeholder="+33 6 12 34 56 78"
          type="tel"
          :disabled="readonly"
          class="w-full"
        />
      </div>
    </div>

    <div v-if="!readonly" class="flex justify-end gap-3 pt-4 ">
      <UButton
        type="button"
        color="neutral"
        variant="ghost"
        label="Annuler"
        @click="$emit('cancel')"
      />
      <UButton
        type="submit"
        color="primary"
        label="Enregistrer"
        :loading="loading"
      />
    </div>
  </form>
</template>

<script setup lang="ts">
import type { ExtendedPartnershipItem } from "~/types/partnership";
import { getEventsSponsoringPacks, type SponsoringPack } from "~/utils/api";

interface Props {
  partnership?: ExtendedPartnershipItem | null;
  loading?: boolean;
  showAdminActions?: boolean;
  readonly?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  showAdminActions: true,
  readonly: false
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
const selectedPackPrice = ref<number | null>(null);
const selectedOptions = ref<Array<{
  id: string;
  name: string;
  description?: string | null;
  price?: number | null;
  type?: string;
  quantity?: number | null;
  total_price?: number | null;
  selected_quantity?: number;
  selected_value?: { id: string; value: string; price: number } | string;
}>>([]);

// Computed pour afficher le pack avec son prix
const selectedPackDisplay = computed(() => {
  if (!selectedPackName.value) return '';
  if (selectedPackPrice.value !== null) {
    return `${selectedPackName.value} (${selectedPackPrice.value} €)`;
  }
  return selectedPackName.value;
});

// Charger les packs disponibles
async function loadPacks() {
  try {
    const response = await getEventsSponsoringPacks(eventSlug.value);
    packs.value = response.data;
    updateSelectedPackName();
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
    selectedPackPrice.value = pack?.base_price ?? null;
  } else if (props.partnership?.suggested_pack_name) {
    selectedPackName.value = `${props.partnership.suggested_pack_name} (suggéré)`;
    selectedPackPrice.value = null;
  } else {
    selectedPackName.value = '';
    selectedPackPrice.value = null;
  }
}

// Mettre à jour les options sélectionnées
function updateSelectedOptions() {
  selectedOptions.value = [];

  const packId = props.partnership?.selected_pack_id || props.partnership?.suggested_pack_id;
  const pack = packId ? packs.value.find(p => p.id === packId) : null;
  const packOptions = pack ? ((pack as any).options || pack.optional_options || []) : [];

  console.log('=== UPDATE SELECTED OPTIONS ===');
  console.log('Partnership data:', props.partnership);
  console.log('Has pack_options?', !!props.partnership?.pack_options);
  console.log('pack_options length:', props.partnership?.pack_options?.length);
  console.log('pack_options content:', props.partnership?.pack_options);
  console.log('Option selections:', props.partnership?.option_selections);

  // Si option_selections est disponible (avec détails quantité/valeur), on l'utilise en priorité
  if (props.partnership?.option_selections && props.partnership.option_selections.length > 0) {
    selectedOptions.value = props.partnership.option_selections.map(selection => {
      const fullOption = packOptions.find((po: any) => po.id === selection.option_id);

      let selectedValue = undefined;
      if (selection.selected_value_id && fullOption) {
        // Trouver le nom de la valeur sélectionnée
        const selectableValues = (fullOption as any).selectable_values || [];
        const valueObj = selectableValues.find((v: any) => {
          if (typeof v === 'object') {
            return (v.id || v.value) === selection.selected_value_id;
          }
          return v === selection.selected_value_id;
        });
        selectedValue = typeof valueObj === 'object' ? (valueObj.name || valueObj.label || valueObj.value) : valueObj;
      }

      return {
        id: selection.option_id,
        name: fullOption?.name || 'Option inconnue',
        description: fullOption?.description || null,
        price: fullOption?.price ?? null,
        type: fullOption?.type,
        selected_quantity: selection.selected_quantity,
        selected_value: selectedValue
      };
    });
    return;
  }

  // Si pack_options est fourni directement (nouveau format depuis getEventsPartnershipDetailed),
  // on l'utilise directement car il contient déjà toutes les informations nécessaires
  if (props.partnership?.pack_options && props.partnership.pack_options.length > 0) {
    console.log('Using pack_options from partnership:', props.partnership.pack_options);
    selectedOptions.value = props.partnership.pack_options.map((opt: any) => {
      console.log('Option details:', opt);
      return {
        id: opt.id,
        name: opt.name,
        description: opt.description || null,
        price: opt.price ?? null,
        type: opt.type,
        quantity: opt.quantity ?? null,
        total_price: opt.total_price ?? null,
        selected_value: opt.selected_value ?? null
      };
    });
    console.log('Final selectedOptions:', selectedOptions.value);
    return;
  }

  // Sinon, on garde l'ancienne méthode de chargement via l'API getEventsSponsoringPacks
  if (!props.partnership?.option_ids || props.partnership.option_ids.length === 0) {
    return;
  }

  if (!pack) return;

  // Filtrer les options qui sont dans option_ids
  selectedOptions.value = packOptions
    .filter((opt: any) => props.partnership?.option_ids?.includes(opt.id))
    .map((opt: any) => ({
      id: opt.id,
      name: opt.name,
      description: opt.description || null,
      price: opt.price ?? null,
      type: opt.type
    }));
}

const form = ref({
  contact_name: props.partnership?.contact.display_name || '',
  contact_role: props.partnership?.contact.role || '',
  language: props.partnership?.language || 'fr',
  emails: props.partnership?.emails || '',
  phone: props.partnership?.phone || ''
});

// Mettre à jour le formulaire si les props changent
watch(() => props.partnership, (newPartnership) => {
  if (newPartnership) {
    form.value = {
      contact_name: newPartnership.contact.display_name,
      contact_role: newPartnership.contact.role,
      language: newPartnership.language,
      emails: newPartnership.emails || '',
      phone: newPartnership.phone || ''
    };
    updateSelectedPackName();
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