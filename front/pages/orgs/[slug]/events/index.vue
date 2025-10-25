<template>
  <Dashboard :main-links="orgLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <UButton
            :to="`/orgs/${slug}`"
            icon="i-heroicons-arrow-left"
            color="neutral"
            variant="ghost"
            class="mb-2"
            label="Retour"
          />
          <h1 class="text-2xl font-bold text-gray-900">Événements - {{ organisationName }}</h1>
        </div>
        <UButton
          :to="`/orgs/${slug}/events/create`"
          label="Créer un événement"
          icon="i-heroicons-plus"
          color="primary"
        />
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="5" :rows="8" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <UTable
        v-else
        :data="data"
        :columns="columns"
        @select="onSelectEvent"
       
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEvents, getOrgs, type EventSummary } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type {TableRow} from "@nuxt/ui";

const route = useRoute();
const router = useRouter();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});
const {formatDate} = useDateFormatter();
const columns = [
  {
    header: 'Nom',
    accessorKey: 'name',
    cell: (info: TableRow<EventSummary>) => info.getValue('name')
  },
  {
    header: 'Date de début',
    accessorKey: 'start_time',
    cell: (info: TableRow<EventSummary>) => formatDate(info.getValue('start_time'))
  },
  {
    header: 'Date de fin',
    accessorKey: 'end_time',
    cell: (info: TableRow<EventSummary>) => formatDate(info.getValue('end_time'))
  },
  {
    header: 'Date de début CFP',
    accessorKey: 'submission_start_time',
    cell: (info: TableRow<EventSummary>) => formatDate(info.getValue('submission_start_time'))
  },
  {
    header: 'Date de fin CFP',
    accessorKey: 'submission_end_time',
    cell: (info: TableRow<EventSummary>) => formatDate(info.getValue('submission_end_time'))
  }
];
const slug = computed(() => route.params.slug as string);
const data = ref<EventSummary[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const organisationName = ref<string>('');

// Menu contextuel pour la page d'événements de l'organisation
const orgLinks = computed(() => [
  {
    label: 'Informations',
    icon: 'i-heroicons-information-circle',
    to: `/orgs/${slug.value}`
  },
  {
    label: 'Événements',
    icon: 'i-heroicons-calendar',
    to: `/orgs/${slug.value}/events`
  },
  {
    label: 'Utilisateurs',
    icon: 'i-heroicons-users',
    to: `/orgs/${slug.value}/users`
  },
  {
    label: 'Partenariats',
    icon: 'i-heroicons-handshake',
    to: `/orgs/${slug.value}/partnerships`
  }
]);

const onSelectEvent = (row: TableRow<EventSummary>) => {
  router.push(`/orgs/${slug.value}/events/${row.original.slug}`);
};

async function loadEvents() {
  try {
    loading.value = true;
    error.value = null;

    // Charger le nom de l'organisation
    const orgResponse = await getOrgs(slug.value);
    organisationName.value = orgResponse.data.name;

    // Charger les événements
    const response = await getOrgsEvents(slug.value);
    data.value = response.data.items;
  } catch (err) {
    console.error('Failed to load events:', err);
    error.value = 'Impossible de charger les événements';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadEvents();
});

// Recharger si le slug change
watch(slug, () => {
  loadEvents();
});

useHead({
  title: computed(() => `Événements - ${organisationName.value || 'Organisation'} | DevLille`)
});
</script>
