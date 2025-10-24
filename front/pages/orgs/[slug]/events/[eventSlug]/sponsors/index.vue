<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Sponsors - {{ eventName }}</h1>
        </div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/sponsors/create`"
          label="Créer un sponsor"
          icon="i-heroicons-plus"
          color="primary"
        />
      </div>
    </div>

    <div class="p-6">
      <div v-if="loading" class="flex justify-center py-8">
        <div class="text-gray-500">Chargement...</div>
      </div>

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else-if="partnerships.length === 0" class="text-center py-12">
        <div class="text-gray-500 mb-4">Aucun sponsor pour le moment</div>
      </div>

      <template v-else>
        <!-- Statistiques -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <UCard>
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-medium text-gray-600">Total Sponsors</p>
                <p class="text-2xl font-bold text-gray-900 mt-1">{{ totalSponsors }}</p>
              </div>
              <div class="bg-blue-100 p-3 rounded-full">
                <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              </div>
            </div>
          </UCard>

          <UCard v-for="stat in packStats" :key="stat.name">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-medium text-gray-600">{{ stat.name }}</p>
                <p class="text-2xl font-bold text-gray-900 mt-1">{{ stat.count }}</p>
              </div>
              <div class="bg-green-100 p-3 rounded-full">
                <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                </svg>
              </div>
            </div>
          </UCard>

          <UCard v-if="sponsorsWithSuggestions > 0">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-medium text-gray-600">Suggestions</p>
                <p class="text-2xl font-bold text-gray-900 mt-1">{{ sponsorsWithSuggestions }}</p>
              </div>
              <div class="bg-yellow-100 p-3 rounded-full">
                <svg class="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </UCard>
        </div>

        <!-- Tableau -->
        <UTable
          :data="partnerships"
          @select="onRowClick"
        >
        <template #company_name-data="{ row }">
          <div class="font-medium text-gray-900">{{ row.company_name }}</div>
        </template>

        <template #contact-data="{ row }">
          <div>
            <div class="font-medium text-gray-900">{{ row.contact.display_name }}</div>
            <div class="text-sm text-gray-500">{{ row.contact.role }}</div>
          </div>
        </template>

        <template #pack_name-data="{ row }">
          <span v-if="row.pack_name" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
            {{ row.pack_name }}
          </span>
          <span v-else-if="row.suggested_pack_name" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
            {{ row.suggested_pack_name }} (suggéré)
          </span>
          <span v-else class="text-gray-400">-</span>
        </template>

        <template #emails-data="{ row }">
          <div v-if="row.emails" class="text-sm text-gray-900">{{ row.emails }}</div>
          <span v-else class="text-gray-400">-</span>
        </template>

        <template #phone-data="{ row }">
          <div v-if="row.phone" class="text-sm text-gray-900">{{ row.phone }}</div>
          <span v-else class="text-gray-400">-</span>
        </template>

        <template #created_at-data="{ row }">
          <div class="text-sm text-gray-500">{{ formatDate(row.created_at) }}</div>
        </template>
      </UTable>
      </template>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership, getEventBySlug, type PartnershipItem } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type {TableRow} from "@nuxt/ui";

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

const partnerships = ref<PartnershipItem[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Menu contextuel pour la page des sponsors
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Formater les dates
const { formatDate } = useDateFormatter();

// Statistiques calculées
const totalSponsors = computed(() => partnerships.value.length);

const packStats = computed(() => {
  const stats = new Map<string, number>();

  partnerships.value.forEach(p => {
    if (p.pack_name) {
      stats.set(p.pack_name, (stats.get(p.pack_name) || 0) + 1);
    }
  });

  return Array.from(stats.entries()).map(([name, count]) => ({
    name,
    count
  }));
});

const sponsorsWithSuggestions = computed(() => {
  return partnerships.value.filter(p => p.suggested_pack_name && !p.pack_name).length;
});

async function loadPartnerships() {
  try {
    loading.value = true;
    error.value = null;

    // Charger le nom de l'événement
    const eventResponse = await getEventBySlug(eventSlug.value);
    eventName.value = eventResponse.data.event.name;

    console.log(orgSlug.value, eventSlug.value)
    // Charger les partnerships (sponsors)
    const response = await getOrgsEventsPartnership(orgSlug.value, eventSlug.value);
    partnerships.value = response.data;
  } catch (err) {
    console.error('Failed to load partnerships:', err);
    error.value = 'Impossible de charger les sponsors';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadPartnerships();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug], () => {
  loadPartnerships();
});

// Gérer le clic sur une ligne du tableau
function onRowClick(row: TableRow<PartnershipItem>) {
  navigateTo(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors/${row.original.id}`);
}

useHead({
  title: computed(() => `Sponsors - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
