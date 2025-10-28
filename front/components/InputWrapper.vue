<template>
  <p>
    <label :for="id">{{ label }}</label>
    <slot />
  </p>
</template>

<script setup lang="ts">
defineProps<{
  id: string;
  label: string;
}>();
</script>

<style lang="css" scoped>
p {
  position: relative;
}

label {
  position: absolute;
  top: 0;
  left: 0.25rem;
  width: 100%;
  font-size: 0.9em;
  font-variation-settings: var(--fvs-light);
  text-align: left;
  padding: 0.5rem;
}

:deep(input),
:deep(select) {
  width: 100%;
  padding: 2.5rem 0.5em 0.5em;
}
</style>
