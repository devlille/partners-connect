<template>
  <fieldset>
    <legend>{{ legend }}</legend>
    <div v-for="option in options" :key="option.value">
      <label>
        <input
          :id="`${name}-${option.value}`"
          type="radio"
          :name="name"
          :value="option.value"
          :checked="modelValue === option.value"
          @change="$emit('update:modelValue', option.value)"
        />
        {{ option.label }} - {{ option.price }} €
      </label>
    </div>
  </fieldset>
</template>

<script setup lang="ts">
export interface RadioOption {
  value: string;
  label: string;
  price: number;
}

defineProps<{
  legend: string;
  name: string;
  modelValue?: string;
  options: RadioOption[];
}>();

defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>

<style lang="css" scoped>
fieldset {
  border: 1px solid white;
  padding: 1.5rem;
  margin: 2rem 0 2rem 0;
  box-sizing: border-box;
}

label, legend {
  color: white;
}
</style>
