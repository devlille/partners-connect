<template>
  <TextInput
    :id="id"
    :label="label"
    type="tel"
    :name="name"
    :autocomplete="autocomplete"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
  />
</template>

<script setup lang="ts">
defineProps<{
  id: string;
  label: string;
  name: string;
  autocomplete?: string;
  modelValue?: string;
}>();

defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>
