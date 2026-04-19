<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div>
        <PageTitle>Offres d'emploi</PageTitle>
        <p class="mt-1 text-sm text-gray-500">
          Offres d'emploi de {{ partnershipName }}
        </p>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="8" />
      <AlertMessage v-else-if="error" type="error" :message="error" />

      <div v-else class="bg-white rounded-lg shadow">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">
            Offres d'emploi ({{ jobOffers.length }})
          </h2>
        </div>

        <div v-if="jobOffers.length === 0" class="px-6 py-12 text-center">
          <i class="i-heroicons-briefcase text-gray-400 text-5xl mx-auto block mb-4" aria-hidden="true" />
          <h3 class="mt-2 text-sm font-medium text-gray-900">Aucune offre d'emploi</h3>
          <p class="mt-1 text-sm text-gray-500">Cette entreprise n'a pas encore créé d'offre d'emploi.</p>
        </div>

        <ul v-else class="divide-y divide-gray-200">
          <li
            v-for="job in jobOffers"
            :key="job.id"
            class="px-6 py-4 hover:bg-gray-50"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 flex-wrap">
                  <h3 class="text-base font-semibold text-gray-900">{{ job.title }}</h3>
                  <StatusBadge v-if="promotionByJobId[job.id]" :status="promotionByJobId[job.id].status" />
                </div>
                <div class="mt-2 space-y-1">
                  <p class="text-sm text-gray-600">
                    <span class="font-medium">Localisation :</span> {{ job.location }}
                  </p>
                  <p v-if="job.salary" class="text-sm text-gray-600">
                    <span class="font-medium">Salaire :</span> {{ job.salary }}
                  </p>
                  <p v-if="job.experience_years" class="text-sm text-gray-600">
                    <span class="font-medium">Expérience :</span> {{ job.experience_years }} an(s)
                  </p>
                  <p class="text-sm text-gray-600">
                    <span class="font-medium">Publié le :</span> {{ formatDate(job.publication_date) }}
                  </p>
                  <p v-if="promotionByJobId[job.id]?.decline_reason" class="text-sm text-red-600">
                    Motif de refus : {{ promotionByJobId[job.id].decline_reason }}
                  </p>
                  <a
                    :href="job.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="text-sm text-primary-600 hover:text-primary-800 underline"
                  >
                    Voir l'offre complète
                  </a>
                </div>
              </div>

              <div class="flex flex-col items-end gap-2 shrink-0">
                <UButton
                  v-if="!promotionByJobId[job.id]"
                  color="primary"
                  variant="outline"
                  size="sm"
                  icon="i-heroicons-paper-airplane"
                  :loading="promotingJobId === job.id"
                  :disabled="!!promotingJobId"
                  @click="handlePromote(job)"
                >
                  Associer à l'événement
                </UButton>

                <template v-if="promotionByJobId[job.id]?.status === 'pending'">
                  <UButton
                    color="success"
                    variant="outline"
                    size="sm"
                    icon="i-heroicons-check"
                    :loading="approvingId === promotionByJobId[job.id].id"
                    :disabled="!!approvingId || !!decliningId"
                    @click="handleApprove(promotionByJobId[job.id])"
                  >
                    Approuver
                  </UButton>
                  <UButton
                    color="error"
                    variant="outline"
                    size="sm"
                    icon="i-heroicons-x-mark"
                    :disabled="!!approvingId || !!decliningId"
                    @click="openDeclineModal(promotionByJobId[job.id])"
                  >
                    Refuser
                  </UButton>
                </template>
              </div>
            </div>
          </li>
        </ul>
      </div>
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
                Refuser la promotion de <strong>{{ promotionToDecline?.job_offer.title }}</strong> ?
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
  getEventsPartnershipDetailed,
  getCompaniesJobOffers,
  listEventJobOfferPromotions,
  promoteJobOfferToPartnership,
  approveJobOfferPromotion,
  declineJobOfferPromotion,
  type JobOfferResponseSchema,
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
const sponsorId = computed(() => {
  const p = route.params.sponsorId;
  return Array.isArray(p) ? p[0] : p;
});

const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

