<template>
  <NuxtLayout
    name="minimal-sidebar"
    :sidebar-title="partnership?.company_name || 'Partenariat'"
    :sidebar-links="sidebarLinks"
  >
    <div class="min-h-screen bg-gray-50">
      <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8" role="main">
        <header class="bg-white rounded-lg shadow p-6 mb-6">
          <PageTitle>Booth</PageTitle>
          <p class="text-sm text-gray-600 mt-1">Plan et emplacement de votre booth</p>
        </header>

        <div v-if="loading" role="status" aria-live="polite" aria-label="Chargement des données">
          <TableSkeleton :columns="2" :rows="4" />
          <span class="sr-only">Chargement des informations du booth...</span>
        </div>

        <AlertMessage v-else-if="error" type="error" :message="error" />

        <div v-else class="space-y-6">
          <!-- Emplacement du booth -->
          <section class="bg-white rounded-lg shadow p-6" aria-labelledby="location-heading">
            <h2 id="location-heading" class="text-lg font-semibold text-gray-900 mb-4">
              Votre emplacement
            </h2>
            <div
              v-if="boothLocation"
              class="flex items-center gap-3 p-4 bg-primary-50 border border-primary-200 rounded-lg"
            >
              <i class="i-heroicons-map-pin text-primary-600 text-2xl" aria-hidden="true" />
              <div>
                <p class="text-sm text-gray-600">Identifiant du booth</p>
                <p class="text-2xl font-bold text-gray-900">{{ boothLocation }}</p>
              </div>
            </div>
            <div
              v-else
              class="flex items-center gap-3 p-4 bg-gray-50 border border-gray-200 rounded-lg text-sm text-gray-600"
            >
              <i class="i-heroicons-information-circle text-xl text-gray-400" aria-hidden="true" />
              <span>Aucun emplacement n'a encore été assigné à votre booth.</span>
            </div>
          </section>

          <!-- Plan des booths -->
          <section class="bg-white rounded-lg shadow p-6" aria-labelledby="plan-heading">
            <h2 id="plan-heading" class="text-lg font-semibold text-gray-900 mb-4">
              Plan des booths
            </h2>
            <div v-if="boothPlanImageUrl" class="border border-gray-200 rounded-lg overflow-hidden">
              <img
                :src="boothPlanImageUrl"
                alt="Plan des booths"
                class="w-full object-contain max-h-[600px]"
              />
            </div>
            <div
              v-else
              class="flex items-center gap-3 p-4 bg-gray-50 border border-gray-200 rounded-lg text-sm text-gray-600"
            >
              <i class="i-heroicons-information-circle text-xl text-gray-400" aria-hidden="true" />
              <span>Le plan des booths n'est pas encore disponible.</span>
            </div>
          </section>
        </div>
      </main>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
definePageMeta({
  auth: false,
  ssr: false,
  validate: async (route) => {
    const eventSlug = Array.isArray(route.params.eventSlug) ? route.params.eventSlug[0] : route.params.eventSlug;
    const partnershipId = Array.isArray(route.params.partnershipId) ? route.params.partnershipId[0] : route.params.partnershipId;

    const isValidFormat = /^[a-zA-Z0-9-_]+$/;
    return isValidFormat.test(eventSlug) && isValidFormat.test(partnershipId);
  }
});

const {
  eventSlug,
  partnershipId,
  partnership,
  company,
  loading,
  error,
  boothLocation,
  boothPlanImageUrl,
  loadPartnership,
} = usePublicPartnership();

const { isPartnershipComplete, isCompanyComplete } = usePartnershipValidation();

const sidebarLinks = computed(() => {
  const partnershipComplete = isPartnershipComplete(partnership.value);
  const companyComplete = isCompanyComplete(company.value);

  return [
    {
      label: 'Partenariat',
      icon: 'i-heroicons-hand-raised',
      to: `/${eventSlug.value}/${partnershipId.value}`,
      badge: !partnershipComplete ? {
        label: '!',
        color: 'error' as const,
        title: 'Informations incomplètes'
      } : undefined
    },
    {
      label: 'Entreprise',
      icon: 'i-heroicons-building-office',
      to: `/${eventSlug.value}/${partnershipId.value}/company`,
      badge: !companyComplete ? {
        label: '!',
        color: 'error' as const,
        title: 'Informations incomplètes'
      } : undefined
    },
    {
      label: 'Offres d\'emploi',
      icon: 'i-heroicons-briefcase',
      to: `/${eventSlug.value}/${partnershipId.value}/job-offers`
    },
    {
      label: 'Vos Activités',
      icon: 'i-heroicons-calendar-days',
      to: `/${eventSlug.value}/${partnershipId.value}/activities`
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
    },
    {
      label: 'Booth',
      icon: 'i-heroicons-map-pin',
      to: `/${eventSlug.value}/${partnershipId.value}/booth`
    }
  ];
});

onMounted(loadPartnership);

watch([eventSlug, partnershipId], loadPartnership);

useHead({
  title: computed(() => `Booth - ${partnership.value?.company_name || 'Partenariat'} | DevLille`)
});
</script>
