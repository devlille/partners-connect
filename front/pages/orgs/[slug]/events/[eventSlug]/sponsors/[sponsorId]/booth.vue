<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div>
        <PageTitle>{{ companyName || 'Sponsor' }}</PageTitle>
        <p class="text-sm text-gray-600 mt-1">Booth</p>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="2" :rows="4" />

      <div
        v-else-if="loadError"
        class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded"
      >
        {{ loadError }}
      </div>

      <div v-else class="space-y-6">
        <!-- Plan des booths -->
        <div class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Plan des Booths</h2>

          <div v-if="boothPlanUrl" class="border border-gray-200 rounded-lg overflow-hidden">
            <img
              :src="boothPlanUrl"
              alt="Plan des booths"
              class="w-full object-contain max-h-[600px]"
            />
          </div>

          <div
            v-else
            class="flex items-center gap-3 p-4 bg-gray-50 border border-gray-200 rounded-lg text-sm text-gray-600"
          >
            <i class="i-heroicons-information-circle text-xl text-gray-400" aria-hidden="true" />
            <span>
              Aucun plan des booths disponible. Téléchargez-en un depuis la page
              <UButton
                :to="`/orgs/${orgSlug}/events/${eventSlug}/booth-plan`"
                variant="link"
                color="primary"
                class="p-0 h-auto"
                label="Plan des Booths"
              />
              de l'événement.
            </span>
          </div>
        </div>

        <!-- Assignation du booth -->
        <div class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-2">Emplacement du Booth</h2>
          <p class="text-sm text-gray-600 mb-4">Assignez un emplacement de booth à ce sponsor.</p>

          <div
            v-if="saveError"
            class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 text-sm"
          >
            {{ saveError }}
          </div>

          <div class="flex gap-3 items-end">
            <div class="flex-1">
              <label for="booth-location" class="block text-sm font-medium text-gray-700 mb-1">
                Identifiant du booth
              </label>
              <UInput
                id="booth-location"
                v-model="boothLocation"
                placeholder="Ex: A1, B3, Stand 12..."
                class="w-full"
              />
            </div>

            <UButton
              color="primary"
              :loading="isSaving"
              :disabled="!boothLocation.trim()"
              @click="saveboothLocation"
            >
              <i class="i-heroicons-check mr-2" aria-hidden="true" />
              Assigner
            </UButton>
          </div>

          <div v-if="currentLocation" class="mt-4 flex items-center gap-2 text-sm text-gray-600">
            <i class="i-heroicons-map-pin text-primary-600" aria-hidden="true" />
            <span
              >Emplacement actuel :
              <strong class="text-gray-900">{{ currentLocation }}</strong></span
            >
          </div>
        </div>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventsPartnershipDetailed, putOrgsEventsPartnershipBoothLocation } from "~/utils/api";
import { useBoothPlanStore } from "~/stores/boothPlan";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const { footerLinks } = useDashboardLinks();
const boothPlanStore = useBoothPlanStore();

definePageMeta({
  middleware: authMiddleware,
  ssr: false,
});

const orgSlug = computed(() => {
  const params = route.params.slug;
  return Array.isArray(params) ? params[0] : (params as string);
});

const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  return Array.isArray(params) ? params[1] : (params as string);
});

const sponsorId = computed(() => {
  const params = route.params.sponsorId;
  return Array.isArray(params) ? params[0] : (params as string);
});

const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

const companyName = ref<string | null>(null);
const loading = ref(true);
const loadError = ref<string | null>(null);
const boothLocation = ref("");
const currentLocation = ref<string | null>(null);
const isSaving = ref(false);
const saveError = ref<string | null>(null);

const boothPlanUrl = computed(() => boothPlanStore.getUrl(eventSlug.value));

async function loadPartnership() {
  try {
    loading.value = true;
    loadError.value = null;

    const response = await getEventsPartnershipDetailed(eventSlug.value, sponsorId.value);
    companyName.value = response.data.company.name;

    const boothPlanImageUrl = (response.data.event as { booth_plan_image_url?: string | null }).booth_plan_image_url;
    if (boothPlanImageUrl) {
      boothPlanStore.setUrl(eventSlug.value, boothPlanImageUrl);
    }

    const boothLocationData = (response.data.partnership as { booth_location?: string | null }).booth_location;
    if (boothLocationData) {
      currentLocation.value = boothLocationData;
      boothLocation.value = boothLocationData;
    }
  } catch {
    loadError.value = "Impossible de charger les informations du sponsor";
  } finally {
    loading.value = false;
  }
}

async function saveboothLocation() {
  if (!boothLocation.value.trim()) return;

  isSaving.value = true;
  saveError.value = null;

  try {
    const response = await putOrgsEventsPartnershipBoothLocation(
      orgSlug.value,
      eventSlug.value,
      sponsorId.value,
      { location: boothLocation.value.trim() }
    );

    currentLocation.value = response.data.location;

    const toast = useCustomToast();
    toast.success("L'emplacement du booth a été assigné avec succès");
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } };
    saveError.value =
      axiosError.response?.data?.message ?? "Impossible d'assigner l'emplacement du booth";

    const toast = useCustomToast();
    toast.error(saveError.value ?? "Erreur lors de l'assignation");
  } finally {
    isSaving.value = false;
  }
}

onMounted(loadPartnership);

watch([orgSlug, eventSlug, sponsorId], loadPartnership);

useHead({
  title: computed(() => `Booth - ${companyName.value || 'Sponsor'} | DevLille`),
});
</script>
