<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <PageTitle>Vidéos de présentation</PageTitle>
      <p class="mt-1 text-sm text-gray-500">Vidéos soumises par les sponsors pour cet événement.</p>
    </div>

    <div class="p-6 space-y-6">
      <TableSkeleton v-if="loading" :columns="3" :rows="6" />
      <AlertMessage v-else-if="error" type="error" :message="error" />

      <template v-else>
        <div class="grid grid-cols-3 gap-4">
          <button
            class="bg-white rounded-lg shadow p-4 text-center transition-colors"
            :class="statusFilter === 'pending' ? 'ring-2 ring-yellow-400' : 'hover:bg-gray-50'"
            @click="statusFilter = statusFilter === 'pending' ? null : 'pending'"
          >
            <div class="text-2xl font-bold text-yellow-600">{{ pendingCount }}</div>
            <div class="text-sm text-gray-600">En attente</div>
          </button>
          <button
            class="bg-white rounded-lg shadow p-4 text-center transition-colors"
            :class="statusFilter === 'approved' ? 'ring-2 ring-green-400' : 'hover:bg-gray-50'"
            @click="statusFilter = statusFilter === 'approved' ? null : 'approved'"
          >
            <div class="text-2xl font-bold text-green-600">{{ approvedCount }}</div>
            <div class="text-sm text-gray-600">Approuvées</div>
          </button>
          <button
            class="bg-white rounded-lg shadow p-4 text-center transition-colors"
            :class="statusFilter === 'declined' ? 'ring-2 ring-red-400' : 'hover:bg-gray-50'"
            @click="statusFilter = statusFilter === 'declined' ? null : 'declined'"
          >
            <div class="text-2xl font-bold text-red-600">{{ declinedCount }}</div>
            <div class="text-sm text-gray-600">Refusées</div>
          </button>
        </div>

        <div class="bg-white rounded-lg shadow">
          <div class="px-6 py-4 border-b border-gray-200">
            <h2 class="text-lg font-semibold text-gray-900">
              {{ filteredVideos.length }} vidéo{{ filteredVideos.length !== 1 ? 's' : '' }}
              <span v-if="statusFilter" class="text-sm font-normal text-gray-500 ml-1">
                (filtrées : {{ statusLabel }})
              </span>
            </h2>
          </div>

          <div v-if="filteredVideos.length === 0" class="px-6 py-12 text-center">
            <i
              class="i-heroicons-video-camera text-gray-400 text-5xl mx-auto block mb-4"
              aria-hidden="true"
            />
            <p class="text-sm text-gray-500">Aucune vidéo de présentation pour cet événement</p>
          </div>

          <ul v-else class="divide-y divide-gray-200">
            <li v-for="video in filteredVideos" :key="video.id" class="px-6 py-4">
              <div class="flex items-start gap-4">
                <div
                  class="w-64 shrink-0 rounded-lg overflow-hidden border border-gray-200 bg-black"
                >
                  <video
                    :src="video.url"
                    controls
                    preload="metadata"
                    class="w-full h-36 object-contain bg-black"
                  />
                </div>

                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <h3 class="text-base font-semibold text-gray-900 truncate">
                      Partenariat {{ video.partnership_id.slice(0, 8) }}
                    </h3>
                    <StatusBadge :status="video.status.toLowerCase()" />
                  </div>
                  <div class="mt-1 space-y-0.5">
                    <p class="text-sm text-gray-500">
                      Soumise le {{ formatDate(video.submitted_at) }}
                    </p>
                    <p v-if="video.reviewed_at" class="text-sm text-gray-500">
                      Traitée le {{ formatDate(video.reviewed_at) }}
                      <span v-if="video.reviewed_by"> par {{ video.reviewed_by }}</span>
                    </p>
                    <p v-if="video.decline_reason" class="text-sm text-red-600">
                      Motif : {{ video.decline_reason }}
                    </p>
                    <NuxtLink
                      :to="`/orgs/${orgSlug}/events/${eventSlug}/sponsors/${video.partnership_id}`"
                      class="text-sm text-primary-600 hover:text-primary-800 underline"
                    >
                      Voir le partenariat
                    </NuxtLink>
                  </div>
                </div>

                <div v-if="video.status === 'pending'" class="flex gap-2 shrink-0">
                  <UButton
                    color="success"
                    variant="outline"
                    size="sm"
                    icon="i-heroicons-check"
                    :loading="approvingId === video.id"
                    :disabled="!!approvingId || !!decliningId"
                    @click="handleApprove(video)"
                  >
                    Approuver
                  </UButton>
                  <UButton
                    color="error"
                    variant="outline"
                    size="sm"
                    icon="i-heroicons-x-mark"
                    :disabled="!!approvingId || !!decliningId"
                    @click="openDeclineModal(video)"
                  >
                    Refuser
                  </UButton>
                </div>
              </div>
            </li>
          </ul>
        </div>
      </template>
    </div>

    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="declineModalOpen"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="decline-modal-title"
        >
          <div
            class="fixed inset-0 bg-black bg-opacity-50"
            aria-hidden="true"
            @click="closeDeclineModal"
          />
          <div
            class="relative bg-white rounded-lg shadow-xl max-w-lg w-full transform transition-all"
          >
            <div class="px-6 py-4 border-b border-gray-200">
              <h3 id="decline-modal-title" class="text-lg font-semibold text-gray-900">
                Refuser la vidéo
              </h3>
            </div>
            <div class="px-6 py-4 space-y-4">
              <p class="text-sm text-gray-700">
                Refuser la vidéo de présentation soumise par ce partenariat ?
              </p>
              <div>
                <label for="decline-reason" class="block text-sm font-medium text-gray-700 mb-1">
                  Motif (optionnel)
                </label>
                <textarea
                  id="decline-reason"
                  v-model="declineReason"
                  rows="3"
                  maxlength="500"
                  :disabled="!!decliningId"
                  placeholder="Expliquez pourquoi cette vidéo est refusée..."
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:opacity-50"
                />
                <p class="text-xs text-gray-500 mt-1">{{ declineReason.length }} / 500</p>
              </div>
            </div>
            <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
              <UButton
                color="neutral"
                variant="ghost"
                :disabled="!!decliningId"
                @click="closeDeclineModal"
              >
                Annuler
              </UButton>
              <UButton color="error" :loading="!!decliningId" @click="handleDecline">
                Refuser
              </UButton>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </Dashboard>
