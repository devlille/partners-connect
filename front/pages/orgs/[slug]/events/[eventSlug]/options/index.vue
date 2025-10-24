<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Options de sponsoring - {{ eventName }}</h1>
        </div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/options/create`"
          icon="i-heroicons-plus"
          color="primary"
        >
          Créer une option
        </UButton>
      </div>
    </div>

    <div class="p-6">
      <div v-if="loading" class="flex justify-center py-8">
        <div class="text-gray-500">Chargement...</div>
      </div>

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else-if="options.length === 0" class="text-center py-12">
        <div class="text-gray-500 mb-4">Aucune option pour le moment</div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/options/create`"
          icon="i-heroicons-plus"
          color="primary"
        >
          Créer une option
        </UButton>
      </div>

      <UTable v-else :data="options" :columns="columns" @select="onRowClick" />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsOptions, getEventBySlug, type SponsoringOption } from "~/utils/api";
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

const options = ref<SponsoringOption[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);
const { getOptionName } = useOptionTranslation();

// Colonnes du tableau
const columns = [
  {
    header: 'Nom',
    accessorKey: 'name',
    cell: (info: TableRow<SponsoringOption>) => {
      return getOptionName(info.row.original);
    }
  },
  {
    header: 'Prix',
    accessorKey: 'price',
    cell: (info: TableRow<SponsoringOption>) => {
      const price = info.getValue('price');
      return price ? `${price}€` : 'Gratuit';
    }
  }
];

async function loadOptions() {
  try {
    loading.value = true;
    error.value = null;

    const [optionsResponse, eventResponse] = await Promise.all([
      getOrgsEventsOptions(orgSlug.value, eventSlug.value),
      getEventBySlug(eventSlug.value)
    ]);

    options.value = optionsResponse.data;
    eventName.value = eventResponse.data.event.name;
  } catch (err) {
    console.error('Failed to load options:', err);
    error.value = 'Impossible de charger les options';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadOptions();
});

watch([orgSlug, eventSlug], () => {
  loadOptions();
});

function onRowClick(row: TableRow<SponsoringOption>) {
  navigateTo(`/orgs/${orgSlug.value}/events/${eventSlug.value}/options/${row.original.id}`);
}

useHead({
  title: computed(() => `Options - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
