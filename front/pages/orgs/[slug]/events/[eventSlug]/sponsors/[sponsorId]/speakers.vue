<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div>
        <PageTitle>{{ partnership?.company_name || 'Sponsor' }}</PageTitle>
        <p class="text-sm text-gray-600 mt-1">Speakers associés au partenariat</p>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else-if="allSpeakers.length === 0" class="text-center py-12">
        <i class="i-heroicons-microphone text-gray-400 text-4xl mb-2" aria-hidden="true" />
        <h3 class="mt-2 text-sm font-medium text-gray-900">Aucun speaker disponible</h3>
        <p class="mt-1 text-sm text-gray-500">
          Synchronisez l'agenda pour importer les speakers depuis votre intégration.
        </p>
      </div>

      <div v-else class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12">
                Sélection
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Photo
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Nom
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Entreprise
              </th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="speaker in allSpeakers" :key="speaker.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">
                <input
                  type="checkbox"
                  :checked="isAttached(speaker.id)"
                  :disabled="togglingIds.has(speaker.id)"
                  class="h-4 w-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                  @change="toggleSpeaker(speaker.id)"
                />
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <img
                  v-if="speaker.photo_url"
                  :src="speaker.photo_url"
                  :alt="speaker.name"
                  class="h-10 w-10 rounded-full object-cover"
                />
                <div
                  v-else
                  class="h-10 w-10 rounded-full bg-gray-200 flex items-center justify-center"
                >
                  <i class="i-heroicons-user text-gray-400" aria-hidden="true" />
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                {{ speaker.name }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ speaker.company || '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import {
  getEventsPartnershipDetailed,
  getEventAgendaPublic,
  attachSpeakerToPartnership,
  detachSpeakerFromPartnership,
  type SpeakerSchema,
} from '~/utils/api';
import authMiddleware from '~/middleware/auth';

const route = useRoute();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false,
});

const orgSlug = computed(() => {
  const params = route.params.slug;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const sponsorId = computed(() => {
  const params = route.params.sponsorId;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const partnership = ref<{ company_name: string } | null>(null);
const allSpeakers = ref<SpeakerSchema[]>([]);
const attachedSpeakerIds = ref<Set<string>>(new Set());
const togglingIds = ref<Set<string>>(new Set());
const loading = ref(true);
const error = ref<string | null>(null);

const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

function isAttached(speakerId: string): boolean {
  return attachedSpeakerIds.value.has(speakerId);
}

async function toggleSpeaker(speakerId: string) {
  togglingIds.value.add(speakerId);
  try {
    if (isAttached(speakerId)) {
      await detachSpeakerFromPartnership(eventSlug.value, sponsorId.value, speakerId);
      attachedSpeakerIds.value.delete(speakerId);
      attachedSpeakerIds.value = new Set(attachedSpeakerIds.value);
    } else {
      await attachSpeakerToPartnership(eventSlug.value, sponsorId.value, speakerId);
      attachedSpeakerIds.value.add(speakerId);
      attachedSpeakerIds.value = new Set(attachedSpeakerIds.value);
    }
  } catch {
    error.value = 'Impossible de modifier l\'association du speaker.';
  } finally {
    togglingIds.value.delete(speakerId);
    togglingIds.value = new Set(togglingIds.value);
  }
}

async function loadData() {
  try {
    loading.value = true;
    error.value = null;

    const [partnershipRes, agendaRes] = await Promise.all([
      getEventsPartnershipDetailed(eventSlug.value, sponsorId.value),
      getEventAgendaPublic(eventSlug.value),
    ]);

    partnership.value = { company_name: partnershipRes.data.company.name };
    allSpeakers.value = agendaRes.data.speakers.sort((a, b) => a.name.localeCompare(b.name));

    const attached = partnershipRes.data.speakers ?? [];
    attachedSpeakerIds.value = new Set(attached.map((s) => s.id));
  } catch {
    error.value = 'Impossible de charger les données.';
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);

useHead({
  title: computed(() => `Speakers - ${partnership.value?.company_name || 'Sponsor'} | DevLille`),
});
</script>
