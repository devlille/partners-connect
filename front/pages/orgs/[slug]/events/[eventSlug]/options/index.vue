<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Options de sponsoring - {{ eventName }}</h1>
        </div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/options/create`"
          label="Créer une option"
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
        :data="optionsFormatted"
        @select="onSelectOption"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsOptions, getEventBySlug, type SponsoringOption } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type { TableRow } from "@nuxt/ui";

const route = useRoute();
const router = useRouter();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const orgSlug = computed(() => route.params.slug as string);
const eventSlug = computed(() => route.params.eventSlug as string);

const options = ref<SponsoringOption[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Menu contextuel pour la page des options
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);



// Type pour les lignes du tableau
type OptionTableRow = {
  id: string;
  name: string;
  description: string;
  price: string;
  _original: SponsoringOption;
};

// Formater les données pour le tableau
const optionsFormatted = computed(() => {
  return options.value.map(option => ({
    id: option.id,
    name: option.name,
    description: option.description || '-',
    price: option.price ? `${option.price}€` : 'Gratuit',
    _original: option
  }));
});

const onSelectOption = (row: TableRow<OptionTableRow>) => {
  const option = row.original._original;
  router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/options/${option.id}`);
};

async function loadOptions() {
  try {
    loading.value = true;
    error.value = null;

    // Charger le nom de l'événement
    const eventResponse = await getEventBySlug(eventSlug.value);
    eventName.value = eventResponse.data.event.name;

    // Charger les options
    const response = await getOrgsEventsOptions(orgSlug.value, eventSlug.value);
    options.value = response.data;
  } catch (err) {
    console.error('Failed to load options:', err);
    error.value = 'Impossible de charger les options';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadOptions();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug], () => {
  loadOptions();
});

useHead({
  title: computed(() => `Options de sponsoring - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
