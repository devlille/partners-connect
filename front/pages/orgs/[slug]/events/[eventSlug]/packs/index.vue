<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Packs de sponsoring - {{ eventName }}</h1>
        </div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/packs/create`"
          label="Créer un pack"
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

      <UTable
        v-else
        :data="packs"
        :columns="columns"
        @select="onSelectPack"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPacks, getEventBySlug, type SponsoringPack } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type { TableRow } from "@nuxt/ui";

const route = useRoute();
const router = useRouter();
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

const packs = ref<SponsoringPack[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Menu contextuel pour la page des packs
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Colonnes du tableau
const columns = [
  {
    header: 'Nom',
    accessorKey: 'name',
    cell: (info: TableRow<SponsoringPack>) => info.getValue('name')
  },
  {
    header: 'Prix',
    accessorKey: 'base_price',
    cell: (info: TableRow<SponsoringPack>) => `${info.getValue('base_price')}€`
  },
  {
    header: 'Quantité',
    accessorKey: 'max_quantity',
    cell: (info: TableRow<SponsoringPack>) => info.getValue('max_quantity') || '-'
  }
];

const onSelectPack = (row: TableRow<SponsoringPack>) => {
  router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/packs/${row.original.id}`);
};

async function loadPacks() {
  try {
    loading.value = true;
    error.value = null;

    // Charger le nom de l'événement
    const eventResponse = await getEventBySlug(eventSlug.value);
    eventName.value = eventResponse.data.event.name;

    // Charger les packs
    const response = await getOrgsEventsPacks(orgSlug.value, eventSlug.value);
    packs.value = response.data;
  } catch (err) {
    console.error('Failed to load packs:', err);
    error.value = 'Impossible de charger les packs';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadPacks();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug], () => {
  loadPacks();
});

useHead({
  title: computed(() => `Packs de sponsoring - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
