<template>
  <NuxtLayout
    name="minimal-sidebar"
    :sidebar-title="partnership?.company_name || 'Partenariat'"
    :sidebar-links="sidebarLinks"
  >
    <div class="min-h-screen bg-gray-50">
      <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8" role="main">
        <!-- Header -->
        <header class="bg-white rounded-lg shadow p-6 mb-6">
          <PageTitle>Prestataires</PageTitle>
          <p class="text-sm text-gray-600 mt-1">
            Prestataires et partenaires de {{ partnership?.event_name }}
          </p>
        </header>

        <!-- Loading State -->
        <div v-if="loadingProviders" role="status" aria-live="polite">
          <TableSkeleton :columns="5" :rows="6" />
          <span class="sr-only">Chargement des prestataires...</span>
        </div>

        <!-- Error State -->
        <AlertMessage v-else-if="providersError" type="error" :message="providersError" />

        <!-- Empty State -->
        <div v-else-if="providers.length === 0" class="bg-white rounded-lg shadow p-12 text-center">
          <i class="i-heroicons-user-group text-gray-400 text-5xl mx-auto mb-4" aria-hidden="true" />
          <h3 class="text-lg font-medium text-gray-900 mb-2">Aucun prestataire</h3>
          <p class="text-sm text-gray-500">
            Aucun prestataire n'a été configuré pour cet événement.
          </p>
        </div>

        <!-- Providers List -->
        <section v-else class="bg-white rounded-lg shadow overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200">
            <h2 class="text-lg font-semibold text-gray-900">Prestataires de l'événement</h2>
            <p class="mt-1 text-sm text-gray-500">Liste des prestataires disponibles</p>
          </div>

          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Nom
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Type
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Site web
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Email
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Téléphone
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="provider in providers" :key="provider.id" class="hover:bg-gray-50">
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {{ provider.name }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      {{ provider.type }}
                    </span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    <a
                      v-if="provider.website"
                      :href="provider.website"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="text-blue-600 hover:text-blue-800 hover:underline inline-flex items-center gap-1"
                    >
                      Visiter
                      <i class="i-heroicons-arrow-top-right-on-square text-xs" aria-hidden="true" />
                    </a>
                    <span v-else class="text-gray-400">-</span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    <a
                      v-if="provider.email"
                      :href="`mailto:${provider.email}`"
                      class="text-blue-600 hover:text-blue-800 hover:underline inline-flex items-center gap-1"
                    >
                      <i class="i-heroicons-envelope text-sm" aria-hidden="true" />
                      {{ provider.email }}
                    </a>
                    <span v-else class="text-gray-400">-</span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    <span v-if="provider.phone" class="inline-flex items-center gap-1">
                      <i class="i-heroicons-phone text-sm" aria-hidden="true" />
                      {{ provider.phone }}
                    </span>
                    <span v-else class="text-gray-400">-</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import { getProviders, type ProviderSchema } from '~/utils/api';

definePageMeta({
  auth: false,
  ssr: false,
  validate: async (route) => {
    const eventSlug = Array.isArray(route.params.eventSlug) ? route.params.eventSlug[0] : route.params.eventSlug;
    const partnershipId = Array.isArray(route.params.partnershipId) ? route.params.partnershipId[0] : route.params.partnershipId;

    const isValidFormat = /^[a-zA-Z0-9-_]+$/;
    return isValidFormat.test(eventSlug) && isValidFormat.test(partnershipId);
  }
});

const {
  eventSlug,
  partnershipId,
  partnership,
  loading: partnershipLoading,
  loadPartnership,
  company
} = usePublicPartnership();

const { isPartnershipComplete, isCompanyComplete } = usePartnershipValidation();

const providers = ref<ProviderSchema[]>([]);
const loadingProviders = ref(true);
const providersError = ref<string | null>(null);

async function loadProviders() {
  try {
    loadingProviders.value = true;
    providersError.value = null;

    const response = await getProviders();
    providers.value = response.data.items;
  } catch (err: any) {
    console.error('Failed to load providers:', err);
    providersError.value = 'Impossible de charger les prestataires';
  } finally {
    loadingProviders.value = false;
  }
}

// Sidebar navigation configuration
const sidebarLinks = computed(() => {
  const partnershipComplete = isPartnershipComplete(partnership.value);
  const companyComplete = isCompanyComplete(company.value);

  return [
    {
      label: 'Partenariat',
      icon: 'i-heroicons-hand-raised',
      to: `/${eventSlug.value}/${partnershipId.value}`,
      badge: !partnershipComplete ? {
        label: '!',
        color: 'error' as const,
        title: 'Informations incomplètes'
      } : undefined
    },
    {
      label: 'Entreprise',
      icon: 'i-heroicons-building-office',
      to: `/${eventSlug.value}/${partnershipId.value}/company`,
      badge: !companyComplete ? {
        label: '!',
        color: 'error' as const,
        title: 'Informations incomplètes'
      } : undefined
    },
  {
    label: 'Offres d\'emploi',
    icon: 'i-heroicons-briefcase',
    to: `/${eventSlug.value}/${partnershipId.value}/job-offers`
  },
  {
    label: 'Liens utiles',
    icon: 'i-heroicons-link',
    to: `/${eventSlug.value}/${partnershipId.value}/external-links`
  },
    {
      label: 'Prestataires',
      icon: 'i-heroicons-user-group',
      to: `/${eventSlug.value}/${partnershipId.value}/providers`
    }
  ];
});

onMounted(() => {
  loadPartnership();
  loadProviders();
});

useHead({
  title: computed(() => `Prestataires - ${partnership.value?.company_name || 'Partenariat'} | DevLille`)
});
</script>
