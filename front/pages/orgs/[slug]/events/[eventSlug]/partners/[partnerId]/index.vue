<template>
  <Dashboard :main-links="partnerLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <PageTitle>{{ company?.name || 'Partenaire' }}</PageTitle>
          <p class="text-sm text-gray-600 mt-1">Partenariat</p>
        </div>
      </div>
    </div>

    <div class="p-6 space-y-4">
      <TableSkeleton v-if="loading" :columns="2" :rows="4" />

      <AlertMessage v-else-if="error" type="error" :message="error" />

      <AlertMessage
        v-else-if="success"
        type="success"
        message="Partenariat mis à jour avec succès."
      />

      <form
        v-if="!loading && !error"
        class="bg-white rounded-lg shadow p-6 space-y-4 max-w-2xl"
        @submit.prevent="onSave"
      >
        <div>
          <label for="category" class="block text-sm font-medium text-gray-700 mb-1">
            Catégorie *
          </label>
          <select
            id="category"
            v-model="form.category_id"
            required
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </div>

        <div>
          <label for="language" class="block text-sm font-medium text-gray-700 mb-1">
            Langue *
          </label>
          <select
            id="language"
            v-model="form.language"
            required
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="fr">Français</option>
            <option value="en">English</option>
          </select>
        </div>

        <div>
          <label for="emails" class="block text-sm font-medium text-gray-700 mb-1">
            Emails de contact
          </label>
          <UInput id="emails" v-model="emailsInput" />
          <p class="text-xs text-gray-500 mt-1">Séparez plusieurs emails par une virgule.</p>
        </div>

        <div class="text-xs text-gray-500 border-t pt-3">
          <p v-if="partner?.validated_at">Validé le {{ formatDate(partner.validated_at) }}</p>
          <p v-else-if="partner?.declined_at">Refusé le {{ formatDate(partner.declined_at) }}</p>
        </div>

        <div class="flex justify-end gap-3 pt-2">
          <UButton type="submit" color="primary" :loading="saving">Enregistrer</UButton>
        </div>
      </form>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import {
  getCompanyById,
  getEventsEcosystemPartner,
  getOrgsEventsEcosystemPartnerCategories,
  postOrgsEventsEcosystemPartnerDecline,
  postOrgsEventsEcosystemPartnerValidate,
  putEventsEcosystemPartner,
  type Company,
  type EcosystemPartner,
  type EcosystemPartnerCategory,
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

const loading = ref(true);
const saving = ref(false);
const success = ref(false);
const error = ref<string | null>(null);
const partner = ref<EcosystemPartner | null>(null);
const company = ref<Company | null>(null);
const categories = ref<EcosystemPartnerCategory[]>([]);

const form = reactive({
  category_id: "",
  language: "fr",
});
const emailsInput = ref<string>("");

function parseEmails(): string[] {
  return emailsInput.value
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString("fr-FR");
  } catch {
    return iso;
  }
}

async function loadData() {
  try {
    loading.value = true;
    error.value = null;
    const [partnerResponse, categoriesResponse] = await Promise.all([
      getEventsEcosystemPartner(eventSlug.value, partnerId.value),
      getOrgsEventsEcosystemPartnerCategories(orgSlug.value, eventSlug.value),
    ]);
    partner.value = partnerResponse.data;
    categories.value = categoriesResponse.data;

    form.category_id = partner.value.category.id;
    form.language = partner.value.language;
    emailsInput.value = (partner.value.emails ?? []).join(", ");

    const companyResponse = await getCompanyById(partner.value.company_id);
    company.value = companyResponse.data;
  } catch (err) {
    console.error("Failed to load partner:", err);
    error.value = "Impossible de charger le partenaire.";
  } finally {
    loading.value = false;
  }
}

async function onSave() {
  if (!partner.value) return;
  try {
    saving.value = true;
    error.value = null;
    success.value = false;

    // Edits to a validated row are locked at the API level. We implicitly cycle
    // decline -> PUT -> validate so the organiser sees a single "Save" action.
    const wasValidated = partner.value.validated_at != null;
    if (wasValidated) {
      await postOrgsEventsEcosystemPartnerDecline(
        orgSlug.value,
        eventSlug.value,
        partnerId.value,
      );
    }

    await putEventsEcosystemPartner(eventSlug.value, partnerId.value, {
      category_id: form.category_id,
      language: form.language,
      emails: parseEmails(),
    });

    if (wasValidated) {
      await postOrgsEventsEcosystemPartnerValidate(
        orgSlug.value,
        eventSlug.value,
        partnerId.value,
      );
    }

    await loadData();
    success.value = true;
  } catch (err) {
    console.error("Failed to update partner:", err);
    error.value = "Impossible de mettre à jour le partenaire.";
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadData();
});

useHead({
  title: computed(
    () => `${company.value?.name || "Partenaire"} | DevLille`,
  ),
});
</script>
