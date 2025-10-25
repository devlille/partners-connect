<template>
  <Dashboard :main-links="orgLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <UButton
            :to="`/orgs/${orgSlug}/events`"
            icon="i-heroicons-arrow-left"
            color="neutral"
            variant="ghost"
            class="mb-2"
            label="Retour"
          />
          <h1 class="text-2xl font-bold text-gray-900">{{ event?.name }} - {{ organisationName }}</h1>
        </div>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else-if="event">
        <EventForm
          :data="event"
          @save="onSave"
        />
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventBySlug, putOrgsEvents, type EventDisplay } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

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

const event = ref<EventDisplay | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);
const organisationName = ref<string>('');

// Menu contextuel pour la page de détail d'événement
const { eventLinks: orgLinks } = useEventLinks(orgSlug.value, eventSlug.value);

async function loadEvent() {
  try {
    loading.value = true;
    error.value = null;

    const response = await getEventBySlug(eventSlug.value);
    event.value = response.data.event;
    organisationName.value = response.data.organisation.name;
  } catch (err) {
    console.error('Failed to load event:', err);
    error.value = 'Impossible de charger les informations de l\'événement';
  } finally {
    loading.value = false;
  }
}

async function onSave(updatedEvent: Omit<EventDisplay, 'slug'>) {
  try {
    error.value = null;

    // Convertir EventDisplay en EventSchema pour l'API
    const eventPayload = {
      name: updatedEvent.name,
      start_time: updatedEvent.start_time,
      end_time: updatedEvent.end_time,
      submission_start_time: updatedEvent.submission_start_time,
      submission_end_time: updatedEvent.submission_end_time,
      address: updatedEvent.address,
      contact: updatedEvent.contact,
      external_links: updatedEvent.external_links,
      providers: updatedEvent.providers
    };

    await putOrgsEvents(orgSlug.value, eventSlug.value, eventPayload);

    // Recharger les données après la mise à jour
    await loadEvent();

    // Afficher un message de succès (vous pouvez ajouter un toast ici)
    console.log('Événement mis à jour avec succès');
  } catch (err) {
    console.error('Failed to update event:', err);
    error.value = 'Impossible de mettre à jour l\'événement';
  }
}

onMounted(() => {
  loadEvent();
});

// Recharger si le slug change
watch([orgSlug, eventSlug], () => {
  loadEvent();
});

useHead({
  title: computed(() => `${event.value?.name || 'Événement'} - ${organisationName.value || 'Organisation'} | DevLille`)
});
</script>
