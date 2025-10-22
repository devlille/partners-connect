<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <UButton
            :to="`/orgs/${orgSlug}/events/${eventSlug}/packs`"
            icon="i-heroicons-arrow-left"
            color="neutral"
            variant="ghost"
            class="mb-2"
            label="Retour"
          />
          <h1 class="text-2xl font-bold text-gray-900">Créer un pack - {{ eventName }}</h1>
        </div>
      </div>
    </div>

    <div class="p-6">
      <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
        {{ error }}
      </div>

      <div v-if="success" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4">
        Pack créé avec succès ! Redirection en cours...
      </div>

      <SponsoringPackForm
        :data="initialData"
        @save="onSave"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventBySlug, postOrgsEventsPacks, type CreateSponsoringPack } from "~/utils/api";
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
  const params = route.params.slug;
  return Array.isArray(params) ? params[1] as string : params as string;
});

const error = ref<string | null>(null);
const success = ref(false);
const eventName = ref<string>('');

// Menu contextuel pour la page de création de pack
const eventLinks = computed(() => [
  {
    label: 'Informations',
    icon: 'i-heroicons-information-circle',
    to: `/orgs/${orgSlug.value}/events/${eventSlug.value}`
  },
  {
    label: 'Mes Packs',
    icon: 'i-heroicons-cube',
    to: `/orgs/${orgSlug.value}/events/${eventSlug.value}/packs`
  }
]);

// Données initiales pour le formulaire vide
const initialData: Partial<CreateSponsoringPack> = {
  name: '',
  price: 0,
  with_booth: false,
  nb_tickets: 0,
  max_quantity: undefined
};

async function loadEventName() {
  try {
    const eventResponse = await getEventBySlug(eventSlug.value);
    eventName.value = eventResponse.data.event.name;
  } catch (err) {
    console.error('Failed to load event:', err);
    error.value = 'Impossible de charger les informations de l\'événement';
  }
}

async function onSave(packData: CreateSponsoringPack) {
  try {
    error.value = null;
    success.value = false;

    await postOrgsEventsPacks(orgSlug.value, eventSlug.value, packData);
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
  loadEventName();
});

useHead({
  title: computed(() => `Créer un pack - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
