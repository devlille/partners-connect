<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div>
        <BackButton :to="`/orgs/${orgSlug}/events`" label="Retour" />
        <PageTitle>Dashboard — {{ eventName }}</PageTitle>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="9" :rows="10" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <template v-else>
        <UInput
          v-model="search"
          icon="i-heroicons-magnifying-glass"
          placeholder="Rechercher un partenaire par nom"
          class="mb-4 w-full max-w-md"
        />
        <UTable :data="filteredRows" :columns="columns" />
      </template>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { h } from 'vue';
import { getEventBySlug } from '~/utils/api';
import { getEventStats, type PartnerStatsSchema } from '~/utils/eventStats';
import authMiddleware from '~/middleware/auth';

definePageMeta({
  middleware: authMiddleware,
  ssr: false,
});

const { footerLinks } = useDashboardLinks();
const { getOrgSlug, getEventSlug } = useRouteParams();

const orgSlug = computed(() => getOrgSlug());
const eventSlug = computed(() => getEventSlug());
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

interface DashboardRow {
  id: string;
  company_name: string;
  pack: string;
  job_offers: string;
  activities: number;
  qanda: string;
  tickets: number;
  social_links: number;
  communication_plan: number;
  speakers: number;
}

const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref('');
const rows = ref<DashboardRow[]>([]);
const search = ref('');

const filteredRows = computed(() => {
  const query = search.value.trim().toLowerCase();
  if (!query) return rows.value;
  return rows.value.filter((r) => r.company_name.toLowerCase().includes(query));
});

function partnershipPath(partnershipId: string) {
  return `/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors/${partnershipId}`;
}

const linkCell = (id: string, value: string | number, extraClass = '') =>
  h(
    'div',
    {
      onClick: () => navigateTo(partnershipPath(id)),
      class: `cursor-pointer ${extraClass}`,
    },
    String(value),
  );

const columns = [
  {
    header: 'Partenaire',
    accessorKey: 'company_name',
    cell: (info: any) => linkCell(info.row.original.id, info.getValue('company_name'), 'hover:underline font-medium'),
  },
  {
    header: 'Pack',
    accessorKey: 'pack',
    cell: (info: any) => linkCell(info.row.original.id, info.getValue('pack')),
  },
  {
    header: "Offres d'emploi",
    accessorKey: 'job_offers',
    cell: (info: any) => linkCell(info.row.original.id, info.getValue('job_offers')),
  },
  {
    header: 'Activités',
    accessorKey: 'activities',
    cell: (info: any) => linkCell(info.row.original.id, info.getValue('activities')),
  },
  {
    header: 'Q&A (questions/réponses)',
    accessorKey: 'qanda',
    cell: (info: any) => linkCell(info.row.original.id, info.getValue('qanda')),
  },
  {
    header: 'Billets',
    accessorKey: 'tickets',
    cell: (info: any) => linkCell(info.row.original.id, info.getValue('tickets')),
  },
  {
    header: 'Réseaux sociaux',
    accessorKey: 'social_links',
    cell: (info: any) => linkCell(info.row.original.id, info.getValue('social_links')),
  },
  {
    header: 'Plan de comm.',
    accessorKey: 'communication_plan',
    cell: (info: any) => linkCell(info.row.original.id, info.getValue('communication_plan')),
  },
  {
    header: 'Speakers',
    accessorKey: 'speakers',
    cell: (info: any) => linkCell(info.row.original.id, info.getValue('speakers')),
  },
];

function packLabel(p: PartnerStatsSchema['partnership']): string {
  if (p.selected_pack_name) return p.selected_pack_name;
  if (p.suggested_pack_name) return `${p.suggested_pack_name} (suggéré)`;
  return '-';
}

async function loadData() {
  try {
    loading.value = true;
    error.value = null;

    const [eventResponse, statsResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getEventStats(orgSlug.value, eventSlug.value),
    ]);

    eventName.value = eventResponse.data.event.name;

    rows.value = statsResponse.data.partners.map((s) => ({
      id: s.partnership.id,
      company_name: s.partnership.company_name,
      pack: packLabel(s.partnership),
      job_offers: `${s.job_offers.validated} / ${s.job_offers.total}`,
      activities: s.activities,
      qanda: `${s.qanda.questions} / ${s.qanda.answers}`,
      tickets: s.tickets,
      social_links: s.social_links,
      communication_plan: s.communication_plan,
      speakers: s.speakers,
    }));
  } catch (err) {
    console.error(err);
    error.value = 'Impossible de charger le dashboard';
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
watch([orgSlug, eventSlug], loadData);

useHead({
  title: computed(() => `Dashboard — ${eventName.value || 'Événement'} | DevLille`),
});
</script>
