<template>
  <InputWrapper :id="id" :label="label">
    <input
      :id="id"
      :type="type"
      :name="name"
      :autocomplete="autocomplete"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
  </InputWrapper>
</template>

<script setup lang="ts">
defineProps<{
  id: string;
  label: string;
  type?: string;
  name: string;
  autocomplete?: string;
  modelValue?: string;
}>();

defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>
<style lang="css" scoped>
input {
    width: 100% - 1em;
    padding: 2.5rem 0.5em 0.5em;
}
</style>
