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
          <div class="flex items-center justify-between">
            <div>
              <PageTitle>Offres d'emploi</PageTitle>
              <p class="text-sm text-gray-600 mt-1" role="doc-subtitle">
                Gérez les offres d'emploi de votre entreprise
              </p>
            </div>
            <UButton
              color="primary"
              size="lg"
              icon="i-heroicons-plus"
              @click="isAddModalOpen = true"
            >
              Ajouter une offre
            </UButton>
          </div>
        </header>

        <!-- Loading State -->
        <div v-if="loading" role="status" aria-live="polite" aria-label="Chargement des données">
          <TableSkeleton :columns="4" :rows="6" />
          <span class="sr-only">Chargement des offres d'emploi...</span>
        </div>

        <!-- Error State -->
        <div
          v-else-if="error"
          role="alert"
          aria-live="assertive"
          class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded"
        >
          {{ error }}
        </div>

        <!-- Job Offers List -->
        <div v-else class="bg-white rounded-lg shadow">
          <div class="px-6 py-4 border-b border-gray-200">
            <h2 class="text-lg font-semibold text-gray-900">
              Offres d'emploi ({{ jobOffers.length }})
            </h2>
          </div>

          <div v-if="jobOffers.length === 0" class="px-6 py-12 text-center">
            <i class="i-heroicons-briefcase text-gray-400 text-5xl mx-auto block mb-4" aria-hidden="true" />
            <h3 class="mt-2 text-sm font-medium text-gray-900">Aucune offre d'emploi</h3>
            <p class="mt-1 text-sm text-gray-500">Commencez par ajouter une offre d'emploi.</p>
          </div>

          <ul v-else class="divide-y divide-gray-200">
            <li v-for="job in jobOffers" :key="job.id" class="px-6 py-4 hover:bg-gray-50">
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <h3 class="text-base font-semibold text-gray-900">{{ job.title }}</h3>
                  <div class="mt-2 space-y-1">
                    <p class="text-sm text-gray-600">
                      <span class="font-medium">Localisation:</span> {{ job.location }}
                    </p>
                    <p v-if="job.salary" class="text-sm text-gray-600">
                      <span class="font-medium">Salaire:</span> {{ job.salary }}
                    </p>
                    <p v-if="job.experience_years" class="text-sm text-gray-600">
                      <span class="font-medium">Expérience:</span> {{ job.experience_years }} an(s)
                    </p>
                    <p class="text-sm text-gray-600">
                      <span class="font-medium">Publié le:</span> {{ formatDate(job.publication_date) }}
                    </p>
                    <p v-if="job.end_date" class="text-sm text-gray-600">
                      <span class="font-medium">Date limite:</span> {{ formatDate(job.end_date) }}
                    </p>
                    <p class="text-sm text-gray-600">
                      <a :href="job.url" target="_blank" rel="noopener noreferrer" class="text-primary-600 hover:text-primary-800 underline">
                        Voir l'offre complète
                      </a>
                    </p>
                  </div>
                </div>
                <UButton
                  color="error"
                  variant="ghost"
                  size="sm"
                  icon="i-heroicons-trash"
                  :loading="deletingJobId === job.id"
                  @click="confirmDelete(job)"
                >
                  Supprimer
                </UButton>
              </div>
            </li>
          </ul>
        </div>
      </main>
    </div>

    <!-- Modal d'ajout -->
    <Teleport to="body">
      <div v-if="isAddModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="isAddModalOpen = false">
        <div class="w-full max-w-2xl bg-white rounded-lg shadow-xl max-h-[90vh] overflow-y-auto" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Ajouter une offre d'emploi</h3>
          </div>

          <div class="px-6 py-4">
            <form @submit.prevent="handleAddJob" class="space-y-4">
              <div>
                <label for="title" class="block text-sm font-medium text-gray-700 mb-2">
                  Titre du poste <span class="text-red-500">*</span>
                </label>
                <UInput
                  id="title"
                  v-model="newJob.title"
                  placeholder="Ex: Développeur Full Stack"
                  :disabled="isSubmitting"
                  class="w-full"
                />
              </div>

              <div>
                <label for="location" class="block text-sm font-medium text-gray-700 mb-2">
                  Localisation <span class="text-red-500">*</span>
                </label>
                <UInput
                  id="location"
                  v-model="newJob.location"
                  placeholder="Ex: Paris, France"
                  :disabled="isSubmitting"
                  class="w-full"
                />
              </div>

              <div>
                <label for="url" class="block text-sm font-medium text-gray-700 mb-2">
                  URL de l'offre <span class="text-red-500">*</span>
                </label>
                <UInput
                  id="url"
                  v-model="newJob.url"
                  type="url"
                  placeholder="https://exemple.com/jobs/123"
                  :disabled="isSubmitting"
                  class="w-full"
                />
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label for="publication_date" class="block text-sm font-medium text-gray-700 mb-2">
                    Date de publication <span class="text-red-500">*</span>
                  </label>
                  <UInput
                    id="publication_date"
                    v-model="newJob.publication_date"
                    type="date"
                    :disabled="isSubmitting"
                    class="w-full"
                  />
                </div>

                <div>
                  <label for="end_date" class="block text-sm font-medium text-gray-700 mb-2">
                    Date limite (optionnel)
                  </label>
                  <UInput
                    id="end_date"
                    v-model="newJob.end_date"
                    type="date"
                    :disabled="isSubmitting"
                    class="w-full"
                  />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label for="experience_years" class="block text-sm font-medium text-gray-700 mb-2">
                    Années d'expérience (optionnel)
                  </label>
                  <UInput
                    id="experience_years"
                    v-model.number="newJob.experience_years"
                    type="number"
                    min="1"
                    max="20"
                    placeholder="Ex: 3"
                    :disabled="isSubmitting"
                    class="w-full"
                  />
                </div>

                <div>
                  <label for="salary" class="block text-sm font-medium text-gray-700 mb-2">
                    Salaire (optionnel)
                  </label>
                  <UInput
                    id="salary"
                    v-model="newJob.salary"
                    placeholder="Ex: 40k-50k €"
                    :disabled="isSubmitting"
                    class="w-full"
                  />
                </div>
              </div>

              <div v-if="addError" class="text-sm text-red-600">
                {{ addError }}
              </div>
            </form>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="isSubmitting"
              @click="isAddModalOpen = false"
            >
              Annuler
            </UButton>
            <UButton
              color="primary"
              :loading="isSubmitting"
              @click="handleAddJob"
            >
              Ajouter
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal de confirmation de suppression -->
    <Teleport to="body">
      <div v-if="isDeleteModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="isDeleteModalOpen = false">
        <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Confirmer la suppression</h3>
          </div>

          <div class="px-6 py-4 space-y-4">
            <p class="text-sm text-gray-700">
              Êtes-vous sûr de vouloir supprimer l'offre <strong>{{ jobToDelete?.title }}</strong> ?
            </p>
            <p class="text-sm text-gray-500">
              Cette action est irréversible.
            </p>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="!!deletingJobId"
              @click="isDeleteModalOpen = false"
            >
              Annuler
            </UButton>
            <UButton
              color="error"
              :loading="!!deletingJobId"
              @click="handleDelete"
            >
              Supprimer
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>
  </NuxtLayout>
