<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/packs`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Créer un pack - {{ eventName }}</h1>
        </div>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
        {{ error }}
      </div>

      <div v-else-if="success" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4">
        Pack créé avec succès ! Redirection en cours...
      </div>

      <SponsoringPackForm
        v-else
        :data="initialData"
        :options="options"
        @save="onSave"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventBySlug, postOrgsEventsPacks, getOrgsEventsOptions, postOrgsEventsPacksOptions, type CreateSponsoringPack, type SponsoringOption } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const router = useRouter();
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

const error = ref<string | null>(null);
const success = ref(false);
const loading = ref(true);
const eventName = ref<string>('');
const options = ref<SponsoringOption[]>([]);

// Menu contextuel pour la page de création de pack
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Données initiales pour le formulaire vide
const initialData: Partial<CreateSponsoringPack> = {
  name: '',
  price: 0,
  with_booth: false,
  nb_tickets: 0,
  max_quantity: undefined
};

async function loadData() {
  try {
    loading.value = true;
    const [eventResponse, optionsResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsOptions(orgSlug.value, eventSlug.value)
    ]);
    eventName.value = eventResponse.data.event.name;
    options.value = optionsResponse.data;
  } catch (err) {
    console.error('Failed to load data:', err);
    error.value = 'Impossible de charger les informations';
  } finally {
    loading.value = false;
  }
}

async function onSave(data: { pack: CreateSponsoringPack; requiredOptions: string[]; optionalOptions: string[] }) {
  try {
    error.value = null;
    success.value = false;

    // Créer le pack
    const packResponse = await postOrgsEventsPacks(orgSlug.value, eventSlug.value, data.pack);
    const packId = packResponse.data.id;

    // Attacher les options au pack
    if (data.requiredOptions.length > 0 || data.optionalOptions.length > 0) {
      await postOrgsEventsPacksOptions(orgSlug.value, eventSlug.value, packId, {
        required: data.requiredOptions,
        optional: data.optionalOptions
      });
    }

    success.value = true;

    // Rediriger vers la liste des packs après 1 seconde
    setTimeout(() => {
      router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/packs`);
    }, 1000);
  } catch (err) {
    console.error('Failed to create pack:', err);
    error.value = 'Impossible de créer le pack. Vérifiez les données du formulaire.';
  }
}

onMounted(() => {
  loadData();
});

useHead({
  title: computed(() => `Créer un pack - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
