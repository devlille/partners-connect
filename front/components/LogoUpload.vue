<template>
  <div class="space-y-4">
    <div class="flex items-start justify-between">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Logo de l'entreprise
        </label>
        <p class="text-xs text-gray-500">
          Format: SVG, PNG ou JPEG. Taille maximale: 5MB.
        </p>
      </div>
    </div>

    <!-- Logo actuel -->
    <div v-if="currentLogo" class="relative inline-block">
      <div class="w-48 h-48 rounded-lg border-2 border-gray-200 bg-gray-50 flex items-center justify-center overflow-hidden">
        <img
          :src="currentLogo"
          :alt="`Logo de ${companyName}`"
          class="max-w-full max-h-full object-contain"
        />
      </div>
      <div class="mt-2 flex gap-2">
        <UButton
          size="sm"
          color="primary"
          variant="outline"
          icon="i-heroicons-arrow-path"
          @click="triggerFileInput"
          :disabled="uploading"
        >
          Modifier
        </UButton>
        <UButton
          size="sm"
          color="error"
          variant="outline"
          icon="i-heroicons-trash"
          @click="confirmDelete"
          :disabled="uploading"
        >
          Supprimer
        </UButton>
      </div>
    </div>

    <!-- Zone de drop si pas de logo -->
    <div v-else>
      <div
        @click="triggerFileInput"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="handleDrop"
        :class="[
          'w-full border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors',
          isDragging
            ? 'border-primary-500 bg-primary-50'
            : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'
        ]"
      >
        <div class="flex flex-col items-center">
          <i class="i-heroicons-cloud-arrow-up text-4xl text-gray-400 mb-3" />
          <p class="text-sm font-medium text-gray-700 mb-1">
            Cliquez pour uploader ou glissez-déposez
          </p>
          <p class="text-xs text-gray-500">
            SVG, PNG ou JPEG jusqu'à 5MB
          </p>
        </div>
      </div>
    </div>

    <!-- Prévisualisation avant upload -->
    <div v-if="preview && !currentLogo" class="space-y-3">
      <div class="relative inline-block">
        <div class="w-48 h-48 rounded-lg border-2 border-primary-200 bg-gray-50 flex items-center justify-center overflow-hidden">
          <img
            :src="preview"
            alt="Aperçu du logo"
            class="max-w-full max-h-full object-contain"
          />
        </div>
      </div>

      <div class="flex gap-2">
        <UButton
          color="primary"
          icon="i-heroicons-arrow-up-tray"
          @click="handleUpload"
          :loading="uploading"
          :disabled="uploading"
        >
          Uploader le logo
        </UButton>
        <UButton
          color="neutral"
          variant="outline"
          @click="cancelUpload"
          :disabled="uploading"
        >
          Annuler
        </UButton>
      </div>

      <p class="text-xs text-gray-500">
        Fichier: {{ selectedFile?.name }} ({{ formatFileSize(selectedFile?.size || 0) }})
      </p>
    </div>

    <!-- Input file caché -->
    <input
      ref="fileInput"
      type="file"
      accept="image/svg+xml,image/png,image/jpeg,image/jpg"
      class="hidden"
      @change="handleFileSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { MediaSchema } from '~/utils/api';
import { postCompaniesLogo } from '~/utils/api';

interface Props {
  companyId: string;
  companyName?: string;
  currentLogoMedia?: MediaSchema | null;
  disabled?: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  uploaded: [media: MediaSchema];
  deleted: [];
  error: [message: string];
}>();

const toast = useToast();
const fileInput = ref<HTMLInputElement>();
const selectedFile = ref<File | null>(null);
const preview = ref<string | null>(null);
const uploading = ref(false);
const isDragging = ref(false);

const currentLogo = computed(() => {
  if (!props.currentLogoMedia) return null;
  // Utiliser la version 500px pour l'affichage
  return props.currentLogoMedia.png_500 || props.currentLogoMedia.original;
});

function triggerFileInput() {
  if (props.disabled || uploading.value) return;
  fileInput.value?.click();
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (file) {
    validateAndPreviewFile(file);
  }
}

function handleDrop(event: DragEvent) {
  isDragging.value = false;
  const file = event.dataTransfer?.files[0];
  if (file) {
    validateAndPreviewFile(file);
  }
}

function validateAndPreviewFile(file: File) {
  // Vérifier le type
  const validTypes = ['image/svg+xml', 'image/png', 'image/jpeg', 'image/jpg'];
  if (!validTypes.includes(file.type)) {
    toast.add({
      title: 'Format non supporté',
      description: 'Veuillez sélectionner un fichier SVG, PNG ou JPEG',
      color: 'error'
    });
    return;
  }

  // Vérifier la taille (5MB max)
  const maxSize = 5 * 1024 * 1024;
  if (file.size > maxSize) {
    toast.add({
      title: 'Fichier trop volumineux',
      description: 'Le logo ne doit pas dépasser 5MB',
      color: 'error'
    });
    return;
  }

  selectedFile.value = file;

  // Créer un aperçu
  const reader = new FileReader();
  reader.onload = (e) => {
    preview.value = e.target?.result as string;
  };
  reader.readAsDataURL(file);
}

async function handleUpload() {
  if (!selectedFile.value) return;

  uploading.value = true;

  try {
    const media = await postCompaniesLogo(
      props.companyId,
      { file: selectedFile.value }
    );

    toast.add({
      title: 'Succès',
      description: 'Le logo a été uploadé avec succès',
      color: 'success'
    });

    // Réinitialiser
    selectedFile.value = null;
    preview.value = null;
    if (fileInput.value) {
      fileInput.value.value = '';
    }

    emit('uploaded', media);
  } catch (error) {
    console.error('Failed to upload logo:', error);

    const errorMessage = error instanceof Error
      ? error.message
      : 'Impossible d\'uploader le logo';

    toast.add({
      title: 'Erreur',
      description: errorMessage,
      color: 'error'
    });

    emit('error', errorMessage);
  } finally {
    uploading.value = false;
  }
}

function cancelUpload() {
  selectedFile.value = null;
  preview.value = null;
  if (fileInput.value) {
    fileInput.value.value = '';
  }
}

function confirmDelete() {
  // TODO: Implémenter la suppression du logo via l'API si disponible
  toast.add({
    title: 'Fonctionnalité non disponible',
    description: 'La suppression du logo n\'est pas encore implémentée',
    color: 'warning'
  });
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}
</script>