</template>

<script setup lang="ts">
import authMiddleware from '~/middleware/auth';
import {
  listSupportVideos,
  approveSupportVideo,
  declineSupportVideo,
  type SupportVideoResponseSchema,
} from '~/utils/api';

definePageMeta({ middleware: authMiddleware, ssr: false });

const route = useRoute();
const toast = useToast();
const { footerLinks } = useDashboardLinks();

const orgSlug = computed(() => {
  const p = route.params.slug;
  return Array.isArray(p) ? p[0] : p;
});
const eventSlug = computed(() => {
  const p = route.params.eventSlug;
  return Array.isArray(p) ? p[0] : p;
});

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

const videos = ref<SupportVideoResponseSchema[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const statusFilter = ref<'pending' | 'approved' | 'declined' | null>(null);

const approvingId = ref<string | null>(null);
const decliningId = ref<string | null>(null);
const declineModalOpen = ref(false);
const videoToDecline = ref<SupportVideoResponseSchema | null>(null);
const declineReason = ref('');

const pendingCount = computed(() => videos.value.filter((v) => v.status === 'pending').length);
const approvedCount = computed(() => videos.value.filter((v) => v.status === 'approved').length);
const declinedCount = computed(() => videos.value.filter((v) => v.status === 'declined').length);

const filteredVideos = computed(() => {
  if (!statusFilter.value) return videos.value;
  const upper = statusFilter.value.toUpperCase();
  return videos.value.filter((v) => v.status === upper);
});

const statusLabel = computed(() => {
  if (statusFilter.value === 'pending') return 'En attente';
  if (statusFilter.value === 'approved') return 'Approuvées';
  if (statusFilter.value === 'declined') return 'Refusées';
  return '';
});

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

async function loadVideos() {
  try {
    loading.value = true;
    error.value = null;
    const response = await listSupportVideos(orgSlug.value, eventSlug.value, { page_size: 100 });
    videos.value = response.data.items ?? [];
  } catch {
    error.value = 'Impossible de charger les vidéos de présentation.';
  } finally {
    loading.value = false;
  }
}

async function handleApprove(video: SupportVideoResponseSchema) {
  try {
    approvingId.value = video.id;
    await approveSupportVideo(orgSlug.value, eventSlug.value, video.partnership_id, video.id, {});
    toast.add({
      title: 'Vidéo approuvée',
      description: 'Le partenariat a été notifié et le webhook a été déclenché.',
      color: 'success',
    });
    await loadVideos();
  } catch {
    toast.add({
      title: 'Erreur',
      description: "Impossible d'approuver cette vidéo",
      color: 'error',
    });
  } finally {
    approvingId.value = null;
  }
}

function openDeclineModal(video: SupportVideoResponseSchema) {
  videoToDecline.value = video;
  declineReason.value = '';
  declineModalOpen.value = true;
}

function closeDeclineModal() {
  if (decliningId.value) return;
  declineModalOpen.value = false;
  videoToDecline.value = null;
}

async function handleDecline() {
  if (!videoToDecline.value) return;
  try {
    decliningId.value = videoToDecline.value.id;
    await declineSupportVideo(
      orgSlug.value,
      eventSlug.value,
      videoToDecline.value.partnership_id,
      videoToDecline.value.id,
      { reason: declineReason.value || null },
    );
    toast.add({
      title: 'Vidéo refusée',
      description: 'Le partenariat a été notifié du refus.',
      color: 'success',
    });
    await loadVideos();
    closeDeclineModal();
  } catch {
    toast.add({
      title: 'Erreur',
      description: 'Impossible de refuser cette vidéo',
      color: 'error',
    });
  } finally {
    decliningId.value = null;
  }
}

onMounted(loadVideos);

useHead({ title: computed(() => 'Vidéos de présentation | DevLille') });
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-active .relative,
.modal-leave-active .relative {
  transition:
    transform 0.2s ease,
    opacity 0.2s ease;
}
.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
  opacity: 0;
}
</style>
