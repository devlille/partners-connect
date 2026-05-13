<template>
  <NuxtLayout
    name="minimal-sidebar"
    :sidebar-title="partnership?.company_name || 'Partenariat'"
    :sidebar-links="sidebarLinks"
  >
    <div class="min-h-screen bg-gray-50">
      <main class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8" role="main">
        <header class="bg-white rounded-lg shadow p-6 mb-6">
          <PageTitle>Vidéo de présentation</PageTitle>
          <p class="text-sm text-gray-600 mt-1">
            Soumettez une vidéo qui sera diffusée pendant l'événement après validation.
          </p>
        </header>

        <div v-if="loading" role="status" aria-live="polite">
          <TableSkeleton :columns="1" :rows="4" />
          <span class="sr-only">Chargement de la vidéo...</span>
        </div>

        <AlertMessage v-else-if="error" type="error" :message="error" />

        <div v-else class="space-y-6">
          <section class="bg-white rounded-lg shadow p-6" aria-labelledby="status-heading">
            <h2 id="status-heading" class="text-lg font-semibold text-gray-900 mb-4">
              Statut de votre vidéo
            </h2>

            <div v-if="!video" class="space-y-4">
              <div class="flex items-start gap-3 p-4 bg-gray-50 border border-gray-200 rounded-lg">
                <i
                  class="i-heroicons-information-circle text-xl text-gray-400 mt-0.5"
                  aria-hidden="true"
                />
                <p class="text-sm text-gray-700">
                  Aucune vidéo n'a encore été soumise. Envoyez votre vidéo ci-dessous ; elle sera
                  examinée par les organisateurs avant publication.
                </p>
              </div>
              <SupportVideoUpload
                :event-slug="eventSlug"
                :partnership-id="partnershipId"
                @uploaded="loadVideo"
              />
            </div>

            <div v-else class="space-y-4">
              <div
                v-if="video.status === 'PENDING'"
                class="flex items-start gap-3 p-4 bg-yellow-50 border border-yellow-200 rounded-lg"
              >
                <i class="i-heroicons-clock text-xl text-yellow-600 mt-0.5" aria-hidden="true" />
                <div>
                  <p class="text-sm font-medium text-yellow-900">En attente de validation</p>
                  <p class="text-xs text-yellow-800 mt-1">
                    Soumise le {{ formatDate(video.submitted_at) }}. Vous pouvez la remplacer tant
                    qu'elle n'a pas été examinée.
                  </p>
                </div>
              </div>

              <div
                v-else-if="video.status === 'APPROVED'"
                class="flex items-start gap-3 p-4 bg-green-50 border border-green-200 rounded-lg"
              >
                <i
                  class="i-heroicons-check-circle text-xl text-green-600 mt-0.5"
                  aria-hidden="true"
                />
                <div>
                  <p class="text-sm font-medium text-green-900">Vidéo validée</p>
                  <p v-if="video.reviewed_at" class="text-xs text-green-800 mt-1">
                    Validée le {{ formatDate(video.reviewed_at) }}.
                  </p>
                </div>
              </div>

              <div
                v-else-if="video.status === 'DECLINED'"
                class="flex items-start gap-3 p-4 bg-red-50 border border-red-200 rounded-lg"
              >
                <i class="i-heroicons-x-circle text-xl text-red-600 mt-0.5" aria-hidden="true" />
                <div class="flex-1">
                  <p class="text-sm font-medium text-red-900">Vidéo refusée</p>
                  <p v-if="video.reviewed_at" class="text-xs text-red-800 mt-1">
                    Refusée le {{ formatDate(video.reviewed_at) }}.
                  </p>
                  <p v-if="video.decline_reason" class="text-sm text-red-800 mt-2">
                    <span class="font-medium">Motif :</span> {{ video.decline_reason }}
                  </p>
                </div>
              </div>

              <div class="rounded-lg overflow-hidden border border-gray-200 bg-black">
                <video :src="video.url" controls preload="metadata" class="w-full max-h-[480px]" />
              </div>

              <div v-if="canResubmit" class="pt-2 border-t border-gray-200">
                <h3 class="text-sm font-semibold text-gray-900 mb-3">
                  {{
                    video.status === 'DECLINED'
                      ? 'Soumettre une nouvelle version'
                      : 'Remplacer la vidéo'
                  }}
                </h3>
                <SupportVideoUpload
                  :event-slug="eventSlug"
                  :partnership-id="partnershipId"
                  replace-existing
                  @uploaded="loadVideo"
                />
              </div>
            </div>
          </section>
        </div>
      </main>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import { getSupportVideo, type SupportVideoResponseSchema } from '~/utils/api';

definePageMeta({
  auth: false,
  ssr: false,
  validate: async (route) => {
    const eventSlug = Array.isArray(route.params.eventSlug)
      ? route.params.eventSlug[0]
      : route.params.eventSlug;
    const partnershipId = Array.isArray(route.params.partnershipId)
      ? route.params.partnershipId[0]
      : route.params.partnershipId;
    const isValidFormat = /^[a-zA-Z0-9-_]+$/;
    return isValidFormat.test(eventSlug) && isValidFormat.test(partnershipId);
  },
});

const { eventSlug, partnershipId, partnership, loading: partnershipLoading, error: partnershipError, loadPartnership } =
  usePublicPartnership();
const { sidebarLinks } = usePublicPartnershipLinks();

const video = ref<SupportVideoResponseSchema | null>(null);
const videoLoading = ref(true);
const videoError = ref<string | null>(null);

const loading = computed(() => partnershipLoading.value || videoLoading.value);
const error = computed(() => partnershipError.value || videoError.value);

const canResubmit = computed(
  () => video.value?.status === 'PENDING' || video.value?.status === 'DECLINED',
);

async function loadVideo() {
  videoLoading.value = true;
  videoError.value = null;
  try {
    const response = await getSupportVideo(eventSlug.value, partnershipId.value);
    video.value = response.data;
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } }).response?.status;
    if (status === 404) {
      video.value = null;
    } else {
      videoError.value = 'Impossible de charger la vidéo de présentation.';
    }
  } finally {
    videoLoading.value = false;
  }
}

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

onMounted(() => {
  loadPartnership();
  loadVideo();
});

watch([eventSlug, partnershipId], () => {
  loadPartnership();
  loadVideo();
});

useHead({
  title: computed(
    () => `Vidéo de présentation - ${partnership.value?.company_name || 'Partenariat'} | DevLille`,
  ),
});
</script>
