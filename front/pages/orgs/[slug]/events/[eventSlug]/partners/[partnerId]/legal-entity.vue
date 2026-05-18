<template>
  <Dashboard :main-links="partnerLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <PageTitle>{{ company?.name || 'Partenaire' }}</PageTitle>
      <p class="text-sm text-gray-600 mt-1">Entité légale</p>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <AlertMessage v-else-if="error" type="error" :message="error" />

      <div v-else-if="company" class="bg-white rounded-lg shadow p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations de l'entreprise</h2>
        <PartnershipCompanyForm :company="company" :loading="saving" @save="onSaveCompany" />
        <AlertMessage v-if="formError" type="error" :message="formError" class="mt-4" />
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import {
  getCompanyById,
  getEventsEcosystemPartner,
  putCompanyById,
  type CompanySchema,
  type UpdateCompanySchema,
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
const error = ref<string | null>(null);
const formError = ref<string | null>(null);
const company = ref<CompanySchema | null>(null);

async function loadData() {
  try {
    loading.value = true;
    error.value = null;
    const partnerResponse = await getEventsEcosystemPartner(eventSlug.value, partnerId.value);
    const companyResponse = await getCompanyById(partnerResponse.data.company_id);
    company.value = companyResponse.data;
  } catch (err) {
    console.error("Failed to load company:", err);
    error.value = "Impossible de charger l'entité légale.";
  } finally {
    loading.value = false;
  }
}

async function onSaveCompany(updateData: UpdateCompanySchema) {
  if (!company.value) return;
  try {
    saving.value = true;
    formError.value = null;
    await putCompanyById(company.value.id, updateData);
    const refreshed = await getCompanyById(company.value.id);
    company.value = refreshed.data;
    const toast = useCustomToast();
    toast.success("Les informations de l'entreprise ont été mises à jour avec succès");
  } catch (err: any) {
    console.error("Failed to save company:", err);
    const errorMessages: Record<number, string> = {
      404: "Entreprise introuvable",
      403: "Vous n'êtes pas autorisé à modifier cette entreprise",
      400: "Données invalides",
    };
    formError.value =
      err?.response?.data?.message ||
      errorMessages[err?.response?.status as number] ||
      "Impossible de sauvegarder les modifications";
    const toast = useCustomToast();
    toast.error(formError.value);
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadData();
});

useHead({
  title: computed(() => `Entité légale - ${company.value?.name || "Partenaire"} | DevLille`),
});
</script>
