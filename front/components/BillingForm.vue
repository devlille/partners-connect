<template>
  <div class="space-y-6">
    <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
      {{ error }}
    </div>

    <div v-if="success" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded">
      {{ success }}
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-4">
      <div>
        <label for="name" class="block text-sm font-medium text-gray-700 mb-1">
          Nom de l'entreprise (optionnel)
        </label>
        <UInput
          id="name"
          v-model="form.name"
          placeholder="Nom de facturation"
          :disabled="isLoading"
        />
      </div>

      <div>
        <label for="po" class="block text-sm font-medium text-gray-700 mb-1">
          Bon de commande (optionnel)
        </label>
        <UInput
          id="po"
          v-model="form.po"
          placeholder="Numéro de bon de commande"
          :disabled="isLoading"
        />
      </div>

      <div class="border-t pt-4">
        <h3 class="text-sm font-semibold text-gray-900 mb-3">Contact de facturation</h3>

        <div class="space-y-4">
          <div>
            <label for="firstName" class="block text-sm font-medium text-gray-700 mb-1">
              Prénom <span class="text-red-500">*</span>
            </label>
            <UInput
              id="firstName"
              v-model="form.contact.first_name"
              placeholder="Prénom"
              required
              :disabled="isLoading"
            />
          </div>

          <div>
            <label for="lastName" class="block text-sm font-medium text-gray-700 mb-1">
              Nom <span class="text-red-500">*</span>
            </label>
            <UInput
              id="lastName"
              v-model="form.contact.last_name"
              placeholder="Nom"
              required
              :disabled="isLoading"
            />
          </div>

          <div>
            <label for="email" class="block text-sm font-medium text-gray-700 mb-1">
              Email <span class="text-red-500">*</span>
            </label>
            <UInput
              id="email"
              v-model="form.contact.email"
              type="email"
              placeholder="email@example.com"
              required
              :disabled="isLoading"
            />
          </div>
        </div>
      </div>

      <div class="flex gap-3 pt-4">
        <UButton
          type="submit"
          color="primary"
          :loading="isLoading"
          :disabled="!isFormValid"
        >
          {{ existingBilling ? 'Mettre à jour' : 'Créer' }} les informations de facturation
        </UButton>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { getEventsPartnershipBilling, postEventsPartnershipBilling, putEventsPartnershipBilling } from '~/utils/api';
import type { CompanyBillingData } from '~/utils/api';

const props = defineProps<{
  eventSlug: string;
  partnershipId: string;
}>();

const emit = defineEmits<{
  saved: [];
}>();

const form = ref<CompanyBillingData>({
  name: null,
  po: null,
  contact: {
    first_name: '',
    last_name: '',
    email: ''
  }
});

const isLoading = ref(false);
const error = ref<string | null>(null);
const success = ref<string | null>(null);
const existingBilling = ref(false);

const isFormValid = computed(() => {
  return (
    form.value.contact.first_name.trim() !== '' &&
    form.value.contact.last_name.trim() !== '' &&
    form.value.contact.email.trim() !== '' &&
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.contact.email)
  );
});

async function loadBilling() {
  try {
    const response = await getEventsPartnershipBilling(props.eventSlug, props.partnershipId);
    if (response.data) {
      form.value = {
        name: response.data.name || null,
        po: response.data.po || null,
        contact: {
          first_name: response.data.contact.first_name,
          last_name: response.data.contact.last_name,
          email: response.data.contact.email
        }
      };
      existingBilling.value = true;
    }
  } catch (err: any) {
    // Si le billing n'existe pas encore, ce n'est pas une erreur
    if (err.response?.status !== 404) {
      console.error('Failed to load billing data:', err);
    }
  }
}

async function handleSubmit() {
  error.value = null;
  success.value = null;
  isLoading.value = true;

  try {
    const billingData: CompanyBillingData = {
      name: form.value.name || null,
      po: form.value.po || null,
      contact: {
        first_name: form.value.contact.first_name.trim(),
        last_name: form.value.contact.last_name.trim(),
        email: form.value.contact.email.trim()
      }
    };

    if (existingBilling.value) {
      await putEventsPartnershipBilling(props.eventSlug, props.partnershipId, billingData);
      success.value = 'Informations de facturation mises à jour avec succès';
    } else {
      await postEventsPartnershipBilling(props.eventSlug, props.partnershipId, billingData);
      success.value = 'Informations de facturation créées avec succès';
      existingBilling.value = true;
    }

    emit('saved');

    // Effacer le message de succès après 3 secondes
    setTimeout(() => {
      success.value = null;
    }, 3000);
  } catch (err: any) {
    console.error('Failed to save billing data:', err);
    error.value = err.response?.data?.message || 'Impossible de sauvegarder les informations de facturation';
  } finally {
    isLoading.value = false;
  }
}

// Charger les données existantes au montage
onMounted(() => {
  loadBilling();
});
</script>
