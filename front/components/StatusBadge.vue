<template>
  <span :class="['inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium', classes]">
    <i :class="icon" aria-hidden="true" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
const props = defineProps<{ status: string }>();

const config: Record<string, { label: string; icon: string; classes: string }> = {
  pending: {
    label: 'En attente',
    icon: 'i-heroicons-clock',
    classes: 'bg-yellow-100 text-yellow-800',
  },
  approved: {
    label: 'Approuvée',
    icon: 'i-heroicons-check-circle',
    classes: 'bg-green-100 text-green-800',
  },
  declined: {
    label: 'Refusée',
    icon: 'i-heroicons-x-circle',
    classes: 'bg-red-100 text-red-800',
  },
};

const current = computed(() => config[props.status] ?? { label: props.status, icon: '', classes: 'bg-gray-100 text-gray-800' });
const label = computed(() => current.value.label);
const icon = computed(() => current.value.icon);
const classes = computed(() => current.value.classes);
</script>
