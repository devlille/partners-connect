<template>
  <div>
    <label v-if="label" class="block text-sm font-medium text-gray-700 mb-2">
      {{ label }}
    </label>
    <USelectMenu
      :model-value="modelValue"
      :options="languages"
      value-attribute="value"
      option-attribute="label"
      :placeholder="placeholder"
      :disabled="disabled"
      class="w-full"
      @update:model-value="handleUpdate"
    >
      <template #label>
        {{ selectedLabel }}
      </template>
    </USelectMenu>
  </div>
</template>

<script setup lang="ts">
interface Props {
  modelValue: string;
  label?: string;
  placeholder?: string;
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  label: 'Langue',
  placeholder: 'Sélectionner une langue',
  disabled: false
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const languages = [
  { label: 'Français', value: 'fr' },
  { label: 'English', value: 'en' },
  { label: 'Español', value: 'es' },
  { label: 'Deutsch', value: 'de' },
  { label: 'Italiano', value: 'it' },
  { label: 'Português', value: 'pt' },
  { label: 'Nederlands', value: 'nl' }
];

const selectedLabel = computed(() => {
  const found = languages.find(lang => lang.value === props.modelValue);
  return found ? found.label : props.placeholder || 'Sélectionner une langue';
});

function handleUpdate(value: any) {
  if (typeof value === 'string') {
    emit('update:modelValue', value);
  }
}
</script>
