<template>
  <div>
    <label v-if="label" class="block text-sm font-medium text-gray-700 mb-2">
      {{ label }}
    </label>
    <USelectMenu
      :model-value="selectedLanguage"
      :items="languages"
      :placeholder="placeholder"
      :disabled="disabled"
      class="w-full"
      @update:model-value="handleUpdate"
    />
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

const selectedLanguage = computed(() => {
  return languages.find(lang => lang.value === props.modelValue);
});

function handleUpdate(selected: any) {
  if (selected && selected.value) {
    emit('update:modelValue', selected.value);
  }
}
</script>
