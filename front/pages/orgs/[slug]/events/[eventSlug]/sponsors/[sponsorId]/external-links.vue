<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Liens externes - {{ sponsorName }}</h1>
        </div>
      </div>
    </div>

    <div class="p-6 space-y-6">
      <!-- External Links Section -->
      <div class="bg-white rounded-lg shadow">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">Liens externes de l'événement</h2>
          <p class="mt-1 text-sm text-gray-500">Liens configurés pour cet événement</p>
        </div>

        <div v-if="loadingEvent" class="flex justify-center py-8">
          <div class="text-gray-500">Chargement...</div>
        </div>

        <div v-else-if="eventError" class="px-6 py-4 bg-red-50 border border-red-200 text-red-700">
          {{ eventError }}
        </div>

        <div v-else-if="externalLinks.length === 0" class="px-6 py-12 text-center">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
          </svg>
          <h3 class="mt-2 text-sm font-medium text-gray-900">Aucun lien externe</h3>
          <p class="mt-1 text-sm text-gray-500">Aucun lien externe n'a été configuré pour cet événement.</p>
        </div>

        <div v-else>
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Nom</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">URL</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr v-for="link in externalLinks" :key="link.id">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ link.name }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  <a :href="link.url" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 underline">
                    {{ link.url }}
                  </a>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Providers Section -->
      <div class="bg-white rounded-lg shadow">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">Prestataires de l'événement</h2>
          <p class="mt-1 text-sm text-gray-500">Prestataires configurés pour cet événement</p>
        </div>

        <div v-if="loadingProviders" class="flex justify-center py-8">
          <div class="text-gray-500">Chargement...</div>
        </div>

        <div v-else-if="providersError" class="px-6 py-4 bg-red-50 border border-red-200 text-red-700">
          {{ providersError }}
        </div>

        <div v-else-if="providers.length === 0" class="px-6 py-12 text-center">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
          <h3 class="mt-2 text-sm font-medium text-gray-900">Aucun prestataire</h3>
          <p class="mt-1 text-sm text-gray-500">Aucun prestataire n'a été configuré pour cet événement.</p>
        </div>

        <div v-else>
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Nom</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Site web</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Téléphone</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr v-for="provider in providers" :key="provider.id">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ provider.name }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ provider.type }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  <a v-if="provider.website" :href="provider.website" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 underline">
                    {{ provider.website }}
                  </a>
                  <span v-else class="text-gray-400">-</span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  <a v-if="provider.email" :href="`mailto:${provider.email}`" class="text-blue-600 hover:text-blue-800">
                    {{ provider.email }}
                  </a>
                  <span v-else class="text-gray-400">-</span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ provider.phone || '-' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventBySlug, getProviders, getOrgsEventsPartnership, type EventExternalLinkSchema, type ProviderSchema } from '~/utils/api';
import authMiddleware from '~/middleware/auth';

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
  return Array.isArray(params) ? params[0] as string : params as string;
});

const sponsorId = computed(() => {
  const params = route.params.sponsorId;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

const sponsorName = ref<string>('');
const externalLinks = ref<EventExternalLinkSchema[]>([]);
const loadingEvent = ref(true);
const eventError = ref<string | null>(null);

const providers = ref<ProviderSchema[]>([]);
const loadingProviders = ref(true);
const providersError = ref<string | null>(null);

async function loadSponsorName() {
  try {
    const response = await getOrgsEventsPartnership(orgSlug.value, eventSlug.value);
    const partnership = response.data.find(p => p.id === sponsorId.value);
    if (partnership) {
      sponsorName.value = partnership.company_name;
    }
  } catch (err) {
    console.error('Failed to load sponsor name:', err);
  }
}

async function loadExternalLinks() {
  try {
    loadingEvent.value = true;
    eventError.value = null;

    const response = await getEventBySlug(eventSlug.value);
    externalLinks.value = response.data.event.external_links || [];
  } catch (err) {
    console.error('Failed to load external links:', err);
    eventError.value = 'Impossible de charger les liens externes';
  } finally {
    loadingEvent.value = false;
  }
}

async function loadProviders() {
  try {
    loadingProviders.value = true;
    providersError.value = null;

    const response = await getProviders();
    providers.value = response.data.items;
  } catch (err) {
    console.error('Failed to load providers:', err);
    providersError.value = 'Impossible de charger les prestataires';
  } finally {
    loadingProviders.value = false;
  }
}

onMounted(() => {
  loadSponsorName();
  loadExternalLinks();
  loadProviders();
});

watch([orgSlug, eventSlug, sponsorId], () => {
  loadSponsorName();
  loadExternalLinks();
  loadProviders();
});

useHead({
  title: computed(() => `Liens externes - ${sponsorName.value || 'Sponsor'} | DevLille`)
});
</script>
