<template>
  <section class="border-t border-gray-200 pt-6 mb-6">
    <h3 class="text-lg font-semibold text-gray-900 mb-1">
      {{ $t("flyer.templateSection.title") }}
    </h3>
    <p class="text-sm text-gray-600 mb-4">{{ $t("flyer.templateSection.help") }}</p>

    <div v-if="!previewUrl">
      <div
        :class="[
          'w-full border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors',
          isDragging
            ? 'border-primary-500 bg-primary-50'
            : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50',
        ]"
        role="button"
        tabindex="0"
        @click="triggerFileInput"
        @keydown.enter.prevent="triggerFileInput"
        @keydown.space.prevent="triggerFileInput"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="handleDrop"
      >
        <i class="i-heroicons-photo text-4xl text-gray-400 mb-3" aria-hidden="true" />
        <p class="text-sm font-medium text-gray-700">{{ $t("flyer.upload.dropPng") }}</p>
        <p class="text-xs text-gray-500 mt-1">PNG ≤ 10 MB</p>
      </div>
      <input
        ref="fileInput"
        type="file"
        accept="image/png"
        class="hidden"
        @change="handleFileSelect"
      />
    </div>

    <div v-else class="space-y-4">
      <FlyerZonePicker
        :template-url="previewUrl"
        :natural-width="naturalWidth"
        :natural-height="naturalHeight"
        :zone="zone"
        @update:zone="zone = $event"
      />

      <div class="flex gap-2">
        <UButton
          color="primary"
          icon="i-heroicons-check"
          :loading="saving"
          :disabled="!canSave"
          @click="handleSave"
        >
          {{ $t("flyer.save.button") }}
        </UButton>
        <UButton
          color="neutral"
          variant="outline"
          icon="i-heroicons-trash"
          :loading="clearing"
          @click="handleClear"
        >
          {{ $t("flyer.clear.button") }}
        </UButton>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import {
  putOrgsEventsPacksFlyerTemplate,
  deleteOrgsEventsPacksFlyerTemplate,
} from "~/utils/api";
import { type FlyerZone, zoneFitsInsideTemplate } from "~/composables/useFlyerZone";

interface Props {
  orgSlug: string;
  eventSlug: string;
  packId: string;
  initialTemplateUrl: string | null;
  initialZone: FlyerZone | null;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  saved: [payload: { templateUrl: string; zone: FlyerZone }];
  cleared: [];
}>();

const { t } = useI18n();
const toast = useToast();

const MAX_BYTES = 10 * 1024 * 1024;

const fileInput = ref<HTMLInputElement>();
const isDragging = ref(false);
const pendingFile = ref<File | null>(null);
const previewUrl = ref<string | null>(props.initialTemplateUrl);
const naturalWidth = ref<number>(0);
const naturalHeight = ref<number>(0);
const zone = ref<FlyerZone>(
  props.initialZone ?? { x: 0, y: 0, width: 0, height: 0 },
);
const saving = ref(false);
const clearing = ref(false);

if (props.initialTemplateUrl) {
  void loadNaturalSizeFromUrl(props.initialTemplateUrl);
}

const canSave = computed(() => {
  if (!pendingFile.value && !props.initialTemplateUrl) return false;
  if (!naturalWidth.value || !naturalHeight.value) return false;
  return zoneFitsInsideTemplate(zone.value, {
    naturalWidth: naturalWidth.value,
    naturalHeight: naturalHeight.value,
  });
});

function triggerFileInput() {
  fileInput.value?.click();
}

function handleFileSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (file) acceptFile(file);
}

function handleDrop(event: DragEvent) {
  isDragging.value = false;
  const file = event.dataTransfer?.files[0];
  if (file) acceptFile(file);
}

function acceptFile(file: File) {
  if (file.type !== "image/png") {
    toast.add({
      title: t("flyer.upload.errors.notPng"),
      color: "error",
    });
    return;
  }
  if (file.size > MAX_BYTES) {
    toast.add({
      title: t("flyer.upload.errors.tooLarge"),
      color: "error",
    });
    return;
  }
  pendingFile.value = file;
  previewUrl.value = URL.createObjectURL(file);
  void loadNaturalSizeFromUrl(previewUrl.value);
}

function loadNaturalSizeFromUrl(url: string): Promise<void> {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => {
      naturalWidth.value = img.naturalWidth;
      naturalHeight.value = img.naturalHeight;
      if (zone.value.width === 0 || zone.value.height === 0) {
        zone.value = {
          x: 0,
          y: 0,
          width: img.naturalWidth,
          height: img.naturalHeight,
        };
      }
      resolve();
    };
    img.src = url;
  });
}

async function handleSave() {
  if (!canSave.value || !pendingFile.value) return;
  saving.value = true;
  try {
    const response = await putOrgsEventsPacksFlyerTemplate(
      props.orgSlug,
      props.eventSlug,
      props.packId,
      {
        file: pendingFile.value,
        zone: JSON.stringify(zone.value),
      },
    );
    const templateUrl = response.data?.template_url ?? previewUrl.value!;
    toast.add({ title: t("flyer.save.success"), color: "success" });
    pendingFile.value = null;
    emit("saved", { templateUrl, zone: zone.value });
  } catch (error: unknown) {
    const message = resolveErrorMessage(error);
    toast.add({ title: t("flyer.save.failure"), description: message, color: "error" });
  } finally {
    saving.value = false;
  }
}

async function handleClear() {
  clearing.value = true;
  try {
    await deleteOrgsEventsPacksFlyerTemplate(props.orgSlug, props.eventSlug, props.packId);
    toast.add({ title: t("flyer.clear.success"), color: "success" });
    pendingFile.value = null;
    previewUrl.value = null;
    naturalWidth.value = 0;
    naturalHeight.value = 0;
    zone.value = { x: 0, y: 0, width: 0, height: 0 };
    emit("cleared");
  } catch (error: unknown) {
    const message = resolveErrorMessage(error);
    toast.add({ title: t("flyer.clear.failure"), description: message, color: "error" });
  } finally {
    clearing.value = false;
  }
}

function resolveErrorMessage(error: unknown): string {
  const err = error as { response?: { data?: { message?: string } } };
  return err.response?.data?.message ?? t("flyer.errors.generic");
}
</script>
