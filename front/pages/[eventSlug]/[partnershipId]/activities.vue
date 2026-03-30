<template>
  <NuxtLayout
    name="minimal-sidebar"
    :sidebar-title="partnership?.company_name || 'Partenariat'"
    :sidebar-links="sidebarLinks"
  >
    <div class="min-h-screen bg-gray-50">
      <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8" role="main">
        <header class="bg-white rounded-lg shadow p-6 mb-6">
          <PageTitle>Vos Activités</PageTitle>
          <p class="text-sm text-gray-600 mt-1" role="doc-subtitle">
            Gérez les activités prévues sur votre stand
          </p>
        </header>

        <div v-if="loading" role="status" aria-live="polite" aria-label="Chargement des données">
          <TableSkeleton :columns="3" :rows="6" />
        </div>

        <AlertMessage v-else-if="error" type="error" :message="error" />

        <div v-else class="bg-white rounded-lg shadow p-6">
          <ActivitiesManager :event-slug="eventSlug" :partnership-id="partnershipId" />
        </div>
      </main>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import { usePublicPartnership } from '~/composables/usePublicPartnership';
import { usePartnershipValidation } from '~/composables/usePartnershipValidation';

definePageMeta({
  auth: false,
  ssr: false,
  validate: async (route) => {
    const eventSlug = Array.isArray(route.params.eventSlug)
      ? route.params.eventSlug[0]
      : route.params.eventSlug;
    const partnershipId = Array.isArray(route.params.partnershipId)
      ? route.params.partnershipId[0]
      : route.params.partnershipId;
    const isValidFormat = /^[a-zA-Z0-9-_]+$/;
    return isValidFormat.test(eventSlug) && isValidFormat.test(partnershipId);
  },
});

const { eventSlug, partnershipId, partnership, company, loading, error, loadPartnership } =
  usePublicPartnership();

const { isPartnershipComplete, isCompanyComplete } = usePartnershipValidation();

const sidebarLinks = computed(() => {
  const partnershipComplete = isPartnershipComplete(partnership.value);
  const companyComplete = isCompanyComplete(company.value);

  return [
    {
      label: 'Partenariat',
      icon: 'i-heroicons-hand-raised',
      to: `/${eventSlug.value}/${partnershipId.value}`,
      badge: !partnershipComplete
        ? { label: '!', color: 'error' as const, title: 'Informations incomplètes' }
        : undefined,
    },
    {
      label: 'Entreprise',
      icon: 'i-heroicons-building-office',
      to: `/${eventSlug.value}/${partnershipId.value}/company`,
      badge: !companyComplete
        ? { label: '!', color: 'error' as const, title: 'Informations incomplètes' }
        : undefined,
    },
    {
      label: "Offres d'emploi",
      icon: 'i-heroicons-briefcase',
      to: `/${eventSlug.value}/${partnershipId.value}/job-offers`,
    },
    {
      label: 'Vos Activités',
      icon: 'i-heroicons-calendar-days',
      to: `/${eventSlug.value}/${partnershipId.value}/activities`,
    },
    {
      label: 'Liens utiles',
      icon: 'i-heroicons-link',
      to: `/${eventSlug.value}/${partnershipId.value}/external-links`,
    },
    {
      label: 'Prestataires',
      icon: 'i-heroicons-user-group',
      to: `/${eventSlug.value}/${partnershipId.value}/providers`,
    },
    {
      label: 'Booth',
      icon: 'i-heroicons-map-pin',
      to: `/${eventSlug.value}/${partnershipId.value}/booth`,
    },
  ];
});

onMounted(loadPartnership);

watch([eventSlug, partnershipId], loadPartnership);

useHead({
  title: computed(() => `Activités - ${partnership.value?.company_name || 'Partenariat'} | DevLille`),
});
</script>
