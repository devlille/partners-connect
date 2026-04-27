<template>
  <NuxtLayout
    name="minimal-sidebar"
    :sidebar-title="partnership?.company_name || 'Partenariat'"
    :sidebar-links="sidebarLinks"
  >
    <div class="min-h-screen bg-gray-50">
      <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8" role="main">
        <header class="bg-white rounded-lg shadow p-6 mb-6">
          <PageTitle>Tickets</PageTitle>
          <p class="text-sm text-gray-600 mt-1" role="doc-subtitle">
            Gérez les tickets associés à votre partenariat
          </p>
        </header>

        <div v-if="loading" role="status" aria-live="polite" aria-label="Chargement des données">
          <TableSkeleton :columns="4" :rows="6" />
        </div>

        <AlertMessage v-else-if="error" type="error" :message="error" />

        <div v-else class="bg-white rounded-lg shadow p-6">
          <TicketsManager :event-slug="eventSlug" :partnership-id="partnershipId" />
        </div>
      </main>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import { usePublicPartnership } from '~/composables/usePublicPartnership';

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

const { sidebarLinks } = usePublicPartnershipLinks();

onMounted(loadPartnership);

watch([eventSlug, partnershipId], loadPartnership);

useHead({
  title: computed(() => `Tickets - ${partnership.value?.company_name || 'Partenariat'} | DevLille`),
});
</script>
