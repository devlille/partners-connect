<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div>
        <PageTitle>{{ partnership?.company_name || 'Sponsor' }}</PageTitle>
        <p class="text-sm text-gray-600 mt-1">Tickets</p>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else class="bg-white rounded-lg shadow p-6">
        <TicketsManager
          v-if="partnership"
          :event-slug="eventSlug"
          :partnership-id="sponsorId"
          @tickets-updated="handleTicketsUpdated"
        />
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type { ExtendedPartnershipItem } from "~/types/partnership";

const route = useRoute();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const orgSlug = computed(() => {
  const params = route.params.slug;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  return Array.isArray(params) ? params[1] as string : params as string;
});

const sponsorId = computed(() => {
  const params = route.params.sponsorId;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const partnership = ref<ExtendedPartnershipItem | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

// Menu contextuel pour la page du sponsor
const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

async function loadPartnership() {
  try {
    loading.value = true;
    error.value = null;

    const response = await getOrgsEventsPartnership(orgSlug.value, eventSlug.value);
    // L'API retourne maintenant une structure paginée avec items
    const items = Array.isArray(response.data) ? response.data : response.data.items;
    const found = items.find(p => p.id === sponsorId.value);

    if (!found) {
      error.value = 'Sponsor non trouvé';
      return;
    }

    partnership.value = found;
  } catch (err) {
    console.error('Failed to load partnership:', err);
    error.value = 'Impossible de charger les informations du sponsor';
  } finally {
    loading.value = false;
  }
}

function handleTicketsUpdated(_updatedTickets: any) {
  console.log('Tickets updated');
}

onMounted(() => {
  loadPartnership();
});

watch([orgSlug, eventSlug, sponsorId], () => {
  loadPartnership();
});

useHead({
  title: computed(() => `Tickets - ${partnership.value?.company_name || 'Sponsor'} | DevLille`)
});
</script>
