<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/packs`" label="Retour" />
          <PageTitle>Créer une catégorie de partenaires - {{ eventName }}</PageTitle>
        </div>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="2" :rows="3" />

      <div
        v-else-if="error"
        class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4"
      >
        {{ error }}
      </div>

      <div
        v-else-if="success"
        class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4"
      >
        Catégorie créée avec succès ! Redirection en cours...
      </div>

      <form
        v-else
        class="bg-white rounded-lg shadow p-6 space-y-4 max-w-2xl"
        @submit.prevent="onSave"
      >
        <div>
          <label for="name" class="block text-sm font-medium text-gray-700 mb-1">Nom *</label>
          <UInput id="name" v-model="form.name" required placeholder="ex. Communauté" />
        </div>
        <div>
          <label for="display_order" class="block text-sm font-medium text-gray-700 mb-1">
            Ordre d'affichage
          </label>
          <UInput
            id="display_order"
            v-model.number="form.display_order"
            type="number"
            :min="0"
            placeholder="0"
          />
          <p class="text-xs text-gray-500 mt-1">
            Plus la valeur est petite, plus la catégorie apparaît tôt sur la page publique.
          </p>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <UButton
            type="button"
            color="neutral"
            variant="ghost"
            :to="`/orgs/${orgSlug}/events/${eventSlug}/packs`"
          >
            Annuler
          </UButton>
          <UButton type="submit" color="primary" :loading="saving">Créer</UButton>
        </div>
      </form>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import {
  getEventBySlug,
  postOrgsEventsEcosystemPartnerCategory,
  type RegisterEcosystemPartnerCategory,
} from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const router = useRouter();
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

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

const loading = ref(true);
const saving = ref(false);
const success = ref(false);
const error = ref<string | null>(null);
const eventName = ref<string>("");

const form = reactive<RegisterEcosystemPartnerCategory>({
  name: "",
  display_order: undefined,
});

async function loadEvent() {
  try {
    loading.value = true;
    const eventResponse = await getEventBySlug(eventSlug.value);
    eventName.value = eventResponse.data.event.name;
  } catch (err) {
    console.error("Failed to load event:", err);
    error.value = "Impossible de charger l'événement.";
  } finally {
    loading.value = false;
  }
}

async function onSave() {
  try {
    error.value = null;
    saving.value = true;
    await postOrgsEventsEcosystemPartnerCategory(orgSlug.value, eventSlug.value, {
      name: form.name,
      display_order:
        form.display_order === undefined || (form.display_order as unknown) === ""
          ? undefined
          : Number(form.display_order),
    });
    success.value = true;
    setTimeout(() => {
      router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/packs`);
    }, 800);
  } catch (err) {
    console.error("Failed to create category:", err);
    error.value =
      "Impossible de créer la catégorie. Le nom est peut-être déjà utilisé sur cet événement.";
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadEvent();
});

useHead({
  title: computed(
    () => `Créer une catégorie - ${eventName.value || "Événement"} | DevLille`,
  ),
});
</script>
