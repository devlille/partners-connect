<template>
  <UButton
    :to="to"
    icon="i-heroicons-arrow-left"
    color="neutral"
    variant="ghost"
    :label="label"
    class="mb-2"
  />
</template>

<script setup lang="ts">
defineProps<{
  to: string
  label?: string
}>()
</script>
