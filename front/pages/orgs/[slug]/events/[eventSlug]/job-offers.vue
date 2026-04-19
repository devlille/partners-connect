<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <PageTitle>Offres d'emploi</PageTitle>
      <p class="mt-1 text-sm text-gray-500">
        Offres d'emploi soumises par les sponsors pour cet événement
      </p>
    </div>

    <div class="p-6 space-y-6">
      <TableSkeleton v-if="loading" :columns="5" :rows="8" />
      <AlertMessage v-else-if="error" type="error" :message="error" />

      <template v-else>
        <!-- Compteurs par statut -->
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

        <!-- Liste -->
        <div class="bg-white rounded-lg shadow">
          <div class="px-6 py-4 border-b border-gray-200">
            <h2 class="text-lg font-semibold text-gray-900">
              {{ filteredPromotions.length }} offre{{ filteredPromotions.length !== 1 ? 's' : '' }}
              <span v-if="statusFilter" class="text-sm font-normal text-gray-500 ml-1">
                (filtrées : {{ statusLabel }})
              </span>
            </h2>
          </div>

          <div v-if="filteredPromotions.length === 0" class="px-6 py-12 text-center">
            <i class="i-heroicons-briefcase text-gray-400 text-5xl mx-auto block mb-4" aria-hidden="true" />
            <p class="text-sm text-gray-500">Aucune offre d'emploi pour cet événement</p>
          </div>

          <ul v-else class="divide-y divide-gray-200">
            <li
              v-for="promotion in filteredPromotions"
              :key="promotion.id"
              class="px-6 py-4 hover:bg-gray-50"
            >
              <div class="flex items-start justify-between gap-4">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <h3 class="text-base font-semibold text-gray-900 truncate">
                      {{ promotion.job_offer.title }}
                    </h3>
                    <StatusBadge :status="promotion.status" />
                  </div>
                  <div class="mt-1 space-y-0.5">
                    <p class="text-sm text-gray-500">
                      Soumis le {{ formatDate(promotion.promoted_at) }}
                    </p>
                    <p v-if="promotion.reviewed_at" class="text-sm text-gray-500">
                      Traité le {{ formatDate(promotion.reviewed_at) }}
                    </p>
                    <p v-if="promotion.decline_reason" class="text-sm text-red-600">
                      Motif : {{ promotion.decline_reason }}
                    </p>
                    <a
                      :href="promotion.job_offer.url"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="text-sm text-primary-600 hover:text-primary-800 underline"
                    >
                      Voir l'offre
                    </a>
                  </div>
                </div>

                <div v-if="promotion.status === 'pending'" class="flex gap-2 shrink-0">
                  <UButton
                    color="success"
                    variant="outline"
                    size="sm"
                    icon="i-heroicons-check"
                    :loading="approvingId === promotion.id"
                    :disabled="!!approvingId || !!decliningId"
                    @click="handleApprove(promotion)"
                  >
                    Approuver
                  </UButton>
                  <UButton
                    color="error"
                    variant="outline"
                    size="sm"
                    icon="i-heroicons-x-mark"
                    :disabled="!!approvingId || !!decliningId"
                    @click="openDeclineModal(promotion)"
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

    <!-- Modale de refus -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="declineModalOpen"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="decline-modal-title"
        >
          <div class="fixed inset-0 bg-black bg-opacity-50" aria-hidden="true" @click="closeDeclineModal" />
          <div class="relative bg-white rounded-lg shadow-xl max-w-lg w-full transform transition-all">
            <div class="px-6 py-4 border-b border-gray-200">
              <h3 id="decline-modal-title" class="text-lg font-semibold text-gray-900">
                Refuser l'offre
              </h3>
            </div>
            <div class="px-6 py-4 space-y-4">
              <p class="text-sm text-gray-700">
                Refuser l'offre <strong>{{ promotionToDecline?.job_offer.title }}</strong> ?
              </p>
              <div>
                <label for="decline-reason" class="block text-sm font-medium text-gray-700 mb-1">
                  Motif (optionnel)
                </label>
                <textarea
                  id="decline-reason"
                  v-model="declineReason"
                  rows="3"
                  :disabled="!!decliningId"
                  placeholder="Expliquez pourquoi cette offre est refusée..."
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:opacity-50"
                />
              </div>
            </div>
            <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
              <UButton color="neutral" variant="ghost" :disabled="!!decliningId" @click="closeDeclineModal">
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
  listEventJobOfferPromotions,
  approveJobOfferPromotion,
  declineJobOfferPromotion,
  type JobOfferPromotionResponseSchema,
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