</template>

<script setup lang="ts">
import { getCompaniesJobOffers, postCompaniesJobOffers, deleteCompaniesJobOffersById, type JobOfferResponseSchema, type CreateJobOfferSchema } from '~/utils/api';

definePageMeta({
  auth: false,
  ssr: false,
  validate: async (route) => {
    // Validate both eventSlug and partnershipId format (alphanumeric, hyphens, underscores)
    const eventSlug = Array.isArray(route.params.eventSlug) ? route.params.eventSlug[0] : route.params.eventSlug;
    const partnershipId = Array.isArray(route.params.partnershipId) ? route.params.partnershipId[0] : route.params.partnershipId;

    const isValidFormat = /^[a-zA-Z0-9-_]+$/;
    return isValidFormat.test(eventSlug) && isValidFormat.test(partnershipId);
  }
});

const route = useRoute();
const toast = useToast();

const {
  eventSlug,
  partnershipId,
  partnership,
  company,
  loadPartnership
} = usePublicPartnership();

const { isPartnershipComplete, isCompanyComplete } = usePartnershipValidation();

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

// Job offers state
const jobOffers = ref<JobOfferResponseSchema[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

// Add modal state
const isAddModalOpen = ref(false);
const newJob = ref<Partial<CreateJobOfferSchema>>({
  title: '',
  location: '',
  url: '',
  publication_date: new Date().toISOString().split('T')[0],
  end_date: null,
  experience_years: null,
  salary: null
});
const isSubmitting = ref(false);
const addError = ref<string | null>(null);

// Delete modal state
const isDeleteModalOpen = ref(false);
const jobToDelete = ref<JobOfferResponseSchema | null>(null);
const deletingJobId = ref<string | null>(null);

/**
 * Load job offers for the company
 */
async function loadJobOffers() {
  if (!company.value?.id) {
    // Wait for company data to be loaded
    return;
  }

  try {
    loading.value = true;
    error.value = null;
    const response = await getCompaniesJobOffers(company.value.id);
    jobOffers.value = response.data.items;
  } catch (err: any) {
    console.error('Failed to load job offers:', err);
    error.value = 'Impossible de charger les offres d\'emploi';
  } finally {
    loading.value = false;
  }
}

/**
 * Format date string to French locale
 */
function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
}

