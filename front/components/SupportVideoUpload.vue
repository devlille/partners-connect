<template>
  <div class="space-y-4">
    <div>
      <p class="text-xs text-gray-500">Format : MP4 ou WebM. Taille maximale : 500 MB.</p>
    </div>

    <div v-if="!selectedFile">
      <div
        :class="[
          'w-full border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors',
          isDragging
            ? 'border-primary-500 bg-primary-50'
            : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50',
          disabled ? 'opacity-50 cursor-not-allowed' : '',
        ]"
        role="button"
        tabindex="0"
        :aria-disabled="disabled"
        :aria-label="dropZoneLabel"
        @click="triggerFileInput"
        @keydown.enter.prevent="triggerFileInput"
        @keydown.space.prevent="triggerFileInput"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="handleDrop"
      >
        <div class="flex flex-col items-center">
          <i class="i-heroicons-cloud-arrow-up text-4xl text-gray-400 mb-3" aria-hidden="true" />
          <p class="text-sm font-medium text-gray-700 mb-1">
            Cliquez pour sélectionner ou glissez-déposez
          </p>
          <p class="text-xs text-gray-500">MP4 ou WebM, jusqu'à 500 MB</p>
        </div>
      </div>
    </div>

    <div v-else class="space-y-3">
      <div class="flex items-start gap-3 p-4 bg-gray-50 border border-gray-200 rounded-lg">
        <i class="i-heroicons-film text-2xl text-primary-600 mt-0.5" aria-hidden="true" />
        <div class="flex-1 min-w-0">
          <p class="text-sm font-medium text-gray-900 truncate">{{ selectedFile.name }}</p>
          <p class="text-xs text-gray-500 mt-0.5">{{ formatFileSize(selectedFile.size) }}</p>
        </div>
      </div>

      <div v-if="uploading" class="space-y-1">
        <div class="w-full bg-gray-200 rounded-full h-2 overflow-hidden">
          <div
            class="bg-primary-600 h-2 transition-[width] duration-150"
            :style="{ width: `${progress}%` }"
            role="progressbar"
            aria-label="Progression de l'upload"
            :aria-valuenow="progress"
            aria-valuemin="0"
            aria-valuemax="100"
          />
        </div>
        <p class="text-xs text-gray-500 text-right">{{ progress }} %</p>
      </div>

      <div class="flex gap-2">
        <UButton
          color="primary"
          icon="i-heroicons-arrow-up-tray"
          :loading="uploading"
          :disabled="uploading"
          @click="handleUpload"
        >
          {{ replaceExisting ? 'Remplacer la vidéo' : 'Envoyer la vidéo' }}
        </UButton>
        <UButton color="neutral" variant="outline" :disabled="uploading" @click="cancelUpload">
          Annuler
        </UButton>
      </div>
    </div>

    <input
      ref="fileInput"
      type="file"
      accept="video/mp4,video/webm"
      class="hidden"
      @change="handleFileSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { uploadSupportVideo } from '~/utils/api';

interface Props {
  eventSlug: string;
  partnershipId: string;
  replaceExisting?: boolean;
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  replaceExisting: false,
  disabled: false,
});

const emit = defineEmits<{
  uploaded: [];
  error: [message: string];
}>();

const SUPPORTED_TYPES = ['video/mp4', 'video/webm'];
const MAX_BYTES = 500 * 1024 * 1024;

const toast = useToast();
const fileInput = ref<HTMLInputElement>();
const selectedFile = ref<File | null>(null);
const isDragging = ref(false);
const uploading = ref(false);
const progress = ref(0);

const dropZoneLabel = computed(() =>
  props.replaceExisting
    ? 'Remplacer la vidéo de présentation'
    : 'Ajouter une vidéo de présentation',
);

function triggerFileInput() {
  if (props.disabled || uploading.value) return;
  fileInput.value?.click();
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (file) validateAndSelect(file);
}

function handleDrop(event: DragEvent) {
  isDragging.value = false;
  if (props.disabled || uploading.value) return;
  const file = event.dataTransfer?.files[0];
  if (file) validateAndSelect(file);
}

function validateAndSelect(file: File) {
  if (!SUPPORTED_TYPES.includes(file.type)) {
    toast.add({
      title: 'Format non supporté',
      description: 'Seuls les formats MP4 et WebM sont acceptés.',
      color: 'error',
    });
    return;
  }
  if (file.size > MAX_BYTES) {
    toast.add({
      title: 'Fichier trop volumineux',
      description: `La vidéo ne doit pas dépasser 500 MB (taille actuelle : ${formatFileSize(file.size)}).`,
      color: 'error',
    });
    return;
  }
  selectedFile.value = file;
  progress.value = 0;
}

async function handleUpload() {
  if (!selectedFile.value) return;

  uploading.value = true;
  progress.value = 0;

  try {
    await uploadSupportVideo(
      props.eventSlug,
      props.partnershipId,
      { file: selectedFile.value },
      {
        onUploadProgress: (event) => {
          if (event.total) {
            progress.value = Math.round((event.loaded * 100) / event.total);
          }
        },
      },
    );

    toast.add({
      title: 'Vidéo envoyée',
      description: 'Votre vidéo est en attente de validation par les organisateurs.',
      color: 'success',
    });

    selectedFile.value = null;
    progress.value = 0;
    if (fileInput.value) fileInput.value.value = '';
    emit('uploaded');
  } catch (error: unknown) {
    const message = resolveErrorMessage(error);
    toast.add({ title: "Échec de l'envoi", description: message, color: 'error' });
    emit('error', message);
  } finally {
    uploading.value = false;
  }
}

function resolveErrorMessage(error: unknown): string {
  const err = error as { response?: { status?: number; data?: { message?: string } } };
  const status = err.response?.status;
  if (status === 413) {
    return 'Le fichier dépasse la limite de 500 MB autorisée par le serveur.';
  }
  if (status === 415) {
    return "Le format de la vidéo n'est pas supporté. Utilisez MP4 ou WebM.";
  }
  if (status === 409) {
    return 'Une vidéo a déjà été approuvée pour ce partenariat ; vous ne pouvez plus la modifier.';
  }
  return err.response?.data?.message || "Impossible d'envoyer la vidéo. Veuillez réessayer.";
}

function cancelUpload() {
  selectedFile.value = null;
  progress.value = 0;
  if (fileInput.value) fileInput.value.value = '';
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${Math.round((bytes / Math.pow(1024, i)) * 100) / 100} ${units[i]}`;
}
</script>
