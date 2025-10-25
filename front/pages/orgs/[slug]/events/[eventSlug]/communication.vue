<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Plan de Communication - {{ eventName }}</h1>
          <p class="text-sm text-gray-600 mt-1">Gérez les communications de vos partenariats</p>
        </div>

        <!-- Toggle vue + Sauvegarder -->
        <div class="flex items-center gap-3">
          <div class="flex items-center gap-2 bg-gray-100 rounded-lg p-1">
            <button
              :class="[
                'px-3 py-2 rounded text-sm font-medium transition-colors',
                viewMode === 'grid'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              ]"
              @click="viewMode = 'grid'"
            >
              <i class="i-heroicons-squares-2x2 mr-1" />
              Grille
            </button>
            <button
              :class="[
                'px-3 py-2 rounded text-sm font-medium transition-colors',
                viewMode === 'calendar'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              ]"
              @click="viewMode = 'calendar'"
            >
              <i class="i-heroicons-calendar-days mr-1" />
              Calendrier
            </button>
          </div>

          <UButton
            color="primary"
            icon="i-heroicons-arrow-down-tray"
            @click="handleExport"
          >
            Exporter
          </UButton>
        </div>
      </div>
    </div>

    <div class="p-6 space-y-6">
      <!-- Loading skeleton -->
      <TableSkeleton v-if="loading" :columns="4" :rows="5" />

      <!-- Error message -->
      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <!-- Vue Calendrier -->
      <div v-else-if="viewMode === 'calendar'">
        <CommunicationCalendar
          :communications="allCommunications"
          @select-communication="handleSelectCommunication"
        />
      </div>

      <!-- Vue Grille (Communication Plan) -->
      <div v-else class="space-y-8">
        <!-- Section: Non planifiées -->
        <section v-if="communicationPlan.unplanned.length > 0">
          <div class="flex items-center gap-3 mb-4">
            <div class="flex items-center justify-center w-10 h-10 rounded-full bg-gray-100">
              <i class="i-heroicons-clock text-xl text-gray-600" />
            </div>
            <div>
              <h2 class="text-lg font-semibold text-gray-900">Communications non planifiées</h2>
              <p class="text-sm text-gray-600">{{ communicationPlan.unplanned.length }} partenariat(s)</p>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <CommunicationCard
              v-for="item in communicationPlan.unplanned"
              :key="item.partnership_id"
              :item="item"
              status="unplanned"
              @schedule="openScheduleModal"
              @upload="openUploadModal"
            />
          </div>
        </section>

        <!-- Section: Planifiées -->
        <section v-if="communicationPlan.planned.length > 0">
          <div class="flex items-center gap-3 mb-4">
            <div class="flex items-center justify-center w-10 h-10 rounded-full bg-blue-100">
              <i class="i-heroicons-calendar text-xl text-blue-600" />
            </div>
            <div>
              <h2 class="text-lg font-semibold text-gray-900">Communications planifiées</h2>
              <p class="text-sm text-gray-600">{{ communicationPlan.planned.length }} partenariat(s)</p>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <CommunicationCard
              v-for="item in communicationPlan.planned"
              :key="item.partnership_id"
              :item="item"
              status="planned"
              @schedule="openScheduleModal"
              @upload="openUploadModal"
            />
          </div>
        </section>

        <!-- Section: Terminées -->
        <section v-if="communicationPlan.done.length > 0">
          <div class="flex items-center gap-3 mb-4">
            <div class="flex items-center justify-center w-10 h-10 rounded-full bg-green-100">
              <i class="i-heroicons-check-circle text-xl text-green-600" />
            </div>
            <div>
              <h2 class="text-lg font-semibold text-gray-900">Communications terminées</h2>
              <p class="text-sm text-gray-600">{{ communicationPlan.done.length }} partenariat(s)</p>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <CommunicationCard
              v-for="item in communicationPlan.done"
              :key="item.partnership_id"
              :item="item"
              status="done"
              @schedule="openScheduleModal"
              @upload="openUploadModal"
            />
          </div>
        </section>

        <!-- Empty state -->
        <div v-if="communicationPlan.unplanned.length === 0 && communicationPlan.planned.length === 0 && communicationPlan.done.length === 0" class="text-center py-12">
          <i class="i-heroicons-inbox text-6xl text-gray-400 mb-4" />
          <p class="text-gray-600 text-lg">Aucune communication à afficher</p>
          <p class="text-gray-500 text-sm mt-2">Les communications apparaîtront ici une fois que vous aurez des partenariats.</p>
        </div>
      </div>
    </div>

    <!-- Modal: Planifier une date de publication -->
    <Teleport to="body">
      <div v-if="isScheduleModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="isScheduleModalOpen = false">
        <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Planifier la date de publication</h3>
          </div>

          <div class="px-6 py-4 space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">
                Entreprise
              </label>
              <input
                type="text"
                :value="selectedItem?.company_name"
                disabled
                class="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-gray-600"
              />
            </div>

            <div>
              <label for="publication-date" class="block text-sm font-medium text-gray-700 mb-2">
                Date de publication *
              </label>
              <input
                id="publication-date"
                v-model="publicationDate"
                type="date"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
                required
              />
            </div>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              @click="isScheduleModalOpen = false"
            >
              Annuler
            </UButton>
            <UButton
              color="primary"
              :loading="schedulingDate"
              :disabled="!publicationDate"
              @click="handleSchedulePublication"
            >
              Planifier
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal: Upload support visuel -->
    <Teleport to="body">
      <div v-if="isUploadModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="isUploadModalOpen = false">
        <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Ajouter un support visuel</h3>
          </div>

          <div class="px-6 py-4 space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">
                Entreprise
              </label>
              <input
                type="text"
                :value="selectedItem?.company_name"
                disabled
                class="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-gray-600"
              />
            </div>

            <!-- Preview de l'image existante -->
            <div v-if="selectedItem?.support_url">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                Support actuel
              </label>
              <img
                :src="selectedItem.support_url"
                alt="Support visuel actuel"
                class="w-full h-48 object-contain border border-gray-300 rounded-md bg-gray-50"
              />
            </div>

            <div>
              <label for="support-file" class="block text-sm font-medium text-gray-700 mb-2">
                {{ selectedItem?.support_url ? 'Remplacer le support visuel' : 'Support visuel' }} *
              </label>
              <input
                id="support-file"
                type="file"
                accept="image/png,image/jpeg,image/jpg"
                @change="handleFileSelect"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
              <p class="text-xs text-gray-500 mt-1">Format PNG ou JPEG, max 5MB</p>
            </div>

            <!-- Preview de la nouvelle image -->
            <div v-if="filePreview">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                Aperçu
              </label>
              <img
                :src="filePreview"
                alt="Aperçu"
                class="w-full h-48 object-contain border border-gray-300 rounded-md bg-gray-50"
              />
            </div>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              @click="isUploadModalOpen = false"
            >
              Annuler
            </UButton>
            <UButton
              color="primary"
              :loading="uploadingSupport"
              :disabled="!selectedFile"
              @click="handleUploadSupport"
            >
              Uploader
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsCommunication, getEventBySlug, putOrgsEventsPartnershipCommunicationPublication, putOrgsEventsPartnershipCommunicationSupport, type CommunicationPlanSchema, type CommunicationItemSchema } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import { ERROR_MESSAGES, SUCCESS_MESSAGES } from "~/constants/errors";

