<template>
  <Dashboard :main-links="orgLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${slug}/events`" label="Retour" />
          <PageTitle>Créer un événement - {{ organisationName }}</PageTitle>
        </div>
      </div>
    </div>

    <div class="p-6">
      <AlertMessage v-if="error" type="error" :message="error" class="mb-4" />

      <AlertMessage 
        v-if="success" 
        type="success" 
        message="Événement créé avec succès ! Redirection en cours..." 
        class="mb-4"
      />

      <EventForm
        :data="initialData"
        @save="onSave"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgs, postOrgsEvents, type EventDisplay } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const router = useRouter();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const slug = computed(() => route.params.slug as string);
const error = ref<string | null>(null);
const success = ref(false);
const organisationName = ref<string>('');

// Menu contextuel pour la page de création d'événement
const orgLinks = computed(() => [
  {
    label: 'Informations',
    icon: 'i-heroicons-information-circle',
    to: `/orgs/${slug.value}`
  },
  {
    label: 'Événements',
    icon: 'i-heroicons-calendar',
    to: `/orgs/${slug.value}/events`
  },
  {
    label: 'Utilisateurs',
    icon: 'i-heroicons-users',
    to: `/orgs/${slug.value}/users`
  }
]);

// Données initiales pour le formulaire vide
const initialData: Omit<EventDisplay, 'slug'> = {
  name: '',
  start_time: '',
  end_time: '',
  submission_start_time: '',
  submission_end_time: '',
  address: '',
  contact: {
    email: '',
    phone: null
  },
  external_links: [],
  providers: {
    cfp: null,
    communication: null
  }
};

async function loadOrganisation() {
  try {
    const orgResponse = await getOrgs(slug.value);
    organisationName.value = orgResponse.data.name;
  } catch (err) {
    console.error('Failed to load organization:', err);
    error.value = 'Impossible de charger les informations de l\'organisation';
  }
}

async function onSave(eventData: Omit<EventDisplay, 'slug'>) {
  try {
    error.value = null;
    success.value = false;

    // Convertir EventDisplay en EventSchema pour l'API
    const eventPayload = {
      name: eventData.name,
      start_time: eventData.start_time,
      end_time: eventData.end_time,
      submission_start_time: eventData.submission_start_time,
      submission_end_time: eventData.submission_end_time,
      address: eventData.address,
      contact: eventData.contact,
      external_links: eventData.external_links,
      providers: eventData.providers
    };

    const response = await postOrgsEvents(slug.value, eventPayload);
    success.value = true;

    // Rediriger vers la liste des événements après 1 seconde
    setTimeout(() => {
      router.push(`/orgs/${slug.value}/events`);
    }, 1000);
  } catch (err) {
    console.error('Failed to create event:', err);
    error.value = 'Impossible de créer l\'événement. Vérifiez les données du formulaire.';
  }
}

onMounted(() => {
  loadOrganisation();
});

useHead({
  title: computed(() => `Créer un événement - ${organisationName.value || 'Organisation'} | DevLille`)
});
</script>
