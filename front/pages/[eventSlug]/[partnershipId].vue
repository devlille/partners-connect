<template>
  <div class="min-h-screen bg-gray-50">
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8" role="main">
      <!-- Header -->
      <header class="bg-white rounded-lg shadow p-6 mb-6">
        <div class="flex items-center justify-between">
          <div>
            <PageTitle>{{ partnership?.company_name || 'Partenariat' }}</PageTitle>
            <p class="text-sm text-gray-600 mt-1" role="doc-subtitle">
              {{ partnership?.event_name }}
            </p>
          </div>
        </div>
      </header>

      <!-- Loading State -->
      <div v-if="loading" role="status" aria-live="polite" aria-label="Chargement des données">
        <TableSkeleton :columns="4" :rows="6" />
        <span class="sr-only">Chargement des informations du partenariat...</span>
      </div>

      <!-- Error State -->
      <div
        v-else-if="error"
        role="alert"
        aria-live="assertive"
        class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded"
      >
        {{ error }}
      </div>

      <!-- Content -->
      <div v-else class="space-y-6" :aria-busy="savingCompany || savingBilling ? 'true' : 'false'">
        <!-- Partnership Information -->
        <section class="bg-white rounded-lg shadow p-6" aria-labelledby="partnership-heading">
          <h2 id="partnership-heading" class="text-lg font-semibold text-gray-900 mb-4">
            Informations du partenariat
          </h2>
          <PartnershipForm
            :partnership="partnership"
            :loading="false"
            :show-admin-actions="false"
            :readonly="true"
          />
        </section>

        <!-- Company Information -->
        <section
          v-if="company"
          class="bg-white rounded-lg shadow p-6"
          aria-labelledby="company-heading"
          :aria-busy="savingCompany ? 'true' : 'false'"
        >
          <h2 id="company-heading" class="text-lg font-semibold text-gray-900 mb-4">
            Informations de l'entreprise
          </h2>
          <div v-if="savingCompany" role="status" aria-live="polite" class="sr-only">
            Enregistrement des informations de l'entreprise en cours...
          </div>
          <CompanyForm
            :company="company"
            :readonly="false"
            :loading="savingCompany"
            @save="handleCompanySave"
          />
        </section>

        <!-- Billing Information -->
        <section
          v-if="partnership"
          class="bg-white rounded-lg shadow p-6"
          aria-labelledby="billing-heading"
          :aria-busy="savingBilling ? 'true' : 'false'"
        >
          <h2 id="billing-heading" class="text-lg font-semibold text-gray-900 mb-4">
            Informations de facturation
          </h2>
          <div v-if="savingBilling" role="status" aria-live="polite" class="sr-only">
            Enregistrement des informations de facturation en cours...
          </div>
          <BillingForm
            :partnership="partnership"
            :billing="billing"
            :readonly="false"
            :loading="savingBilling"
            @save="handleBillingSave"
          />
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { getEventsPartnershipDetailed, getEventsPartnershipBilling, putCompanyById, putEventsPartnershipBilling } from "~/utils/api";
import type { ExtendedPartnershipItem } from "~/types/partnership";
import type { CompanyBillingData, UpdateCompanySchema } from "~/utils/api";
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
const savingCompany = ref(false);
const savingBilling = ref(false);
const toast = useToast();

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
      error.value = `404 Not Found: Partnership not found for event "${eventSlug.value}" and ID "${partnershipId.value}". Please verify the URL.`;
    } else if (err.response?.status >= 500) {
      const statusText = err.response?.statusText || 'Internal Server Error';
      error.value = `${err.response.status} Server Error: ${statusText}. The server encountered an error processing your request. Please try again later or contact support if the problem persists.`;
    } else if (!navigator.onLine) {
      error.value = 'Network Error: No internet connection detected. Please check your network connection and refresh the page to retry.';
    } else if (err.response?.status) {
      error.value = `HTTP ${err.response.status} Error: ${err.response.statusText || err.message || 'Failed to load partnership data'}. Please try again or contact support.`;
    } else {
      error.value = `Error: ${err.message || 'Unknown error occurred while loading partnership data'}. Please refresh the page to retry.`;
    }
  } finally {
    loading.value = false;
  }
}

/**
 * Handle company information save
 */
async function handleCompanySave(data: UpdateCompanySchema) {
  if (!company.value?.id) return;

  try {
    savingCompany.value = true;
    error.value = null;

    await putCompanyById(company.value.id, data);

    // Reload partnership data to get updated information
    await loadPartnership();

    // Show success message to user
    toast.add({
      title: 'Informations mises à jour',
      description: 'Les informations de l\'entreprise ont été enregistrées avec succès',
      color: 'success',
      timeout: 3000
    });
  } catch (err: any) {
    console.error('Failed to update company:', err);
    const errorMessage = `Impossible de mettre à jour les informations de l'entreprise: ${err.message || 'Erreur inconnue'}`;
    error.value = errorMessage;

    toast.add({
      title: 'Erreur de sauvegarde',
      description: errorMessage,
      color: 'error',
      timeout: 5000
    });
  } finally {
    savingCompany.value = false;
  }
}

/**
 * Handle billing information save
 */
async function handleBillingSave(data: CompanyBillingData) {
  try {
    savingBilling.value = true;
    error.value = null;

    await putEventsPartnershipBilling(eventSlug.value, partnershipId.value, data);

    // Reload partnership data to get updated billing
    await loadPartnership();

    // Show success message to user
    toast.add({
      title: 'Informations mises à jour',
      description: 'Les informations de facturation ont été enregistrées avec succès',
      color: 'success',
      timeout: 3000
    });
  } catch (err: any) {
    console.error('Failed to update billing:', err);
    const errorMessage = `Impossible de mettre à jour les informations de facturation: ${err.message || 'Erreur inconnue'}`;
    error.value = errorMessage;

    toast.add({
      title: 'Erreur de sauvegarde',
      description: errorMessage,
      color: 'error',
      timeout: 5000
    });
  } finally {
    savingBilling.value = false;
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
