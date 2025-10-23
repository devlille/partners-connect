<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/options`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Créer une option - {{ eventName }}</h1>
        </div>
      </div>
    </div>

    <div class="p-6">
      <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
        {{ error }}
      </div>

      <div v-if="success" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4">
        Option créée avec succès ! Redirection en cours...
      </div>

      <SponsoringOptionForm
        :data="initialData"
        :packs="packs"
        @save="onSave"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventBySlug, getOrgsEventsPacks, postOrgsEventsOptions, postOrgsEventsPacksOptions, type SponsoringPack } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const router = useRouter();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const orgSlug = computed(() => route.params.slug as string);
const eventSlug = computed(() => route.params.eventSlug as string);

const error = ref<string | null>(null);
const success = ref(false);
const eventName = ref<string>('');
const packs = ref<SponsoringPack[]>([]);

// Menu contextuel pour la page de création d'option
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Données initiales pour le formulaire vide
const initialData = {
  name: '',
  description: '',
  price: undefined,
  selectedPacks: []
};

async function loadEventData() {
  try {
    const eventResponse = await getEventBySlug(eventSlug.value);
    eventName.value = eventResponse.data.event.name;

    // Charger les packs disponibles
    const packsResponse = await getOrgsEventsPacks(orgSlug.value, eventSlug.value);
    packs.value = packsResponse.data;
  } catch (err) {
    console.error('Failed to load event data:', err);
    error.value = 'Impossible de charger les informations de l\'événement';
  }
}

async function onSave(data: { option: any; selectedPacks: string[] }) {
  try {
    error.value = null;
    success.value = false;

    // Créer l'option
    const optionResponse = await postOrgsEventsOptions(orgSlug.value, eventSlug.value, data.option);
    const optionId = optionResponse.data.id;

    // Associer l'option aux packs sélectionnés
    if (data.selectedPacks.length > 0) {
      await Promise.all(
        data.selectedPacks.map(packId =>
          postOrgsEventsPacksOptions(orgSlug.value, eventSlug.value, packId, {
            required: [optionId],
            optional: []
          })
        )
      );
    }

    success.value = true;

    // Rediriger vers la liste des options après 1 seconde
    setTimeout(() => {
      router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/options`);
    }, 1000);
  } catch (err) {
    console.error('Failed to create option:', err);
    error.value = 'Impossible de créer l\'option. Vérifiez les données du formulaire.';
  }
}

onMounted(() => {
  loadEventData();
});

useHead({
  title: computed(() => `Créer une option - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
