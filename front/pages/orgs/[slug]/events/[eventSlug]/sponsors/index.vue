<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Sponsors - {{ eventName }}</h1>
        </div>
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

      <UTable
        v-else
        :data="partnerships"
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
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership, getEventBySlug, type PartnershipItem } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

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

useHead({
  title: computed(() => `Sponsors - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
