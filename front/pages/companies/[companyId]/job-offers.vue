<template>
  <Dashboard :main-links="dashboardLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <PageTitle>Offres d'emploi</PageTitle>
          <p class="mt-1 text-sm text-gray-500">Gérez les offres d'emploi de votre entreprise</p>
        </div>
        <UButton color="primary" size="lg" icon="i-heroicons-plus" @click="isAddModalOpen = true">
          Ajouter une offre
        </UButton>
      </div>
    </div>

    <div class="p-6">
      <!-- Loading state -->
      <TableSkeleton v-if="loading" :columns="4" :rows="8" />

      <!-- Error state -->
      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <!-- Job offers list -->
      <div v-else class="bg-white rounded-lg shadow">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">
            Offres d'emploi ({{ jobOffers.length }})
          </h2>
        </div>

        <div v-if="jobOffers.length === 0" class="px-6 py-12 text-center">
          <svg
            class="mx-auto h-12 w-12 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
            />
          </svg>
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
                    <span class="font-medium">Publié le:</span>
                    {{ formatDate(job.publication_date) }}
                  </p>
                  <p v-if="job.end_date" class="text-sm text-gray-600">
                    <span class="font-medium">Date limite:</span> {{ formatDate(job.end_date) }}
                  </p>
                  <p class="text-sm text-gray-600">
                    <a
                      :href="job.url"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="text-primary-600 hover:text-primary-800 underline"
                    >
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
    </div>

    <!-- Modal d'ajout -->
    <Teleport to="body">
      <div
        v-if="isAddModalOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
        @click.self="isAddModalOpen = false"
      >
        <div
          class="w-full max-w-2xl bg-white rounded-lg shadow-xl max-h-[90vh] overflow-y-auto"
          @click.stop
        >
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
                  <label
                    for="publication_date"
                    class="block text-sm font-medium text-gray-700 mb-2"
                  >
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
                  <label
                    for="experience_years"
                    class="block text-sm font-medium text-gray-700 mb-2"
                  >
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
            <UButton color="primary" :loading="isSubmitting" @click="handleAddJob">
              Ajouter
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal de confirmation de suppression -->
    <Teleport to="body">
      <div
        v-if="isDeleteModalOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
        @click.self="isDeleteModalOpen = false"
      >
        <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Confirmer la suppression</h3>
          </div>

          <div class="px-6 py-4 space-y-4">
            <p class="text-sm text-gray-700">
              Êtes-vous sûr de vouloir supprimer l'offre <strong>{{ jobToDelete?.title }}</strong> ?
            </p>
            <p class="text-sm text-gray-500">Cette action est irréversible.</p>
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
            <UButton color="error" :loading="!!deletingJobId" @click="handleDelete">
              Supprimer
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>
  </Dashboard>
</template>

<script setup lang="ts">
import { getCompaniesJobOffers, postCompaniesJobOffers, deleteCompaniesJobOffersById, type JobOfferResponseSchema, type CreateJobOfferSchema } from '~/utils/api';
import authMiddleware from '~/middleware/auth';

const route = useRoute();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const companyId = computed(() => {
  const params = route.params.companyId;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const dashboardLinks = computed(() => [
  { label: 'Partenariats', icon: 'i-heroicons-handshake', to: `/companies/${companyId.value}/partnerships` },
  { label: 'Offres d\'emploi', icon: 'i-heroicons-briefcase', to: `/companies/${companyId.value}/job-offers` }
]);

const jobOffers = ref<JobOfferResponseSchema[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

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

const isDeleteModalOpen = ref(false);
const jobToDelete = ref<JobOfferResponseSchema | null>(null);
const deletingJobId = ref<string | null>(null);

async function loadJobOffers() {
  try {
    loading.value = true;
    error.value = null;
    const response = await getCompaniesJobOffers(companyId.value);
    jobOffers.value = response.data.items;
  } catch (err) {
    console.error('Failed to load job offers:', err);
    error.value = 'Impossible de charger les offres d\'emploi';
  } finally {
    loading.value = false;
  }
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
}

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

    await postCompaniesJobOffers(companyId.value, jobData);

    // Recharger la liste
    await loadJobOffers();

    // Fermer le modal et réinitialiser
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
  } catch (err) {
    console.error('Failed to add job offer:', err);
    addError.value = 'Impossible d\'ajouter l\'offre d\'emploi. Vérifiez les informations.';
  } finally {
    isSubmitting.value = false;
  }
}

function confirmDelete(job: JobOfferResponseSchema) {
  jobToDelete.value = job;
  isDeleteModalOpen.value = true;
}

async function handleDelete() {
  if (!jobToDelete.value) return;

  try {
    deletingJobId.value = jobToDelete.value.id;

    await deleteCompaniesJobOffersById(companyId.value, jobToDelete.value.id);

    // Recharger la liste
    await loadJobOffers();

    // Fermer le modal
    isDeleteModalOpen.value = false;
    jobToDelete.value = null;
  } catch (err) {
    console.error('Failed to delete job offer:', err);
    error.value = 'Impossible de supprimer l\'offre d\'emploi';
  } finally {
    deletingJobId.value = null;
  }
}

onMounted(() => {
  loadJobOffers();
});

useHead({
  title: computed(() => `Offres d'emploi | DevLille`)
});
</script>
