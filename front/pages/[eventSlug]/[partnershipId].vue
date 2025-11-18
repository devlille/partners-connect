<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Header -->
      <div class="bg-white rounded-lg shadow p-6 mb-6">
        <div class="flex items-center justify-between">
          <div>
            <PageTitle>{{ partnership?.company_name || 'Partenariat' }}</PageTitle>
            <p class="text-sm text-gray-600 mt-1">{{ partnership?.event_name }}</p>
          </div>
        </div>
      </div>

      <!-- Loading State -->
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <!-- Error State -->
      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <!-- Content -->
      <div v-else class="space-y-6">
        <!-- Partnership Information -->
        <div class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations du partenariat</h2>
          <PartnershipForm
            :partnership="partnership"
            :loading="false"
            :show-admin-actions="false"
            :readonly="true"
          />
        </div>

        <!-- Company Information -->
        <div v-if="company" class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations de l'entreprise</h2>
          <CompanyForm
            :company="company"
            :readonly="true"
            :loading="false"
          />
        </div>

        <!-- Billing Information -->
        <div v-if="partnership" class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations de facturation</h2>
          <BillingForm
            :partnership="partnership"
            :billing="billing"
            :readonly="true"
            :loading="false"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getEventsPartnershipDetailed, getEventsPartnershipBilling } from "~/utils/api";
import type { ExtendedPartnershipItem } from "~/types/partnership";
import type { CompanyBillingData } from "~/utils/api";
import PartnershipForm from "~/components/partnership/PartnershipForm.vue";
import CompanyForm from "~/components/partnership/CompanyForm.vue";
import BillingForm from "~/components/partnership/BillingForm.vue";

definePageMeta({
  auth: false,
  layout: 'minimal',
  ssr: false,
  validate: async (route) => {
    // Validate both eventSlug and partnershipId format (alphanumeric, hyphens, underscores)
    const eventSlug = Array.isArray(route.params.eventSlug) ? route.params.eventSlug[0] : route.params.eventSlug;
    const partnershipId = Array.isArray(route.params.partnershipId) ? route.params.partnershipId[0] : route.params.partnershipId;

    const isValidFormat = /^[a-zA-Z0-9-_]+$/;
    return isValidFormat.test(eventSlug) && isValidFormat.test(partnershipId);
  }
});

const route = useRoute();

const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const partnershipId = computed(() => {
  const params = route.params.partnershipId;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const partnership = ref<ExtendedPartnershipItem | null>(null);
const company = ref<any | null>(null);
const billing = ref<CompanyBillingData | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

/**
 * Load partnership data using public API endpoints
 */
async function loadPartnership() {
  try {
    loading.value = true;
    error.value = null;

    // Load partnership details
    const response = await getEventsPartnershipDetailed(eventSlug.value, partnershipId.value);
    const { partnership: p, company: c, event } = response.data;

    // Store company data
    company.value = c;

    // Load billing data
    try {
      const billingResponse = await getEventsPartnershipBilling(eventSlug.value, partnershipId.value);
      billing.value = billingResponse.data || null;
    } catch (billingErr: any) {
      // If billing doesn't exist yet, it's not an error
      if (billingErr.response?.status !== 404) {
        console.error('Failed to load billing data:', billingErr);
      }
      billing.value = null;
    }

    // Extract pack options
    const packOptions = ((p.selected_pack as any)?.options || p.selected_pack?.optional_options || []).map((opt: any) => ({
      id: opt.id,
      name: opt.name,
      description: opt.description || null
    }));
    const optionIds = packOptions.map(opt => opt.id);

    // Map data to ExtendedPartnershipItem
    partnership.value = {
      id: p.id,
      contact: {
        display_name: p.contact_name,
        role: p.contact_role
      },
      company_name: c.name,
      event_name: event.name,
      selected_pack_id: p.selected_pack?.id || null,
      selected_pack_name: p.selected_pack?.name || null,
      suggested_pack_id: p.suggestion_pack?.id || null,
      suggested_pack_name: p.suggestion_pack?.name || null,
      validated_pack_id: p.validated_pack?.id || null,
      language: p.language,
      phone: p.phone || null,
      emails: p.emails.join(', '),
      created_at: p.created_at,
      validated: p.process_status?.validated_at !== null && p.process_status?.validated_at !== undefined,
      paid: p.process_status?.billing_status?.toLowerCase() === 'paid',
      suggestion: false,
      agreement_generated: p.process_status?.agreement_url !== null && p.process_status?.agreement_url !== undefined,
      agreement_signed: p.process_status?.agreement_signed_url !== null && p.process_status?.agreement_signed_url !== undefined,
      option_ids: optionIds,
      pack_options: packOptions
    };
  } catch (err: any) {
    console.error('Failed to load partnership:', err);

    if (err.response?.status === 404) {
      error.value = 'Partenariat introuvable. Veuillez vérifier le lien fourni.';
    } else if (err.response?.status >= 500) {
      error.value = 'Erreur serveur. Veuillez réessayer plus tard.';
    } else if (!navigator.onLine) {
      error.value = 'Erreur de connexion. Veuillez vérifier votre connexion internet.';
    } else {
      error.value = 'Impossible de charger les informations du partenariat.';
    }
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadPartnership();
});

// Reload if partnership ID or event slug changes
watch([eventSlug, partnershipId], () => {
  loadPartnership();
});

useHead({
  title: computed(() => `${partnership.value?.company_name || 'Partenariat'} | DevLille`)
});
</script>
