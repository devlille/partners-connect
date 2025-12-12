<template>
  <fieldset v-if="options.length > 0">
    <legend>{{ legend }}</legend>
    <div v-for="option in options" :key="option.id">
      <label>
        <input
          :id="`option-${option.id}`"
          type="checkbox"
          :name="`option-${option.id}`"
          :value="option.id"
          :checked="modelValue.includes(option.id)"
          @change="handleChange(option.id, ($event.target as HTMLInputElement).checked)"
        />
        {{ getOptionName(option) }} - {{ option.price || 0 }} €
      </label>
    </div>
  </fieldset>
</template>

<script setup lang="ts">
import type { SponsoringOption } from "~/utils/api";

const props = defineProps<{
  legend: string;
  options: SponsoringOption[];
  modelValue: string[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void;
}>();

const { getOptionName } = useOptionTranslation();

function handleChange(optionId: string, checked: boolean) {
  const newValue = checked
    ? [...props.modelValue, optionId]
    : props.modelValue.filter(id => id !== optionId);
  emit('update:modelValue', newValue);
}
</script>

<style lang="css" scoped>
fieldset {
  border: 1px solid white;
  padding: 1.5rem;
  margin: 0;
  box-sizing: border-box;
}
label, legend {
  color: white;
}
</style>
