<template>
  <Dashboard :main-links="mainLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <PageTitle>Espace partenaire</PageTitle>
      <p class="mt-1 text-sm text-gray-500">
        Gérez vos partenariats et vos offres d'emploi
      </p>
    </div>

    <div class="p-6">
      <!-- Loading state -->
      <TableSkeleton v-if="loading" :columns="3" :rows="6" />

      <!-- Error state -->
      <AlertMessage v-else-if="error" :message="error" type="error" />

      <!-- Company selection -->
      <div v-else-if="companies.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="company in companies"
          :key="company.id"
          class="bg-white rounded-lg shadow hover:shadow-lg transition-shadow p-6 border border-gray-200"
        >
          <h3 class="text-lg font-semibold text-gray-900">{{ company.name }}</h3>
          <p v-if="company.description" class="mt-2 text-sm text-gray-600">{{ company.description }}</p>

          <div class="mt-4 space-y-2">
            <NuxtLink
              :to="`/companies/${company.id}`"
              class="block w-full text-center px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700 transition-colors"
            >
              Gérer l'entreprise
            </NuxtLink>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="text-center py-12">
        <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">Aucune entreprise trouvée</h3>
        <p class="mt-1 text-sm text-gray-500">Vous n'avez pas encore d'entreprise enregistrée.</p>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getCompanies, type CompanySchema } from '~/utils/api';
import authMiddleware from '~/middleware/auth';

const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const mainLinks = [
  { label: 'Mes entreprises', icon: 'i-heroicons-building-office', to: '/hall' }
];

const companies = ref<CompanySchema[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

async function loadCompanies() {
  try {
    loading.value = true;
    error.value = null;
    const response = await getCompanies();
    companies.value = response.data.items;
  } catch (err) {
    console.error('Failed to load companies:', err);
    error.value = 'Impossible de charger les entreprises';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadCompanies();
});

useHead({
  title: "Espace partenaire | DevLille"
});
</script>
