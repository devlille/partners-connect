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
  width: calc(100% - 1em);
  padding: 2.5rem 0.5em 0.5em;
}

:deep(select) {
  width: calc(100% + 0.25rem);
}
</style>
