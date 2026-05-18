<template>
  <Dashboard :main-links="partnerLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton
            :to="`/orgs/${orgSlug}/events/${eventSlug}/partners/${partnerId}`"
            label="Retour"
          />
          <PageTitle>Liens externes - {{ partnerName }}</PageTitle>
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

        <TableSkeleton v-if="loadingEvent" :columns="2" :rows="6" />

        <div v-else-if="eventError" class="px-6 py-4 bg-red-50 border border-red-200 text-red-700">
          {{ eventError }}
        </div>

        <div v-else-if="externalLinks.length === 0" class="px-6 py-12 text-center">
          <h3 class="text-sm font-medium text-gray-900">Aucun lien externe</h3>
          <p class="mt-1 text-sm text-gray-500">
            Aucun lien externe n'a été configuré pour cet événement.
          </p>
        </div>

        <div v-else>
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Nom
                </th>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  URL
                </th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr v-for="link in externalLinks" :key="link.id">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ link.name }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  <a
                    :href="link.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="text-blue-600 hover:text-blue-800 underline"
                  >
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

        <TableSkeleton v-if="loadingProviders" :columns="5" :rows="6" />

        <div
          v-else-if="providersError"
          class="px-6 py-4 bg-red-50 border border-red-200 text-red-700"
        >
          {{ providersError }}
        </div>

        <div v-else-if="providers.length === 0" class="px-6 py-12 text-center">
          <h3 class="text-sm font-medium text-gray-900">Aucun prestataire</h3>
          <p class="mt-1 text-sm text-gray-500">
            Aucun prestataire n'a été configuré pour cet événement.
          </p>
        </div>

        <div v-else>
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Nom
                </th>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Type
                </th>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Site web
                </th>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Email
                </th>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Téléphone
                </th>
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
                  <a
                    v-if="provider.website"
                    :href="provider.website"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="text-blue-600 hover:text-blue-800 underline"
                  >
                    {{ provider.website }}
                  </a>
                  <span v-else class="text-gray-400">-</span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  <a
                    v-if="provider.email"
                    :href="`mailto:${provider.email}`"
                    class="text-blue-600 hover:text-blue-800"
                  >
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
import {
  getCompanyById,
  getEventBySlug,
  getEventsEcosystemPartner,
  getProviders,
  type EventExternalLinkSchema,
  type ProviderSchema,
} from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false,
});

const orgSlug = computed(() => {
  const params = route.params.slug;
  return Array.isArray(params) ? (params[0] as string) : (params as string);
});
const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  return Array.isArray(params) ? (params[1] as string) : (params as string);
});
const partnerId = computed(() => route.params.partnerId as string);

const { partnerLinks } = usePartnerLinks(orgSlug.value, eventSlug.value, partnerId.value);

const partnerName = ref<string>("");
const externalLinks = ref<EventExternalLinkSchema[]>([]);
const loadingEvent = ref(true);
const eventError = ref<string | null>(null);

const providers = ref<ProviderSchema[]>([]);
const loadingProviders = ref(true);
const providersError = ref<string | null>(null);

async function loadPartnerName() {
  try {
    const partnerResponse = await getEventsEcosystemPartner(eventSlug.value, partnerId.value);
    const companyResponse = await getCompanyById(partnerResponse.data.company_id);
    partnerName.value = companyResponse.data.name;
  } catch (err) {
    console.error("Failed to load partner name:", err);
  }
}

async function loadExternalLinks() {
  try {
    loadingEvent.value = true;
    eventError.value = null;
    const response = await getEventBySlug(eventSlug.value);
    externalLinks.value = response.data.event.external_links || [];
  } catch (err) {
    console.error("Failed to load external links:", err);
    eventError.value = "Impossible de charger les liens externes";
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
    console.error("Failed to load providers:", err);
    providersError.value = "Impossible de charger les prestataires";
  } finally {
    loadingProviders.value = false;
  }
}

onMounted(() => {
  loadPartnerName();
  loadExternalLinks();
  loadProviders();
});

useHead({
  title: computed(() => `Liens externes - ${partnerName.value || "Partenaire"} | DevLille`),
});
</script>
