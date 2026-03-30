<template>
  <Dashboard :main-links="orgLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}`"
          icon="i-heroicons-arrow-left"
          color="neutral"
          variant="ghost"
          class="mb-2"
          label="Retour"
        />
        <PageTitle>Plan des Booths</PageTitle>
      </div>
    </div>

    <div class="p-6">
      <div class="bg-white rounded-lg shadow p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Plan des Booths</h2>
        <p class="text-sm text-gray-600 mb-6">
          Téléchargez le plan des booths pour cet événement (PNG, JPEG, GIF). Ce plan sera utilisé
          pour assigner les emplacements aux sponsors.
        </p>

        <div
          v-if="uploadError"
          class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 text-sm"
        >
          {{ uploadError }}
        </div>

        <div v-if="boothPlanUrl" class="mb-6">
          <h3 class="text-sm font-medium text-gray-900 mb-2">Plan actuel</h3>
          <div class="border border-gray-200 rounded-lg overflow-hidden">
            <img
              :src="boothPlanUrl"
              alt="Plan des booths"
              class="w-full object-contain max-h-[600px]"
            />
          </div>
        </div>

        <div class="flex gap-3 items-center">
          <input
            ref="fileInput"
            type="file"
            accept="image/png,image/jpeg,image/jpg,image/gif"
            class="hidden"
            @change="handleFileChange"
          />
          <UButton color="primary" :loading="isUploading" @click="() => fileInput?.click()">
            <i class="i-heroicons-arrow-up-tray mr-2" aria-hidden="true" />
            {{ boothPlanUrl ? 'Remplacer le plan' : 'Télécharger le plan' }}
          </UButton>

          <span v-if="selectedFile" class="text-sm text-gray-600">
            {{ selectedFile.name }}
          </span>
        </div>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { postOrgsEventsBoothPlan } from "~/utils/api";
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

const { eventLinks: orgLinks } = useEventLinks(orgSlug.value, eventSlug.value);

const fileInput = ref<HTMLInputElement | null>(null);
const selectedFile = ref<File | null>(null);
const isUploading = ref(false);
const uploadError = ref<string | null>(null);

const boothPlanUrl = computed(() => boothPlanStore.getUrl(eventSlug.value));

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (!file) return;

  if (!["image/png", "image/jpeg", "image/jpg", "image/gif"].includes(file.type)) {
    uploadError.value = "Le fichier doit être au format PNG, JPEG ou GIF";
    return;
  }

  uploadError.value = null;
  selectedFile.value = file;
  uploadBoothPlan(file);
}

async function uploadBoothPlan(file: File) {
  isUploading.value = true;
  uploadError.value = null;

  try {
    const response = await postOrgsEventsBoothPlan(orgSlug.value, eventSlug.value, { file });
    boothPlanStore.setUrl(eventSlug.value, response.data.url);

    const toast = useCustomToast();
    toast.success("Le plan des booths a été téléchargé avec succès");

    selectedFile.value = null;
    if (fileInput.value) {
      fileInput.value.value = "";
    }
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } };
    uploadError.value =
      axiosError.response?.data?.message ?? "Impossible de télécharger le plan des booths";

    const toast = useCustomToast();
    toast.error(uploadError.value ?? "Erreur lors du téléchargement");
  } finally {
    isUploading.value = false;
  }
}

useHead({
  title: computed(() => `Plan des Booths | DevLille`),
});
</script>