const promotions = ref<JobOfferPromotionResponseSchema[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const statusFilter = ref<'pending' | 'approved' | 'declined' | null>(null);

const approvingId = ref<string | null>(null);
const decliningId = ref<string | null>(null);
const declineModalOpen = ref(false);
const promotionToDecline = ref<JobOfferPromotionResponseSchema | null>(null);
const declineReason = ref('');

const pendingCount = computed(() => promotions.value.filter((p) => p.status === 'pending').length);
const approvedCount = computed(() => promotions.value.filter((p) => p.status === 'approved').length);
const declinedCount = computed(() => promotions.value.filter((p) => p.status === 'declined').length);

const filteredPromotions = computed(() =>
  statusFilter.value ? promotions.value.filter((p) => p.status === statusFilter.value) : promotions.value,
);

const statusLabel = computed(() => {
  if (statusFilter.value === 'pending') return 'En attente';
  if (statusFilter.value === 'approved') return 'Approuvées';
  if (statusFilter.value === 'declined') return 'Refusées';
  return '';
});

function formatDate(dateString: string) {
  return new Date(dateString).toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

async function loadPromotions() {
  try {
    loading.value = true;
    error.value = null;
    const response = await listEventJobOfferPromotions(orgSlug.value, eventSlug.value);
    promotions.value = response.data.items ?? [];
  } catch {
    error.value = "Impossible de charger les offres d'emploi";
  } finally {
    loading.value = false;
  }
}

async function handleApprove(promotion: JobOfferPromotionResponseSchema) {
  try {
    approvingId.value = promotion.id;
    await approveJobOfferPromotion(orgSlug.value, eventSlug.value, promotion.partnership_id, promotion.id, {});
    toast.add({ title: 'Offre approuvée', description: `"${promotion.job_offer.title}" a été approuvée`, color: 'success' });
    await loadPromotions();
  } catch {
    toast.add({ title: 'Erreur', description: "Impossible d'approuver cette offre", color: 'error' });
  } finally {
    approvingId.value = null;
  }
}

function openDeclineModal(promotion: JobOfferPromotionResponseSchema) {
  promotionToDecline.value = promotion;
  declineReason.value = '';
  declineModalOpen.value = true;
}

function closeDeclineModal() {
  if (decliningId.value) return;
  declineModalOpen.value = false;
  promotionToDecline.value = null;
}

async function handleDecline() {
  if (!promotionToDecline.value) return;
  try {
    decliningId.value = promotionToDecline.value.id;
    await declineJobOfferPromotion(
      orgSlug.value,
      eventSlug.value,
      promotionToDecline.value.partnership_id,
      promotionToDecline.value.id,
      { reason: declineReason.value || null },
    );
    toast.add({ title: 'Offre refusée', description: `"${promotionToDecline.value.job_offer.title}" a été refusée`, color: 'success' });
    await loadPromotions();
    closeDeclineModal();
  } catch {
    toast.add({ title: 'Erreur', description: 'Impossible de refuser cette offre', color: 'error' });
  } finally {
    decliningId.value = null;
  }
}

onMounted(loadPromotions);

useHead({ title: computed(() => `Offres d'emploi | DevLille`) });
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
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
  opacity: 0;
}
</style>
