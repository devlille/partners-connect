<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <PageTitle>Agenda</PageTitle>
          <p class="mt-1 text-sm text-gray-500">Speakers importés depuis l'intégration agenda</p>
        </div>
        <UButton
          color="primary"
          size="lg"
          icon="i-heroicons-arrow-path"
          :loading="syncing"
          :disabled="!hasAgendaIntegration"
          @click="handleSync"
        >
          Synchroniser l'agenda
        </UButton>
      </div>
    </div>

    <div class="p-6">
      <div
        v-if="syncSuccess"
        class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-6 text-sm"
      >
        Agenda synchronisé avec succès.
      </div>

      <div
        v-if="error"
        class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-6 text-sm"
      >
        {{ error }}
      </div>

      <TableSkeleton v-if="loading" :columns="3" :rows="6" />

      <div v-else-if="speakers.length === 0" class="text-center py-12">
        <i class="i-heroicons-user-group text-gray-400 text-4xl mb-2" aria-hidden="true" />
        <h3 class="mt-2 text-sm font-medium text-gray-900">Aucun speaker</h3>
        <p class="mt-1 text-sm text-gray-500">
          Synchronisez l'agenda pour importer les speakers depuis votre intégration.
        </p>
      </div>

      <div v-else class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
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
            <tr v-for="speaker in speakers" :key="speaker.id" class="hover:bg-gray-50">
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
import { getEventAgendaPublic, updateEventAgenda, getOrgsEventsIntegrations, type SpeakerSchema } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

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

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

const speakers = ref<SpeakerSchema[]>([]);
const loading = ref(false);
const syncing = ref(false);
const error = ref<string | null>(null);
const syncSuccess = ref(false);
const hasAgendaIntegration = ref(false);

async function loadAgenda() {
  try {
    loading.value = true;
    error.value = null;
    const response = await getEventAgendaPublic(eventSlug.value);
    speakers.value = response.data.speakers.sort((a, b) => a.name.localeCompare(b.name));
  } catch {
    speakers.value = [];
  } finally {
    loading.value = false;
  }
}

async function handleSync() {
  try {
    syncing.value = true;
    error.value = null;
    syncSuccess.value = false;
    await updateEventAgenda(orgSlug.value, eventSlug.value);
    syncSuccess.value = true;
    await loadAgenda();
  } catch {
    error.value = "Impossible de synchroniser l'agenda. Vérifiez la configuration de l'intégration.";
  } finally {
    syncing.value = false;
  }
}

onMounted(async () => {
  loadAgenda();
  try {
    const response = await getOrgsEventsIntegrations(orgSlug.value, eventSlug.value);
    hasAgendaIntegration.value = response.data.some(
      (integration) => integration.usage === "agenda",
    );
  } catch {
    hasAgendaIntegration.value = false;
  }
});
</script>