/**
 * Handle adding a new job offer
 */
async function handleAddJob() {
  addError.value = null;

  // Validation
  if (!newJob.value.title || !newJob.value.title.trim()) {
    addError.value = 'Le titre est obligatoire';
    return;
  }

  if (!newJob.value.location || !newJob.value.location.trim()) {
    addError.value = 'La localisation est obligatoire';
    return;
  }

  if (!newJob.value.url || !newJob.value.url.trim()) {
    addError.value = 'L\'URL est obligatoire';
    return;
  }

  if (!newJob.value.publication_date) {
    addError.value = 'La date de publication est obligatoire';
    return;
  }

  if (!company.value?.id) {
    addError.value = 'Impossible de trouver les informations de l\'entreprise';
    return;
  }

  try {
    isSubmitting.value = true;

    const jobData: CreateJobOfferSchema = {
      title: newJob.value.title!,
      location: newJob.value.location!,
      url: newJob.value.url!,
      publication_date: newJob.value.publication_date! + 'T00:00:00',
      end_date: newJob.value.end_date ? newJob.value.end_date + 'T23:59:59' : null,
      experience_years: newJob.value.experience_years || null,
      salary: newJob.value.salary || null
    };

    await postCompaniesJobOffers(company.value.id, jobData);

    // Reload job offers list
    await loadJobOffers();

    // Show success toast
    toast.add({
      title: 'Offre ajoutée',
      description: 'L\'offre d\'emploi a été ajoutée avec succès',
      color: 'success'
    });

    // Close modal and reset form
    isAddModalOpen.value = false;
    newJob.value = {
      title: '',
      location: '',
      url: '',
      publication_date: new Date().toISOString().split('T')[0],
      end_date: null,
      experience_years: null,
      salary: null
    };
  } catch (err: any) {
    console.error('Failed to add job offer:', err);
    addError.value = 'Impossible d\'ajouter l\'offre d\'emploi. Vérifiez les informations.';
  } finally {
    isSubmitting.value = false;
  }
}

/**
 * Confirm job offer deletion
 */
function confirmDelete(job: JobOfferResponseSchema) {
  jobToDelete.value = job;
  isDeleteModalOpen.value = true;
}

/**
 * Handle job offer deletion
 */
async function handleDelete() {
  if (!jobToDelete.value || !company.value?.id) return;

  try {
    deletingJobId.value = jobToDelete.value.id;

    await deleteCompaniesJobOffersById(company.value.id, jobToDelete.value.id);

    // Reload job offers list
    await loadJobOffers();

    // Show success toast
    toast.add({
      title: 'Offre supprimée',
      description: 'L\'offre d\'emploi a été supprimée avec succès',
      color: 'success'
    });

    // Close modal
    isDeleteModalOpen.value = false;
    jobToDelete.value = null;
  } catch (err: any) {
    console.error('Failed to delete job offer:', err);
    error.value = 'Impossible de supprimer l\'offre d\'emploi';

    toast.add({
      title: 'Erreur',
      description: 'Impossible de supprimer l\'offre d\'emploi',
      color: 'error'
    });
  } finally {
    deletingJobId.value = null;
  }
}

// Load partnership data on mount
onMounted(async () => {
  await loadPartnership();
  // Once partnership is loaded, load job offers
  if (company.value?.id) {
    await loadJobOffers();
  }
});

// Reload job offers when company changes
watch(() => company.value?.id, (newId) => {
  if (newId) {
    loadJobOffers();
  }
});

// Reload if partnership ID or event slug changes
watch([eventSlug, partnershipId], async () => {
  await loadPartnership();
});

useHead({
  title: computed(() => `Offres d'emploi - ${partnership.value?.company_name || 'Partenariat'} | DevLille`)
});
</script>
