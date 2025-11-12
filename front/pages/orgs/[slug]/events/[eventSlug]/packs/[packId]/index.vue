<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/packs`" label="Retour" />
          <PageTitle>{{ pack?.name }} - {{ eventName }}</PageTitle>
        </div>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else-if="pack">
        <SponsoringPackForm
          :data="packFormData"
          :options="options"
          :initial-required-options="initialRequiredOptions"
          :initial-optional-options="initialOptionalOptions"
          @save="onSave"
        />
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventBySlug, getOrgsEventsPacks, putOrgsEventsPacks, getOrgsEventsOptions, postOrgsEventsPacksOptions, type SponsoringPack, type CreateSponsoringPack, type SponsoringOption } from "~/utils/api";
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
const packId = computed(() => route.params.packId as string);

const pack = ref<SponsoringPack | null>(null);
const options = ref<SponsoringOption[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Menu contextuel pour la page d'édition de pack
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Options initiales sélectionnées
const initialRequiredOptions = computed(() => {
  return pack.value?.required_options.map(o => o.id) || [];
});

const initialOptionalOptions = computed(() => {
  return pack.value?.optional_options.map(o => o.id) || [];
});

// Convertir SponsoringPack en CreateSponsoringPack pour le formulaire
const packFormData = computed((): Partial<CreateSponsoringPack> => {
  if (!pack.value) return {};

  // Extraire les informations du pack
  const hasStand = pack.value.required_options.some(o => o.name === 'Stand');

  // Note: Le nombre de billets n'est pas directement disponible dans SponsoringOptionSchema
  // Il faudra peut-être ajuster selon la structure réelle de vos données
  // Pour l'instant, on initialise à 0
  const nb_tickets = 0;

  return {
    name: pack.value.name,
    price: pack.value.base_price,
    with_booth: hasStand,
    nb_tickets: nb_tickets,
    max_quantity: pack.value.max_quantity || undefined
  };
});

async function loadPack() {
  try {
    loading.value = true;
    error.value = null;

    // Charger toutes les données en parallèle
    const [eventResponse, packsResponse, optionsResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsPacks(orgSlug.value, eventSlug.value),
      getOrgsEventsOptions(orgSlug.value, eventSlug.value)
    ]);

    eventName.value = eventResponse.data.event.name;
    options.value = optionsResponse.data;

    // Trouver le pack correspondant
    const foundPack = packsResponse.data.find(p => p.id === packId.value);

    if (!foundPack) {
      error.value = 'Pack non trouvé';
      return;
    }

    pack.value = foundPack;
  } catch (err) {
    console.error('Failed to load pack:', err);
    error.value = 'Impossible de charger les informations du pack';
  } finally {
    loading.value = false;
  }
}

async function onSave(data: { pack: CreateSponsoringPack; requiredOptions: string[]; optionalOptions: string[] }) {
  try {
    error.value = null;

    // Mettre à jour le pack
    await putOrgsEventsPacks(orgSlug.value, eventSlug.value, packId.value, data.pack);

    // Mettre à jour les options associées
    if (data.requiredOptions.length > 0 || data.optionalOptions.length > 0) {
      await postOrgsEventsPacksOptions(orgSlug.value, eventSlug.value, packId.value, {
        required: data.requiredOptions,
        optional: data.optionalOptions
      });
    }

    // Recharger les données après la mise à jour
    await loadPack();

    // Afficher un message de succès (vous pouvez ajouter un toast ici)
    console.log('Pack mis à jour avec succès');

    // Rediriger vers la liste des packs
    setTimeout(() => {
      router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/packs`);
    }, 500);
  } catch (err) {
    console.error('Failed to update pack:', err);
    error.value = 'Impossible de mettre à jour le pack';
  }
}

onMounted(() => {
  loadPack();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug, packId], () => {
  loadPack();
});

useHead({
  title: computed(() => `${pack.value?.name || 'Pack'} - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
