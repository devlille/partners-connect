<template>
  <InputWrapper :id="id" :label="label">
    <select
      :id="id"
      :name="name"
      :value="modelValue"
      @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
    >
      <slot />
    </select>
  </InputWrapper>
</template>

<script setup lang="ts">
defineProps<{
  id: string;
  label: string;
  name: string;
  modelValue?: string | number;
}>();

defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>

<style lang="css" scoped>
select {
    width: 100%;
    padding: 2.5rem 0.5em 0.5em;
  }
</style>