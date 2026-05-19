<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton
            :to="`/orgs/${orgSlug}/events/${eventSlug}/partners`"
            label="Retour aux partenaires"
          />
          <PageTitle>Créer un partenaire - {{ eventName }}</PageTitle>
        </div>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="2" :rows="6" />

      <AlertMessage v-else-if="error" type="error" :message="error" class="mb-4" />

      <AlertMessage
        v-else-if="success"
        type="success"
        message="Partenaire créé avec succès ! Redirection en cours..."
        class="mb-4"
      />

      <div v-else class="bg-white rounded-lg shadow p-6 max-w-3xl">
        <form class="space-y-6" @submit.prevent="onSave">
          <!-- Entité légale -->
          <div>
            <h3 class="text-md font-semibold text-gray-900 mb-4">Entité légale</h3>
            <div class="grid grid-cols-1 gap-4">
              <div>
                <label for="company_name" class="block text-sm font-medium text-gray-700 mb-1">
                  Nom de l'entreprise *
                </label>
                <UInput
                  id="company_name"
                  v-model="form.company_name"
                  required
                  placeholder="ex. ACME Media"
                />
              </div>
              <div>
                <label for="site_url" class="block text-sm font-medium text-gray-700 mb-1">
                  Site web
                </label>
                <UInput
                  id="site_url"
                  v-model="form.site_url"
                  type="url"
                  placeholder="https://acme.example"
                />
                <p class="text-xs text-gray-500 mt-1">
                  Vous pourrez ajouter le logo depuis l'onglet "Entité légale" après création.
                </p>
              </div>
            </div>
          </div>

          <!-- Partenariat -->
          <div class="border-t pt-6">
            <h3 class="text-md font-semibold text-gray-900 mb-4">Partenariat</h3>
            <div class="grid grid-cols-1 gap-4">
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
                  <option value="">-- Choisir une catégorie --</option>
                  <option v-for="category in categories" :key="category.id" :value="category.id">
                    {{ category.name }}
                  </option>
                </select>
                <p v-if="categories.length === 0" class="text-xs text-orange-600 mt-1">
                  Aucune catégorie n'existe. Créez-en une depuis la page Packs.
                </p>
              </div>

              <div>
                <label for="language" class="block text-sm font-medium text-gray-700 mb-1">
                  Langue des notifications *
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
                <UInput
                  id="emails"
                  v-model="emailsInput"
                  placeholder="contact@acme.example, hello@acme.example"
                />
                <p class="text-xs text-gray-500 mt-1">Séparez plusieurs emails par une virgule.</p>
              </div>
            </div>
          </div>

          <div class="flex justify-end gap-3 pt-2">
            <UButton
              type="button"
              color="neutral"
              variant="ghost"
              :to="`/orgs/${orgSlug}/events/${eventSlug}/partners`"
            >
              Annuler
            </UButton>
            <UButton
              type="submit"
              color="primary"
              :loading="saving"
              :disabled="categories.length === 0"
            >
              Créer le partenaire
            </UButton>
          </div>
        </form>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import {
  getEventBySlug,
  getOrgsEventsEcosystemPartnerCategories,
  postCompanies,
  postEventsEcosystemPartners,
  postOrgsEventsEcosystemPartnerValidate,
  type CreateCompany,
  type EcosystemPartnerCategory,
  type RegisterEcosystemPartner,
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
const categories = ref<EcosystemPartnerCategory[]>([]);

const form = reactive({
  company_name: "",
  site_url: "",
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

async function loadData() {
  try {
    loading.value = true;
    const [eventResponse, categoriesResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsEcosystemPartnerCategories(orgSlug.value, eventSlug.value),
    ]);
    eventName.value = eventResponse.data.event.name;
    categories.value = categoriesResponse.data;
  } catch (err) {
    console.error("Failed to load data:", err);
    error.value = "Impossible de charger les informations.";
  } finally {
    loading.value = false;
  }
}

async function onSave() {
  try {
    saving.value = true;
    error.value = null;

    // Step 1: create the legal entity (company). For ecosystem partners we
    // only collect name + site URL; head_office, siret, and vat stay null so
    // we don't trip the API's pattern validation (empty string does not match
    // the SIRET / VAT regex and is not null either).
    const companyData: CreateCompany = {
      name: form.company_name,
      head_office: null,
      siret: null,
      vat: null,
      site_url: form.site_url || null,
    };
    const companyResponse = await postCompanies(companyData);
    const companyId = companyResponse.data.id;

    // Step 2: register the ecosystem partner (public endpoint)
    const partnerData: RegisterEcosystemPartner = {
      company_id: companyId,
      category_id: form.category_id,
      language: form.language,
      emails: parseEmails(),
    };
    const partnerResponse = await postEventsEcosystemPartners(eventSlug.value, partnerData);
    const partnerId = partnerResponse.data.id;

    // Step 3: auto-validate (organiser-created partners are immediately public)
    await postOrgsEventsEcosystemPartnerValidate(orgSlug.value, eventSlug.value, partnerId);

    success.value = true;
    setTimeout(() => {
      router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/partners/${partnerId}`);
    }, 800);
  } catch (err) {
    console.error("Failed to create partner:", err);
    error.value = "Impossible de créer le partenaire. Vérifiez les données du formulaire.";
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadData();
});

useHead({
  title: computed(
    () => `Créer un partenaire - ${eventName.value || "Événement"} | DevLille`,
  ),
});
</script>