const partnershipName = ref('');
const companyId = ref('');
const jobOffers = ref<JobOfferResponseSchema[]>([]);
const promotions = ref<JobOfferPromotionResponseSchema[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

const promotingJobId = ref<string | null>(null);
const approvingId = ref<string | null>(null);
const decliningId = ref<string | null>(null);
const declineModalOpen = ref(false);
const promotionToDecline = ref<JobOfferPromotionResponseSchema | null>(null);
const declineReason = ref('');

const promotionByJobId = computed(() =>
  Object.fromEntries(promotions.value.map((p) => [p.job_offer_id, p])),
);

function formatDate(dateString: string) {
  return new Date(dateString).toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

async function load() {
  try {
    loading.value = true;
    error.value = null;

    const partnershipRes = await getEventsPartnershipDetailed(eventSlug.value, sponsorId.value);
    const { company } = partnershipRes.data;
    partnershipName.value = company.name;
    companyId.value = company.id;

    const [firstPageRes, promotionsRes] = await Promise.all([
      getCompaniesJobOffers(company.id, { page: 1, page_size: 100 }),
      listEventJobOfferPromotions(orgSlug.value, eventSlug.value, { page_size: 100 }),
    ]);

    const allOffers = [...firstPageRes.data.items];
    const totalPages = Math.ceil(firstPageRes.data.total / 100);
    if (totalPages > 1) {
      const remainingPages = await Promise.all(
        Array.from({ length: totalPages - 1 }, (_, i) =>
          getCompaniesJobOffers(company.id, { page: i + 2, page_size: 100 }),
        ),
      );
      for (const page of remainingPages) allOffers.push(...page.data.items);
    }

    jobOffers.value = allOffers;
    promotions.value = (promotionsRes.data.items ?? []).filter(
      (p) => p.partnership_id === sponsorId.value,
    );
  } catch {
    error.value = "Impossible de charger les offres d'emploi";
  } finally {
    loading.value = false;
  }
}

async function handlePromote(job: JobOfferResponseSchema) {
  try {
    promotingJobId.value = job.id;
    await promoteJobOfferToPartnership(companyId.value, sponsorId.value, { job_offer_id: job.id });
    toast.add({ title: 'Offre associée', description: `"${job.title}" a été soumise à l'événement`, color: 'success' });
    const promotionsRes = await listEventJobOfferPromotions(orgSlug.value, eventSlug.value, { page_size: 100 });
    promotions.value = (promotionsRes.data.items ?? []).filter((p) => p.partnership_id === sponsorId.value);
  } catch (err: any) {
    toast.add({
      title: 'Erreur',
      description: err?.response?.data?.message ?? "Impossible d'associer cette offre",
      color: 'error',
    });
  } finally {
    promotingJobId.value = null;
  }
}

async function handleApprove(promotion: JobOfferPromotionResponseSchema) {
  try {
    approvingId.value = promotion.id;
    await approveJobOfferPromotion(orgSlug.value, eventSlug.value, sponsorId.value, promotion.id, {});
    toast.add({ title: 'Offre approuvée', description: `"${promotion.job_offer.title}" a été approuvée`, color: 'success' });
    const promotionsRes = await listEventJobOfferPromotions(orgSlug.value, eventSlug.value, { page_size: 100 });
    promotions.value = (promotionsRes.data.items ?? []).filter((p) => p.partnership_id === sponsorId.value);
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
      sponsorId.value,
      promotionToDecline.value.id,
      { reason: declineReason.value || null },
    );
    toast.add({ title: 'Offre refusée', description: `"${promotionToDecline.value.job_offer.title}" a été refusée`, color: 'success' });
    const promotionsRes = await listEventJobOfferPromotions(orgSlug.value, eventSlug.value, { page_size: 100 });
    promotions.value = (promotionsRes.data.items ?? []).filter((p) => p.partnership_id === sponsorId.value);
    closeDeclineModal();
  } catch {
    toast.add({ title: 'Erreur', description: 'Impossible de refuser cette offre', color: 'error' });
  } finally {
    decliningId.value = null;
  }
}

onMounted(load);

useHead({ title: computed(() => `Offres d'emploi - ${partnershipName.value} | DevLille`) });
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
