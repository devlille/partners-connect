<template>
  <div class="space-y-3">
    <div
      ref="container"
      class="relative inline-block max-w-full select-none"
      @mousedown="onMouseDown"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
      @mouseleave="onMouseUp"
    >
      <img
        ref="image"
        :src="templateUrl"
        class="block max-w-full h-auto"
        @load="onImageLoad"
        alt="Flyer template preview"
      />
      <div
        v-if="overlay"
        class="absolute border-2 border-primary-500 bg-primary-500/20 pointer-events-none"
        :style="{
          left: `${overlay.x}px`,
          top: `${overlay.y}px`,
          width: `${overlay.width}px`,
          height: `${overlay.height}px`,
        }"
      />
    </div>

    <div class="grid grid-cols-4 gap-2">
      <label class="text-xs font-medium text-gray-700">
        X
        <UInput v-model.number="local.x" type="number" :min="0" @change="emitZone" />
      </label>
      <label class="text-xs font-medium text-gray-700">
        Y
        <UInput v-model.number="local.y" type="number" :min="0" @change="emitZone" />
      </label>
      <label class="text-xs font-medium text-gray-700">
        {{ $t("flyer.zone.widthLabel") }}
        <UInput v-model.number="local.width" type="number" :min="1" @change="emitZone" />
      </label>
      <label class="text-xs font-medium text-gray-700">
        {{ $t("flyer.zone.heightLabel") }}
        <UInput v-model.number="local.height" type="number" :min="1" @change="emitZone" />
      </label>
    </div>

    <p v-if="isOutOfBounds" class="text-sm text-red-600" role="alert">
      {{ $t("flyer.zone.errors.outOfBounds") }}
    </p>
  </div>
</template>

<script setup lang="ts">
import {
  type FlyerZone,
  toTemplatePixels,
  fromTemplatePixels,
  zoneFitsInsideTemplate,
} from "~/composables/useFlyerZone";

interface Props {
  templateUrl: string;
  naturalWidth: number;
  naturalHeight: number;
  zone: FlyerZone;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  "update:zone": [zone: FlyerZone];
}>();

const container = ref<HTMLDivElement>();
const image = ref<HTMLImageElement>();
const displayWidth = ref(props.naturalWidth);
const displayHeight = ref(props.naturalHeight);

const local = ref<FlyerZone>({ ...props.zone });

watch(
  () => props.zone,
  (next) => {
    local.value = { ...next };
  },
);

const overlay = computed(() => {
  if (!displayWidth.value || !displayHeight.value) return null;
  return fromTemplatePixels(local.value, {
    displayWidth: displayWidth.value,
    displayHeight: displayHeight.value,
    naturalWidth: props.naturalWidth,
    naturalHeight: props.naturalHeight,
  });
});

const isOutOfBounds = computed(
  () =>
    !zoneFitsInsideTemplate(local.value, {
      naturalWidth: props.naturalWidth,
      naturalHeight: props.naturalHeight,
    }),
);

function onImageLoad() {
  if (!image.value) return;
  displayWidth.value = image.value.clientWidth;
  displayHeight.value = image.value.clientHeight;
}

let dragStart: { x: number; y: number } | null = null;

function onMouseDown(event: MouseEvent) {
  if (!container.value) return;
  const rect = container.value.getBoundingClientRect();
  dragStart = { x: event.clientX - rect.left, y: event.clientY - rect.top };
}

function onMouseMove(event: MouseEvent) {
  if (!dragStart || !container.value) return;
  const rect = container.value.getBoundingClientRect();
  const currentX = event.clientX - rect.left;
  const currentY = event.clientY - rect.top;
  const displayZone = {
    x: Math.min(dragStart.x, currentX),
    y: Math.min(dragStart.y, currentY),
    width: Math.abs(currentX - dragStart.x),
    height: Math.abs(currentY - dragStart.y),
  };
  local.value = toTemplatePixels(displayZone, {
    displayWidth: displayWidth.value,
    displayHeight: displayHeight.value,
    naturalWidth: props.naturalWidth,
    naturalHeight: props.naturalHeight,
  });
}

function onMouseUp() {
  if (dragStart) {
    dragStart = null;
    emitZone();
  }
}

function emitZone() {
  emit("update:zone", { ...local.value });
}
</script>
