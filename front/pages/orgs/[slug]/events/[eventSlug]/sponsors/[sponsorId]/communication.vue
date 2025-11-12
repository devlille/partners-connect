<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div>
        <PageTitle>{{ partnership?.company_name || 'Sponsor' }}</PageTitle>
        <p class="text-sm text-gray-600 mt-1">Communication</p>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else class="bg-white rounded-lg shadow p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Communication</h2>

        <TableSkeleton v-if="loadingCommunication" :columns="2" :rows="1" />

        <div v-else-if="communicationError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {{ communicationError }}
        </div>

        <div v-else class="space-y-4">
          <div class="bg-gray-50 rounded-lg p-6">
            <div class="flex items-start gap-4">
              <div class="shrink-0">
                <i class="i-heroicons-calendar-days text-3xl text-primary-600" />
              </div>
              <div class="flex-1">
                <h3 class="text-sm font-medium text-gray-900 mb-1">Date de communication prévue</h3>
                <p v-if="communicationData?.publication_date && communicationData.publication_date !== null" class="text-2xl font-semibold text-gray-900">
                  {{ formatDateSafe(communicationData.publication_date) }}
                </p>
                <p v-else class="text-lg text-gray-500 italic">
                  Aucune date planifiée
                </p>
                <p class="text-sm text-gray-600 mt-2">
                  Statut:
                  <span v-if="!communicationData?.publication_date || communicationData.publication_date === null" class="text-orange-600 font-medium">Non planifié</span>
                  <span v-else-if="isDatePassed(communicationData.publication_date)" class="text-green-600 font-medium">Effectuée</span>
                  <span v-else class="text-blue-600 font-medium">Planifiée</span>
                </p>
              </div>
            </div>
          </div>

          <div v-if="communicationData?.support_url" class="bg-gray-50 rounded-lg p-6">
            <div class="flex items-start gap-4">
              <div class="shrink-0">
                <i class="i-heroicons-photo text-3xl text-primary-600" />
              </div>
              <div class="flex-1">
                <h3 class="text-sm font-medium text-gray-900 mb-2">Support visuel</h3>
                <a
                  :href="communicationData.support_url"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="inline-flex items-center gap-2 text-primary-600 hover:text-primary-800 font-medium"
                >
                  Voir le support
                  <i class="i-heroicons-arrow-top-right-on-square" />
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership, getOrgsEventsCommunication, type CommunicationItemSchema } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type { ExtendedPartnershipItem } from "~/types/partnership";

const route = useRoute();
const { footerLinks } = useDashboardLinks();
const { formatDate } = useFormatters();

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

const communicationData = ref<CommunicationItemSchema | null>(null);
const loadingCommunication = ref(false);
const communicationError = ref<string | null>(null);

// Menu contextuel pour la page du sponsor
const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

async function loadPartnership() {
  try {
    loading.value = true;
    error.value = null;

    const response = await getOrgsEventsPartnership(orgSlug.value, eventSlug.value);
    const found = response.data.find(p => p.id === sponsorId.value);

    if (!found) {
      error.value = 'Sponsor non trouvé';
      return;
    }

    partnership.value = found;

    await loadCommunicationInfo();
  } catch (err) {
    console.error('Failed to load partnership:', err);
    error.value = 'Impossible de charger les informations du sponsor';
  } finally {
    loading.value = false;
  }
}

async function loadCommunicationInfo() {
  try {
    loadingCommunication.value = true;
    communicationError.value = null;

    const communicationPlan = await getOrgsEventsCommunication(orgSlug.value, eventSlug.value);

    const allCommunications = [
      ...communicationPlan.data.done,
      ...communicationPlan.data.planned,
      ...communicationPlan.data.unplanned
    ];

    const found = allCommunications.find(c => c.partnership_id === sponsorId.value);

    if (found) {
      communicationData.value = found;
    } else {
      communicationData.value = {
        partnership_id: sponsorId.value,
        company_name: partnership.value?.company_name || '',
        publication_date: null,
        support_url: null
      };
    }
  } catch (err) {
    console.error('Failed to load communication info:', err);
    communicationError.value = 'Impossible de charger les informations de communication';
  } finally {
    loadingCommunication.value = false;
  }
}

function formatDateSafe(dateString: string | null | undefined): string {
  if (!dateString) return 'Date invalide';

  try {
    const formatted = formatDate(dateString, 'long');
    if (!formatted) {
      return new Date(dateString).toLocaleDateString('fr-FR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    }
    return formatted;
  } catch {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
}

function isDatePassed(dateString: string): boolean {
  try {
    const date = new Date(dateString);
    const now = new Date();
    return date < now;
  } catch {
    return false;
  }
}

onMounted(() => {
  loadPartnership();
});

watch([orgSlug, eventSlug, sponsorId], () => {
  loadPartnership();
});

useHead({
  title: computed(() => `Communication - ${partnership.value?.company_name || 'Sponsor'} | DevLille`)
});
</script>
