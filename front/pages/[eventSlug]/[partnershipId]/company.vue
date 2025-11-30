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
          <span class="sr-only">Chargement des informations de l'entreprise...</span>
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

        <!-- Company Information -->
        <section
          v-else-if="company"
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
      </main>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import CompanyForm from "~/components/partnership/CompanyForm.vue";

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
  company,
  loading,
  error,
  savingCompany,
  loadPartnership,
  handleCompanySave
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
  },
  {
    label: 'Offres d\'emploi',
    icon: 'i-heroicons-briefcase',
    to: `/${eventSlug.value}/${partnershipId.value}/job-offers`
  },
  {
    label: 'Liens utiles',
    icon: 'i-heroicons-link',
    to: `/${eventSlug.value}/${partnershipId.value}/external-links`
  },
  {
    label: 'Prestataires',
    icon: 'i-heroicons-user-group',
    to: `/${eventSlug.value}/${partnershipId.value}/providers`
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
  title: computed(() => `${company.value?.name || 'Entreprise'} | DevLille`)
});
</script>
