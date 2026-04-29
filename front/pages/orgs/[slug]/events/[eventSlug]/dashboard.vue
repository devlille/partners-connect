<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div>
        <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
        <PageTitle>Dashboard — {{ eventName }}</PageTitle>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="10" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <UTable v-else :data="rows" :columns="columns" />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import {
  getOrgsEventsPartnership,
  getEventBySlug,
  getPartnershipJobOffers,
  listBoothActivities,
  listEventQandaQuestions,
  type PartnershipItemSchema,
  type BoothActivityResponseSchema,
} from '~/utils/api';
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
  job_offers_count: number;
  activities_count: number;
  scanzee_count: number;
}

const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref('');
const rows = ref<DashboardRow[]>([]);

const columns = [
  { header: 'Partenaire', accessorKey: 'company_name' },
  { header: "Offres d'emploi", accessorKey: 'job_offers_count' },
  { header: 'Activités', accessorKey: 'activities_count' },
  { header: 'Questions Scanzee', accessorKey: 'scanzee_count' },
];

async function loadData() {
  try {
    loading.value = true;
    error.value = null;

    const [eventResponse, partnershipsResponse, qandaResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsPartnership(orgSlug.value, eventSlug.value, { page: 1, page_size: 100 }),
      listEventQandaQuestions(eventSlug.value),
    ]);

    eventName.value = eventResponse.data.event.name;

    const partnerships = (partnershipsResponse.data as any).items as PartnershipItemSchema[];

    const qandaByPartnership = new Map<string, number>();
    for (const summary of qandaResponse.data) {
      qandaByPartnership.set(summary.partnership_id, summary.questions.length);
    }

    rows.value = await Promise.all(
      partnerships.map(async (p) => {
        const [jobOffersRes, activitiesRes] = await Promise.all([
          getPartnershipJobOffers(eventSlug.value, p.id, { page: 1, page_size: 1 }),
          listBoothActivities(eventSlug.value, p.id),
        ]);
        return {
          id: p.id,
          company_name: p.company_name,
          job_offers_count: jobOffersRes.data.total,
          activities_count: (activitiesRes.data as BoothActivityResponseSchema[]).length,
          scanzee_count: qandaByPartnership.get(p.id) ?? 0,
        };
      })
    );
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
