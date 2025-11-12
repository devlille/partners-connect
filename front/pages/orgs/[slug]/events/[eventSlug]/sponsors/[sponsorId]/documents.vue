<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div>
        <PageTitle>{{ partnership?.company_name || 'Sponsor' }}</PageTitle>
        <p class="text-sm text-gray-600 mt-1">Documents</p>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else class="bg-white rounded-lg shadow p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Documents</h2>

        <div v-if="agreementError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
          {{ agreementError }}
        </div>

        <div class="space-y-6">
          <!-- Convention de partenariat -->
          <div class="bg-gray-50 rounded-lg p-6">
            <div class="flex items-start gap-4">
              <div class="shrink-0">
                <i class="i-heroicons-document-text text-3xl text-primary-600" aria-hidden="true" />
              </div>
              <div class="flex-1">
                <h3 class="text-sm font-medium text-gray-900 mb-2">Convention de partenariat</h3>
                <p class="text-sm text-gray-600 mb-4">
                  Générez la convention de partenariat au format PDF pour ce sponsor.
                </p>

                <div class="flex gap-3 items-center">
                  <UButton
                    color="primary"
                    :loading="isGeneratingAgreement"
                    :aria-label="`Générer la convention pour ${partnership?.company_name}`"
                    @click="handleGenerateAgreement"
                  >
                    <i class="i-heroicons-document-arrow-down mr-2" aria-hidden="true" />
                    Générer la convention
                  </UButton>

                  <a
                    v-if="agreementUrl"
                    :href="agreementUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="inline-flex items-center gap-2 text-primary-600 hover:text-primary-800 font-medium text-sm"
                  >
                    Voir la dernière convention générée
                    <i class="i-heroicons-arrow-top-right-on-square" aria-hidden="true" />
                  </a>
                </div>
              </div>
            </div>
          </div>

          <!-- Section pour futurs documents -->
          <div class="border-t pt-6">
            <p class="text-sm text-gray-500 italic">
              D'autres documents seront disponibles prochainement (factures, contrats signés, etc.)
            </p>
          </div>
        </div>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type { ExtendedPartnershipItem } from "~/types/partnership";

const route = useRoute();
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

const sponsorId = computed(() => {
  const params = route.params.sponsorId;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const partnership = ref<ExtendedPartnershipItem | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

// États pour les documents
const isGeneratingAgreement = ref(false);
const agreementUrl = ref<string | null>(null);
const agreementError = ref<string | null>(null);

// Menu contextuel pour la page du sponsor
const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

async function loadPartnership() {
  try {
    loading.value = true;
    error.value = null;

    const response = await getOrgsEventsPartnership(orgSlug.value, eventSlug.value);
    const found = response.data.find(p => p.id === sponsorId.value);

    if (!found) {
      error.value = 'Sponsor non trouvé';
      return;
    }

    partnership.value = found;
  } catch (err) {
    console.error('Failed to load partnership:', err);
    error.value = 'Impossible de charger les informations du sponsor';
  } finally {
    loading.value = false;
  }
}

/**
 * Génère la convention de partenariat
 */
async function handleGenerateAgreement() {
  if (!partnership.value) return;

  agreementError.value = null;
  isGeneratingAgreement.value = true;

  try {
    const { postOrgsEventsPartnershipAgreement } = await import('~/utils/api');

    const response = await postOrgsEventsPartnershipAgreement(
      orgSlug.value,
      eventSlug.value,
      sponsorId.value
    );

    if (response.data.url) {
      agreementUrl.value = response.data.url;

      // Ouvrir le PDF dans un nouvel onglet
      window.open(response.data.url, '_blank');

      const toast = useCustomToast();
      toast.success('La convention a été générée avec succès');
    } else {
      agreementError.value = 'Aucune URL de convention n\'a été retournée';
    }
  } catch (err: any) {
    console.error('Failed to generate agreement:', err);

    // Essayer d'extraire le message d'erreur du serveur
    if (err.response?.data?.message) {
      agreementError.value = err.response.data.message;
    } else if (err.response) {
      if (err.response.status === 404) {
        agreementError.value = 'Partenariat introuvable';
      } else if (err.response.status === 403) {
        agreementError.value = 'Vous n\'êtes pas autorisé à générer cette convention';
      } else {
        agreementError.value = 'Une erreur est survenue lors de la génération de la convention';
      }
    } else {
      agreementError.value = 'Une erreur est survenue lors de la génération de la convention';
    }

    // Afficher aussi l'erreur dans un toast
    const toast = useCustomToast();
    toast.error(agreementError.value);
  } finally {
    isGeneratingAgreement.value = false;
  }
}

onMounted(() => {
  loadPartnership();
});

watch([orgSlug, eventSlug, sponsorId], () => {
  loadPartnership();
});

useHead({
  title: computed(() => `Documents - ${partnership.value?.company_name || 'Sponsor'} | DevLille`)
});
</script>