const route = useRoute();
const toast = useToast();
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

const communicationPlan = ref<CommunicationPlanSchema>({
  done: [],
  planned: [],
  unplanned: []
});
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Mode d'affichage (grille ou calendrier)
const viewMode = ref<'grid' | 'calendar'>('grid');

// Toutes les communications pour la vue calendrier
const allCommunications = computed(() => {
  return [
    ...communicationPlan.value.done,
    ...communicationPlan.value.planned,
    ...communicationPlan.value.unplanned
  ];
});

// Menu contextuel pour la page
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Modal de planification
const isScheduleModalOpen = ref(false);
const selectedItem = ref<CommunicationItemSchema | null>(null);
const publicationDate = ref<string>('');
const schedulingDate = ref(false);

// Modal d'upload
const isUploadModalOpen = ref(false);
const selectedFile = ref<File | null>(null);
const filePreview = ref<string | null>(null);
const uploadingSupport = ref(false);

function openScheduleModal(item: CommunicationItemSchema) {
  selectedItem.value = item;
  // Pré-remplir avec la date existante si elle existe
  if (item.publication_date) {
    publicationDate.value = item.publication_date.split('T')[0];
  } else {
    publicationDate.value = '';
  }
  isScheduleModalOpen.value = true;
}

function openUploadModal(item: CommunicationItemSchema) {
  selectedItem.value = item;
  selectedFile.value = null;
  filePreview.value = null;
  isUploadModalOpen.value = true;
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (file) {
    // Vérifier le type
    if (!['image/png', 'image/jpeg', 'image/jpg'].includes(file.type)) {
      toast.add({
        title: 'Erreur',
        description: 'Veuillez sélectionner une image PNG ou JPEG',
        color: 'error'
      });
      return;
    }

    // Vérifier la taille (5MB max)
    if (file.size > 5 * 1024 * 1024) {
      toast.add({
        title: 'Erreur',
        description: 'L\'image ne doit pas dépasser 5MB',
        color: 'error'
      });
      return;
    }

    selectedFile.value = file;

    // Créer un aperçu
    const reader = new FileReader();
    reader.onload = (e) => {
      filePreview.value = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  }
}

async function handleSchedulePublication() {
  if (!selectedItem.value || !publicationDate.value) return;

  try {
    schedulingDate.value = true;

    // Convertir la date au format LocalDateTime ISO 8601 (YYYY-MM-DDTHH:mm:ss)
    const dateTimeString = `${publicationDate.value}T00:00:00`;

    await putOrgsEventsPartnershipCommunicationPublication(
      orgSlug.value,
      eventSlug.value,
      selectedItem.value.partnership_id,
      { publication_date: dateTimeString }
    );

    toast.add({
      title: 'Succès',
      description: SUCCESS_MESSAGES.UPDATED('Date de publication'),
      color: 'green'
    });

    // Recharger les données
    await loadCommunicationPlan();

    // Fermer le modal
    isScheduleModalOpen.value = false;
    publicationDate.value = '';
    selectedItem.value = null;
  } catch (err) {
    console.error('Failed to schedule publication:', err);
    toast.add({
      title: 'Erreur',
      description: ERROR_MESSAGES.UPDATE_FAILED('la date de publication'),
      color: 'error'
    });
  } finally {
    schedulingDate.value = false;
  }
}

async function handleUploadSupport() {
  if (!selectedItem.value || !selectedFile.value) return;

  try {
    uploadingSupport.value = true;

    await putOrgsEventsPartnershipCommunicationSupport(
      orgSlug.value,
      eventSlug.value,
      selectedItem.value.partnership_id,
      selectedFile.value
    );

    toast.add({
      title: 'Succès',
      description: SUCCESS_MESSAGES.UPDATED('Support visuel'),
      color: 'green'
    });

    // Recharger les données
    await loadCommunicationPlan();

    // Fermer le modal
    isUploadModalOpen.value = false;
    selectedFile.value = null;
    filePreview.value = null;
    selectedItem.value = null;
  } catch (err) {
    console.error('Failed to upload support:', err);
    toast.add({
      title: 'Erreur',
      description: ERROR_MESSAGES.UPDATE_FAILED('le support visuel'),
      color: 'error'
    });
  } finally {
    uploadingSupport.value = false;
  }
}

function handleSelectCommunication(comm: CommunicationItemSchema) {
  // Ouvrir le modal de planification avec la communication sélectionnée
  openScheduleModal(comm);
}

function handleExport() {
  // Créer un CSV des communications
  const csvData = [];
  csvData.push(['Entreprise', 'Date de publication', 'Statut', 'Support visuel']);

  allCommunications.value.forEach(comm => {
    const status = comm.publication_date
      ? new Date(comm.publication_date) < new Date()
        ? 'Terminée'
        : 'Planifiée'
      : 'Non planifiée';

    csvData.push([
      comm.company_name,
      comm.publication_date ? new Date(comm.publication_date).toLocaleDateString('fr-FR') : '-',
      status,
      comm.support_url ? 'Oui' : 'Non'
    ]);
  });

  // Convertir en CSV
  const csvContent = csvData.map(row => row.join(';')).join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);

  link.setAttribute('href', url);
  link.setAttribute('download', `plan-communication-${eventSlug.value}-${new Date().toISOString().split('T')[0]}.csv`);
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  toast.add({
    title: 'Succès',
    description: 'Plan de communication exporté',
    color: 'green'
  });
}

async function loadCommunicationPlan() {
  try {
    loading.value = true;
    error.value = null;

    // Charger le nom de l'événement
    const eventResponse = await getEventBySlug(eventSlug.value);
    eventName.value = eventResponse.data.event.name;

    // Charger le plan de communication
    const response = await getOrgsEventsCommunication(orgSlug.value, eventSlug.value);
    communicationPlan.value = response.data;
  } catch (err) {
    console.error('Failed to load communication plan:', err);
    error.value = ERROR_MESSAGES.LOAD_FAILED('le plan de communication');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadCommunicationPlan();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug], () => {
  loadCommunicationPlan();
});

useHead({
  title: computed(() => `Plan de Communication - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
