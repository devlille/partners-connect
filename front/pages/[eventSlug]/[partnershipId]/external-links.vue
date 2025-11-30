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
          <PageTitle>Liens utiles</PageTitle>
          <p class="text-sm text-gray-600 mt-1">
            Liens externes et ressources pour {{ partnership?.event_name }}
          </p>
        </header>

        <!-- Loading State -->
        <div v-if="loadingEvent" role="status" aria-live="polite">
          <TableSkeleton :columns="2" :rows="6" />
          <span class="sr-only">Chargement des liens utiles...</span>
        </div>

        <!-- Error State -->
        <AlertMessage v-else-if="eventError" type="error" :message="eventError" />

        <!-- Empty State -->
        <div v-else-if="externalLinks.length === 0" class="bg-white rounded-lg shadow p-12 text-center">
          <i class="i-heroicons-link text-gray-400 text-5xl mx-auto mb-4" aria-hidden="true" />
          <h3 class="text-lg font-medium text-gray-900 mb-2">Aucun lien externe</h3>
          <p class="text-sm text-gray-500">
            Aucun lien externe n'a été configuré pour cet événement.
          </p>
        </div>

        <!-- External Links List -->
        <section v-else class="bg-white rounded-lg shadow overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200">
            <h2 class="text-lg font-semibold text-gray-900">Liens externes de l'événement</h2>
            <p class="mt-1 text-sm text-gray-500">Ressources et liens utiles</p>
          </div>

          <div class="divide-y divide-gray-200">
            <div
              v-for="link in externalLinks"
              :key="link.id"
              class="px-6 py-4 hover:bg-gray-50 transition-colors"
            >
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3 flex-1">
                  <i class="i-heroicons-link text-gray-400 text-xl shrink-0" aria-hidden="true" />
                  <div class="flex-1 min-w-0">
                    <h3 class="text-sm font-medium text-gray-900">
                      {{ link.name }}
                    </h3>
                    <p class="text-sm text-gray-500 truncate mt-1">
                      {{ link.url }}
                    </p>
                  </div>
                </div>
                <UButton
                  :to="link.url"
                  target="_blank"
                  rel="noopener noreferrer"
                  color="primary"
                  variant="outline"
                  size="sm"
                  :aria-label="`Ouvrir ${link.name}`"
                >
                  <i class="i-heroicons-arrow-top-right-on-square mr-1" aria-hidden="true" />
                  Ouvrir
                </UButton>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import { getEventBySlug, type EventExternalLinkSchema } from '~/utils/api';

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
  loading: partnershipLoading,
  loadPartnership
} = usePublicPartnership();

const externalLinks = ref<EventExternalLinkSchema[]>([]);
const loadingEvent = ref(true);
const eventError = ref<string | null>(null);

async function loadExternalLinks() {
  try {
    loadingEvent.value = true;
    eventError.value = null;

    const response = await getEventBySlug(eventSlug.value);
    externalLinks.value = response.data.event.external_links || [];
  } catch (err: any) {
    console.error('Failed to load external links:', err);
    eventError.value = 'Impossible de charger les liens externes';
  } finally {
    loadingEvent.value = false;
  }
}

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
  loadExternalLinks();
});

watch(eventSlug, () => {
  loadExternalLinks();
});

useHead({
  title: computed(() => `Liens utiles - ${partnership.value?.company_name || 'Partenariat'} | DevLille`)
});
</script>
