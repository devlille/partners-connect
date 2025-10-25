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

      <UTable
        v-else
        :data="partnerships"
        :columns="columns"
        @select="onRowClick"
      />
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

const columns = [
  {
    header: 'Nom de l\'entreprise',
    accessorKey: 'company_name',
    cell: (info: TableRow<PartnershipItem>) => info.getValue('company_name')
  },
  {
    header: 'Pack',
    accessorKey: 'pack_name',
    cell: (info: TableRow<PartnershipItem>) => {
      const packName = info.getValue('pack_name');
      const suggestedPackName = info.row.original.suggested_pack_name;

      if (packName) {
        return packName;
      } else if (suggestedPackName) {
        return `${suggestedPackName} (suggéré)`;
      }
      return '-';
    }
  }
];

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
