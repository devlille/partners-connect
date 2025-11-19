<template>
  <NuxtLayout
    name="minimal-sidebar"
    :sidebar-title="partnership?.company_name || 'Partenariat'"
    :sidebar-links="sidebarLinks"
  >
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

        <!-- Partnership Information -->
        <section v-else class="bg-white rounded-lg shadow p-6" aria-labelledby="partnership-heading">
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

        <!-- Billing Information -->
        <section
          v-if="!loading && !error && partnership && partnership.validated"
          class="bg-white rounded-lg shadow p-6 mt-6"
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

        <!-- Message if not validated -->
        <section
          v-if="!loading && !error && partnership && !partnership.validated"
          class="bg-blue-50 border border-blue-200 rounded-lg p-6 mt-6"
        >
          <div class="flex items-start gap-3">
            <i class="i-heroicons-information-circle text-blue-600 text-xl flex-shrink-0 mt-0.5" aria-hidden="true" />
            <div>
              <h3 class="text-sm font-semibold text-blue-900 mb-1">
                Partenariat en attente de validation
              </h3>
              <p class="text-sm text-blue-700">
                Votre demande de partenariat est en cours de traitement. Les informations de facturation seront disponibles une fois que votre partenariat aura été validé par nos équipes.
              </p>
            </div>
          </div>
        </section>
      </main>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import PartnershipForm from "~/components/partnership/PartnershipForm.vue";
import BillingForm from "~/components/partnership/BillingForm.vue";

definePageMeta({
  auth: false,
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

const {
  eventSlug,
  partnershipId,
  partnership,
  billing,
  loading,
  error,
  savingBilling,
  loadPartnership,
  handleBillingSave
} = usePublicPartnership();

// Sidebar navigation configuration
const sidebarLinks = computed(() => [
  {
    label: 'Partenariat',
    icon: 'i-heroicons-hand-raised',
    to: `/${eventSlug.value}/${partnershipId.value}`
  },
  {
    label: 'Entreprise',
    icon: 'i-heroicons-building-office',
    to: `/${eventSlug.value}/${partnershipId.value}/company`
  }
]);

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
